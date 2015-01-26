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

package ch.rasc.wampspring.handler;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampSessionScopeTests {

	private WampSessionScope scope;

	@SuppressWarnings("rawtypes")
	private ObjectFactory objectFactory;

	private WampSession wampSession;

	private Map<String, Object> sessionAttributesMap;

	@Before
	public void setUp() {
		this.scope = new WampSessionScope();
		this.objectFactory = Mockito.mock(ObjectFactory.class);
		this.sessionAttributesMap = new HashMap<>();

		@SuppressWarnings("resource")
		WebSocketSession nativeSession = Mockito.mock(WebSocketSession.class);
		Mockito.when(nativeSession.getId()).thenReturn("ws1");
		Mockito.when(nativeSession.getAttributes()).thenReturn(this.sessionAttributesMap);
		this.wampSession = new WampSession(nativeSession);
		WampSessionContextHolder.setAttributes(this.wampSession);
	}

	@After
	public void tearDown() {
		WampSessionContextHolder.resetAttributes();
	}

	@Test
	public void get() {
		this.wampSession.setAttribute("name", "value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
	}

	@Test
	public void getWithObjectFactory() {
		Mockito.when(this.objectFactory.getObject()).thenReturn("value");
		Object actual = this.scope.get("name", this.objectFactory);

		assertThat(actual).isEqualTo("value");
		assertThat((String) this.wampSession.getAttribute("name")).isEqualTo("value");
	}

	@Test
	public void remove() {
		this.wampSession.setAttribute("name", "value");

		Object removed = this.scope.remove("name");
		assertThat(removed).isEqualTo("value");
		assertThat((String) this.wampSession.getAttribute("name")).isNull();

		removed = this.scope.remove("name");
		assertThat(removed).isNull();
	}

	@Test
	public void registerDestructionCallback() {
		Runnable runnable = Mockito.mock(Runnable.class);
		this.scope.registerDestructionCallback("name", runnable);

		this.wampSession.sessionCompleted();
		verify(runnable, times(1)).run();
	}

	@Test
	public void getSessionId() {
		assertThat(this.scope.getConversationId()).isEqualTo("ws1");
	}

}
