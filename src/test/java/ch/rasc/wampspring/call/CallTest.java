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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.message.CallErrorMessage;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;

@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT, classes = CallTest.Config.class)
public class CallTest extends BaseWampTest {

	@Test
	public void testCallArguments() throws Exception {
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("callID", "callService.simpleTest", "argument", 12));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;

		assertThat(result.getCallID()).isEqualTo("callID");
		assertThat(result.getResult()).isNull();
	}

	@Test
	public void testNoParameters() throws Exception {
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("callID2", "callService.noParams"));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;

		assertThat(result.getCallID()).isEqualTo("callID2");
		assertThat(result.getResult()).isEqualTo("nothing here");
	}

	@Test
	public void testCallOwnProcUri() throws Exception {
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("theCallId", "myOwnProcURI", "argument", 13));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;

		assertThat(result.getCallID()).isEqualTo("theCallId");
		assertThat(result.getResult()).isNull();
	}

	@Test
	public void testReturnValue() throws Exception {
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("12", "callService.sum", 3, 4));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;
		assertThat(result.getCallID()).isEqualTo("12");
		assertThat(result.getResult()).isEqualTo(7);
	}

	@Test
	public void testWithError() throws Exception {
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("13", "callService.error", "theArgument"));
		assertThat(receivedMessage).isInstanceOf(CallErrorMessage.class);
		CallErrorMessage result = (CallErrorMessage) receivedMessage;
		assertThat(result.getCallID()).isEqualTo("13");
		assertThat(result.getErrorURI()).isEqualTo("");
		assertThat(result.getErrorDesc()).isEqualTo("java.lang.NullPointerException");
		assertThat(result.getErrorDetails()).isNull();
	}

	@Test
	public void testDto() throws Exception {
		TestDto dto = new TestDto();
		dto.setName("Hi");
		WampMessage receivedMessage = sendWampMessage(
				new CallMessage("13", "callService.callWithObject", dto));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;
		assertThat(result.getCallID()).isEqualTo("13");
		assertThat(result.getResult()).isEqualTo("HI");
	}

	@Test
	public void testWithMessageAndObjectParameter() throws Exception {
		TestDto dto = new TestDto();
		dto.setName("Hi");
		WampMessage receivedMessage = sendWampMessage(new CallMessage("13",
				"callService.callWithObjectAndMessage", dto, "thesecondargument"));
		assertThat(receivedMessage).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) receivedMessage;
		assertThat(result.getCallID()).isEqualTo("13");
		assertThat(result.getResult()).isEqualTo("HI/thesecondargument");
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config {

		@Bean
		public CallService callService() {
			return new CallService();
		}

	}

}
