package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.util.exception.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structure name registry for filters.
 *
 * The desktop version additionally loads JavaFX icons for the overlay menu.
 * Icons are a UI concern, so this keeps only the name data the filters need:
 * {@link #isValidName(String)} and {@link #getAlts(String)}.
 */
public final class StructureRegistry {

	private static final Gson GSON = new GsonBuilder().create();

	private static final Set<String> valid = new HashSet<>();
	private static final Map<String, Set<String>> alts = new HashMap<>();

	private StructureRegistry() {}

	static {
		init();
	}

	public static void init() {
		valid.clear();
		alts.clear();
		load("mapping/registry/structures.json");
	}

	private static void load(String resource) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			StructureRegistry.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8))) {
			if (reader == null) {
				return;
			}
			List<StructureEntry> parsed = GSON.fromJson(reader,
				new TypeToken<List<StructureEntry>>() {}.getType());
			if (parsed == null) {
				return;
			}
			for (StructureEntry e : parsed) {
				valid.add(e.name());
				Set<String> set = new HashSet<>();
				if (e.alt() != null) {
					set.addAll(e.alt());
				}
				set.add(e.name());
				alts.put(e.name(), set);
			}
		} catch (IOException | RuntimeException e) {
			throw new ParseException("failed to load structure registry", e);
		}
	}

	private record StructureEntry(String name, List<String> alt, String icon, String display) {}

	public static boolean isValidName(String name) {
		return valid.contains(name);
	}

	/** Alternative names a structure is known by, including itself. */
	public static Set<String> getAlts(String name) {
		Set<String> a = alts.get(name);
		return a == null ? Collections.singleton(name) : a;
	}

	public static Collection<String> getValidNames() {
		return Collections.unmodifiableSet(valid);
	}
}
