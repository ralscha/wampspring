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
package ch.rasc.wampspring.method;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.message.PublishMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test fixture for {@link PayloadArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Ralph Schaer
 */
public class PayloadArgumentResolverTest {

	private PayloadArgumentResolver resolver;

	private Method payloadMethod;

	private MethodParameter paramAnnotated;

	private MethodParameter paramAnnotatedNotRequired;

	private MethodParameter paramAnnotatedRequired;

	private MethodParameter paramWithSpelExpression;

	private MethodParameter paramNotAnnotated;

	private MethodParameter paramValidatedNotAnnotated;

	private MethodParameter paramValidated;

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@SuppressWarnings("resource")
	@Before
	public void setup() throws Exception {
		DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
		dlbf.registerSingleton("mvcValidator", testValidator());
		GenericApplicationContext ctx = new GenericApplicationContext(dlbf);
		ctx.refresh();
		this.resolver = new PayloadArgumentResolver(ctx, new MethodParameterConverter(
				new ObjectMapper(), new GenericConversionService()));
		this.payloadMethod = getClass().getDeclaredMethod("handleMessage", String.class,
				String.class, String.class, String.class, String.class, String.class,
				String.class, Integer.class);

		this.paramAnnotated = getMethodParameter(this.payloadMethod, 0);
		this.paramAnnotatedNotRequired = getMethodParameter(this.payloadMethod, 1);
		this.paramAnnotatedRequired = getMethodParameter(this.payloadMethod, 2);
		this.paramWithSpelExpression = getMethodParameter(this.payloadMethod, 3);
		this.paramValidated = getMethodParameter(this.payloadMethod, 4);
		this.paramValidated
				.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.paramValidatedNotAnnotated = getMethodParameter(this.payloadMethod, 5);
		this.paramNotAnnotated = getMethodParameter(this.payloadMethod, 6);
	}

	@Test
	public void supportsParameterTest() throws NoSuchMethodException, SecurityException {
		assertThat(this.resolver.supportsParameter(this.paramAnnotated)).isTrue();

		Method nonPublishMethod = getClass().getDeclaredMethod("nonPublishMethod",
				String.class);
		assertThat(
				this.resolver.supportsParameter(getMethodParameter(nonPublishMethod, 0)))
				.isFalse();
	}

	@Test
	public void resolveRequired() throws Exception {
		PublishMessage message = new PublishMessage("pub", "ABC");
		assertThat(this.resolver.resolveArgument(this.paramAnnotated, message))
				.isEqualTo("ABC");
		assertThat(this.resolver.resolveArgument(this.paramAnnotatedRequired, message))
				.isEqualTo("ABC");
	}

	@Test(expected = MethodArgumentNotValidException.class)
	public void resolveRequiredEmpty() throws Exception {
		PublishMessage message = new PublishMessage("pub", "");
		this.resolver.resolveArgument(this.paramAnnotated, message);
	}

	@Test(expected = MethodArgumentNotValidException.class)
	public void resolveRequiredEmptyAnnotated() throws Exception {
		PublishMessage message = new PublishMessage("pub", "");
		this.resolver.resolveArgument(this.paramAnnotatedRequired, message);
	}

	@Test(expected = MethodArgumentNotValidException.class)
	public void resolveRequiredEmptyNonAnnotatedParameter() throws Exception {
		PublishMessage message = new PublishMessage("pub", "");
		this.resolver.resolveArgument(this.paramNotAnnotated, message);
	}

	@Test
	public void resolveNotRequired() throws Exception {
		PublishMessage emptyByteArrayMessage = new PublishMessage("pub", new byte[0]);
		assertNull(this.resolver.resolveArgument(this.paramAnnotatedNotRequired,
				emptyByteArrayMessage));

		PublishMessage emptyStringMessage = new PublishMessage("pub", "");
		assertNull(this.resolver.resolveArgument(this.paramAnnotatedNotRequired,
				emptyStringMessage));

		PublishMessage notEmptyMessage = new PublishMessage("pub", "ABC");
		assertEquals("ABC", this.resolver.resolveArgument(this.paramAnnotatedNotRequired,
				notEmptyMessage));
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveNonConvertibleParam() throws Exception {
		PublishMessage notEmptyMessage = new PublishMessage("pub", "xy");
		this.resolver.resolveArgument(getMethodParameter(this.payloadMethod, 7),
				notEmptyMessage);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveSpelExpressionNotSupported() throws Exception {
		PublishMessage message = new PublishMessage("pub", "ABC");
		this.resolver.resolveArgument(this.paramWithSpelExpression, message);
	}

	@Test
	public void resolveValidation() throws Exception {
		PublishMessage message = new PublishMessage("pub", "ABC");
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	@Test(expected = MethodArgumentNotValidException.class)
	public void resolveFailValidation() throws Exception {
		// See testValidator()
		PublishMessage message = new PublishMessage("pub", "invalidValue");
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	@Test(expected = MethodArgumentNotValidException.class)
	public void resolveFailValidationNoConversionNecessary() throws Exception {
		PublishMessage message = new PublishMessage("pub", "invalidValue");
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	@Test
	public void resolveNonAnnotatedParameter() throws Exception {
		PublishMessage notEmptyMessage = new PublishMessage("pub", "ABC");
		assertEquals("ABC",
				this.resolver.resolveArgument(this.paramNotAnnotated, notEmptyMessage));

		Message<?> emptyStringMessage = MessageBuilder.withPayload("").build();
		this.thrown.expect(MethodArgumentNotValidException.class);
		this.resolver.resolveArgument(this.paramValidated, emptyStringMessage);
	}

	@Test
	public void resolveNonAnnotatedParameterFailValidation() throws Exception {
		// See testValidator()
		PublishMessage message = new PublishMessage("pub", "invalidValue");

		this.thrown.expect(MethodArgumentNotValidException.class);
		this.thrown.expectMessage("invalid value");
		assertEquals("invalidValue",
				this.resolver.resolveArgument(this.paramValidatedNotAnnotated, message));
	}

	private static Validator testValidator() {
		return new Validator() {
			@Override
			public boolean supports(Class<?> clazz) {
				return String.class.isAssignableFrom(clazz);
			}

			@Override
			public void validate(Object target, Errors errors) {
				String value = (String) target;
				if ("invalidValue".equals(value)) {
					errors.reject("invalid value");
				}
			}
		};
	}

	private static MethodParameter getMethodParameter(Method method, int index) {
		Assert.notNull(method, "Method must be set");
		return new MethodParameter(method, index);
	}

	@SuppressWarnings({ "unused", "hiding" })
	@WampPublishListener
	private void handleMessage(@Payload String param,
			@Payload(required = false) String paramNotRequired,
			@Payload(required = true) String requiredParam,
			@Payload("foo.bar") String paramWithSpelExpression,
			@MyValid @Payload String validParam,
			@Validated String validParamNotAnnotated, String paramNotAnnotated,
			Integer numberParameter) {
		// nothing here
	}

	@SuppressWarnings("unused")
	private void nonPublishMethod(String param) {
		// nothing here
	}

	@Validated
	@Target({ ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyValid {
		// nothing here
	}

}
