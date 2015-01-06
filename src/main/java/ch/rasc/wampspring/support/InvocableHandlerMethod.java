/**
 * Copyright 2014-2015 Ralph Schaer <ralphschaer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.support;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;

import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Invokes the handler method for a given message after resolving its method argument
 * values through registered {@link HandlerMethodArgumentResolver}s.
 * <p>
 * Use {@link #setMessageMethodArgumentResolvers(HandlerMethodArgumentResolverComposite)}
 * to customize the list of argument resolvers.
 * <p>
 * Credit goes to the Spring class
 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod} . This
 * class is just a copy where {@link org.springframework.messaging.Message} is replaced
 * with {@link WampMessage}
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private final ConversionService conversionService;

	private final ObjectMapper objectMapper;

	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod, ObjectMapper objectMapper,
			ConversionService conversionService) {
		super(handlerMethod);
		this.objectMapper = objectMapper;
		this.conversionService = conversionService;
	}

	/**
	 * Set {@link HandlerMethodArgumentResolver}s to use to use for resolving method
	 * argument values.
	 */
	public void setMessageMethodArgumentResolvers(
			HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed (e.g.
	 * default request attribute name).
	 * <p>
	 * Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Invoke the method with the given message.
	 *
	 * @throws Exception raised if no suitable argument resolver can be found, or the
	 * method raised an exception
	 */
	public final Object invoke(WampMessage message, Object... providedArgs)
			throws Exception {
		Object[] args = getMethodArgumentValues(message, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking [" + this.getBeanType().getSimpleName() + "."
					+ getMethod().getName() + "] method with arguments "
					+ Arrays.asList(args));
		}
		Object returnValue = invoke(args);
		if (logger.isTraceEnabled()) {
			logger.trace("Method [" + getMethod().getName() + "] returned ["
					+ returnValue + "]");
		}
		return returnValue;
	}

	/**
	 * Get the method argument values for the current request.
	 */
	private Object[] getMethodArgumentValues(WampMessage message, Object... providedArgs)
			throws Exception {

		MethodParameter[] parameters = getMethodParameters();
		Object[] args = new Object[parameters.length];
		int argIndex = 0;
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());

			if (this.argumentResolvers.supportsParameter(parameter)) {
				try {
					args[i] = this.argumentResolvers.resolveArgument(parameter, message);
					continue;
				}
				catch (Exception ex) {
					if (logger.isTraceEnabled()) {
						logger.trace(
								getArgumentResolutionErrorMessage(
										"Error resolving argument", i), ex);
					}
					throw ex;
				}
			}

			if (providedArgs != null) {
				args[i] = resolveProvidedArgument(parameter, providedArgs[argIndex]);
				if (args[i] != null) {
					argIndex++;
					continue;
				}
			}

			if (args[i] == null) {
				String msg = getArgumentResolutionErrorMessage(
						"No suitable resolver for argument", i);
				throw new IllegalStateException(msg);
			}
		}
		return args;
	}

	private String getArgumentResolutionErrorMessage(String message, int index) {
		MethodParameter param = getMethodParameters()[index];
		return getDetailedErrorMessage(message + " [" + index + "] [type="
				+ param.getParameterType().getName() + "]");
	}

	/**
	 * Adds HandlerMethod details such as the controller type and method signature to the
	 * given error message.
	 *
	 * @param message error message to append the HandlerMethod details to
	 */
	String getDetailedErrorMessage(String message) {
		return message + "\n" + "HandlerMethod details: \n" + "Bean ["
				+ getBeanType().getName() + "]\n" + "Method ["
				+ getBridgedMethod().toGenericString() + "]\n";
	}

	/**
	 * Attempt to resolve a method parameter from the list of provided argument values.
	 */
	private Object resolveProvidedArgument(MethodParameter parameter, Object argument) {
		if (argument == null) {
			return null;
		}

		Class<?> sourceClass = argument.getClass();
		Class<?> targetClass = parameter.getParameterType();

		TypeDescriptor td = new TypeDescriptor(parameter);

		if (targetClass.isAssignableFrom(sourceClass)) {
			return convertListElements(td, argument);
		}

		if (conversionService.canConvert(sourceClass, targetClass)) {
			try {
				return convertListElements(td,
						conversionService.convert(argument, targetClass));
			}
			catch (Exception e) {

				TypeFactory typeFactory = objectMapper.getTypeFactory();
				if (td.isCollection()) {
					JavaType type = CollectionType.construct(td.getType(), typeFactory
							.constructType(td.getElementTypeDescriptor().getType()));
					return objectMapper.convertValue(argument, type);
				}
				else if (td.isArray()) {
					JavaType type = typeFactory.constructArrayType(td
							.getElementTypeDescriptor().getType());
					return objectMapper.convertValue(argument, type);
				}

				throw e;
			}
		}
		return objectMapper.convertValue(argument, targetClass);
	}

	@SuppressWarnings("unchecked")
	private Object convertListElements(TypeDescriptor td, Object convertedValue) {
		if (List.class.isAssignableFrom(convertedValue.getClass())) {
			if (td.isCollection() && td.getElementTypeDescriptor() != null) {
				Class<?> elementType = td.getElementTypeDescriptor().getType();

				Collection<Object> convertedList = new ArrayList<>();
				for (Object record : (List<Object>) convertedValue) {
					Object convertedObject = objectMapper.convertValue(record,
							elementType);
					convertedList.add(convertedObject);
				}
				return convertedList;
			}
		}
		return convertedValue;
	}

	/**
	 * Invoke the handler method with the given argument values.
	 */
	Object invoke(Object... args) throws Exception {
		ReflectionUtils.makeAccessible(this.getBridgedMethod());
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException e) {
			String msg = getInvocationErrorMessage(e.getMessage(), args);
			throw new IllegalArgumentException(msg, e);
		}
		catch (InvocationTargetException e) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = e.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			}
			else if (targetException instanceof Error) {
				throw (Error) targetException;
			}
			else if (targetException instanceof Exception) {
				throw (Exception) targetException;
			}
			else {
				String msg = getInvocationErrorMessage(
						"Failed to invoke controller method", args);
				throw new IllegalStateException(msg, targetException);
			}
		}
	}

	private String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
			sb.append("[").append(i).append("] ");
			if (resolvedArgs[i] == null) {
				sb.append("[null] \n");
			}
			else {
				sb.append("[type=").append(resolvedArgs[i].getClass().getName())
						.append("] ");
				sb.append("[value=").append(resolvedArgs[i]).append("]\n");
			}
		}
		return sb.toString();
	}

}
