package nl.rrd.utils.http;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class models HTTP response data. It's returned by {@link HttpClient2
 * HttpClient2}. The class has several read*() methods that check for error
 * response codes, and that can parse the response data.
 *
 * @author Dennis Hofs (RRD)
 */
public class HttpResponse {
	private int code;
	private String reason;
	private Map<String,String> headers = new LinkedHashMap<>();
	private String contentType = null;
	private String contentEncoding = null;
	private byte[] content;

	/**
	 * Returns the response code.
	 *
	 * @return the response code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Sets the response code.
	 *
	 * @param code the response code
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * Returns the reason phrase, in particular with error codes.
	 *
	 * @return the reason phrase, in particular with error codes
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Sets the reason phrase, in particular with error codes
	 *
	 * @param reason the reason phrase, in particular with error codes
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Returns the response headers. All header names are in lower case.
	 *
	 * @return the response headers
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	/**
	 * Sets the response headers. All header names should be in lower case.
	 *
	 * @param headers the response headers
	 */
	public void setHeaders(Map<String,String> headers) {
		this.headers = headers;
	}

	/**
	 * Adds a response header. The header name should be in lower case.
	 *
	 * @param name the header name in lower case
	 * @param value the header value
	 */
	public void addHeader(String name, String value) {
		headers.put(name, value);
	}

	/**
	 * Returns the content type, from the Content-Type header. If no content
	 * type was specified, this method returns null.
	 *
	 * @return the content type or null
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Sets the content type, from the Content-Type header. If no content type
	 * is specified, this should be null.
	 *
	 * @param contentType the content type or null
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Returns the content encoding, from the Content-Type header. If no
	 * encoding was specified, this method returns null.
	 *
	 * @return the content encoding or null
	 */
	public String getContentEncoding() {
		return contentEncoding;
	}

	/**
	 * Sets the content encoding, from the Content-Type header. If no encoding
	 * is specified, this should be null.
	 *
	 * @param contentEncoding the content encoding or null
	 */
	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	/**
	 * Returns the raw content as a byte array. You should usually call one of
	 * the read*() methods, which check for error response codes, and can parse
	 * the raw bytes if needed. These bytes can contain the returned data in
	 * case of sucess, or error information in case of an error code.
	 *
	 * @return the raw content as a byte array
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * Sets the raw content as a byte array. These bytes can contain the
	 * returned data in case of sucess, or error information in case of an error
	 * code.
	 *
	 * @param content the raw content as a byte array
	 */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * Converts the raw byte array content as a string, using the correct
	 * encoding if known, or UTF-8 otherwise. You should usually call one of
	 * the read*() methods, which check for error response codes. The raw
	 * content can contain the returned data in case of sucess, or error
	 * information in case of an error code.
	 *
	 * @return the raw byte array content as a string
	 */
	public String toContentString() {
		Charset charset = StandardCharsets.UTF_8;
		if (contentEncoding != null && Charset.isSupported(contentEncoding)) {
			charset = Charset.forName(contentEncoding);
		}
		ByteBuffer bb = ByteBuffer.wrap(content);
		return charset.decode(bb).toString();
	}

	/**
	 * Reads the content as a string. This method first checks whether the
	 * response code indicates an error. In that case it will throw an
	 * {@link HttpClientException HttpClientException}.
	 *
	 * @return the content as a string
	 * @throws HttpClientException if the response code indicates an error
	 */
	public String readString() throws HttpClientException {
		checkError();
		return toContentString();
	}

	/**
	 * Reads the content as a JSON object. The content should be a JSON string,
	 * which is converted to an object of the specified class. This method first
	 * checks whether the response code indicates an error. In that case it will
	 * throw an {@link HttpClientException HttpClientException}.
	 *
	 * @return the content as a JSON object
	 * @throws HttpClientException if the response code indicates an error
	 */
	public <T> T readJson(Class<T> clazz) throws HttpClientException,
			ParseException {
		checkError();
		return JsonMapper.parse(readString(), clazz);
	}

	/**
	 * Reads the content as a JSON object. The content should be a JSON string,
	 * which is converted to an object of the specified type. This method first
	 * checks whether the response code indicates an error. In that case it will
	 * throw an {@link HttpClientException HttpClientException}.
	 *
	 * @return the content as a JSON object
	 * @throws HttpClientException if the response code indicates an error
	 */
	public <T> T readJson(TypeReference<T> typeRef) throws HttpClientException,
			ParseException {
		checkError();
		return JsonMapper.parse(readString(), typeRef);
	}

	/**
	 * Reads the content as a byte array. This method first checks whether the
	 * response code indicates an error. In that case it will throw an {@link
	 * HttpClientException HttpClientException}.
	 *
	 * @return the content as a byte array
	 * @throws HttpClientException if the response code indicates an error
	 */
	public byte[] readBytes() throws HttpClientException {
		checkError();
		return content;
	}

	/**
	 * Checks whether the response code indicates an error. If so, it throws
	 * an {@link HttpClientException HttpClientException}.
	 *
	 * @throws HttpClientException if the response code indicates an error
	 */
	private void checkError() throws HttpClientException {
		if (code / 100 != 2)
			throw new HttpClientException(code, reason, toContentString());
	}
}
