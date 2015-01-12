/**
 * Copyright 2002-2015 the original author or authors.
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
package ch.rasc.wampspring.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.util.ReflectionUtils;

import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Invokes the handler method for a given message after resolving its method argument
 * values through registered {@link HandlerMethodArgumentResolver}s.
 *
 * <p>
 * Use {@link #setMessageMethodArgumentResolvers(HandlerMethodArgumentResolver)} to
 * customize the list of argument resolvers.
 * <p>
 * Credit goes to the Spring class
 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod} . This
 * class is just a copy where {@link org.springframework.messaging.Message} is replaced
 * with {@link WampMessage}
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Ralph Schaer
 */
public class InvocableWampHandlerMethod extends HandlerMethod {

	private HandlerMethodArgumentResolver argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private final ConversionService conversionService;

	private final ObjectMapper objectMapper;

	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public InvocableWampHandlerMethod(HandlerMethod handlerMethod,
			ObjectMapper objectMapper, ConversionService conversionService) {
		super(handlerMethod);
		this.objectMapper = objectMapper;
		this.conversionService = conversionService;
	}

	public void setMessageMethodArgumentResolvers(
			HandlerMethodArgumentResolver argumentResolvers) {
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
	 * @throws Exception raised if no suitable argument resolver can be found, or the
	 * method raised an exception
	 */
	public Object invoke(WampMessage message, Object... providedArgs) throws Exception {
		Object[] args = getMethodArgumentValues(message, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved arguments: " + Arrays.asList(args));
		}
		Object returnValue = doInvoke(args);
		if (logger.isTraceEnabled()) {
			logger.trace("Returned value: " + returnValue);
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
				String error = getArgumentResolutionErrorMessage(
						"No suitable resolver for argument", i);
				throw new IllegalStateException(error);
			}
		}
		return args;
	}

	private String getArgumentResolutionErrorMessage(String message, int index) {
		MethodParameter param = getMethodParameters()[index];
		message += " [" + index + "] [type=" + param.getParameterType().getName() + "]";
		return getDetailedErrorMessage(message);
	}

	/**
	 * Adds HandlerMethod details such as the controller type and method signature to the
	 * given error message.
	 * @param message error message to append the HandlerMethod details to
	 */
	protected String getDetailedErrorMessage(String message) {
		StringBuilder sb = new StringBuilder(message).append("\n");
		sb.append("HandlerMethod details: \n");
		sb.append("Bean [").append(getBeanType().getName()).append("]\n");
		sb.append("Method [").append(getBridgedMethod().toGenericString()).append("]\n");
		return sb.toString();
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
		if (List.class.isAssignableFrom(convertedValue.getClass()) && td.isCollection()
				&& td.getElementTypeDescriptor() != null) {
			Class<?> elementType = td.getElementTypeDescriptor().getType();

			Collection<Object> convertedList = new ArrayList<>();
			for (Object record : (List<Object>) convertedValue) {
				Object convertedObject = objectMapper.convertValue(record, elementType);
				convertedList.add(convertedObject);
			}
			return convertedList;

		}
		return convertedValue;
	}

	/**
	 * Invoke the handler method with the given argument values.
	 */
	protected Object doInvoke(Object... args) throws Exception {
		ReflectionUtils.makeAccessible(getBridgedMethod());
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(getBridgedMethod(), getBean(), args);
			throw new IllegalStateException(getInvocationErrorMessage(ex.getMessage(),
					args), ex);
		}
		catch (InvocationTargetException ex) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = ex.getTargetException();
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

	/**
	 * Assert that the target bean class is an instance of the class where the given
	 * method is declared. In some cases the actual controller instance at request-
	 * processing time may be a JDK dynamic proxy (lazy initialization, prototype beans,
	 * and others). {@code @Controller}'s that require proxying should prefer class-based
	 * proxy mechanisms.
	 */
	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The mapped controller method class '"
					+ methodDeclaringClass.getName()
					+ "' is not an instance of the actual controller bean instance '"
					+ targetBeanClass.getName()
					+ "'. If the controller requires proxying "
					+ "(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(msg, args));
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
