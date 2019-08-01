package be.etrovub.c3d4java.parameters;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class ParameterModel {
	public final static int BLOCK_SIZE = 512;

	public ParameterModel() {
		_name = "";
		_description = "";
	}

	private String _name;

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	private String _description;

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

	// private sbyte _id;
	private byte _id;

	// public void setId(sbyte value){ _id = value;}
	public void setId(byte value) {
		_id = value;
	}

	// public sbyte getId(){return _id;}
	public byte getId() {
		return _id;
	}

	private long _offsetInFile = -1;

	public long getOffsetInFile() {
		return _offsetInFile;
	}

	public void setOffsetInFile(long value) throws Exception {
		if (_offsetInFile == -1)
			_offsetInFile = value;
		else
			throw new Exception(
					"FileOffset has been set already for parameter " + _name);
	}

	public void ResetOffsetInFile() {
		_offsetInFile = -1;
	}

	protected abstract short GetContentLength();

	// protected abstract void WriteContent(BinaryWriter writer);
	public abstract void WriteContent(RandomAccessFile writer)
			throws IOException;

	public void WriteTo(RandomAccessFile writer) throws IOException{
		WriteTo(writer, false);
	}
	// public void WriteTo(BinaryWriter writer, boolean isLast) {
	public void WriteTo(RandomAccessFile writer, boolean isLast)
			throws IOException {
		writer.write((byte) _name.length()); // writer.Write((sbyte)
											// Name.length);
		writer.write((byte) getId()); // writer.Write((sbyte) getId());
		writer.writeChars(_name);// Write(Name.toCharArray()); // string name =
								// ParameterModel.ReadName(_reader,
								// Math.Abs(nameLen));

		// compute offset of the next item
		short nextItem = (short) (isLast ? 0 : (_description.length() + 2 // next
																			// item
																			// number
				+ 1 // desc length number
				+ GetContentLength()));

		writer.write(nextItem);
		WriteContent(writer);

		writer.write((byte) (_description.length()));
		writer.writeChars(_description);// Write(Description.toCharArray());
	}

	//
	// static member for C3D format reading
	//
	
	// public static sbyte ReadGroupID(BinaryReader reader) {
	public static byte ReadGroupID(RandomAccessFile reader) throws IOException {
		Byte b = reader.readByte();
		return b; // ReadSByte();
	}

	public static byte ReadNameLength(RandomAccessFile reader)
			throws IOException {// public static sbyte
								// ReadNameLength(BinaryReader reader) {
		return reader.readByte();// ReadSByte();
	}

	public static byte ReadDescLength(RandomAccessFile reader) throws IOException {
		return reader.readByte();//ReadByte();
	}

	public static short ReadNextItemOffset(RandomAccessFile reader)
			throws IOException {
		
		byte b1 = reader.readByte();
		byte b2 = reader.readByte();
		
		short c = (short)((b2 << 8) | b1);
		
		c = (short) (b1 & 0xFF | (b2 & 0xFF) << 8);
		
		return c;//reader.readShort();
	}

	public static String ReadName(RandomAccessFile reader, int length) throws IOException {
		return ParameterModel.ReadString(reader, length);
	}

	public static String ReadDesc(RandomAccessFile reader, int length) throws IOException {
		return ParameterModel.ReadString(reader, length);
	}

	private static String ReadString(RandomAccessFile reader, int length)
			throws IOException {
		byte[] buf = new byte[length];
		reader.read(buf, 0, length);
		return new String(buf, "UTF8"); // System.Text.Encoding.UTF8.GetString(reader.ReadBytes(length),
										// 0, length);
	}
}