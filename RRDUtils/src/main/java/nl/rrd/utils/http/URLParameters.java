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

package nl.rrd.utils.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.rrd.utils.exception.ParseException;

/**
 * Methods for creating and parsing URL-encoded parameter strings.
 *
 * @author Dennis Hofs (RRD)
 */
public class URLParameters {

	/**
	 * Creates a URL-encoded parameter string for the specified parameters.
	 * They will first be sorted by key. It uses UTF-8.
	 *
	 * @param params the parameters
	 * @return the parameter string
	 */
	public static String getSortedParameterString(Map<String,String> params) {
		Map<String,String> sortedMap = new LinkedHashMap<String,String>();
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			sortedMap.put(key, params.get(key));
		}
		return getParameterString(sortedMap);
	}

	/**
	 * Creates a URL-encoded parameter string for the specified parameters. It
	 * uses UTF-8.
	 *
	 * @param params the parameters
	 * @return the parameter string
	 */
	public static String getParameterString(Map<String,String> params) {
		StringBuilder builder = new StringBuilder();
		for (String key : params.keySet()) {
			if (builder.length() > 0)
				builder.append("&");
			try {
				builder.append(key + "=" + URLEncoder.encode(params.get(key),
						"UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				throw new RuntimeException("UTF-8 not supported: " +
						ex.getMessage(), ex);
			}
		}
		return builder.toString();
	}

	/**
	 * Parses a URL-encoded parameter string. It uses UTF-8.
	 *
	 * @param paramStr the parameter string
	 * @return the parameters
	 * @throws ParseException if the parameter string is invalid
	 */
	public static Map<String,String> parseParameterString(String paramStr)
			throws ParseException {
		Map<String,String> params = new LinkedHashMap<String,String>();
		String[] paramList = paramStr.split("&");
		for (String keyValStr : paramList) {
			String[] keyVal = keyValStr.split("=");
			if (keyVal.length != 2) {
				throw new ParseException("Invalid parameter string: " +
						paramStr);
			}
			try {
				if (keyVal[0].length() == 0) {
					throw new ParseException(
							"Empty key in parameter string: " + paramStr);
				}
				params.put(keyVal[0], URLDecoder.decode(keyVal[1], "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				throw new RuntimeException("UTF-8 not supported: " +
						ex.getMessage(), ex);
			}
		}
		return params;
	}

	/**
	 * Extracts a parameter string from the specified URL. It will search for
	 * a ? and return everything after that until a possible #. If there is no
	 * ? this method returns null.
	 *
	 * @param url the URL
	 * @return the parameter string or null
	 */
	public static String extractParameterString(String url) {
		int startIndex = url.indexOf('?');
		if (startIndex == -1)
			return null;
		int endIndex = url.indexOf('#', startIndex);
		if (endIndex == -1)
			return url.substring(startIndex + 1);
		else
			return url.substring(startIndex + 1, endIndex);
	}

	/**
	 * Extracts a parameter string from the specified URL and parses it into
	 * a map. It uses UTF-8. If the URL doesn't have a parameter string, this
	 * method returns null. This is a convenience method for {@link
	 * #extractParameterString(String) extractParameterString()} and {@link
	 * #parseParameterString(String) parseParameterString()}.
	 *
	 * @param url the URL
	 * @return the parameter map or null
	 * @throws ParseException if the parameter string is invalid
	 */
	public static Map<String,String> extractParameters(String url)
	throws ParseException {
		String paramStr = extractParameterString(url);
		if (paramStr == null)
			return null;
		return parseParameterString(paramStr);
	}
}
