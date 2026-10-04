package net.querz.mcaselector.config;

/**
 * Minimal stand-in for the desktop {@code ConfigProvider}.
 *
 * The upstream class pulls in JavaFX colours, translations and the whole
 * settings UI. None of that applies to an embedded library, so this only keeps
 * what the core engine actually reads: the world directories.
 */
public final class ConfigProvider {

	public static final WorldConfig WORLD = new WorldConfig();

	private ConfigProvider() {}
}
