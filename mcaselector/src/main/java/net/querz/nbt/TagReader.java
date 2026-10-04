package net.querz.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagReader<T extends Tag> {

	T read(DataInput in, boolean rawArrays, int depth) throws IOException;

	TagTypeVisitor.ValueResult read(DataInput in, TagTypeVisitor visitor, boolean rawArrays) throws IOException;

	void skip(DataInput in) throws IOException;

	default byte[] readByteArray(DataInput in, int length) throws IOException {
		byte[] data = new byte[length];
		in.readFully(data);
		return data;
	}

}
