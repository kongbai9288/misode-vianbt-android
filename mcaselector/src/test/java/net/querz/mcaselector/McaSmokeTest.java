package net.querz.mcaselector;

import kotlin.Pair;
import net.querz.mcaselector.android.MinecraftCoordinates;
import net.querz.mcaselector.util.collection.Long2ObjectMap;
import net.querz.mcaselector.logging.LogManager;
import net.querz.mcaselector.logging.Logger;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.util.collection.IntConsumer;
import net.querz.mcaselector.util.collection.Long2ObjectOpenHashMap;
import net.querz.mcaselector.util.collection.LongOpenHashSet;
import net.querz.nbt.CompoundTag;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Smoke test for the MCA Selector core running as a plain JVM unit test
 * (`gradle :mcaselector:test`).
 *
 * It answers the question that matters for the Android port: does the code
 * still behave like upstream once JavaFX, AWT, log4j and fastutil are gone?
 */
public class McaSmokeTest {

	/* --------------------------------------------- Minecraft <-> screen math */

	@Test
	public void chunkAndRegionMath() {
		assertEquals(16, MinecraftCoordinates.CHUNK_SIZE);
		assertEquals(32, MinecraftCoordinates.REGION_SIZE);
		assertEquals(512, MinecraftCoordinates.REGION_BLOCK_SIZE);

		assertEquals(2, MinecraftCoordinates.blockToChunk(32));
		assertEquals(1, MinecraftCoordinates.chunkToRegion(35));
		assertEquals(3, MinecraftCoordinates.chunkIndexInRegion(35));
		assertEquals(15, MinecraftCoordinates.blockIndexInChunk(1023));

		// chunk <-> region round trip
		assertEquals(1, MinecraftCoordinates.chunkToRegion(35));
		assertEquals(3, MinecraftCoordinates.chunkIndexInRegion(35));
		int[] back = new int[]{
			MinecraftCoordinates.regionToChunk(1, 1, 3, 3).getFirst().intValue(),
			MinecraftCoordinates.regionToChunk(1, 1, 3, 3).getSecond().intValue(),
		};
		assertEquals(35, back[0]);
		assertEquals(35, back[1]);
	}

	@Test
	public void regionHeaderOffsetMath() {
		// .mca header: 4 bytes per chunk, index = z * 32 + x
		assertEquals(0, MinecraftCoordinates.chunkIndexToOffset(0, 0));
		assertEquals(4, MinecraftCoordinates.chunkIndexToOffset(1, 0));
		assertEquals(128, MinecraftCoordinates.chunkIndexToOffset(0, 1));

		assertEquals(1, MinecraftCoordinates.chunkOffsetToIndex(4).getFirst().intValue());
		assertEquals(0, MinecraftCoordinates.chunkOffsetToIndex(4).getSecond().intValue());
		assertEquals(0, MinecraftCoordinates.chunkOffsetToIndex(128).getFirst().intValue());
		assertEquals(1, MinecraftCoordinates.chunkOffsetToIndex(128).getSecond().intValue());
	}

	@Test
	public void yawMapsOntoCanvasRotation() {
		// MC yaw: 0 = south, 90 = west, 180 = north, 270 = east.
		// Canvas: 0 = pointing right, positive = clockwise.
		assertEquals(90f, MinecraftCoordinates.yawToCanvasAngle(0f), 0.001f);
		assertEquals(180f, MinecraftCoordinates.yawToCanvasAngle(90f), 0.001f);
		assertEquals(270f, MinecraftCoordinates.yawToCanvasAngle(180f), 0.001f);
		assertEquals(0f, MinecraftCoordinates.yawToCanvasAngle(270f), 0.001f);

		// round trip through the inverse
		for (float yaw = 0f; yaw < 360f; yaw += 15f) {
			float back = MinecraftCoordinates.canvasAngleToYaw(MinecraftCoordinates.yawToCanvasAngle(yaw));
			assertEquals(yaw, back, 0.001f);
		}
	}

	@Test
	public void yawScreenDirectionIsUnitLength() {
		for (float yaw = 0f; yaw < 360f; yaw += 30f) {
			Pair<Float, Float> d = MinecraftCoordinates.yawToScreenDirection(yaw);
			double len = Math.sqrt(d.getFirst() * d.getFirst() + d.getSecond() * d.getSecond());
			assertEquals(1.0, len, 0.001);
		}
		// yaw = 0 faces south, which is +Y on screen (downwards).
		Pair<Float, Float> south = MinecraftCoordinates.yawToScreenDirection(0f);
		assertEquals(0f, south.getFirst().floatValue(), 0.001f);
		assertEquals(1f, south.getSecond().floatValue(), 0.001f);
	}

	@Test
	public void blockPixelRoundTrip() {
		int originX = -512;
		int originZ = 1024;
		float scale = 0.25f;
		Pair<Float, Float> px = MinecraftCoordinates.blockToPixel(0, 0, originX, originZ, scale);
		Pair<Integer, Integer> block = MinecraftCoordinates.pixelToBlock(
			px.getFirst(), px.getSecond(), originX, originZ, scale);
		assertEquals(0, block.getFirst().intValue());
		assertEquals(0, block.getSecond().intValue());
	}

	/* ------------------------------------------------------------- ChunkSet */

	@Test
	public void chunkSetTracksBits() {
		ChunkSet set = new ChunkSet();
		assertTrue(set.isEmpty());
		set.set(0);
		set.set(31);
		set.set(1023);
		assertFalse(set.isEmpty());
		assertEquals(3, set.size());
		assertTrue(set.get(0));
		assertTrue(set.get(1023));
		assertFalse(set.get(1));

		set.clear(31);
		assertEquals(2, set.size());
	}

	@Test
	public void chunkSetIteratesEverySetIndex() {
		ChunkSet set = new ChunkSet();
		set.set(5);
		set.set(700);
		set.set(1023);

		List<Integer> seen = new ArrayList<>();
		for (Integer i : set) {
			seen.add(i);
		}
		assertEquals(3, seen.size());
		assertTrue(seen.contains(5));
		assertTrue(seen.contains(700));
		assertTrue(seen.contains(1023));

		List<Integer> viaForEach = new ArrayList<>();
		set.forEach((IntConsumer) viaForEach::add);
		assertEquals(seen, viaForEach);
	}

	@Test
	public void chunkSetSurvivesSerialisation() {
		ChunkSet set = new ChunkSet();
		set.set(1);
		set.set(512);
		byte[] bytes = set.toBytes();
		assertEquals(16 * Long.BYTES, bytes.length);

		ChunkSet read = ChunkSet.fromBytes(bytes);
		assertEquals(set.size(), read.size());
		assertTrue(read.get(1));
		assertTrue(read.get(512));
	}

	@Test
	public void chunkSetBooleanAlgebra() {
		ChunkSet a = new ChunkSet();
		a.set(1);
		a.set(2);
		ChunkSet b = new ChunkSet();
		b.set(2);
		b.set(3);

		ChunkSet union = a.clone();
		union.or(b);
		assertEquals(3, union.size());

		ChunkSet intersection = a.clone();
		intersection.and(b);
		assertEquals(1, intersection.size());
		assertTrue(intersection.get(2));

		ChunkSet flipped = new ChunkSet();
		flipped.set(0);
		flipped = flipped.flip();
		assertEquals(1024 - 1, flipped.size());
	}

	/* ------------------------------------------- replacement collections */

	@Test
	public void longKeyedMapBehaves() {
		Long2ObjectOpenHashMap<String> map = new Long2ObjectOpenHashMap<>();
		map.put(7L, "seven");
		map.put(-1L, "minus");
		assertEquals(2, map.size());
		assertEquals("seven", map.get(7L));
		assertTrue(map.containsKey(-1L));
		assertEquals("fallback", map.getOrDefault(99L, "fallback"));

		List<Long> keys = new ArrayList<>();
		for (Long2ObjectMap.Entry<String> e : map.long2ObjectEntrySet()) {
			keys.add(e.getLongKey());
		}
		assertEquals(2, keys.size());
		assertTrue(keys.contains(7L));
	}

	@Test
	public void longSetSupportsUnion() {
		LongOpenHashSet all = new LongOpenHashSet();
		all.add(1L);
		LongOpenHashSet other = LongOpenHashSet.of(2L, 3L);
		all.addAll(other);
		assertEquals(3, all.size());

		long[] array = all.toLongArray();
		assertEquals(3, array.length);
	}

	/* ------------------------------------------------------- NBT (Querz) */

	@Test
	public void nbtTagsRoundTrip() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("DataVersion", 3953);
		tag.putString("id", "minecraft:stone");
		tag.putByte("Count", (byte) 64);

		assertEquals(3953, tag.getInt("DataVersion"));
		assertEquals("minecraft:stone", tag.getString("id"));

		CompoundTag copy = tag.copy();
		assertEquals(tag, copy);
		assertEquals(tag.hashCode(), copy.hashCode());
		assertEquals(3953, copy.getInt("DataVersion"));
	}

	/* ------------------------------------------------- ported dependencies */

	@Test
	public void loggingWorksOffDevice() {
		Logger log = LogManager.getLogger(McaSmokeTest.class);
		assertNotNull(log);
		// Must not throw: this is the android.util.Log stub trap.
		log.debug("smoke %s", "test");
		log.warn("warned", new IllegalStateException("ignored"));
		log.error(new IllegalStateException("ignored"));
		assertEquals("McaSmokeTest", log.getName());
	}

	@Test
	public void noDesktopDependenciesOnTheClasspath() {
		// Only libraries that are NOT part of the JDK can be asserted here.
		// java.awt ships with every desktop JVM, so a classpath check would
		// always pass/fail for the wrong reason; the CI job verifies AWT and
		// JavaFX by scanning the compiled .aar instead.
		String[] banned = {
			"it.unimi.dsi.fastutil.ints.IntArrayList",
			"it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap",
			"it.unimi.dsi.fastutil.longs.LongOpenHashSet",
			"it.unimi.dsi.fastutil.shorts.ShortPredicate",
			"javafx.scene.image.Image",
			"org.apache.logging.log4j.LogManager",
			"org.apache.logging.log4j.Logger",
		};
		for (String name : banned) {
			try {
				Class.forName(name);
				fail("desktop dependency still present: " + name);
			} catch (ClassNotFoundException expected) {
				// expected: the port is dependency free
			}
		}
	}

	@Test
	public void distanceHelpers() {
		assertEquals(5.0, MinecraftCoordinates.planarDistance(0, 0, 3, 4), 0.001);
		assertEquals(3.0, MinecraftCoordinates.distance3D(0, 0, 0, 0, 3, 0), 0.001);
	}
}
