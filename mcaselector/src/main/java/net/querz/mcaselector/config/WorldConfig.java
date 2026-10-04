package net.querz.mcaselector.config;

import net.querz.mcaselector.io.WorldDirectories;
import java.io.File;

/**
 * Minimal stand-in for the desktop {@code WorldConfig}.
 *
 * Holds only the region / poi / entities directories that
 * {@code FileHelper} needs to build .mca paths.
 */
public class WorldConfig {

	private WorldDirectories worldDirs = new WorldDirectories();

	public WorldDirectories getWorldDirs() {
		return worldDirs;
	}

	public void setWorldDirs(WorldDirectories worldDirs) {
		this.worldDirs = worldDirs == null ? new WorldDirectories() : worldDirs;
	}

	public File getRegionDir() {
		return worldDirs.getRegion();
	}

	public File getPoiDir() {
		return worldDirs.getPoi();
	}

	public File getEntitiesDir() {
		return worldDirs.getEntities();
	}
}
