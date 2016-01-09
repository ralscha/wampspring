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

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Service;

import ch.rasc.wampspring.annotation.WampCallListener;

@Service
public class CallDestinationTestService {

	// By default destination mappings are treated as Ant-style, slash-separated, path
	// patterns, e.g. "/foo*",
	// "/foo/**". etc. They can also contain template variables, e.g. "/foo/{id}" that can
	// then be referenced via
	// @DestinationVariable-annotated method arguments.

	@WampCallListener(value = "dest?")
	public String questionMarkDestination() {
		return "questionMarkDestination called";
	}

	@WampCallListener(value = "path*")
	public String starDestination() {
		return "starDestination called";
	}

	@WampCallListener(value = "/start/**/end")
	public String doubleStarDestination() {
		return "doubleStarDestination called";
	}

	@WampCallListener(value = "/dvar1/{id}")
	public String destVar(@DestinationVariable(value = "id") String id) {
		return "destVar:" + id;
	}

	@WampCallListener(value = "/dvar2/{path}/{id}")
	public String destVar(@DestinationVariable(value = "path") String p,
			@DestinationVariable int id) {
		return "destVar:/" + p + "/" + id;
	}
}
