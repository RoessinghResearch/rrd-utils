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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvHelper implements Closeable {
	private Writer writer;
	private String newline;
	
	public CsvHelper(Writer writer) {
		this.writer = writer;
		newline = System.getProperty("line.separator");
	}
	
	public void createHeaderRow(List<String> fields) throws IOException {
		writer.write("sep=;" + newline);
		boolean first = true;
		for (String field : fields) {
			if (!first)
				writer.write(";");
			else
				first = false;
			writer.write(field);
		}
		writer.write(newline);
	}

	public void createDataRow(List<Object> values) throws IOException {
		boolean first = true;
		for (Object value : values) {
			if (!first)
				writer.write(";");
			else
				first = false;
			writer.write(CsvUtils.valueToString(value));
		}
		writer.write(newline);
	}
	
	public void createDataRow(List<String> fields, Map<String,?> data)
			throws IOException {
		List<Object> values = new ArrayList<>();
		for (String field : fields) {
			values.add(data.get(field));
		}
		createDataRow(values);
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
