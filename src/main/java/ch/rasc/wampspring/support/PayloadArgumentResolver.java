/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.support;

import java.lang.annotation.Annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.handler.MethodParameterConverter;

/**
 * A resolver to extract and convert the payload of a message. It also validates the
 * payload using a {@link Validator} if the argument is annotated with a Validation
 * annotation.
 *
 * <p>
 * This {@link HandlerMethodArgumentResolver} should be ordered last as it supports all
 * types and does not require the {@link Payload} annotation.
 * <p>
 * This resolver only supports {@link WampPublishListener} annotated methods.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Ralph Schaer
 */
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String MVC_VALIDATOR_NAME = "mvcValidator";

	private final Validator validator;

	private final MethodParameterConverter methodParameterConverter;

	public PayloadArgumentResolver(ApplicationContext applicationContext,
			MethodParameterConverter methodParameterConverter) {
		this.methodParameterConverter = methodParameterConverter;

		if (applicationContext.containsBean(MVC_VALIDATOR_NAME)) {
			this.validator = applicationContext.getBean(MVC_VALIDATOR_NAME,
					Validator.class);
		}
		else if (ClassUtils.isPresent("javax.validation.Validator", getClass()
				.getClassLoader())) {
			Class<?> clazz;
			try {
				String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
				clazz = ClassUtils.forName(className,
						AbstractMessageBrokerConfiguration.class.getClassLoader());
			}
			catch (Throwable ex) {
				throw new BeanInitializationException(
						"Could not find default validator class", ex);
			}
			this.validator = (Validator) BeanUtils.instantiate(clazz);
		}
		else {
			this.validator = new Validator() {
				@Override
				public boolean supports(Class<?> clazz) {
					return false;
				}

				@Override
				public void validate(Object target, Errors errors) {
					// nothing here
				}
			};
		}
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getMethod().getAnnotation(WampPublishListener.class) != null;
	}

	@Override
	public Object resolveArgument(MethodParameter param, Message<?> message)
			throws Exception {
		Payload ann = param.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			throw new IllegalStateException(
					"@Payload SpEL expressions not supported by this resolver");
		}

		Object payload = message.getPayload();
		if (isEmptyPayload(payload)) {
			if (ann == null || ann.required()) {
				String paramName = getParameterName(param);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload,
						paramName);
				bindingResult.addError(new ObjectError(paramName,
						"@Payload param is required"));
				throw new MethodArgumentNotValidException(message, param, bindingResult);
			}
			return null;
		}

		Class<?> targetClass = param.getParameterType();
		if (ClassUtils.isAssignable(targetClass, payload.getClass())) {
			validate(message, param, payload);
			return payload;
		}

		payload = this.methodParameterConverter.convert(param, message.getPayload());
		if (payload == null) {
			throw new MessageConversionException(message,
					"No converter found to convert to " + targetClass + ", message="
							+ message);
		}

		validate(message, param, payload);
		return payload;
	}

	/**
	 * Specify if the given {@code payload} is empty.
	 * @param payload the payload to check (can be {@code null})
	 */
	protected boolean isEmptyPayload(Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[]) {
			return ((byte[]) payload).length == 0;
		}
		else if (payload instanceof String) {
			return !StringUtils.hasText((String) payload);
		}
		else {
			return false;
		}
	}

	private static String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return paramName != null ? paramName : "Arg " + param.getParameterIndex();
	}

	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null
					|| ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = validatedAnn != null ? validatedAnn.value()
						: AnnotationUtils.getValue(ann);
				Object[] validationHints = hints instanceof Object[] ? (Object[]) hints
						: new Object[] { hints };
				BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
						target, getParameterName(parameter));
				if (!ObjectUtils.isEmpty(validationHints)
						&& this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult,
							validationHints);
				}
				else {
					this.validator.validate(target, bindingResult);
				}
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter,
							bindingResult);
				}
				break;
			}
		}
	}

}
