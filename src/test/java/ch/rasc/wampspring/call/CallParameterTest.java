/**
 * Copyright 2014-2016 Ralph Schaer <ralphschaer@gmail.com>
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
package ch.rasc.wampspring.call;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;

@SpringApplicationConfiguration(classes = CallParameterTest.Config.class)
public class CallParameterTest extends BaseWampTest {

	@Test
	public void testHeaderMethod() throws Exception {
		WampMessage response = sendWampMessage(new CallMessage("callID", "headerMethod"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID");
		assertThat(result.getResult()).isEqualTo("headerMethod called: true");

		response = sendWampMessage(new CallMessage("callID2", "headerMethod"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID2");
		assertThat(result.getResult()).isEqualTo("headerMethod called: true");
	}

	@Test
	public void testHeadersMethod() throws Exception {
		WampMessage response = sendWampMessage(
				new CallMessage("callID3", "headersMethod"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID3");
		assertThat(result.getResult()).isEqualTo("headersMethod called");
	}

	@Test
	public void testWampSesionMethod() throws Exception {
		WampMessage response = sendWampMessage(
				new CallMessage("callID5", "wampSesionMethod"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID5");
		assertThat(result.getResult()).isEqualTo("wampSesionMethod called");
	}

	@Test
	public void testMessageMethod() throws Exception {
		WampMessage response = sendWampMessage(
				new CallMessage("callID6", "messageMethod"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID6");
		assertThat(result.getResult())
				.isEqualTo("messageMethod called: callID6/messageMethod");
	}

	@Test
	public void testMix() throws Exception {
		WampMessage response = sendWampMessage(new CallMessage("callMix",
				"messageMethod/23", "param1", 2, 3.3f, "param4"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callMix");
		assertThat(result.getResult()).isEqualTo("mix");
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config {

		@Bean
		public CallParameterTestService callParameterTestService() {
			return new CallParameterTestService();
		}

	}

}
