package net.querz.mcaselector.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stand-in for {@code org.apache.logging.log4j.LogManager}. */
public final class LogManager {

	private static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();

	private LogManager() {}

	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getSimpleName());
	}

	public static Logger getLogger(String name) {
		return LOGGERS.computeIfAbsent(name, Logger::new);
	}
}
