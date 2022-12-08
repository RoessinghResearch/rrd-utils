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

package nl.rrd.utils.io;

import nl.rrd.utils.exception.TimeoutException;

public class TimeoutRunner {
	/**
	 * Runs the specified runner and waits until it is completed or the time-out
	 * duration expired. If the time-out duration expires, the runner will be
	 * stopped.
	 *
	 * @param runner the runner
	 * @param timeout the time-out duration in milliseconds
	 * @throws TimeoutException if the runner has timed out
	 */
	public static void run(Runner runner, int timeout) throws TimeoutException {
		RunState runState = new RunState();
		new Thread(() -> waitTimeout(runner, timeout, runState)).start();
		try {
			runner.run();
		} finally {
			synchronized (runState.lock) {
				runState.finished = true;
				runState.lock.notifyAll();
			}
		}
		if (runState.timedOut) {
			throw new TimeoutException(String.format(
					"Time-out of %s ms expired", timeout));
		}
	}

	private static void waitTimeout(Runner runner, int timeout,
			RunState runState) {
		long now = System.currentTimeMillis();
		long end = now + timeout;
		synchronized (runState.lock) {
			while (!runState.finished && now < end) {
				try {
					runState.lock.wait(end - now);
				} catch (InterruptedException ex) {
					throw new RuntimeException("Thread interrupted: " +
							ex.getMessage(), ex);
				}
				now = System.currentTimeMillis();
			}
			if (runState.finished)
				return;
			runState.timedOut = true;
		}
		// time-out expired
		runner.stop();
	}

	private static class RunState {
		public final Object lock = new Object();
		public boolean finished = false;
		public boolean timedOut = false;
	}

	public interface Runner {
		void run();
		void stop();
	}
}
