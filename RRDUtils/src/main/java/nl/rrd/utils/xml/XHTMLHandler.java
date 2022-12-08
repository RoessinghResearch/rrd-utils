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
import org.xml.sax.Attributes;

import java.util.List;

public class XHTMLHandler extends AbstractSimpleSAXHandler<String> {
	private StringBuilder stringContent = new StringBuilder();

	@Override
	public void startElement(String name, Attributes atts, List<String> parents)
			throws ParseException {
		stringContent.append("<");
		stringContent.append(name);
		for (int i = 0; i < atts.getLength(); i++) {
			String attName = atts.getLocalName(i);
			String attValue = atts.getValue(i);
			stringContent.append(" ");
			stringContent.append(attName);
			stringContent.append("=\"");
			stringContent.append(XMLWriter.escapeString(attValue));
			stringContent.append("\"");
		}
		stringContent.append(">");
	}

	@Override
	public void endElement(String name, List<String> parents)
			throws ParseException {
		stringContent.append("</");
		stringContent.append(name);
		stringContent.append(">");
	}

	@Override
	public void characters(String ch, List<String> parents)
			throws ParseException {
		String trimmed = ch.replaceAll("\\s+", " ");
		stringContent.append(XMLWriter.escapeString(trimmed));
	}

	@Override
	public String getObject() {
		return stringContent.toString();
	}
}
