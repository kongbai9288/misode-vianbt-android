package net.querz.mcaselector.logging;

/**
 * Dependency-free stand-in for {@code org.apache.logging.log4j.Logger}.
 *
 * MCA Selector logs through log4j everywhere; on Android that is dead weight
 * (it also drags in a GraalVM annotation processor), so calls are forwarded to
 * {@code android.util.Log} when running on a device.
 *
 * The sink is resolved reflectively so that plain JVM unit tests do not load
 * the android.jar stub, which throws "not mocked" the moment it is touched.
 */
public final class Logger {

	/** Log levels, mirroring android.util.Log. */
	static final int V = 2, D = 3, I = 4, W = 5, E = 6, A = 7;

	private static final String TAG = "MCA";

	private static Sink sink;

	private final String name;

	Logger(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/** Sink used when not running on Android (unit tests, desktop tooling). */
	interface Sink {
		void log(int level, String tag, String message, Throwable t);
	}

	static synchronized Sink sink() {
		if (sink == null) {
			sink = resolveSink();
		}
		return sink;
	}

	private static Sink resolveSink() {
		try {
			Class<?> androidLog = Class.forName("android.util.Log");
			// Only use it if it is a real implementation, not the stub jar.
			androidLog.getMethod("isLoggable", String.class, int.class);
			return new AndroidSink(androidLog);
		} catch (Throwable notOnAndroid) {
			return new StdOutSink();
		}
	}

	static final class AndroidSink implements Sink {

		private final Class<?> logClass;

		AndroidSink(Class<?> logClass) {
			this.logClass = logClass;
		}

		@Override
		public void log(int level, String tag, String message, Throwable t) {
			try {
				String m = message;
				if (t != null) {
					m = message + "\n" + android.util.Log.getStackTraceString(t);
				}
				android.util.Log.println(level, tag, m);
			} catch (Throwable ignored) {
				// Never let logging break the caller.
			}
		}
	}

	static final class StdOutSink implements Sink {

		@Override
		public void log(int level, String tag, String message, Throwable t) {
			StringBuilder sb = new StringBuilder();
			sb.append(levelChar(level)).append('/').append(tag).append(": ").append(message);
			if (t != null) {
				sb.append('\n').append(t);
			}
			System.out.println(sb);
		}

		private static char levelChar(int level) {
			switch (level) {
				case V: return 'V';
				case D: return 'D';
				case I: return 'I';
				case W: return 'W';
				case E: return 'E';
				default: return 'A';
			}
		}
	}

	private void log(int level, String message, Throwable t) {
		sink().log(level, TAG, name + ": " + message, t);
	}

	private static String fmt(String message, Object... args) {
		if (args == null || args.length == 0) {
			return message;
		}
		try {
			return String.format(message, args);
		} catch (Exception e) {
			return message;
		}
	}

	public void trace(String message) { log(V, message, null); }
	public void trace(String message, Object... args) { log(V, fmt(message, args), null); }
	public void debug(String message) { log(D, message, null); }
	public void debug(String message, Object... args) { log(D, fmt(message, args), null); }
	public void info(String message) { log(I, message, null); }
	public void info(String message, Object... args) { log(I, fmt(message, args), null); }
	public void warn(String message) { log(W, message, null); }
	public void warn(String message, Object... args) { log(W, fmt(message, args), null); }
	public void warn(String message, Throwable t) { log(W, message, t); }
	public void error(String message) { log(E, message, null); }
	public void error(String message, Object... args) { log(E, fmt(message, args), null); }
	public void error(String message, Throwable t) { log(E, message, t); }
	public void warn(Throwable t) { log(W, String.valueOf(t.getMessage()), t); }
	public void error(Throwable t) { log(E, String.valueOf(t.getMessage()), t); }
	public void fatal(String message) { log(A, message, null); }
	public void fatal(String message, Object... args) { log(A, fmt(message, args), null); }
	public void fatal(String message, Throwable t) { log(A, message, t); }
	public void fatal(String message, String s, Throwable t) { log(A, message + " " + s, t); }

	public boolean isDebugEnabled() { return true; }
	public boolean isTraceEnabled() { return true; }
}
