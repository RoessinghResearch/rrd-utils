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

package nl.rrd.utils.xml;

import nl.rrd.utils.exception.ParseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Values parser for SQL date values. It accepts strings formatted as
 * yyyy-MM-dd.
 * 
 * @author Dennis Hofs (RRD)
 */
public class SQLDateValueParser implements XMLValueParser<LocalDate> {

	@Override
	public LocalDate parse(String xml) throws ParseException {
		try {
			DateTimeFormatter parser = DateTimeFormatter.ofPattern(
					"yyyy-MM-dd");
			return parser.parse(xml, LocalDate::from);
		} catch (Exception ex) {
			throw new ParseException("Value is not an SQL date string: \"" +
					xml + "\": " + ex.getMessage(), ex);
		}
	}
}
