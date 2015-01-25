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

import static org.fest.assertions.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.handler.WampSession;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.WampMessageHeader;

public class WampSessionMethodArgumentResolverTest {

	private WampSessionMethodArgumentResolver resolver;
	private MethodParameter wampSessionParameter;
	private MethodParameter stringParameter;
	private WebSocketSession nativeSession;

	@Before
	public void setup() throws Exception {
		Method testMethod = getClass().getDeclaredMethod("handleMessage",
				WampSession.class, String.class);
		this.resolver = new WampSessionMethodArgumentResolver();
		this.wampSessionParameter = new MethodParameter(testMethod, 0);
		this.stringParameter = new MethodParameter(testMethod, 1);

		this.nativeSession = Mockito.mock(WebSocketSession.class);
		Mockito.when(this.nativeSession.getId()).thenReturn("ws1");
	}

	@Test
	public void supportsParameterTest() {
		assertThat(this.resolver.supportsParameter(this.wampSessionParameter)).isTrue();
		assertThat(this.resolver.supportsParameter(this.stringParameter)).isFalse();
	}

	@Test
	public void resolveArgumentTest() throws Exception {
		CallMessage callMessage = new CallMessage("1", "call");

		WampSession wampSession = new WampSession(this.nativeSession);
		callMessage.setHeader(WampMessageHeader.WAMP_SESSION, wampSession);

		assertThat(this.resolver.resolveArgument(this.wampSessionParameter, callMessage))
				.isEqualTo(wampSession);
	}

	@Test(expected = MissingWampSessionException.class)
	public void missingWampSessionTest() throws Exception {
		CallMessage callMessage = new CallMessage("1", "call");
		WampSession wampSession = new WampSession(this.nativeSession);
		assertThat(this.resolver.resolveArgument(this.wampSessionParameter, callMessage))
				.isEqualTo(wampSession);
	}

	@SuppressWarnings({ "unused" })
	@WampPublishListener
	private void handleMessage(WampSession wampSession, String anotherParam) {
		// nothing here
	}

}
