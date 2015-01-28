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
package ch.rasc.wampspring.method;

import static org.fest.assertions.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.MessageHandlingException;

import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.testsupport.TestPrincipal;

public class PrincipalMethodArgumentResolverTest {

	private PrincipalMethodArgumentResolver resolver;
	private MethodParameter principalParameter;
	private MethodParameter stringParameter;

	@Before
	public void setup() throws Exception {
		Method testMethod = getClass().getDeclaredMethod("handleMessage",
				Principal.class, String.class);
		this.resolver = new PrincipalMethodArgumentResolver();
		this.principalParameter = new MethodParameter(testMethod, 0);
		this.stringParameter = new MethodParameter(testMethod, 1);
	}

	@Test
	public void supportsParameterTest() {
		assertThat(this.resolver.supportsParameter(this.principalParameter)).isTrue();
		assertThat(this.resolver.supportsParameter(this.stringParameter)).isFalse();
	}

	@Test
	public void resolveArgumentTest() throws Exception {
		CallMessage callMessage = new CallMessage("1", "call");
		TestPrincipal testPrincipal = new TestPrincipal("testPrincipal");
		callMessage.setHeader(WampMessageHeader.PRINCIPAL, testPrincipal);

		assertThat(this.resolver.resolveArgument(this.principalParameter, callMessage))
				.isEqualTo(testPrincipal);
	}

	@Test(expected = MessageHandlingException.class)
	public void missingPrincipalTest() throws Exception {
		CallMessage callMessage = new CallMessage("1", "call");
		TestPrincipal testPrincipal = new TestPrincipal("testPrincipal");
		assertThat(this.resolver.resolveArgument(this.principalParameter, callMessage))
				.isEqualTo(testPrincipal);
	}

	@SuppressWarnings({ "unused" })
	@WampPublishListener
	private void handleMessage(Principal myPrincipal, String anotherParam) {
		// nothing here
	}
}
