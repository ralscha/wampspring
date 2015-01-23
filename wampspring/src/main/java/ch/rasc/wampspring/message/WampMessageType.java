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
package ch.rasc.wampspring.message;

/**
 * Enumeration of the WAMP message types.
 */
public enum WampMessageType {

	// Server-to-client Auxiliary
	WELCOME(0),

	// Client-to-server Auxiliary
	PREFIX(1),

	// Client-to-server RPC
	CALL(2),

	// Server-to-client RPC
	CALLRESULT(3),

	// Server-to-client RPC
	CALLERROR(4),

	// Client-to-server PubSub
	SUBSCRIBE(5),

	// Client-to-server PubSub
	UNSUBSCRIBE(6),

	// Client-to-server PubSub
	PUBLISH(7),

	// Server-to-client PubSub
	EVENT(8);

	private final int typeId;

	private WampMessageType(int typeId) {
		this.typeId = typeId;
	}

	public int getTypeId() {
		return this.typeId;
	}

	public static WampMessageType fromTypeId(int typeId) {
		switch (typeId) {
		case 0:
			return WELCOME;
		case 1:
			return PREFIX;
		case 2:
			return CALL;
		case 3:
			return CALLRESULT;
		case 4:
			return CALLERROR;
		case 5:
			return SUBSCRIBE;
		case 6:
			return UNSUBSCRIBE;
		case 7:
			return PUBLISH;
		case 8:
			return EVENT;
		default:
			return null;
		}

	}
}
