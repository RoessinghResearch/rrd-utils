package nl.rrd.utils.http;

public class DefaultAsyncResult<T> implements AsyncResult<T> {
	private final Object lock = new Object();

	private CallbackRunner runner;

	private SuccessHandler<T> successHandler = null;
	private ErrorHandler<T> errorHandler = null;

	private boolean isSuccess = false;
	private T result = null;
	private Exception error = null;

	public DefaultAsyncResult(CallbackRunner runner) {
		this.runner = runner;
	}

	public void setSuccess(T result) {
		SuccessHandler<T> handler;
		synchronized (lock) {
			if (isSuccess || error != null)
				return;
			isSuccess = true;
			this.result = result;
			handler = successHandler;
		}
		if (handler != null)
			runner.run(() -> handler.onSuccess(result));
	}

	public void setError(Exception error) {
		ErrorHandler<T> handler;
		synchronized (lock) {
			if (isSuccess || error != null)
				return;
			this.error = error;
			handler = errorHandler;
		}
		if (handler != null)
			runner.run(() -> handler.onError(error));
	}

	@Override
	public AsyncResult<T> onSuccess(SuccessHandler<T> handler) {
		boolean isSuccess;
		T result;
		synchronized (lock) {
			this.successHandler = handler;
			isSuccess = this.isSuccess;
			result = this.result;
		}
		if (isSuccess)
			runner.run(() -> handler.onSuccess(result));
		return this;
	}

	@Override
	public AsyncResult<T> onError(ErrorHandler<T> handler) {
		Exception error;
		synchronized (lock) {
			this.errorHandler = handler;
			error = this.error;
		}
		if (error != null)
			runner.run(() -> handler.onError(error));
		return this;
	}

	public interface CallbackRunner {
		void run(Runnable runnable);
	}
}
