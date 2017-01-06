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
package ch.rasc.wampspring.cra;

import java.util.Map;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.message.CallMessage;

public interface AuthenticationHandler {

	@WampCallListener(value = "http://api.wamp.ws/procedure#authreq",
			authenticated = false)
	Object handleAuthReq(String authKey, Map<String, Object> extra, CallMessage message);

	@WampCallListener(value = "http://api.wamp.ws/procedure#auth", authenticated = false)
	Object handleAuth(String clientSignature, CallMessage message);

}