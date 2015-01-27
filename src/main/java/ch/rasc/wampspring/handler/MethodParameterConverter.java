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
package ch.rasc.wampspring.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.UsesJava8;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class MethodParameterConverter {

	private final ObjectMapper objectMapper;

	private final ConversionService conversionService;

	public MethodParameterConverter(ObjectMapper objectMapper,
			ConversionService conversionService) {
		this.objectMapper = objectMapper;
		this.conversionService = conversionService;
	}

	public Object convert(MethodParameter parameter, Object argument) {
		if (argument == null) {
			if (parameter.getParameterType().getName().equals("java.util.Optional")) {
				return OptionalUnwrapper.empty();
			}

			return null;
		}

		Class<?> sourceClass = argument.getClass();
		Class<?> targetClass = parameter.getParameterType();

		TypeDescriptor td = new TypeDescriptor(parameter);

		if (targetClass.isAssignableFrom(sourceClass)) {
			return convertListElements(td, argument);
		}

		if (this.conversionService.canConvert(sourceClass, targetClass)) {
			try {
				return convertListElements(td,
						this.conversionService.convert(argument, targetClass));
			}
			catch (Exception e) {

				TypeFactory typeFactory = this.objectMapper.getTypeFactory();
				if (td.isCollection()) {
					JavaType type = CollectionType.construct(td.getType(), typeFactory
							.constructType(td.getElementTypeDescriptor().getType()));
					return this.objectMapper.convertValue(argument, type);
				}
				else if (td.isArray()) {
					JavaType type = typeFactory.constructArrayType(td
							.getElementTypeDescriptor().getType());
					return this.objectMapper.convertValue(argument, type);
				}

				throw e;
			}
		}
		return this.objectMapper.convertValue(argument, targetClass);
	}

	@SuppressWarnings("unchecked")
	private Object convertListElements(TypeDescriptor td, Object convertedValue) {
		if (List.class.isAssignableFrom(convertedValue.getClass()) && td.isCollection()
				&& td.getElementTypeDescriptor() != null) {
			Class<?> elementType = td.getElementTypeDescriptor().getType();

			Collection<Object> convertedList = new ArrayList<>();
			for (Object record : (List<Object>) convertedValue) {
				Object convertedObject = this.objectMapper.convertValue(record,
						elementType);
				convertedList.add(convertedObject);
			}
			return convertedList;

		}
		return convertedValue;
	}

	@UsesJava8
	private static class OptionalUnwrapper {
		public static Object empty() {
			return Optional.empty();
		}
	}
}
