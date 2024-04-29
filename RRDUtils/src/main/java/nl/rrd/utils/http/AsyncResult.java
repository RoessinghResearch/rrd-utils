package nl.rrd.utils.http;

public interface AsyncResult<T> {
	AsyncResult<T> onSuccess(SuccessHandler<T> handler);

	AsyncResult<T> onError(ErrorHandler<T> handler);

	interface SuccessHandler<T> {
		void onSuccess(T result);
	}

	interface ErrorHandler<T> {
		void onError(Exception error);
	}
}
