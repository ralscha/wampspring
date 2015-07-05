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
package ch.rasc.wampspring.call;

import static org.fest.assertions.api.Assertions.assertThat;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.message.CallMessage;

public class CallService {

	@WampCallListener
	public void simpleTest(String arg1, Integer arg2) {
		assertThat(arg1).isEqualTo("argument");
		assertThat(arg2).isEqualTo(12);
	}

	@WampCallListener(value = "myOwnProcURI")
	public void simpleTest(CallMessage callMessage) {
		assertThat(callMessage.getCallID()).isEqualTo("theCallId");
		assertThat(callMessage.getProcURI()).isEqualTo("myOwnProcURI");
		assertThat(callMessage.getArguments()).containsExactly("argument", 13);
	}

	@WampCallListener
	public Integer sum(Integer a, Integer b) {
		return a + b;
	}

	@WampCallListener
	public String noParams() {
		return "nothing here";
	}

	@WampCallListener
	public Integer error(String argument) {
		assertThat(argument).isEqualTo("theArgument");
		throw new NullPointerException();
	}

	@WampCallListener
	public String callWithObject(TestDto testDto) {
		assertThat(testDto.getName()).isEqualTo("Hi");
		return testDto.getName().toUpperCase();
	}

	@WampCallListener
	public String callWithObjectAndMessage(TestDto testDto, CallMessage callMessage,
			String secondArgument) {
		assertThat(callMessage).isNotNull();
		assertThat(testDto.getName()).isEqualTo("Hi");
		assertThat(secondArgument).isEqualTo("thesecondargument");
		return testDto.getName().toUpperCase() + "/" + secondArgument;
	}
}
