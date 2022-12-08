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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains methods to read and validate data from an XML DOM.
 * 
 * @author Dennis Hofs (RRD)
 */
public class XMLDocumentParser {
	
	/**
	 * Retrieves the string content of the specified child element and parses
	 * it using the specified parser. If the child element is not found, it
	 * returns the default value.
	 * 
	 * @param parent the parent element
	 * @param name the name of the child element
	 * @param parser the parser
	 * @param defaultVal the default value
	 * @param <T> the value type
	 * @return the parsed value or the default value
	 * @throws ParseException if the string content of the child element is
	 * invalid
	 * @see #parseStringContent(Element)
	 */
	public <T> T parseElemValue(Element parent, String name,
			XMLValueParser<T> parser, T defaultVal) throws ParseException {
		Element child = getChild(parent, name);
		if (child == null)
			return defaultVal;
		String content = parseStringContent(child);
		return parser.parse(content);
	}
	
	/**
	 * Retrieves the value of the specified attribute and parses it using the
	 * specified parser. If the attribute is not found, it returns the default
	 * value.
	 * 
	 * @param elem the element
	 * @param name the name of the attribute
	 * @param parser the parser
	 * @param defaultVal the default value
	 * @param <T> the value type
	 * @return the parsed value or the default value
	 * @throws ParseException if the attribute value is invalid
	 */
	public <T> T parseAttrValue(Element elem, String name,
			XMLValueParser<T> parser, T defaultVal) throws ParseException {
		Attr attr = elem.getAttributeNode(name);
		if (attr == null)
			return defaultVal;
		return parser.parse(attr.getValue());
	}

	/**
	 * Retrieves the string content of a required child element and parses it
	 * using the specified parser. If the child element is not found, it throws
	 * an exception.
	 * 
	 * @param parent the parent element
	 * @param name the name of the child element
	 * @param parser the parser
	 * @param <T> the value type
	 * @return the parsed value
	 * @throws ParseException if the child element is not found or the string
	 * content of the child element is invalid
	 * @see #parseStringContent(Element)
	 */
	public <T> T parseRequiredElemValue(Element parent, String name,
			XMLValueParser<T> parser) throws ParseException {
		Element child = getChild(parent, name);
		if (child == null)
			throw new ParseException("Element \"" + name + "\" not found");
		String content = parseStringContent(child);
		return parser.parse(content);
	}

	/**
	 * Retrieves the value of a required attribute and parses it using the
	 * specified parser. If the attribute is not found, it throws an exception.
	 * 
	 * @param elem the element
	 * @param name the name of the attribute
	 * @param parser the parser
	 * @param <T> the value type
	 * @return the parsed value
	 * @throws ParseException if the attribute is not found or the attribute
	 * value is invalid
	 */
	public <T> T parseRequiredAttrValue(Element elem, String name,
			XMLValueParser<T> parser) throws ParseException {
		Attr attr = elem.getAttributeNode(name);
		if (attr == null)
			throw new ParseException("Attribute \"" + name + "\" not found");
		return parser.parse(attr.getValue());
	}
	
	/**
	 * Retrieves the string content of the specified element and parses it
	 * using the specified parser.
	 * 
	 * @param elem the element
	 * @param parser the parser
	 * @param <T> the value type
	 * @return the parsed value
	 * @throws ParseException if the string content is invalid
	 * @see #parseStringContent(Element)
	 */
	public <T> T parseContent(Element elem, XMLValueParser<T> parser)
			throws ParseException {
		String content = parseStringContent(elem);
		return parser.parse(content);
	}
	
	/**
	 * Parses the string content of the specified element. It will trim white
	 * space at the start and end. If the element contains other nodes than
	 * text nodes, then those nodes and any white space around them will be
	 * replaced by a single space.
	 * 
	 * @param elem the element
	 * @return the string content
	 */
	public String parseStringContent(Element elem) {
		NodeList nodes = elem.getChildNodes();
		String result = "";
		boolean prevNodeIsText = false;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() != Node.TEXT_NODE) {
				prevNodeIsText = false;
				continue;
			}
			String val = node.getNodeValue();
			if (!prevNodeIsText) {
				result = result.trim();
				String post = val.replaceAll("^\\s+", "");
				if (result.length() > 0 && post.length() > 0)
					result += " ";
				result += post;
			} else {
				result += val;
			}
		}
		return result.trim();
	}

	/**
	 * Returns the first child element with the specified name. If no such
	 * child exists, this method returns null.
	 * 
	 * @param parent the parent element
	 * @param name the name of the child element
	 * @return the child element or null
	 */
	public Element getChild(Element parent, String name) {
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equals(name)) {
				return (Element)node;
			}
		}
		return null;
	}
}
