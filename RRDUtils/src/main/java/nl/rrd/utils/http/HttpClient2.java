package nl.rrd.utils.http;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.io.FileUtils;
import nl.rrd.utils.json.JsonMapper;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpClient2 implements Closeable {
	private String method = "GET";
	private String url;
	private Map<String,String> queryParams = new LinkedHashMap<>();
	private Map<String,String> headers = new LinkedHashMap<>();

	private Closeable request = null;

	private static final Object LOCK = new Object();
	private boolean closed = false;

	public HttpClient2(String url) {
		this.url = url;
	}

	@Override
	public void close() {
		synchronized (LOCK) {
			if (closed)
				return;
			closed = true;
			if (request != null) {
				tryClose(request);
				request = null;
			}
		}
	}

	private void tryClose(Closeable closeable) {
		Logger logger = AppComponents.getLogger(
				HttpClient2.class.getSimpleName());
		try {
			closeable.close();
		} catch (IOException ex) {
			logger.error("Failed to close: " + ex.getMessage(), ex);
		}
	}

	public HttpClient2 setMethod(String method) {
		this.method = method;
		return this;
	}

	public HttpClient2 addQueryParam(String name, String value) {
		queryParams.put(name, value);
		return this;
	}

	public HttpClient2 setQueryParams(Map<String,String> params) {
		this.queryParams = params;
		return this;
	}

	public HttpClient2 addHeader(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public HttpClient2 setHeaders(Map<String,String> headers) {
		this.headers = headers;
		return this;
	}

	public String getUrl() {
		String url = this.url;
		if (queryParams != null && !queryParams.isEmpty())
			url += "?" + URLParameters.getParameterString(queryParams);
		return url;
	}

	private ClassicRequestBuilder createRequestBuilder() {
		ClassicRequestBuilder builder = ClassicRequestBuilder.create(method)
				.setUri(getUrl());
		for (String header : headers.keySet()) {
			builder.addHeader(header, headers.get(header));
		}
		return builder;
	}

	public StreamingRequest<Response> openRequestStream(ContentType contentType,
			String contentEncoding) throws IOException {
		return openRequestStream(contentType, contentEncoding,
				new TextResponseHandler());
	}

	public <T> StreamingRequest<T> openRequestStream(ContentType contentType,
			String contentEncoding,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		StreamingRequest<T> request = new StreamingRequest<>(contentType,
				contentEncoding, responseHandler);
		synchronized (LOCK) {
			if (closed) {
				request.close();
				throw new IOException("Client closed");
			}
			this.request = request;
			return request;
		}
	}

	public Response writeJson(Object obj) throws IOException {
		return writeJson(obj, new TextResponseHandler());
	}

	public <T> T writeJson(Object obj,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		String json = JsonMapper.generate(obj);
		HttpEntity entity = EntityBuilder.create()
				.setContentType(ContentType.APPLICATION_JSON)
				.setContentEncoding("utf-8")
				.setText(json)
				.build();
		return executeRequest(entity, responseHandler);
	}

	public Response writePostParams(Map<String,String> postParams)
			throws IOException {
		return writePostParams(postParams, new TextResponseHandler());
	}

	public <T> T writePostParams(Map<String,String> postParams,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		StringBuilder data = new StringBuilder();
		for (String key : postParams.keySet()) {
			if (data.length() > 0)
				data.append("&");
			data.append(key);
			data.append("=");
			String value = postParams.get(key);
			data.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		}
		HttpEntity entity = EntityBuilder.create()
				.setContentType(ContentType.APPLICATION_FORM_URLENCODED)
				.setContentEncoding("utf-8")
				.setText(data.toString())
				.build();
		return executeRequest(entity, responseHandler);
	}

	private <T> T executeRequest(HttpEntity entity,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		FixedLengthRequest<T> request = new FixedLengthRequest<>(entity,
				responseHandler);
		synchronized (LOCK) {
			if (closed) {
				request.close();
				throw new IOException("Client closed");
			}
			this.request = request;
		}
		try {
			return request.execute();
		} finally {
			close();
		}
	}

	private class FixedLengthRequest<T> implements Closeable {
		private CloseableHttpClient client;
		private HttpEntity entity;

		private HttpClientResponseHandler<T> responseHandler;
		private ClassicHttpRequest request;

		private final Object LOCK = new Object();
		private boolean closed = false;

		private FixedLengthRequest(HttpEntity entity,
				HttpClientResponseHandler<T> responseHandler) {
			this.entity = entity;
			this.responseHandler = responseHandler;
			ClassicRequestBuilder requestBuilder = createRequestBuilder();
			request = requestBuilder.setEntity(entity).build();
			client = HttpClientBuilder.create().build();
		}

		public T execute() throws IOException {
			try {
				return client.execute(request, responseHandler);
			} finally {
				close();
			}
		}

		@Override
		public void close() {
			synchronized (LOCK) {
				if (closed)
					return;
				closed = true;
				LOCK.notifyAll();
				if (client != null) {
					tryClose(client);
					client = null;
				}
				if (entity != null) {
					tryClose(entity);
					entity = null;
				}
			}
		}
	}

	public class StreamingRequest<T> implements Closeable {
		private CloseableHttpClient client;
		private HttpEntity entity;
		private PipedInputStream input;
		private PipedOutputStream output;

		private HttpClientResponseHandler<T> responseHandler;
		private ClassicHttpRequest request;

		private T executeResult = null;
		private IOException executeError = null;
		private boolean executeCompleted = false;

		private final Object LOCK = new Object();
		private boolean closed = false;

		private StreamingRequest(ContentType contentType,
				String contentEncoding,
				HttpClientResponseHandler<T> responseHandler)
				throws IOException {
			try {
				EntityBuilder entityBuilder = EntityBuilder.create();
				if (contentType != null)
					entityBuilder.setContentType(contentType);
				if (contentEncoding != null)
					entityBuilder.setContentEncoding(contentEncoding);
				input = new PipedInputStream();
				output = new PipedOutputStream(input);
				entityBuilder.setStream(input);
				entity = entityBuilder.build();
				this.responseHandler = responseHandler;
				ClassicRequestBuilder requestBuilder = createRequestBuilder();
				request = requestBuilder.setEntity(entity).build();
				client = HttpClientBuilder.create().build();
				new Thread(this::runRequestThread).start();
			} catch (IOException ex) {
				close();
				throw ex;
			}
		}

		public OutputStream getStream() {
			return output;
		}

		private void runRequestThread() {
			CloseableHttpClient client;
			synchronized (LOCK) {
				if (closed)
					return;
				client = this.client;
			}
			try {
				executeResult = client.execute(request, responseHandler);
			} catch (IOException ex) {
				executeError = ex;
			} finally {
				synchronized (LOCK) {
					executeCompleted = true;
					LOCK.notifyAll();
				}
			}
		}

		public T execute() throws IOException {
			synchronized (LOCK) {
				if (closed)
					throw new IOException("Client closed");
				output.close();
				while (!closed && !executeCompleted) {
					try {
						LOCK.wait();
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex.getMessage(), ex);
					}
				}
				if (closed)
					throw new IOException("Client closed");
			}
			close();
			if (executeError != null)
				throw executeError;
			return executeResult;
		}

		@Override
		public void close() {
			synchronized (LOCK) {
				if (closed)
					return;
				closed = true;
				LOCK.notifyAll();
				if (client != null) {
					tryClose(client);
					client = null;
				}
				if (output != null) {
					tryClose(output);
					output = null;
				}
				if (entity != null) {
					tryClose(entity);
					entity = null;
				}
				if (input != null) {
					tryClose(input);
					input = null;
				}
			}
		}
	}

	public static class TextResponseHandler implements
			HttpClientResponseHandler<Response> {
		@Override
		public Response handleResponse(ClassicHttpResponse response)
				throws HttpException, IOException {
			try (response) {
				return doHandleResponse(response);
			}
		}

		private Response doHandleResponse(ClassicHttpResponse response)
				throws HttpException, IOException {
			Response result = new Response();
			result.code = response.getCode();
			result.reason = response.getReasonPhrase();
			result.headers = new LinkedHashMap<>();
			for (Header header : response.getHeaders()) {
				result.headers.put(header.getName().toLowerCase(),
						header.getValue());
			}
			HttpEntity entity = response.getEntity();
			String encoding = entity.getContentEncoding();
			Charset charset = StandardCharsets.UTF_8;
			if (encoding != null && Charset.isSupported(encoding)) {
				charset = Charset.forName(encoding);
			}
			try (InputStream input = entity.getContent()) {
				result.content = FileUtils.readFileString(input, charset);
			}
			EntityUtils.consume(entity);
			return result;
		}
	}

	public static class Response {
		private int code;
		private String reason;
		private Map<String,String> headers;
		private String content;

		private Response() {
		}

		public int getCode() {
			return code;
		}

		public String getReason() {
			return reason;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public String getContent() {
			return content;
		}
	}
}
