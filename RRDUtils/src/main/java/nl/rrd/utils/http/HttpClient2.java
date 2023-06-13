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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class can be used to run HTTP requests. At construction it takes a URL.
 * You may configure the client further by setting the HTTP method (default
 * GET) and adding headers and query parameters (if you didn't include them in
 * the URL). After that there are various methods to write data and get the
 * response.
 *
 * <p><ul>
 * <li>{@link #readResponse() readResponse()}:<br />
 * Don't write any data but just get the response immediately.</li>
 * <li>{@link #writeJson(Object) writeJson(()}<br />
 * {@link #writePostParams(Map) writePostParams()}<br />
 * {@link #writeString(String, ContentType, String) writeString()}<br />
 * {@link #writeBytes(byte[], ContentType) writeBytes()}:<br />
 * Write data without streaming and then get the response.</li>
 * <li>{@link #openRequestStream(ContentType, String) openRequestStream()}:<br />
 * Open a stream where you can write request data. Then get the response when
 * you're ready.</li>
 * </ul></p>
 *
 * <p>With the above methods you eventually get a {@link HttpResponse
 * HttpResponse} object with all the data. Each method also has a version where
 * you can specify your own response handler. This allows you to read response
 * data from a stream for example.</p>
 *
 * <p>When you no longer need the client, you should call {@link #close()
 * close()}.</p>
 *
 * <p>Redirects (response code 3xx) are automatically handled. Strings will be
 * read and written as UTF-8 unless specified otherwise.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class HttpClient2 implements Closeable {
	private String method = "GET";
	private String url;
	private Map<String,String> queryParams = new LinkedHashMap<>();
	private Map<String,String> headers = new LinkedHashMap<>();

	private Closeable request = null;

	private static final Object LOCK = new Object();
	private boolean closed = false;

	/**
	 * Constructs a new HTTP client. If you want to use query parameters in the
	 * HTTP request, you can specify the URL without query parameters and then
	 * call {@link #addQueryParam(String, String) addQueryParam()}.
	 * Alternatively you can include the query parameters in the URL.
	 *
	 * @param url the URL
	 */
	public HttpClient2(String url) {
		this.url = url;
	}

	/**
	 * Closes this client. You should always call this method when you no
	 * longer need the client.
	 */
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

	/**
	 * Tries to close the specified Closeable. If it throws an IOException,
	 * the exception will be logged and this method does not throw the
	 * exception.
	 *
	 * @param closeable the closeable
	 */
	private void tryClose(Closeable closeable) {
		Logger logger = AppComponents.getLogger(
				HttpClient2.class.getSimpleName());
		try {
			closeable.close();
		} catch (IOException ex) {
			logger.error("Failed to close: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Sets the HTTP method. The default is GET.
	 *
	 * @param method the HTTP method
	 * @return this client (so you can chain method calls)
	 */
	public HttpClient2 setMethod(String method) {
		this.method = method;
		return this;
	}

	/**
	 * Adds a query parameter. This will be appended to the request URL.
	 *
	 * @param name the parameter name
	 * @param value the parameter value
	 * @return this client (so you can chain method calls)
	 */
	public HttpClient2 addQueryParam(String name, String value) {
		queryParams.put(name, value);
		return this;
	}

	/**
	 * Sets a map with query parameters. They will be appended to the request
	 * URL. This method overwrites any parameters you have added with {@link
	 * #addQueryParam(String, String) addQueryParam()}.
	 *
	 * @param params the query parameters
	 * @return this client (so you can chain method calls)
	 */
	public HttpClient2 setQueryParams(Map<String,String> params) {
		this.queryParams = params;
		return this;
	}

	/**
	 * Adds a header to the HTTP request. If you use one of the write*() methods
	 * to write data without streaming, then the Content-Length and Content-Type
	 * headers will already be added and you should not add them here.
	 *
	 * @param name the header name
	 * @param value the header value
	 * @return this client (so you can chain method calls)
	 */
	public HttpClient2 addHeader(String name, String value) {
		headers.put(name, value);
		return this;
	}

	/**
	 * Sets a map with the HTTP headers. If you use one of the write*() methods
	 * to write data without streaming, then the Content-Length and Content-Type
	 * headers will already be added and you should not add them here. This
	 * method overwrites any headers you have added with {@link
	 * #addHeader(String, String) addHeader()}.
	 *
	 * @param headers the headers
	 * @return this client (so you can chain method calls)
	 */
	public HttpClient2 setHeaders(Map<String,String> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 * Returns the URL including query parameters.
	 *
	 * @return the URL including query parameters
	 */
	public String getUrl() {
		String url = this.url;
		if (queryParams == null || queryParams.isEmpty())
			return url;
		if (url.contains("?"))
			url += "&";
		else
			url += "?";
		url += URLParameters.getParameterString(queryParams);
		return url;
	}

	/**
	 * Creates a request builder that is already configured with the specified
	 * HTTP method, URL and headers.
	 *
	 * @return the request builder
	 */
	private ClassicRequestBuilder createRequestBuilder() {
		ClassicRequestBuilder builder = ClassicRequestBuilder.create(method)
				.setUri(getUrl());
		for (String header : headers.keySet()) {
			builder.addHeader(header, headers.get(header));
		}
		return builder;
	}

	/**
	 * Creates an entity builder that is already configured with the specified
	 * content type and encoding.
	 *
	 * @param contentType the content type, or null if no content type should be
	 * included
	 * @param contentEncoding the content encoding, for example utf-8, or null
	 * if no content encoding should be included
	 * @return the entity builder
	 */
	private EntityBuilder createEntityBuilder(ContentType contentType,
			String contentEncoding) {
		EntityBuilder builder = EntityBuilder.create();
		if (contentType != null)
			builder.setContentType(contentType);
		if (contentEncoding != null)
			builder.setContentEncoding(contentEncoding);
		return builder;
	}

	/**
	 * Reads the response without writing any data. It returns a response object
	 * with all the data, and methods to process the data further. If you want
	 * to receive data in streaming mode, you can call {@link
	 * #readResponse(HttpClientResponseHandler) readResponse(responseHandler)}
	 * with your own response handler.
	 *
	 * @return the response
	 * @throws IOException if a communication error occurs
	 */
	public HttpResponse readResponse() throws IOException {
		return readResponse(new TextResponseHandler());
	}

	/**
	 * Reads the response without writing any data. With this method you specify
	 * your own response handler, so you can receive data in streaming mode for
	 * example. Make sure to close the response and to consume it with
	 * {@link EntityUtils#consume(HttpEntity) EntityUtils.consume()} when you're
	 * done.
	 *
	 * <p>See {@link #readResponse() readResponse()} for an easier non-streaming
	 * version of this method.</p>
	 *
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
	public <T> T readResponse(HttpClientResponseHandler<T> responseHandler)
			throws IOException {
		return executeRequest(null, responseHandler);
	}

	/**
	 * Writes the specified object as a JSON string and then reads the response.
	 * It returns a response object with all the data, and methods to process
	 * the data further. If you want to receive data in streaming mode, you can
	 * call {@link #writeJson(Object, HttpClientResponseHandler)
	 * writeJson(obj, responseHandler)} with your own response handler.
	 *
	 * @param obj the object to write
	 * @return the response
	 * @throws IOException if a communication error occurs
	 */
	public HttpResponse writeJson(Object obj) throws IOException {
		return writeJson(obj, new TextResponseHandler());
	}

	/**
	 * Writes the specified object as a JSON string and then reads the response.
	 * With this method you specify your own response handler, so you can
	 * receive data in streaming mode for example. Make sure to close the
	 * response and to consume it with {@link EntityUtils#consume(HttpEntity)
	 * EntityUtils.consume()} when you're done.
	 *
	 * <p>See {@link #writeJson(Object) writeJson(obj)} for an easier
	 * non-streaming version of this method.</p>
	 *
	 * @param obj the object to write
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
	public <T> T writeJson(Object obj,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		String json = JsonMapper.generate(obj);
		HttpEntity entity = createEntityBuilder(
				ContentType.APPLICATION_JSON, "utf-8")
				.setText(json)
				.build();
		return executeRequest(entity, responseHandler);
	}

	/**
	 * Writes the specified POST parameters as a URL-encoded parameter string
	 * with content type application/x-www-form-urlencoded, and then reads the
	 * response. It returns a response object with all the data, and methods to
	 * process the data further. If you want to receive data in streaming mode,
	 * you can call {@link #writePostParams(Map, HttpClientResponseHandler)
	 * writePostParams(postParams, responseHandler)} with your own response
	 * handler.
	 *
	 * @param postParams the POST parameters
	 * @return the response
	 * @throws IOException if a communication error occurs
	 */
	public HttpResponse writePostParams(Map<String,String> postParams)
			throws IOException {
		return writePostParams(postParams, new TextResponseHandler());
	}

	/**
	 * Writes the specified POST parameters as a URL-encoded parameter string
	 * with content type application/x-www-form-urlencoded, and then reads the
	 * response. With this method you specify your own response handler, so you
	 * can receive data in streaming mode for example. Make sure to close the
	 * response and to consume it with {@link EntityUtils#consume(HttpEntity)
	 * EntityUtils.consume()} when you're done.
	 *
	 * <p>See {@link #writePostParams(Map) writePostParams(postParams)} for an
	 * easier non-streaming version of this method.</p>
	 *
	 * @param postParams the POST parameters
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
	public <T> T writePostParams(Map<String,String> postParams,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		StringBuilder data = new StringBuilder();
		for (String key : postParams.keySet()) {
			if (data.length() > 0)
				data.append("&");
			data.append(key);
			data.append("=");
			String value = postParams.get(key);
			// URLEncoder.encode(String, Charset) not supported in Android 26
			data.append(URLEncoder.encode(value,
					StandardCharsets.UTF_8.name()));
		}
		HttpEntity entity = createEntityBuilder(
				ContentType.APPLICATION_FORM_URLENCODED, "utf-8")
				.setText(data.toString())
				.build();
		return executeRequest(entity, responseHandler);
	}

	/**
	 * Writes a string with the specified content type and encoding, for example
	 * text/plain and utf-8, and then reads the response. It returns a response
	 * object with all the data, and methods to process the data further. If you
	 * want to receive data in streaming mode, you can call {@link
	 * #writeString(String, ContentType, String, HttpClientResponseHandler)
	 * writeString(content, contentType, contentEncoding, responseHandler)} with
	 * your own response handler.
	 *
	 * @param content the content string
	 * @param contentType the content type, for example text/plain, or null if
	 * no content type should be included
	 * @param contentEncoding the content encoding, for example utf-8, or null
	 * if no content encoding should be included
	 * @return the response
	 * @throws IOException if a communication error occurs
	 */
	public HttpResponse writeString(String content, ContentType contentType,
			String contentEncoding) throws IOException {
		return writeString(content, contentType, contentEncoding,
				new TextResponseHandler());
	}

	/**
	 * Writes a string with the specified content type and encoding, for example
	 * text/plain and utf-8, and then reads the response. With this method you
	 * specify your own response handler, so you can receive data in streaming
	 * mode for example. Make sure to close the response and to consume it with
	 * {@link EntityUtils#consume(HttpEntity) EntityUtils.consume()} when you're
	 * done.
	 *
	 * <p>See {@link #writeString(String, ContentType, String)
	 * writeString(content, contentType, contentEncoding)} for an easier
	 * non-streaming version of this method.</p>
	 *
	 * @param content the content string
	 * @param contentType the content type, for example text/plain, or null if
	 * no content type should be included
	 * @param contentEncoding the content encoding, for example utf-8, or null
	 * if no content encoding should be included
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
	public <T> T writeString(String content, ContentType contentType,
			String contentEncoding,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		HttpEntity entity = createEntityBuilder(contentType, contentEncoding)
				.setText(content).build();
		return executeRequest(entity, responseHandler);
	}

	/**
	 * Writes a byte array with the specified content type, for example
	 * application/octet-stream, and then reads the response. It returns a
	 * response object with all the data, and methods to process the data
	 * further. If you want to receive data in streaming mode, you can call
	 * {@link #writeBytes(byte[], ContentType, HttpClientResponseHandler)
	 * writeBytes(bs, contentType, responseHandler)} with your own response
	 * handler.
	 *
	 * @param bs the byte array
	 * @param contentType the content type, for example
	 * application/octet-stream, or null if no content type should be included
	 * @return the response
	 * @throws IOException if a communication error occurs
	 */
	public HttpResponse writeBytes(byte[] bs, ContentType contentType)
			throws IOException {
		return writeBytes(bs, contentType, new TextResponseHandler());
	}

	/**
	 * Writes a byte array with the specified content type, for example
	 * application/octet-stream, and then reads the response. With this method
	 * you specify your own response handler, so you can receive data in
	 * streaming mode for example. Make sure to close the response and to
	 * consume it with {@link EntityUtils#consume(HttpEntity)
	 * EntityUtils.consume()} when you're done.
	 *
	 * <p>See {@link #writeBytes(byte[], ContentType)
	 * writeBytes(bs, contentType)} for an easier non-streaming version of this
	 * method.</p>
	 *
	 * @param bs the byte array
	 * @param contentType the content type, for example
	 * application/octet-stream, or null if no content type should be included
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
	public <T> T writeBytes(byte[] bs, ContentType contentType,
			HttpClientResponseHandler<T> responseHandler) throws IOException {
		HttpEntity entity = createEntityBuilder(contentType, null)
				.setBinary(bs).build();
		return executeRequest(entity, responseHandler);
	}

	/**
	 * Opens a request that allows you to write request data to a stream. It
	 * returns a {@link StreamingRequest StreamingRequest} object. From that
	 * object, you can first get the stream with {@link
	 * StreamingRequest#getStream() getStream()} and write data to it. Then you
	 * can call {@link StreamingRequest#execute() execute()}.
	 *
	 * <p>The method {@link StreamingRequest#execute() execute()} returns a
	 * response object with all the data, and methods to process the data
	 * further. If you want to receive data in streaming mode, you can call
	 * {@link #openRequestStream(ContentType, String, HttpClientResponseHandler)
	 * openRequestStream(contentType, contentEncoding, responseHandler)} with
	 * your own response handler.</p>
	 *
	 * @param contentType the content type, or null if no content type should be
	 * included
	 * @param contentEncoding the content encoding, for example utf-8, or null
	 * if no content encoding should be included
	 * @return the streaming request
	 * @throws IOException if a communication error occurs
	 */
	public StreamingRequest<HttpResponse> openRequestStream(
			ContentType contentType, String contentEncoding)
			throws IOException {
		return openRequestStream(contentType, contentEncoding,
				new TextResponseHandler());
	}

	/**
	 * Opens a request that allows you to write request data to a stream. It
	 * returns a {@link StreamingRequest StreamingRequest} object. From that
	 * object, you can first get the stream with {@link
	 * StreamingRequest#getStream() getStream()} and write data to it. Then you
	 * can call {@link StreamingRequest#execute() execute()}.
	 *
	 * <p>With this method you specify your own response handler, so you can
	 * receive data in streaming mode for example. Make sure to close the
	 * response and to consume it with {@link EntityUtils#consume(HttpEntity)
	 * EntityUtils.consume()} when you're done.</p>
	 *
	 * <p>See {@link #openRequestStream(ContentType, String)
	 * openRequestStream(contentType, contentEncoding)} for an easier version
	 * of this method without streaming of the response data.</p>
	 *
	 * @param contentType the content type, or null if no content type should be
	 * included
	 * @param contentEncoding the content encoding, for example utf-8, or null
	 * if no content encoding should be included
	 * @return the streaming request
	 * @throws IOException if a communication error occurs
	 */
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

	/**
	 * Builds the request with the specified content entity and then executes
	 * the request.
	 *
	 * @param entity the entity with the request content data, or null
	 * @param responseHandler the response handler
	 * @return the result of the response handler
	 * @param <T> the result type of the response handler
	 * @throws IOException if a communication error occurs
	 */
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

	/**
	 * This class can build, execute and close a request with fixed-length data.
	 *
	 * @param <T> the result of the response handler
	 */
	private class FixedLengthRequest<T> implements Closeable {
		private CloseableHttpClient client;
		private HttpEntity entity;

		private HttpClientResponseHandler<T> responseHandler;
		private ClassicHttpRequest request;

		private final Object LOCK = new Object();
		private boolean closed = false;

		/**
		 * Constructs a new fixed-length request.
		 *
		 * @param entity the entity with the request content data, or null
		 * @param responseHandler the response handler
		 */
		private FixedLengthRequest(HttpEntity entity,
				HttpClientResponseHandler<T> responseHandler) {
			this.entity = entity;
			this.responseHandler = responseHandler;
			ClassicRequestBuilder requestBuilder = createRequestBuilder();
			if (entity != null)
				requestBuilder.setEntity(entity);
			request = requestBuilder.build();
			client = HttpClientBuilder.create().build();
		}

		/**
		 * Executes this request.
		 *
		 * @return the result of the response handler
		 * @throws IOException if a communication error occurs
		 */
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

	/**
	 * This class is used for requests where you want to stream data to the
	 * request content. An instance is obtained from {@link
	 * #openRequestStream(ContentType, String) openRequestStream()}.
	 *
	 * <p>When you get this object, you should first call {@link #getStream()
	 * getStream()} and write the request data to that stream. Then you should
	 * call {@link #execute() execute()} to get the response.</p>
	 *
	 * @param <T> the result of the response handler
	 */
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

		/**
		 * Constructs a new streaming request.
		 *
		 * @param contentType the content type, or null if no content type
		 * should be included
		 * @param contentEncoding the content encoding, for example utf-8, or
		 * null if no content encoding should be included
		 * @param responseHandler the response handler
		 * @throws IOException if a communication error occurs
		 */
		private StreamingRequest(ContentType contentType,
				String contentEncoding,
				HttpClientResponseHandler<T> responseHandler)
				throws IOException {
			try {
				EntityBuilder entityBuilder = createEntityBuilder(contentType,
						contentEncoding);
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

		/**
		 * Returns the stream that you can use to write the request data.
		 *
		 * @return the stream that you can use to write the request data
		 */
		public OutputStream getStream() {
			return output;
		}

		/**
		 * Executes the request. This method is run in a worker thread, so it's
		 * possible to stream data from the main thread.
		 */
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

		/**
		 * Executes the request. You should call this method after you have
		 * written the request data to the stream that you got from {@link
		 * #getStream() getStream()}.
		 *
		 * @return the response
		 * @throws IOException if a communication error occurs
		 */
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

	/**
	 * This is the default response handler that gets all the data from the
	 * response and returns it as an {@link HttpResponse HttpResponse} object.
	 */
	private static class TextResponseHandler implements
			HttpClientResponseHandler<HttpResponse> {
		@Override
		public HttpResponse handleResponse(ClassicHttpResponse response)
				throws HttpException, IOException {
			try (response) {
				return doHandleResponse(response);
			}
		}

		private HttpResponse doHandleResponse(ClassicHttpResponse response)
				throws HttpException, IOException {
			HttpResponse result = new HttpResponse();
			result.setCode(response.getCode());
			result.setReason(response.getReasonPhrase());
			for (Header header : response.getHeaders()) {
				result.addHeader(header.getName().toLowerCase(),
						header.getValue());
			}
			try (HttpEntity entity = response.getEntity()) {
				result.setContentType(entity.getContentType());
				result.setContentEncoding(entity.getContentEncoding());
				try (InputStream input = entity.getContent()) {
					result.setContent(FileUtils.readFileBytes(input));
				}
				EntityUtils.consume(entity);
			}
			return result;
		}
	}
}
