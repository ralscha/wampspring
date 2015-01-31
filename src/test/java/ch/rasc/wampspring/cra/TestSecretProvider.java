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
package ch.rasc.wampspring.cra;

import java.util.HashMap;
import java.util.Map;

public class TestSecretProvider implements AuthenticationSecretProvider {

	private final Map<String, String> secretDb = new HashMap<>();

	public TestSecretProvider() {
		this.secretDb.put("a", "secretofa");
		this.secretDb.put("b", "secretofb");
	}

	@Override
	public String getSecret(String authKey) {
		return this.secretDb.get(authKey);
	}

}
