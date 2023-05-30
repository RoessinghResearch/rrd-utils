package nl.rrd.utils.http;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
	private int code;
	private String reason;
	private Map<String,String> headers = new LinkedHashMap<>();
	private String contentType;
	private String contentEncoding;
	private byte[] content;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Map<String,String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String,String> headers) {
		this.headers = headers;
	}

	public void addHeader(String name, String value) {
		headers.put(name, value);
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String toContentString() {
		Charset charset = StandardCharsets.UTF_8;
		if (contentEncoding != null && Charset.isSupported(contentEncoding)) {
			charset = Charset.forName(contentEncoding);
		}
		ByteBuffer bb = ByteBuffer.wrap(content);
		return charset.decode(bb).toString();
	}

	public String readString() throws HttpClientException {
		checkError();
		return toContentString();
	}

	public <T> T readJson(Class<T> clazz) throws HttpClientException,
			ParseException {
		checkError();
		return JsonMapper.parse(readString(), clazz);
	}

	public <T> T readJson(TypeReference<T> typeRef) throws HttpClientException,
			ParseException {
		checkError();
		return JsonMapper.parse(readString(), typeRef);
	}

	public byte[] readBytes() throws HttpClientException {
		checkError();
		return content;
	}

	private void checkError() throws HttpClientException {
		if (code / 100 != 2)
			throw new HttpClientException(code, reason, toContentString());
	}
}
