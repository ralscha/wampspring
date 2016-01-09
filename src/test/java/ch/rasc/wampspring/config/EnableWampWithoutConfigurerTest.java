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
package ch.rasc.wampspring.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;

@SpringApplicationConfiguration(classes = EnableWampWithoutConfigurerTest.Config.class)
public class EnableWampWithoutConfigurerTest extends BaseWampTest {

	@Test
	public void testCall() throws InterruptedException, ExecutionException,
			TimeoutException, IOException {
		WampMessage response = sendWampMessage(new CallMessage("1", "sum", 2, 4));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("1");
		assertThat(result.getResult()).isEqualTo(6);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config {

		@Bean
		TestService testService() {
			return new TestService();
		}
	}

	static class TestService {
		@WampCallListener("sum")
		public int sum(int a, int b) {
			return a + b;
		}
	}

}
