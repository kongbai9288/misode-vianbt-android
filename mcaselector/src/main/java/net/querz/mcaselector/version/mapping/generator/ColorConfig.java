package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.mapping.color.*;
import net.querz.mcaselector.version.mapping.minecraft.*;
import net.querz.mcaselector.version.mapping.util.*;
import net.querz.nbt.CompoundTag;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ColorConfig {

	@SerializedName("states") public BlockStates states;
	@SerializedName("colors") public ColorMapping colors;
	@SerializedName("tints") public BiomeColors tints;
	public transient ColorMapping.TintCache tintCache;
	public transient ColorMapping.LegacyTintCache legacyTintCache;

	public static final ColorProperties colorProperties = FileHelper.loadFromResource(
			"mapping/color_properties.json",
			ColorProperties::load);

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(BitSet.class, new BitSetAdapter())
			.registerTypeHierarchyAdapter(BlockColor.class, new BlockColor.BlockColorAdapter())
			.registerTypeAdapter(SingleStateColors.class, new SingleStateColors.SingleStateColorsAdapter())
			.registerTypeAdapter(BlockStates.class, new BlockStates.BlockStatesTypeAdapter())
			.registerTypeAdapter(BiomeColors.class, new BiomeColors.BiomeColorsTypeAdapter())
			.registerTypeAdapterFactory(ColorMapping.ColorMappingTypeAdapterFactory.getColorMappingTypeAdapterFactory())
			.registerTypeAdapterFactory(StateColors.StateColorsTypeAdapterFactory.getStateColorsTypeAdapterFactory())
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.enableComplexMapKeySerialization()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();

	public ColorConfig() {}

	public static ColorConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static ColorConfig load(Reader reader) {
		ColorConfig cfg = GSON.fromJson(reader, ColorConfig.class);
		cfg.tintCache = cfg.colors.createTintCache(cfg.tints);
		return cfg;
	}

	public static ColorConfig loadLegacy(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return loadLegacy(reader);
		}
	}

	public static ColorConfig loadLegacy(Reader reader) {
		ColorConfig cfg = GSON.fromJson(reader, ColorConfig.class);
		cfg.legacyTintCache = cfg.colors.createLegacyTintCache(cfg.tints);
		return cfg;
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		// Files.writeString is Java 11+; write the bytes directly instead so the
		// code also runs on older Android runtimes.
		Files.write(path, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public BlockColor getColor(String name, String biome, CompoundTag tag) {
		BitSet blockState = states.getState(tag);
		BlockColor blockColor = colors.getBlockColor(name, blockState);
		if ((blockColor.properties & BlockColor.TINTED) > 0) {
			BlockColor tinted = tintCache.getColor(name, biome, blockState);
			return tinted == null ? tintCache.getColor(name, "minecraft:plains", null) : tinted;
		}
		return blockColor;
	}

	public int getLegacyColor(String name, int biome, CompoundTag tag) {
		BitSet blockState = states.getState(tag);
		BlockColor blockColor = colors.getBlockColor(name, blockState);
		if ((blockColor.properties & BlockColor.TINTED) > 0) {
			return legacyTintCache.getColor(name, biome, blockState);
		}
		return blockColor.color;
	}


	private String trimNS(String s) {
		return s.substring(s.indexOf(':') + 1);
	}

	private JsonObject readJSONAsset(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private Map<String, String> resolveTextureMapping(String model, Path assetBlockmodels) throws IOException {
		Path modelPath = assetBlockmodels.resolve(model + ".json");
		Map<String, String> mapping = new HashMap<>();
		Path parent = modelPath;

		while (parent != null) {
			try (BufferedReader reader = Files.newBufferedReader(parent)) {
				JsonObject pRoot = JsonParser.parseReader(reader).getAsJsonObject();
				if (pRoot.has("textures")) {
					for (Map.Entry<String, JsonElement> entry : pRoot.getAsJsonObject("textures").entrySet()) {
						if (entry.getValue().isJsonObject()) {
							mapping.putIfAbsent(entry.getKey(), entry.getValue().getAsJsonObject().get("sprite").getAsString());
						} else {
							mapping.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
						}
					}
				}
				if (!pRoot.has("parent")) {
					parent = null;
				} else {
					String ps = trimNS(pRoot.get("parent").getAsString());
					parent = assetBlockmodels.resolve(ps + ".json");
				}
			}
		}
		return mapping;
	}

	private String getTopTextureName(Map<String, String> mapping) {
		if (mapping.containsKey("top")) {
			return resolveTextureReference("top", mapping);
		} else if (mapping.containsKey("up")) {
			return resolveTextureReference("up", mapping);
		} else if (mapping.containsKey("all")) {
			return resolveTextureReference("all", mapping);
		} else if (mapping.containsKey("particle")) {
			return resolveTextureReference("particle", mapping);
		}
		throw new IllegalStateException("block doesn't have top texture");
	}

	private String resolveTextureReference(String key, Map<String, String> mapping) {
		String t = mapping.get(key);
		while (t.startsWith("#")) {
			t = mapping.get(t.substring(1));
		}
		return t;
	}



	public record ColorProperties(
			@SerializedName("air") Set<String> air,
			@SerializedName("transparent") Set<String> transparent,
			@SerializedName("grass_tint") Set<String> grassTint,
			@SerializedName("foliage_tint") Set<String> foliageTint,
			@SerializedName("dry_foliage_tint") Set<String> dryFoliageTint,
			@SerializedName("water") Set<String> water,
			@SerializedName("foliage") Set<String> foliage,
			@SerializedName("static_tint") Map<String, Integer> staticTint,
			@SerializedName("static_color") Map<String, Integer> staticColor) {

		private static final Gson GSON = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(Integer.class, new HexColorAdapter())
				.create();

		public static ColorProperties load(Path path) throws IOException {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				return load(reader);
			}
		}

		public static ColorProperties load(Reader reader) throws IOException {
			return GSON.fromJson(reader, ColorProperties.class);
		}

		public boolean isAir(String blockName) {
			return air.contains(blockName);
		}

		public int getTransparent(String blockName) {
			return transparent.contains(blockName) ? BlockColor.TRANSPARENT : 0;
		}

		public int getGrassTint(String blockName) {
			return grassTint.contains(blockName) ? BlockColor.GRASS_TINT : 0;
		}

		public int getFoliageTint(String blockName) {
			return foliageTint.contains(blockName) ? BlockColor.FOLIAGE_TINT : 0;
		}

		public int getWater(String blockName) {
			return water.contains(blockName) ? BlockColor.WATER : 0;
		}

		public int getFoliage(String blockName) {
			return foliage.contains(blockName) ? BlockColor.FOLIAGE : 0;
		}

		public int getStaticTint(String blockName) {
			return staticTint.containsKey(blockName) ? BlockColor.STATIC_TINT : 0;
		}

		public int getStaticColor(String blockName) {
			return staticColor.containsKey(blockName) ? BlockColor.STATIC_COLOR : 0;
		}

		public int getDryFoliageTint(String blockName) {
			return dryFoliageTint.contains(blockName) ? BlockColor.DRY_FOLIAGE_TINT : 0;
		}

		public int get(String blockName) {
			return getTransparent(blockName)
					| getGrassTint(blockName)
					| getFoliageTint(blockName)
					| getWater(blockName)
					| getFoliage(blockName)
					| getStaticTint(blockName)
					| getStaticColor(blockName)
					| getDryFoliageTint(blockName);
		}
	}
}
