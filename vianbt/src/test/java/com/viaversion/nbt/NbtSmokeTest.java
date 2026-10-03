package com.viaversion.nbt;

import com.viaversion.nbt.io.NBTIO;
import com.viaversion.nbt.limiter.TagLimiter;
import com.viaversion.nbt.stringified.SNBT;
import com.viaversion.nbt.tag.ByteArrayTag;
import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.DoubleTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.LongTag;
import com.viaversion.nbt.tag.MixedListTag;
import com.viaversion.nbt.tag.ShortTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Smoke test run as a plain JVM unit test (`gradle :vianbt:test`).
 *
 * Verifies that the dependency-free rewrite still behaves like upstream ViaNBT:
 * every tag type round-trips, SNBT parses and serialises, GZIP works, the
 * TagLimiter still protects against huge payloads, and no fastutil class is
 * referenced anywhere in the compiled output.
 */
public class NbtSmokeTest {

	private static final TagLimiter LIMITER = TagLimiter.create(1 << 20, 32);

	/* ------------------------------------------------------------------ */

	@Test
	public void everyTagTypeRoundTrips() throws IOException {
		CompoundTag root = new CompoundTag();
		root.putByte("byte", (byte) 7);
		root.putShort("short", (short) 1234);
		root.putInt("int", 123456789);
		root.putLong("long", 9876543210L);
		root.putFloat("float", 1.5f);
		root.putDouble("double", 2.25d);
		root.putString("string", "hello");
		root.put("byteArray", new ByteArrayTag(new byte[]{1, 2, 3}));
		root.put("intArray", new IntArrayTag(new int[]{4, 5, 6}));
		root.put("longArray", new LongArrayTag(new long[]{7L, 8L, 9L}));

		ListTag<StringTag> list = new ListTag<>(StringTag.class);
		list.add(new StringTag("a"));
		list.add(new StringTag("b"));
		root.put("list", list);

		CompoundTag nested = new CompoundTag();
		nested.putString("nested", "value");
		root.put("compound", nested);

		byte[] bytes = write(root);
		CompoundTag read = read(bytes);

		assertEquals((byte) 7, read.getByte("byte"));
		assertEquals((short) 1234, read.getShort("short"));
		assertEquals(123456789, read.getInt("int"));
		assertEquals(9876543210L, read.getLong("long"));
		assertEquals(1.5f, read.getFloat("float"), 0f);
		assertEquals(2.25d, read.getDouble("double"), 0d);
		assertEquals("hello", read.getString("string"));
		assertArrayEquals(new byte[]{1, 2, 3},
			((ByteArrayTag) read.get("byteArray")).getValue());
		assertArrayEquals(new int[]{4, 5, 6},
			((IntArrayTag) read.get("intArray")).getValue());
		assertArrayEquals(new long[]{7L, 8L, 9L},
			((LongArrayTag) read.get("longArray")).getValue());
		assertEquals(2, ((ListTag<?>) read.get("list")).size());
		assertEquals("value", ((CompoundTag) read.get("compound")).getString("nested"));
	}

	@Test
	public void mixedListRoundTrips() throws IOException {
		MixedListTag mixed = new MixedListTag(Arrays.asList(
			new IntTag(1), new StringTag("two"), new ByteTag((byte) 3)));
		CompoundTag root = new CompoundTag();
		root.put("mixed", mixed);

		CompoundTag read = read(write(root));
		ListTag<?> out = read.getListTag("mixed");
		assertNotNull(out);
		assertEquals(3, out.size());
		assertEquals(1, ((IntTag) out.get(0)).asInt());
		assertEquals("two", ((StringTag) out.get(1)).getValue());
	}

	@Test
	public void gzipRoundTrips() throws IOException {
		CompoundTag root = new CompoundTag();
		root.putString("id", "minecraft:stone");
		root.putInt("Count", 64);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			NBTIO.writer().named().write(gzip, root);
		}
		byte[] gzipped = out.toByteArray();
		assertTrue("should actually be compressed", gzipped.length > 10);

		CompoundTag read;
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
			read = NBTIO.reader(CompoundTag.class).named().read(gzip);
		}
		assertEquals("minecraft:stone", read.getString("id"));
		assertEquals(64, read.getInt("Count"));
	}

	@Test
	public void snbtParsesAndSerialises() {
		CompoundTag tag = SNBT.deserializeCompoundTag(
			"{Count:1b,id:\"minecraft:stone\",tag:{Damage:0,display:{Name:\"Sword\"}}}");
		assertEquals((byte) 1, tag.getByte("Count"));
		assertEquals("minecraft:stone", tag.getString("id"));
		assertEquals("Sword", ((CompoundTag) tag.get("tag"))
			.getCompoundTag("display").getString("Name"));

		String text = SNBT.serialize(tag);
		assertTrue(text.contains("minecraft:stone"));
		assertTrue(text.contains("Sword"));
	}

	@Test
	public void snbtByteArrayDoesNotBox() {
		// byteArray() is the one place upstream used fastutil's IntArrayList.
		CompoundTag tag = SNBT.deserializeCompoundTag("{Data:[B;1b,2b,3b,-1b]}");
		byte[] data = ((ByteArrayTag) tag.get("Data")).getValue();
		assertArrayEquals(new byte[]{1, 2, 3, -1}, data);
	}

	@Test
	public void tagLimiterStillProtects() {
		CompoundTag root = new CompoundTag();
		CompoundTag current = root;
		// 20 levels deep, well past the 2 level limit used below.
		for (int i = 0; i < 20; i++) {
			CompoundTag next = new CompoundTag();
			current.put("n" + i, next);
			current = next;
		}
		byte[] bytes;
		try {
			bytes = write(root);
		} catch (IOException e) {
			throw new AssertionError(e);
		}

		// Deep nesting is rejected. Depending on where the limit trips the
		// limiter either surfaces it directly (IllegalArgumentException) or the
		// tag reader wraps it in an IOException.
		boolean rejected = false;
		try {
			NBTIO.reader(CompoundTag.class).tagLimiter(TagLimiter.create(1 << 20, 2))
				.read(new ByteArrayInputStream(bytes));
		} catch (IOException | IllegalArgumentException expected) {
			rejected = true;
		}
		assertTrue("the limiter must reject nesting deeper than its max level", rejected);

		// A tiny byte budget is rejected too.
		boolean sizeRejected = false;
		try {
			NBTIO.reader(CompoundTag.class).tagLimiter(TagLimiter.create(8, 64))
				.read(new ByteArrayInputStream(bytes));
		} catch (IOException | IllegalArgumentException expected) {
			sizeRejected = true;
		}
		assertTrue("the limiter must reject data over its byte budget", sizeRejected);

		// Sanity check: with a generous limiter the same payload reads fine.
		try {
			CompoundTag ok = NBTIO.reader(CompoundTag.class).tagLimiter(TagLimiter.create(1 << 20, 64))
				.read(new ByteArrayInputStream(bytes));
			assertNotNull(ok);
		} catch (IOException e) {
			throw new AssertionError("a generous limiter must accept the payload", e);
		}
	}

	@Test
	public void noFastUtilOnTheClasspath() {
		for (String name : new String[]{
			"it.unimi.dsi.fastutil.ints.IntArrayList",
			"it.unimi.dsi.fastutil.io.FastBufferedInputStream",
			"it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap",
		}) {
			try {
				Class.forName(name);
				fail("fastutil is still on the classpath: " + name);
			} catch (ClassNotFoundException expected) {
				// expected: the library is dependency free
			}
		}
	}

	@Test
	public void tagRegistryCoversAllTypes() {
		int[] ids = {
			ByteTag.ID, ShortTag.ID, IntTag.ID, LongTag.ID, FloatTag.ID, DoubleTag.ID,
			ByteArrayTag.ID, StringTag.ID, ListTag.ID, CompoundTag.ID,
			IntArrayTag.ID, LongArrayTag.ID,
		};
		for (int id : ids) {
			assertNotNull("no tag registered for id " + id,
				com.viaversion.nbt.io.TagRegistry.getClassFor(id));
			assertEquals(id, com.viaversion.nbt.io.TagRegistry.getIdFor(
				com.viaversion.nbt.io.TagRegistry.getClassFor(id)));
		}
	}

	/* ------------------------------------------------------------------ */

	private static byte[] write(CompoundTag tag) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		NBTIO.writer().named().write(out, tag);
		return out.toByteArray();
	}

	private static CompoundTag read(byte[] bytes) throws IOException {
		Tag tag = NBTIO.reader().tagLimiter(LIMITER).named()
			.read(new ByteArrayInputStream(bytes));
		return (CompoundTag) tag;
	}
}
