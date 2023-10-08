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

package nl.rrd.utils.csv;

import nl.rrd.utils.datetime.DateTimeUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class CsvUtils {
	public static Writer createWriter(File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		boolean created = false;
		try {
			byte[] bom = new byte[] { (byte)0xef, (byte)0xbb, (byte)0xbf };
			out.write(bom);
			out.flush();
			Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			created = true;
			return writer;
		} finally {
			if (!created)
				out.close();
		}
	}

	public static void writeCsvHeader(Writer writer) throws IOException {
		String newline = System.getProperty("line.separator");
		writer.write("sep=;" + newline);
	}
	
	public static String valueToString(Object value) {
		char decimal = ',';
		if (value == null) {
			return "NULL";
		} else if (value instanceof Number) {
			String s = value.toString();
			int sep = s.indexOf('.');
			if (sep != -1)
				s = s.substring(0, sep) + decimal + s.substring(sep + 1);
			return s;
		} else if (value instanceof Boolean) {
			return value.toString();
		} else if (value instanceof LocalDate) {
			LocalDate date = (LocalDate)value;
			return date.format(DateTimeUtils.DATE_FORMAT);
		} else if (value instanceof LocalTime) {
			LocalTime time = (LocalTime)value;
			return time.format(DateTimeUtils.SQL_TIME_FORMAT);
		} else if (value instanceof LocalDateTime) {
			LocalDateTime time = (LocalDateTime)value;
			return time.format(DateTimeUtils.SQL_DATE_TIME_FORMAT);
		} else if (value instanceof ZonedDateTime) {
			ZonedDateTime time = (ZonedDateTime)value;
			return time.format(DateTimeUtils.SQL_DATE_TIME_FORMAT);
		} else {
			return '"' + value.toString().replaceAll("\"", "\"\"") + '"';
		}
	}
}
