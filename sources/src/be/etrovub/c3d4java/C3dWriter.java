package be.etrovub.c3d4java;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.etrovub.c3d4java.parameters.*;

public class C3dWriter {

	
	private boolean _eventsEnabled;
	private String _c3dFile;
	private File _fs = null;
	// private BinaryWriter _writer = null;
	private RandomAccessFile _writer = null;
	private Map<String, ParameterGroup> _nameToGroups;
	private Map<Integer, ParameterGroup> _idToGroups;

	// private int _dataStartOffset;
	// private int _pointFramesOffset;

	// private int _writePos = 0;

	private List<C3dEvent> _events = null;

	private List<String> _pointsLabels;

	public List<String> getLabels() {
		return Collections.unmodifiableList(_pointsLabels);
	}

	private int _currentFrame = 0;

	public int getCurrentFrame() {
		return _currentFrame;
	}

	public int getFramesCount() {
		return _header.getLastSampleNumber();
	}

	public short getPointsCount() {
		return _header.getNumberOfPoints();
	}

	public void setPointsCount(short value) {
		_header.setNumberOfPoints(value);
	}

	private C3dHeader _header = null;

	public C3dHeader getHeader() {
		return _header;
	}

	public C3dWriter() throws Exception {
		this(false);
	}

	public C3dWriter(boolean eventsEnabled) throws Exception {
		_eventsEnabled = eventsEnabled;
		_nameToGroups = new HashMap<String, ParameterGroup>();
		_idToGroups = new HashMap<Integer, ParameterGroup>();
		_pointsLabels = new ArrayList<String>();
		_header = new C3dHeader();

		SetDefaultParametrs();
	}

	/*
	 * ~C3dWriter() { if (_fs != null) { Close(); } }
	 */

	private static String GetTempFile(String file) {
		File f = new File(file);
		return f.getParent() + "/~tmp." + f.getName();// Path.GetDirectoryName(file)
														// + "/~tmp." +
														// Path.GetFileName(file);
	}

	public boolean Open(String c3dFile) throws Exception {

		_c3dFile = c3dFile;
		_header.setLastSampleNumber((short) 0);
		try {
			// PrepareEvents();
			// _fs = new FileInputStream(_eventsEnabled? GetTempFile(_c3dFile) :
			// _c3dFile, FileMode.OpenOrCreate, FileAccess.ReadWrite);
			_fs = new File(_eventsEnabled ? GetTempFile(_c3dFile) : _c3dFile);

			_writer = new RandomAccessFile(_fs, "rw"); // new BinaryWriter(_fs);

			WriteHeader();
			WriteParameters();

			// _writer.BaseStream.Seek(_dataStart, 0);
		} catch (IOException e) {
			System.err.print("C3dReader.Open(\"" + c3dFile + "\"): "
					+ e.getMessage());
			throw new Exception("C3dReader.Open(\"" + c3dFile + "\"): "
					+ e.getMessage());
		}
		return true;
	}

	// [MethodImpl(MethodImplOptions.Synchronized)]
	public boolean Close() throws IOException, Exception {
		if (_fs == null) {
			return false;
		}

		// write number of frames
		SetParameter("POINT:FRAMES", (Short) _header.getLastSampleNumber(),
				Short.class);

		// update header (data start together with number of frames)
		long position = _writer.getFilePointer();// _writer.BaseStream.Position;
		Parameter p = _nameToGroups.get("POINT").GetParameter("DATA_START");
		_header.setDataStart((Short) p.GetData(Short.class));
		_writer.seek(0);// , 0);
		_writer.write(_header.GetRawData());
		_writer.seek(position);// (int)position, 0); // to be sure, put pointer
								// to the end
		_writer.close();
		_writer = null;
		// _fs.Close();
		_fs = null;

		if (_eventsEnabled) {
			RewriteWithEvents();
			RemoveTempFile();
		}
		return true;
	}

	private void RemoveTempFile() {
		// File.Delete(GetTempFile(_c3dFile));
		new File(GetTempFile(_c3dFile)).delete();
	}

	private void RewriteWithEvents() throws Exception {
		WriteEventContexts();
		WriteEvents();

		// reset parameters' offsets to enable writing to new file
		for (int id : _idToGroups.keySet()) {
			_idToGroups.get(id).ResetOffsetInFile();
		}

		_eventsEnabled = false;
		C3dReader reader = new C3dReader();
		reader.Open(GetTempFile(_c3dFile));
		Open(_c3dFile);

		for (int i = 0; i < reader.getFramesCount(); i++) {
			Vector3[] points = reader.ReadFrame();
			if (reader.IsFloat()) {
				this.WriteFloatFrame(points);
			} else if (reader.IsInteger()) {
				this.WriteIntFrame(points);
			}
		}

		reader.Close();
		this.Close();
	}

	public void UpdateParameter(Parameter p) throws IOException {
		long position = _writer.getFilePointer();// _writer.BaseStream.Position;

		_writer.seek(p.getOffsetInFile());// (int)p.getOffsetInFile(), 0);
		p.WriteTo(_writer);

		_writer.seek(position);// (int)position, 0);
	}

	private void WriteParameters() throws Exception {
		byte[] parameters = { 0x01, 0x50, 0x02, 0x54 };
		_writer.write(parameters, 0, 4);
		// _writePos += 4;

		for (int id : _idToGroups.keySet()) {
			ParameterGroup grp = _idToGroups.get(id);
			grp.WriteTo(_writer);

			WriteParametersOfGroup(grp);
		}

		// update data start offset
		int dataStart = (int) ((_writer.getFilePointer()// _writer.BaseStream.Position
		+ 5 // size of the last group
		) / ParameterModel.BLOCK_SIZE) + 2; // 1 because we are counting from
											// zero and 1 because we want to
											// point on to the next block

		SetParameter("POINT:DATA_START", (short) dataStart, Short.class);

		long position = _writer.getFilePointer(); // _writer.BaseStream.Position;
		_writer.seek(512);// (int)512, 0);
		parameters[2] = (byte) (dataStart - 2); // number of blocks with
												// parameters is one less than
												// the number of the data
												// starting block without first
												// block
		_writer.write(parameters, 0, 4);
		_writer.seek(position);// (int)position, 0);

		// write last special group
		ParameterGroup lastTag = new ParameterGroup();
		lastTag.setId((byte) 0);
		lastTag.setName("");
		lastTag.setDescription("");
		lastTag.WriteTo(_writer, true);

		_writer.write(new byte[(dataStart - 1) * 512
				- ((int) _writer.getFilePointer())]); // _writer.BaseStream.Position]);
	}

	private void WriteParametersOfGroup(ParameterGroup grp) throws IOException,
			Exception {
		for (Parameter p : grp.getParameters()) {
			p.setId((byte) -grp.getId());// p.setId((sbyte)-grp.Id);
			p.setOffsetInFile(_writer.getFilePointer());// _writer.BaseStream.Position);
			p.WriteTo(_writer);
		}
	}

	private void WriteHeader() throws IOException {
		_writer.write(_header.GetRawData());
		// _writePos += 512;
	}

	private void SetDefaultParametrs() throws Exception {
		SetParameter("POINT:DATA_START", (short) 2, Short.class);

		_header.setNumberOfPoints((short) 21);
		SetParameter("POINT:USED", (short) _header.getNumberOfPoints(),
				Short.class);

		_header.setLastSampleNumber((short) 0);
		SetParameter("POINT:FRAMES", (short) _header.getLastSampleNumber(),
				Short.class);

		_header.setScaleFactor(1f);
		SetParameter("POINT:SCALE", _header.getScaleFactor(), Float.class);

		_header.setFrameRate(30);
		SetParameter("POINT:RATE", _header.getFrameRate(), Float.class);

		_header.setAnalogSamplesPerFrame((short) 0);
		SetParameter("ANALOG:RATE", _header.getAnalogSamplesPerFrame(),
				Short.class);

		_header.setAnalogChannels((short) 0);
		SetParameter("ANALOG:USED", (short) _header.getAnalogChannels(),
				Short.class);

		SetParameter("ANALOG:SCALE", new Float[] {}, Float[].class);

		SetParameter("ANALOG:GEN_SCALE", (float) 1, Float.class);

		SetParameter("ANALOG:OFFSET", new Short[] {}, Short[].class);
	}

	private byte _nextGroupId = -1; // sbyte _nextGroupId = -1;

	public <T> void SetParameter(String path, T parameterValue, Class<T> klass)
			throws Exception {
		String[] elements = path.split(":");
		if (elements.length != 2) {
			throw new Exception("Wrong path format (use GROUP:PARAMETER)");
		}

		if (!_nameToGroups.containsKey(elements[0])) {
			if (_fs == null) {
				ParameterGroup group = new ParameterGroup();
				group.setId(_nextGroupId--);
				group.setName(elements[0]);
				_nameToGroups.put(group.getName(), group);
				_idToGroups.put(((int) group.getId() & 0xFF), group);
			} else {
				throw new Exception("Cannot create a parameter group "
						+ elements[0] + " after file was open.");
			}
		}

		ParameterGroup grp = _nameToGroups.get(elements[0]);

		Parameter p = grp.HasParameter(elements[1]) ? grp
				.GetParameter(elements[1]) : new Parameter();

		p.setName(elements[1]);
		p.SetData(parameterValue, klass);

		if (!grp.getParameters().contains(p)) {
			if (_fs == null) {
				grp.getParameters().add(p);
			} else {
				throw new Exception("Cannot create a parameter " + elements[0]
						+ " after file was open.");
			}
		}

		// if file is open and we are modifieng an existig an parameter - update
		// changes.
		if (_fs != null && p.getOffsetInFile() > 0) {
			UpdateParameter(p);
		}
	}

	public void AddEvent(C3dEvent e) {
		if (_events == null) {
			_events = new ArrayList<C3dEvent>();
		}

		if (e.getFrame() == 0) {
			e.setFrame(_header.getLastSampleNumber());
		}
		_events.add(e);
	}

	public void WriteEventContexts() throws Exception {
		if (_events == null)
			return;
		// IEnumerable<String> contexts =
		// _events.Select(x=>x.Context).Distinct();
		ArrayList<String> contexts = new ArrayList<String>();// =
																// _events.Select(x=>x.Context).Distinct();

		for (C3dEvent _event : _events) {
			String context = _event.getContext();
			if (!contexts.contains(context)) {
				contexts.add(context);
			}
		}

		String[] descs = new String[contexts.size()]; // new
														// string[contexts.Count<string>()];
		for (int i = 0; i < descs.length; i++)
			descs[i] = "";

		Short[] icon_ids = new Short[contexts.size()];// new
														// short[contexts.Count<string>()];
		Short[] colours = new Short[contexts.size()];// new
														// short[contexts.Count<string>()];

		SetParameter("EVENT_CONTEXT:USED", (short) contexts.size(), Short.class); // (short)contexts.Count<String>(),
																					// short.class);

		SetParameter("EVENT_CONTEXT:LABELS", (String[]) contexts.toArray(),
				String[].class); // contexts.ToArray<String>(), string[].class);

		SetParameter("EVENT_CONTEXT:DESCRIPTIONS", descs, String[].class);

		SetParameter("EVENT_CONTEXT:ICON_IDS", icon_ids, Short[].class);

		SetParameter("EVENT_CONTEXT:COLOURS", colours, Short[].class);
	}

	private void WriteEvents() throws Exception {
		if (_events == null)
			return;

		String[] labels = new String[_events.size()];
		String[] contexts = new String[_events.size()];
		String[] descriptions = new String[_events.size()];
		String[] subjects = new String[_events.size()];
		float[][] times = new float[2][_events.size()];
		Short[] icon_ids = new Short[_events.size()];
		Byte[] generic_flags = new Byte[_events.size()];

		for (int i = 0; i < labels.length; i++) {
			labels[i] = _events.get(i).getLabel();
			contexts[i] = _events.get(i).getContext();
			descriptions[i] = _events.get(i).getDescription();
			subjects[i] = _events.get(i).getSubject();
			icon_ids[i] = _events.get(i).getIconId();
			generic_flags[i] = _events.get(i).getGenericFlag();

			float t = _events.get(i).getFrame() / _header.getFrameRate();
			times[0][i] = ((int) t) / 60; // compute minutes
			times[1][i] = t % 60; // seconds and fraction of seconds
		}

		SetParameter("EVENT:USED", (short) contexts.length, Short.class);
		SetParameter("EVENT:CONTEXTS", contexts, String[].class);
		SetParameter("EVENT:LABELS", labels, String[].class);
		SetParameter("EVENT:DESCRIPTIONS", descriptions, String[].class);
		SetParameter("EVENT:SUBJECTS", subjects, String[].class);
		SetParameter("EVENT:TIMES", times, float[][].class);
		SetParameter("EVENT:ICON_IDS", icon_ids, Short[].class);
		SetParameter("EVENT:GENERIC_FLAGS", generic_flags, Byte[].class);
	}

	public void WriteFloatFrame(Vector3[] data) throws IOException {
		_header.setLastSampleNumber((short) (_header.getLastSampleNumber() + 1));
		for (int i = 0; i < data.length; i++) {
			_writer.writeFloat(data[i].X);
			_writer.writeFloat(data[i].Y);
			_writer.writeFloat(data[i].Z);

			// TODO
			_writer.writeFloat((float) 0);
			// int cc = (int)_reader.ReadSingle();
		}
	}

	public void WriteIntFrame(Vector3[] data) throws IOException {
		_header.setLastSampleNumber((short) (_header.getLastSampleNumber() + 1));
		for (int i = 0; i < data.length; i++) {
			_writer.write((short) data[i].X);
			_writer.write((short) data[i].Y);
			_writer.write((short) data[i].Z);

			// TODO
			_writer.write((short) 0);
		}
	}

	public void WriteFloatAnalogData(float[] data_channels) throws IOException,
			Exception {
		if (data_channels.length != _header.getAnalogChannels()) {
			throw new Exception(
					"Number of channels in data has to be the same as it is declared in header and parameters' section");
		}

		for (int i = 0; i < data_channels.length; i++) {
			_writer.writeFloat(data_channels[i]);
		}
	}

	public void WriteIntAnalogData(short[] data_channels) throws IOException,
			Exception {
		if (data_channels.length != _header.getAnalogChannels()) {
			throw new Exception(
					"Number of channels in data has to be the same as it is declared in header and parameters' section");
		}

		for (int i = 0; i < data_channels.length; i++) {
			_writer.write(data_channels[i]);
		}
	}
}