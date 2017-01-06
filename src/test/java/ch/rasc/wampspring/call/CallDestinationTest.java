/**
 * Copyright 2014-2017 Ralph Schaer <ralphschaer@gmail.com>
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

import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;

@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT, classes = CallDestinationTest.Config.class)
public class CallDestinationTest extends BaseWampTest {

	@Test
	public void testQuestionMarkDestination() throws Exception {
		WampMessage response = sendWampMessage(new CallMessage("callID", "dest1"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID");
		assertThat(result.getResult()).isEqualTo("questionMarkDestination called");

		response = sendWampMessage(new CallMessage("callID", "destA"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID");
		assertThat(result.getResult()).isEqualTo("questionMarkDestination called");

		try {
			sendWampMessage(new CallMessage("callID", "destAB"));
			Assert.fail("call has to timeout");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(TimeoutException.class);
		}
	}

	@Test
	public void testStarDestination() throws Exception {
		WampMessage response = sendWampMessage(new CallMessage("callID2", "path1"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID2");
		assertThat(result.getResult()).isEqualTo("starDestination called");

		response = sendWampMessage(new CallMessage("callID2", "pathMoreStuff"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID2");
		assertThat(result.getResult()).isEqualTo("starDestination called");

		try {
			sendWampMessage(new CallMessage("callID2", "pat"));
			Assert.fail("call has to timeout");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(TimeoutException.class);
		}
	}

	@Test
	public void testDoubleStarDestination() throws Exception {
		WampMessage response = sendWampMessage(
				new CallMessage("callID3", "/start/middle/end"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID3");
		assertThat(result.getResult()).isEqualTo("doubleStarDestination called");

		response = sendWampMessage(
				new CallMessage("callID3", "/start/one/two/three/end"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID3");
		assertThat(result.getResult()).isEqualTo("doubleStarDestination called");

		response = sendWampMessage(new CallMessage("callID3", "/start/end"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID3");
		assertThat(result.getResult()).isEqualTo("doubleStarDestination called");

		try {
			sendWampMessage(new CallMessage("callID3", "/star/end"));
			Assert.fail("call has to timeout");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(TimeoutException.class);
		}
	}

	@Test
	public void testDestinationVariable() throws Exception {
		WampMessage response = sendWampMessage(new CallMessage("callID4", "/dvar1/12"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID4");
		assertThat(result.getResult()).isEqualTo("destVar:12");

		response = sendWampMessage(new CallMessage("callID5", "/dvar2/aPath/14"));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("callID5");
		assertThat(result.getResult()).isEqualTo("destVar:/aPath/14");
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config {

		@Bean
		public CallDestinationTestService destinationTestService() {
			return new CallDestinationTestService();
		}

	}

}
