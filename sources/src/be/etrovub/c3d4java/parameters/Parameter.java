package be.etrovub.c3d4java.parameters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Arrays;

import be.etrovub.c3d4java.BitConverter;

public class Parameter extends ParameterModel {
	private int[] _dimensions = null;
	private byte[] _vectorData;
	private byte _paramType;
	private int _length;

	private boolean IsScalar;

	public boolean isIsScalar() {
		return IsScalar;
	}

	public void setIsScalar(boolean isScalar) {
		IsScalar = isScalar;
	}

	private int _C3DParameterSize = 0;

	public int getC3DParameterSize() {
		return _C3DParameterSize;
	}

	public Parameter(RandomAccessFile reader) throws IOException, Exception {
		long position = reader.getFilePointer();
		// read parameter
		_paramType = reader.readByte();
		byte dimensions = reader.readByte(); // 0 means that parameter is scalar

		if (dimensions > 0) {
			IsScalar = false;
			ReadMatrix(reader, GetType(_paramType), dimensions & 0xFF);
		} else {
			IsScalar = true;
			ReadScalar(reader, GetType(_paramType));
		}

		_C3DParameterSize = (int) (reader.getFilePointer() - position);
	}

	public Parameter() {
	}

	private void ReadMatrix(RandomAccessFile reader, Class klass, int dimensions)
			throws IOException, Exception {
		_length = 1;
		_dimensions = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			_dimensions[i] = reader.read();
			_length *= _dimensions[i];
		}
		_length *= GetSize(_paramType);

		// TODO
		_vectorData = new byte[_length];
		reader.read(_vectorData, 0, _length);
	}

	private void ReadScalar(RandomAccessFile reader, Class klass)
			throws IOException, Exception {
		_vectorData = new byte[GetSize(_paramType)];
		reader.read(_vectorData, 0, GetSize(_paramType));
		// TODO
	}

	@Override
	protected short GetContentLength() {
		return (short) (_vectorData.length + 1 + // to store parameter type
													// (sbyte)
		1 + // to store number of dimensions type (byte)
		_dimensions.length// +
		// _vectorData.Length
		);
	}

	@Override
	public void WriteContent(RandomAccessFile writer) throws IOException {
		writer.write(_paramType);
		writer.write((byte) _dimensions.length);
		for (int i = 0; i < _dimensions.length; i++) {
			writer.write((byte) _dimensions[i]);
		}
		writer.write(_vectorData);
	}

	public <T> void SetData(Object data, Class<T> klass) throws Exception {
		// T ret;
		// = default(T);
		//
		// BASIC TYPES
		//
		if (klass.equals(Character.class)) {
			_paramType = -1;
			_dimensions = new int[] {};
			_vectorData = BitConverter.getBytes((char) data);
			_length = 1;
			IsScalar = true;
		} else if (klass.equals(Byte.class)) {
			_dimensions = new int[] {};
			_paramType = 1;
			_vectorData = BitConverter.getBytes((byte) data);
			_length = 1;
			IsScalar = true;
		} else if (klass.equals(Short.class)) {
			_dimensions = new int[] {};
			_paramType = 2;
			_vectorData = BitConverter.getBytes((short) data);
			_length = 1;
			IsScalar = true;
		} else if (klass.equals(Float.class)) {
			_dimensions = new int[] {};
			_paramType = 4;
			_vectorData = BitConverter.getBytes((float) data);
			_length = 1;
			IsScalar = true;
		} else if (klass.equals(String.class)) {
			_dimensions = new int[] { ((String) data).length() };
			_paramType = -1;
			// Encoding.ASCII.GetBytes((String) (Object) data);
			_vectorData = ((String) data).getBytes();
			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			IsScalar = false;
		}

		//
		// 1D Arrays
		//
		else if (klass.equals(String[].class)) {
			int count = ((String[]) (Object) data).length;
			int maxLen = 0;
			for (String s : ((String[]) (Object) data)) {
				maxLen = Math.max(s.length(), maxLen);
			}
			_dimensions = new int[] { maxLen, count };
			_paramType = -1;
			_vectorData = new byte[count * maxLen];

			// in C# there is really no other method for initializing arrays to
			// a non-default value (without creating temporary objects)
			// this is fastest way, see this ->
			// http://www.dotnetperls.com/initialize-array
			// but yes, it's ugly, indeed
			for (int i = 0; i < _vectorData.length; i++)
				_vectorData[i] = 32;

			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			for (int i = 0; i < count; i++) {
				String s = ((String[]) (Object) data)[i];

				// Encoding.ASCII.GetBytes(s, 0, s.length(), _vectorData, i *
				// maxLen);
				byte[] strBytes = s.getBytes();
				System.arraycopy(strBytes, 0, _vectorData, i * maxLen,
						strBytes.length);
			}
			IsScalar = false;
		} else if (klass.equals(Float[].class)) {
			int count = ((float[]) (Object) data).length;
			_dimensions = new int[] { count };
			_paramType = 4;
			_vectorData = new byte[count * GetSize((byte) 4)];

			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			for (int i = 0; i < count; i++) {
				float f = ((float[]) (Object) data)[i];
				System.arraycopy(BitConverter.getBytes(f), 0, _vectorData, i
						* GetSize(_paramType), GetSize(_paramType));
			}
			IsScalar = false;
		} else if (klass.equals(Float[][].class)) {
			int dim1 = ((float[][]) (Object) data)[0].length;
			int dim2 = ((float[][]) (Object) data)[1].length;
			int count = ((float[][]) (Object) data).length;
			_dimensions = new int[] { dim1, dim2 };
			_paramType = 4;
			_vectorData = new byte[count * GetSize((byte) 4)];

			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			int offset = 0;
			for (int y = 0; y < dim2; y++)
				for (int x = 0; x < dim1; x++) {
					float f = ((float[][]) (Object) data)[x][y];
					System.arraycopy(BitConverter.getBytes(f), 0, _vectorData,
							offset * GetSize(_paramType), GetSize(_paramType));
					offset++;
				}
			IsScalar = false;
		} else if (klass.equals(Short[].class)) {
			int count = ((short[]) (Object) data).length;
			_dimensions = new int[] { count };
			_paramType = 2;
			_vectorData = new byte[count * GetSize((byte) 2)];
			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			for (int i = 0; i < count; i++) {
				short n = ((short[]) (Object) data)[i];

				// TODO : check this
				System.arraycopy(BitConverter.getBytes(n), 0, _vectorData, i
						* GetSize(_paramType), GetSize(_paramType));
			}
			IsScalar = false;
		}
		// else if (typeof(T) == typeof(char[]))
		// {
		// ret = (T)(object)Get1DArray<char>();
		// }
		else if (klass.equals(Byte[].class)) {
			int count = ((byte[]) (Object) data).length;
			_dimensions = new int[] { count };
			_paramType = 1;
			_vectorData = new byte[count * GetSize((byte) 1)];
			_length = _vectorData.length; // it is the same length as it is in
											// string because ASCII encoding
			for (int i = 0; i < count; i++) {
				byte n = ((byte[]) (Object) data)[i];

				// TODO : check this
				System.arraycopy(BitConverter.getBytes(n), 0, _vectorData, i
						* GetSize(_paramType), GetSize(_paramType));
			}
			IsScalar = false;
		}
	}

	public <T> Object GetData(Class<T> klass) throws Exception {
		return GetData(0, klass);
	}

	public <T> T GetData(int i, Class<T> klass) throws Exception {
		T ret;

		// BASIC TYPES
		if (klass.equals(Character.class)) {
			ret = klass.cast((Object) BitConverter.toChar(_vectorData, i));
		} else if (klass.equals(Byte.class)) {
			ret = klass.cast((Object) _vectorData[i]);
		} else if (klass.equals(Short.class)) {
			ret = klass.cast((Object) BitConverter.toInt16(_vectorData, i * 2));
		} else if (klass.equals(Float.class)) {
			ret = klass.cast((Object) BitConverter.toFloat(_vectorData, i * 4));
		} else if (klass.equals(String.class)) {
			ret = klass.cast((Object) DataToString());
		}

		// 1D Arrays
		else if (klass.equals(String[].class)) {
			ret = klass.cast((Object) DataToStringArray());
		} else if (klass.equals(Character[].class)) {
			ret = klass.cast((Object) Get1DArray(Character.class));
		} else if (klass.equals(Byte[].class)) {
			ret = klass.cast((Object) Get1DArray(Byte.class));
		} else if (klass.equals(Short[].class)) {
			ret = klass.cast((Object) Get1DArray(Short.class));
		} else if (klass.equals(Float[].class)) {
			ret = klass.cast((Object) Get1DArray(Float.class));
		}
		//
		// 2D Arrays
		//
		// TODO: DO IT IF YOU NEED IT :)
		//
		// else if (typeof(T) == typeof(Int16 [,]))
		// {
		// ret = (T)(object)Get2DArray<Int16>();
		// }

		else {
			throw new Exception("Unknown type of parameter");
		}
		return ret;
	}

	private <T> T[][] Get2DArray(Class<T> klass) throws Exception {
		if (_dimensions.length != 2)
			throw new Exception("Parameter " + getName() + " is not 2D array.");
		// TODO does this work ???
		T[][] array = (T[][]) Array.newInstance(klass, _dimensions[0],
				_dimensions[1]); // new T[_dimensions[0]][_dimensions[1]]
		for (int x = 0; x < _dimensions[0]; x++) {
			for (int y = 0; y < _dimensions[1]; y++) {
				// TODO: still need to test following line
				array[x][y] = GetData(x + y * x, klass);
			}
		}
		return array;
	}

	private <T> T[] Get1DArray(Class<T> klass) throws Exception {
		if (_dimensions.length != 1)
			throw new Exception("Parameter " + getName() + " is not 1D array.");
		// TODO does this work ???
		T[] array = (T[]) Array.newInstance(klass, _dimensions[0]); // new
																	// T[_dimensions[0]];
		for (int i = 0; i < _dimensions[0]; i++) {
			array[i] = GetData(i, klass);
		}
		return array;
	}

	private String DataToString() throws UnsupportedEncodingException,
			Exception {
		if (_dimensions.length != 1 || _paramType != -1)
			throw new Exception("Parameter " + getName()
					+ " is not string type.");

		byte[] dt = Arrays.copyOfRange(_vectorData, 0, _dimensions[0]);
		return new String(dt, "UTF8");
	}

	private String[] DataToStringArray() throws UnsupportedEncodingException,
			Exception {
		String[] retArray;
		if (_dimensions.length != 2 || _paramType != -1)
			throw new Exception("Parameter " + getName()
					+ " is not string array type.");

		retArray = new String[_dimensions[1]];

		for (int i = 0; i < _dimensions[1]; i++) {
			int from = i * _dimensions[0];
			int to = from + _dimensions[0];
			byte[] dt = Arrays.copyOfRange(_vectorData, from, to);
			retArray[i] = new String(dt, "UTF8");
		}
		return retArray;
	}

	public int getLength() {
		return _length;
	}

	private static Class GetType(byte c3dDataType) throws Exception {
		switch (c3dDataType) {
		case -1:
			return Character.class;
		case 1:
			return Byte.class;
		case 2:
			return Short.class;
		case 4:
			return Float.class;
		default:
			throw new Exception("Unknown data type of c3d parameter");
		}
	}

	private static int GetSize(byte c3dDataType) throws Exception {
		switch (c3dDataType) {
		case -1:
			return 1;
		case 1:
			return 1;
		case 2:
			return 2;
		case 4:
			return 4;
		default:
			throw new Exception("Unknown data type of c3d parameter");
		}
	}
}
