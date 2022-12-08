package nl.rrd.utils.csv;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(
				Locale.getDefault());
		char decimal = symbols.getDecimalSeparator();
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
			return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		} else if (value instanceof LocalTime) {
			LocalTime time = (LocalTime)value;
			return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		} else if (value instanceof LocalDateTime) {
			LocalDateTime time = (LocalDateTime)value;
			return time.format(DateTimeFormatter.ofPattern(
					"yyyy-MM-dd HH:mm:ss"));
		} else if (value instanceof ZonedDateTime) {
			ZonedDateTime time = (ZonedDateTime)value;
			return time.format(DateTimeFormatter.ofPattern(
					"yyyy-MM-dd HH:mm:ss"));
		} else {
			return '"' + value.toString().replaceAll("\"", "\"\"") + '"';
		}
	}
}
