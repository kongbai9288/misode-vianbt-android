package net.querz.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Objects;

public non-sealed class ByteArrayTag extends CollectionTag<ByteTag> {

	private byte[] value;

	public ByteArrayTag(byte[] b) {
		Objects.requireNonNull(b);
		value = b;
	}

	@Override
	public ByteTag get(int index) {
		return ByteTag.valueOf(value[index]);
	}

	@Override
	public ByteTag set(int index, ByteTag tag) {
		byte old = value[index];
		value[index] = tag.asByte();
		return ByteTag.valueOf(old);
	}

	@Override
	public void add(int index, ByteTag tag) {
		if (index > value.length|| index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + value.length);
		}
		byte[] output = new byte[value.length + 1];
		System.arraycopy(value, 0, output, 0, index);
		output[index] = tag.asByte();
		if (index < value.length) {
			System.arraycopy(value, index, output, index + 1, value.length - index);
		}
		value = output;
	}

	@Override
	public ByteTag remove(int index) {
		if (index < 0 || index >= value.length) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + value.length);
		}
		byte old = value[index];
		byte[] output = new byte[value.length - 1];
		System.arraycopy(value, 0, output, 0, index);
		if (index < value.length - 1) {
			System.arraycopy(value, index + 1, output, index, value.length - index - 1);
		}
		value = output;
		return ByteTag.valueOf(old);
	}

	@Override
	public Type getElementType() {
		return Type.BYTE;
	}

	@Override
	public int size() {
		return value.length;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(value.length);
		out.write(value);
	}

	@Override
	public ByteArrayTag copy() {
		byte[] copy = new byte[value.length];
		System.arraycopy(value, 0, copy, 0, value.length);
		return new ByteArrayTag(copy);
	}

	@Override
	public void accept(TagVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else {
			return other instanceof ByteArrayTag && Arrays.equals(value, ((ByteArrayTag) other).value);
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	public void clear() {
		value = new byte[0];
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	public int[] getIntArrayValue() {
		IntBuffer buffer = getAsIntBuffer();
		int[] array = new int[buffer.remaining()];
		buffer.get(array);
		return array;
	}

	public long[] getLongArrayValue() {
		LongBuffer buffer = getAsLongBuffer();
		long[] array = new long[buffer.remaining()];
		buffer.get(array);
		return array;
	}

	public ByteBuffer getAsByteBuffer() {
		return ByteBuffer.wrap(value);
	}

	public IntBuffer getAsIntBuffer() {
		return ByteBuffer.wrap(value).asIntBuffer();
	}

	public LongBuffer getAsLongBuffer() {
		return ByteBuffer.wrap(value).asLongBuffer();
	}

	public IntArrayTag getAsIntArrayTag() {
		return new IntArrayTag(getIntArrayValue());
	}

	public LongArrayTag getAsLongArrayTag() {
		return new LongArrayTag(getLongArrayValue());
	}

	public static final TagReader<ByteArrayTag> READER = new TagReader<>() {

		@Override
		public ByteArrayTag read(DataInput in, boolean rawArrays, int depth) throws IOException {
			return new ByteArrayTag(readByteArray(in, in.readInt()));
		}

		@Override
		public TagTypeVisitor.ValueResult read(DataInput in, TagTypeVisitor visitor, boolean rawArrays) throws IOException {
			return visitor.visit(readByteArray(in, in.readInt()));
		}

		@Override
		public void skip(DataInput in) throws IOException {
			in.skipBytes(in.readInt());
		}
	};
}
