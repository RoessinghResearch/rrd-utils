/*
 * Copyright 2022 Roessingh Research and Development
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package nl.rrd.utils.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import nl.rrd.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

public class EnumIgnoreCaseDeserializer<T extends Enum<?>>
		extends JsonDeserializer<T> {
	private Class<T> enumClass;

	/**
	 * Constructs a new instance. The enum class must have a static method
	 * fromStringValue(String s).
	 *
	 * @param enumClass the enum class
	 */
	public EnumIgnoreCaseDeserializer(Class<T> enumClass) {
		this.enumClass = enumClass;
	}

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		if (!p.getCurrentToken().isScalarValue()) {
			throw new JsonParseException(p,
					"Expected string, found non-scalar value");
		}
		String s = p.getValueAsString();
		if (s == null)
			return null;
		try {
			Object array = ReflectionUtils.invokeStaticMethod(enumClass,
					Object.class, "values");
			for (int i = 0; i < Array.getLength(array); i++) {
				T item = enumClass.cast(Array.get(array, i));
				if (item.toString().equalsIgnoreCase(s))
					return item;
			}
			throw new JsonParseException(p, "Value not found: " + s);
		} catch (NoSuchMethodException | InvocationTargetException ex) {
			throw new RuntimeException("Can't invoke values(): " +
					ex.getMessage(), ex);
		}
	}
}
