/**
 * Copyright 2014-2014 Ralph Schaer <ralphschaer@gmail.com>
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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseMessageTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	static final JsonFactory jsonFactory = new MappingJsonFactory(objectMapper);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static String toJsonArray(Object... arguments) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		for (Object argument : arguments) {
			if (sb.length() > 1) {
				sb.append(",");
			}
			if (argument != null) {
				if (argument instanceof Number) {
					sb.append(argument);
				} else if (argument instanceof Boolean) {
					sb.append(argument);
				} else if (argument instanceof String) {
					sb.append("\"").append(argument).append("\"");
				} else if (argument instanceof List) {
					sb.append("[");
					boolean first = true;
					for (Object entry : (List) argument) {
						if (!first) {
							sb.append(",");
						} else {
							first = false;
						}
						if (entry instanceof String) {
							sb.append("\"").append(entry).append("\"");
						} else {
							sb.append(entry);
						}
					}
					sb.append("]");
				} else if (argument instanceof Map) {
					Map<String, Object> map = (Map<String, Object>) argument;
					sb.append("{");
					boolean first = true;
					for (String key : map.keySet()) {
						if (!first) {
							sb.append(",");
						} else {
							first = false;
						}
						sb.append("\"").append(key).append("\"");
						sb.append(":");
						Object value = map.get(key);
						if (value instanceof String) {
							sb.append("\"").append(value).append("\"");
						} else {
							sb.append(value);
						}
					}
					sb.append("}");
				}
			} else {
				sb.append("null");
			}
		}

		sb.append("]");
		return sb.toString();
	}

	BaseMessageTest() {
		super();
	}

}