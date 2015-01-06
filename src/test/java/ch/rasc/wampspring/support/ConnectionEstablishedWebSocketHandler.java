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

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

class ConnectionEstablishedWebSocketHandler extends AbstractWebSocketHandler {

	private final Deferred<WebSocketSession, Void, Void> deferred = new DeferredObject<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		deferred.resolve(session);
	}

	public Promise<WebSocketSession, Void, Void> getPromise() {
		return deferred.promise();
	}

}
