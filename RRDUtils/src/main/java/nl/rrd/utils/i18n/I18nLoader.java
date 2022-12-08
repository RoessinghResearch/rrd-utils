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

package nl.rrd.utils.i18n;

import nl.rrd.utils.AppComponents;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class can load i18n message resources and keep them in memory for later
 * reference. You can get a resource set with {@link
 * #getI18n(String, Locale, boolean, Class) getI18n()}, which will try to load
 * the set at the first call. It uses {@link I18nResourceFinder
 * I18nResourceFinder} to find a matching file, in .properties format,
 * properties .xml format, or key-string pair .json format. Note that a
 * .properties file will be loaded as UTF-8.
 * 
 * @author Dennis Hofs (RRD)
 */
public class I18nLoader {
	private static final Object STATIC_LOCK = new Object();
	private static I18nLoader instance = null;
	
	private final Object lock = new Object();
	private Map<I18nKey,I18n> i18nMap = new HashMap<>();

	/**
	 * This private constructor is used in {@link #getInstance()
	 * getInstance()}.
	 */
	private I18nLoader() {
	}
	
	/**
	 * Returns the i18n loader.
	 * 
	 * @return the i18n loader
	 */
	public static I18nLoader getInstance() {
		synchronized (STATIC_LOCK) {
			if (instance == null)
				instance = new I18nLoader();
			return instance;
		}
	}

	/**
	 * Returns the i18n message resource for the specified parameters. If the
	 * resource has not been loaded yet, it will be loaded now (.properties or
	 * .xml) and then saved in memory. Note that a .properties file will be
	 * loaded as UTF-8. For more details see {@link I18nResourceFinder
	 * I18nResourceFinder}.
	 * 
	 * @param baseName the base name
	 * @param locale the preferred locale
	 * @param honorifics true if the resource should use honorifics, false
	 * otherwise
	 * @param loadClass the resource loading class or null
	 * @return the message resource
	 * @throws RuntimeException if no matching resource is found, or a matching
	 * resource can't be loaded
	 */
	public I18n getI18n(String baseName, Locale locale, boolean honorifics,
			Class<?> loadClass) throws RuntimeException {
		List<Locale> locales = new ArrayList<>();
		locales.add(locale);
		return getI18n(baseName, locales, honorifics, loadClass);
	}

	/**
	 * Returns the i18n message resource for the specified parameters. If the
	 * resource has not been loaded yet, it will be loaded now (.properties or
	 * .xml) and then saved in memory. Note that a .properties file will be
	 * loaded as UTF-8. For more details see {@link I18nResourceFinder
	 * I18nResourceFinder}.
	 * 
	 * @param baseName the base name
	 * @param locales the preferred locales (at least one)
	 * @param honorifics true if the resource should use honorifics, false
	 * otherwise
	 * @param loadClass the resource loading class or null
	 * @return the message resource
	 * @throws RuntimeException if no matching resource is found, or a matching
	 * resource can't be loaded
	 */
	public I18n getI18n(String baseName, List<Locale> locales,
			boolean honorifics, Class<?> loadClass) throws RuntimeException {
		synchronized (lock) {
			I18nKey key = new I18nKey(baseName, locales, honorifics,
					loadClass);
			if (i18nMap.containsKey(key)) {
				return i18nMap.get(key);
			} else {
				I18n i18n = new I18n(baseName, locales, honorifics, loadClass);
				i18nMap.put(key, i18n);
				return i18n;
			}
		}
	}
	
	/**
	 * The key in the map of i18n message resources.
	 */
	private static class I18nKey {
		private String baseName;
		private List<Locale> locales;
		private boolean honorifics;
		private Class<?> loadClass;
		
		public I18nKey(String baseName, List<Locale> locales,
				boolean honorifics, Class<?> loadClass) {
			this.baseName = baseName;
			this.locales = locales;
			this.honorifics = honorifics;
			this.loadClass = loadClass;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + baseName.hashCode();
			result = prime * result + locales.hashCode();
			result = prime * result + (honorifics ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			I18nKey other = (I18nKey) obj;
			if (!baseName.equals(other.baseName))
				return false;
			if (!locales.equals(other.locales))
				return false;
			if (honorifics != other.honorifics)
				return false;
			if (loadClass != other.loadClass)
				return false;
			return true;
		}
	}
}
