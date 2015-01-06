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
package ch.rasc.wampspring.cra;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.config.WampConfigurerAdapter;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.support.AbstractWebSocketIntegrationTests;

public class MethodEnableTest extends AbstractWebSocketIntegrationTests {

	@Test
	public void testWithoutAuthentication() throws Exception {
		CallMessage sumMessage = new CallMessage("12", "callService.sum", 3, 4);
		WampMessage receivedMessage = sendWampMessage(sumMessage);
		assertThat(receivedMessage).isNull();
	}

	@Test
	public void testAuthenticationDisabledOnMethod() throws Exception {
		CallMessage sumMessage = new CallMessage("13", "callService.subtract", 3, 4);
		WampMessage response = sendWampMessage(sumMessage);
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage resultMessage = (CallResultMessage) response;
		assertThat(resultMessage.getCallID()).isEqualTo("13");
		assertThat(resultMessage.getResult()).isEqualTo(-1);
	}

	@Test
	public void testWithAuthentication() throws Exception {
		CallMessage sumMessage = new CallMessage("12", "callService.sum", 3, 4);
		WampMessage response = sendAuthenticatedMessage(sumMessage);
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage resultMessage = (CallResultMessage) response;
		assertThat(resultMessage.getCallID()).isEqualTo("12");
		assertThat(resultMessage.getResult()).isEqualTo(7);
	}

	@Override
	protected String wampEndpointPath() {
		return "/ws";
	}

	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { Config.class };
	}

	@Configuration
	@EnableWamp
	static class Config extends WampConfigurerAdapter {
		@Bean
		public AuthenticatedMethodService callService() {
			return new AuthenticatedMethodService();
		}

		@Override
		public String wampEndpointPath() {
			return "/ws";
		}

		@Override
		public AuthenticationSecretProvider authenticationSecretProvider() {
			return new TestSecretProvider();
		}

	}

}
