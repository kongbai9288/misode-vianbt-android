package net.querz.mcaselector.logging;

import android.util.Log;

/**
 * Tiny stand-in for {@code org.apache.logging.log4j.Logger}.
 *
 * MCA Selector logs through log4j everywhere; on Android that is dead weight
 * (and pulls in a GraalVM annotation processor), so every call is forwarded to
 * {@link android.util.Log}. Tagged "MCA" so it can be filtered in logcat.
 */
public final class Logger {

	private static final String TAG = "MCA";

	private final String name;

	Logger(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private String prefix() {
		return name + ": ";
	}

	public void trace(String message) {
		Log.v(TAG, prefix() + message);
	}

	public void debug(String message) {
		Log.d(TAG, prefix() + message);
	}

	public void info(String message) {
		Log.i(TAG, prefix() + message);
	}

	public void warn(String message) {
		Log.w(TAG, prefix() + message);
	}

	public void error(String message) {
		Log.e(TAG, prefix() + message);
	}

	public void error(String message, Throwable t) {
		Log.e(TAG, prefix() + message, t);
	}

	public void fatal(String message) {
		Log.wtf(TAG, prefix() + message);
	}

	public void fatal(String message, Throwable t) {
		Log.wtf(TAG, prefix() + message, t);
	}

	public boolean isDebugEnabled() {
		return Log.isLoggable(TAG, Log.DEBUG);
	}

	public boolean isTraceEnabled() {
		return Log.isLoggable(TAG, Log.VERBOSE);
	}
}
