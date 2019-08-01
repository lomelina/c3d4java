//-----------------------------------------------------------------------------
//C3dReader
//
//Reads in 3D position data from C3D file passed in "infile" 
//(files should be opened before calling).
//
//
//ETRO, Vrije Universiteit Brussel
//Copyright (C) 2015 Lubos Omelina. All rights reserved.
//-----------------------------------------------------------------------------
package be.etrovub.c3d4java;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import be.etrovub.c3d4java.parameters.*;

public class C3dReader {

	private String _c3dFile;
	private File _fs;
	private RandomAccessFile _reader;
	private Map<String, ParameterGroup> _nameToGroups;
	private Map<Integer, ParameterGroup> _idToGroups;
	private HashSet<Parameter> _allParameters;

	private int _dataStart;
	private int _pointsNumber;
	private float _pointScale;
	private float _pointRate;

	private float _analogRate;
	private int _analogUsed;
	private Float[] _analogScale;
	private float _analogGenScale;
	private Short[] _analogZeroOffset;

	private List<String> _pointsLabels;
	private Map<String, Integer> _pointsLabelsToId;
	protected List<String> _analogLabels;
	protected Map<String, Integer> _analogLabelsToId;

	public List<String> getLabels() {
		return Collections.unmodifiableList(_pointsLabels);
	}

	public List<String> getAnalogLabels() {
		return Collections.unmodifiableList(_analogLabels);
	}

	private int _currentFrame = 0;

	public int getCurrentFrame() {
		return _currentFrame;
	}

	private AnalogDataArray _analogData = null;

	public AnalogDataArray getAnalogData() {
		return _analogData;
	}

	public int getAnalogChannels() {
		return (int) (_analogRate / _pointRate);
	}

	public Vector3[] _points = null;

	public Vector3[] getPoints() {
		return _points;
	}

	private int _pointFrames = 0;

	public int getFramesCount() {
		return _pointFrames;
	}

	public void setNumberOfPointsInFrame(int value) {
		_pointsNumber = value;
	}

	public int getNumberOfPointsInFrame() {
		return _pointsNumber;
	}

	private C3dHeader _header = null;

	// public C3dHeader Header;

	public C3dHeader getHeader() {
		return _header;
	}

	public boolean IsInteger() {
		return _pointScale >= 0;
	}

	public boolean IsFloat() {
		return _pointScale < 0;
	}

	private List<C3dEvent> _events = new ArrayList<C3dEvent>();

	public List<C3dEvent> getEvents() {
		return _events;
	}

	public C3dReader() {
		_nameToGroups = new HashMap<String, ParameterGroup>();
		_idToGroups = new HashMap<Integer, ParameterGroup>();
		_pointsLabels = new ArrayList<String>();
		_pointsLabelsToId = new HashMap<String, Integer>();
		_allParameters = new HashSet<Parameter>();
		_analogLabels = new ArrayList<String>();
		_analogLabelsToId = new HashMap<String, Integer>();
	}

	public boolean Open(String c3dFile) {
		_c3dFile = c3dFile;
		try {
			_fs = new File(_c3dFile);
			_reader = new RandomAccessFile(_fs, "r");
			_header = new C3dHeader();

			ReadHeader();
			ReadParameters();
			ParseRequiredParameters();

			ReadEvents();

			_reader.seek(_dataStart);// _reader.BaseStream.Seek(_dataStart, 0);
		} catch (Exception e) {
			System.err.print("C3dReader.Open(\"" + c3dFile + "\"): "
					+ e.getMessage());
			return false;
		}
		return true;
	}

	private void ParseRequiredParameters() throws Exception {
		String[] labels = GetParameter("POINT:LABELS", String[].class);
		for (int i = 0; i < labels.length; i++) {
			String label = labels[i].trim();
			_pointsLabelsToId.put(label, i);
			_pointsLabels.add(i, label);
		}
		_dataStart = 512 * (GetParameter("POINT:DATA_START", Short.class) - 1);
		_pointsNumber = labels.length;// GetParameter("POINT:USED",
										// Short.class);
		_pointFrames = GetParameter("POINT:FRAMES", Short.class);
		_pointScale = GetParameter("POINT:SCALE", Float.class);
		_pointRate = GetParameter("POINT:RATE", Float.class);

		_analogRate = GetParameter("ANALOG:RATE", Float.class);
		_analogUsed = GetParameter("ANALOG:USED", Short.class);
		_analogScale = GetParameter("ANALOG:SCALE", Float[].class);
		_analogGenScale = GetParameter("ANALOG:GEN_SCALE", Float.class);
		_analogZeroOffset = GetParameter("ANALOG:OFFSET", Short[].class);

		String[] analogLabels = GetParameter("ANALOG:LABELS", String[].class);
		for (int i = 0; i < analogLabels.length; i++) {
			String label = analogLabels[i].trim();
			_analogLabelsToId.put(label, i);
			_analogLabels.add(i, label);
		}
	}

	private void ReadHeader() throws IOException {
		byte[] data = new byte[512];
		//int n = 
		_reader.read(data, 0, 512);		//DO NOT COMMENT OUT
		_header.SetHeader(data);

		/*
		 * logger.info("\tFirstWord: "+ String.format("0x%8s",
		 * Integer.toHexString(_header.getFirstWord())));
		 * logger.info("\tNumber of 3D points: " + _header.getNumberOfPoints());
		 * logger.info("\tAnalog channels: " + _header.getAnalogChannels());
		 * 
		 * logger.info("\tFirst sample number: " +
		 * _header.getFirstSampleNumber()); logger.info("\tLast sample number: "
		 * + _header.getLastSampleNumber()); logger.info("\tScale factor: " +
		 * _header.getScaleFactor()); logger.info("\tData start: " +
		 * _header.getDataStart()); logger.info("\tAnalog samples per frame: " +
		 * _header.getAnalogSamplesPerFrame()); logger.info("\tFrame rate: " +
		 * _header.getFrameRate());
		 */
	}

	private void ReadParameters() throws Exception {
		byte[] parameters = new byte[4];
		//int nb = 
		_reader.read(parameters, 0, 4);	//DO NOT COMMENT OUT
		// TODO we should not ignore first 4 bytes as it is now

		int nextItem;
		do {
			byte nameLen = ParameterModel.ReadNameLength(_reader);
			//boolean isLocked = nameLen < 0;			//DO NOT COMMENT OUT
			byte id = ParameterModel.ReadGroupID(_reader);
			String name = ParameterModel.ReadName(_reader, Math.abs(nameLen));
			nextItem = ParameterModel.ReadNextItemOffset(_reader);

			ParameterModel param = null;
			int parameterDataSize = 0;
			if (id > 0) // if id > 0 then it is parameter, otherwise it is group
			{
				param = new Parameter(_reader);
				parameterDataSize = ((Parameter) param).getC3DParameterSize();
			} else {
				param = new ParameterGroup();
			}

			byte descLen = ParameterModel.ReadDescLength(_reader);
			String desc = ParameterModel.ReadName(_reader, descLen);

			// general assignment
			param.setName(name);
			// System.out.println(name);
			param.setId(id);
			param.setDescription(desc);

			if (param instanceof Parameter) {
				_allParameters.add((Parameter) param);
			} else {
				_nameToGroups.put(param.getName(), (ParameterGroup) param);
				_idToGroups.put((int) param.getId(), (ParameterGroup) param);
			}

			// if next item is more far read unknown data
			// I assume that there will be no more than 512 bytes of unknown
			// data

			if (nextItem > (descLen + 3 + parameterDataSize)) {
				// If the C3D file is correct this will never happen,
				// but real life is far from perfect
				int toRead = nextItem - (descLen + 1);
				_reader.read(parameters, 0, toRead);
			}
			// else if (nextItem < (descLen + 3 + parameterDataSize)) {
			// Console.Error.WriteLine("Error");
			// }
		} while (nextItem > 0);

		for (Parameter p : _allParameters) {
			if (_idToGroups.containsKey(-p.getId())) {
				_idToGroups.get((int) -p.getId()).getParameters().add(p);
			}
		}
	}

	public <T> T GetParameter(String path, Class<T> klass) throws Exception {
		String[] elements = path.split(":");
		if (elements.length != 2) {
			throw new Exception("Wrong path format (use GROUP:PARAMETER)");
		}

		if (!_nameToGroups.containsKey(elements[0]))
			throw new Exception("Group " + elements[0] + " deasn't exist.");

		ParameterGroup grp = _nameToGroups.get(elements[0]);

		for (Parameter p : grp.getParameters()) {
			if (p.getName().equals(elements[1])) {
				return klass.cast(p.GetData(klass));
			}
		}
		throw new Exception("Parameter " + elements[1] + " deasn't exist.");
	}

	public boolean parameterExists(String path) {
		String[] elements = path.split(":");
		if (elements.length != 2) {
			System.err.print("Wrong path format (use GROUP:PARAMETER)");
			return false;
		}

		if (!_nameToGroups.containsKey(elements[0])) {
			System.err.print("Group " + elements[0] + " deasn't exist.");
			return false;
		}

		ParameterGroup grp = _nameToGroups.get(elements[0]);

		for (Parameter p : grp.getParameters()) {
			if (p.getName().equals(elements[1])) {
				return true;
			}
		}
		return false;
	}

	public Vector3[] ReadFrame() throws Exception {
		Vector3[] data;
		if (_fs == null) {
			throw new Exception(
					"The C3d file is not open. Open the file before reading.");
		}

		// if (_currentFrame >= _pointFrames)
		// {
		// _reader.BaseStream.Seek(_dataStart, 0);
		// _currentFrame = 0;
		// }

		data = _pointScale < 0 ? ReadFloatData() : ReadIntData();

		_currentFrame++;
		// System.out.println(_currentFrame);
		return data;
	}

	private Vector3[] ReadFloatData() throws IOException, Exception {
		if (!IsFloat())
			throw new Exception(
					"Data stored in C3D file are in Inetger format. You are trying to read it as a Floating-point format.");

		_points = new Vector3[_pointsNumber];
		for (int i = 0; i < _pointsNumber; i++) {
			_points[i] = new Vector3(_reader.readFloat(), _reader.readFloat(),
					_reader.readFloat());
			//int cc = (int) 
			_reader.readFloat();	//DO NOT COMMENT OUT
		}

		// Read Analog data
		int samplesPerFrame = (int) (_analogRate / _pointRate);

		float[][] allData = new float[_analogUsed][samplesPerFrame];
		for (int rate = 0; rate < samplesPerFrame; rate++) {
			for (int variable = 0; variable < _analogUsed; variable++) {

				byte b1 = _reader.readByte();
				byte b2 = _reader.readByte();
				byte b3 = _reader.readByte();
				byte b4 = _reader.readByte();

				float data = (float) (b1 & 0xFF | (b2 & 0xFF) << 8
						| (b3 & 0xFF) << 16 | (b4 & 0xFF) << 24);
				// float data = _reader.readFloat();
				// real world value = (data value - zero offset) * channel scale
				// * general scale
				allData[variable][rate] = (data - ((_analogZeroOffset != null && _analogZeroOffset.length > 0) ? (float) _analogZeroOffset[variable]
						: 0.0f))
						* _analogGenScale
						* (_analogScale != null && _analogScale.length > 0 ? (float) _analogScale[variable]
								: 1.0f);
			}
		}
		_analogData = new AnalogDataArray(_analogLabels, _analogLabelsToId,
				allData);

		return _points;
	}

	private Vector3[] ReadIntData() throws IOException, Exception {
		if (!IsInteger())
			throw new Exception(
					"Data stored in C3D file are in Floating-point format. You are trying to read it as a Integer format.");

		_points = new Vector3[_pointsNumber];
		for (int i = 0; i < _pointsNumber; i++) {
			_points[i] = new Vector3(_reader.readShort() * _pointScale,
					_reader.readShort() * _pointScale, _reader.readShort()
							* _pointScale);
			//int cc = 
			_reader.readShort();	//DO NOT COMMENT OUT
		}

		// reading of analog data
		int samplesPerFrame = (int) (_analogRate / _pointRate);
		float[][] allData = new float[_analogUsed][samplesPerFrame];
		for (int rate = 0; rate < samplesPerFrame; rate++) {
			for (int variable = 0; variable < _analogUsed; variable++) {

				byte b1 = _reader.readByte();
				byte b2 = _reader.readByte();

				float data = (short) (b1 & 0xFF | (b2 & 0xFF) << 8);
				// float data = _reader.readShort();
				// real world value = (data value - zero offset) * channel scale
				// * general scale
				allData[variable][rate] = (data - ((_analogZeroOffset != null && _analogZeroOffset.length > 0) ? (float) _analogZeroOffset[variable]
						: 0.0f))
						* _analogGenScale
						* (_analogScale != null && _analogScale.length > 0 ? (float) _analogScale[variable]
								: 1.0f);
			}
		}
		_analogData = new AnalogDataArray(_analogLabels, _analogLabelsToId,
				allData);

		return _points;
	}

	public Vector3 get(int key) throws Exception {
		if (_points == null) {
			throw new Exception("You must open file and read freame first");
		} else if (key < 0 || key >= _points.length) {
			throw new IndexOutOfBoundsException();
		}
		return _points[key];
	}

	public Vector3 get(String key) throws Exception {
		if (_pointsLabels == null) {
			throw new Exception("You must open file and read freame first");
		}
		if (!_pointsLabelsToId.containsKey(key)) {
			throw new Exception("Label " + key
					+ " doesn't exist in the 3D point data section");
		}
		return _points[_pointsLabelsToId.get(key)];
	}

	/*
	 * public void ReadEventContexts() throws Exception { Short contextsCount =
	 * GetParameter("EVENT_CONTEXT:USED", Short.class); String[] contexts =
	 * GetParameter("EVENT_CONTEXT:LABELS", String[].class); String[] desc =
	 * GetParameter("EVENT_CONTEXT:DESCRIPTIONS", String[].class); Short[]
	 * icon_ids = GetParameter("EVENT_CONTEXT:ICON_IDS", Short[].class); Short[]
	 * colours = GetParameter("EVENT_CONTEXT:COLOURS", Short[].class); }
	 */

	private void ReadEvents() throws Exception {
		
		if (!parameterExists("EVENT:USED")) {
			return;
		}

		String[] contexts = GetParameter("EVENT:CONTEXTS", String[].class);
		String[] labels = GetParameter("EVENT:LABELS", String[].class);
		String[] descriptions = GetParameter("EVENT:DESCRIPTIONS",
				String[].class);
		String[] subjects = GetParameter("EVENT:SUBJECTS", String[].class);
		Float[][] times = GetParameter("EVENT:TIMES", Float[][].class);
		Short[] icon_ids = GetParameter("EVENT:ICON_IDS", Short[].class);
		Byte[] generic_flags = GetParameter("EVENT:GENERIC_FLAGS", Byte[].class);

		for (int i = 0; i < labels.length; i++) {
			Float time = times[0][i] * 60 + times[1][i];
			short frame = Short.parseShort(((Float) (time * _header
					.getFrameRate())).toString());

			C3dEvent c3dEvent = new C3dEvent(labels[i], contexts[i], frame);
			c3dEvent.setDescription(descriptions[i]);
			c3dEvent.setSubject(subjects[i]);
			c3dEvent.setIconId(icon_ids[i]);
			c3dEvent.setGenericFlag(generic_flags[i]);

			_events.add(c3dEvent);
		}
	}

	public boolean Close() throws IOException {
		_reader.close();
		return true;
	}
}