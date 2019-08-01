package be.etrovub.c3d4java;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnalogDataArray {
	private List<String> _analogLabels;
	private Map<String, Integer> _analogLabelsToId;
	private float[][] _analogData = null;

	public float[][] getData() {
		return _analogData;
	}

	public List<String> getLabels() {
		return Collections.unmodifiableList(_analogLabels);
	}

	protected AnalogDataArray(List<String> analogLabels,
			Map<String, Integer> analogLabelsToId, float[][] analogData) {
		_analogLabels = analogLabels;
		_analogLabelsToId = analogLabelsToId;
		_analogData = analogData;
	}

	/*
	 * public float this[int key, int channel] { get { if (_analogData == null)
	 * { throw new Exception("You must open file and read freame first"); } else
	 * if (key < 0 || key >= _analogData.length) { throw new
	 * IndexOutOfRangeException(); } return _analogData[key][channel]; } }
	 */
	public float get(int key, int channel) throws Exception {
		if (_analogData == null) {
			throw new Exception("You must open file and read freame first");
		} else if (key < 0 || key >= _analogData.length) {
			throw new IndexOutOfBoundsException();
		}
		return _analogData[key][channel];
	}

	/*
	 * public float this[int key] { get { if (_analogData == null) { throw new
	 * Exception("You must open file and read freame first"); } else if (key < 0
	 * || key >= _analogData.length) { throw new IndexOutOfRangeException(); }
	 * return _analogData[key][0]; } }
	 */
	public float get(int key) throws Exception {
		if (_analogData == null) {
			throw new Exception("You must open file and read freame first");
		} else if (key < 0 || key >= _analogData.length) {
			throw new IndexOutOfBoundsException();
		}
		return _analogData[key][0];
	}

	/*
	 * public float this[string key, int channel] { get { if (_analogLabels ==
	 * null) { throw new Exception("You must open file and read freame first");
	 * } if (!_analogLabelsToId.containsKey(key)) { throw new
	 * Exception("Analog data label " + key +
	 * " doesn't exist in the 3D point data section"); } return
	 * _analogData[_analogLabelsToId[key]][channel]; } }
	 */
	public float get(String key, int channel) throws Exception {
		if (_analogLabels == null) {
			throw new Exception("You must open file and read freame first");
		}
		if (!_analogLabelsToId.containsKey(key)) {
			throw new Exception("Analog data label " + key
					+ " doesn't exist in the 3D point data section");
		}
		return _analogData[_analogLabelsToId.get(key)][channel];
	}

	/*
	 * public float this[string key] { get { if (_analogLabels == null) { throw
	 * new Exception("You must open file and read freame first"); } if
	 * (!_analogLabelsToId.containsKey(key)) { throw new
	 * Exception("Analog data label " + key +
	 * " doesn't exist in the 3D point data section"); } return
	 * _analogData[_analogLabelsToId[key], 0]; } }
	 */
	public float get(String key) throws Exception {
		if (_analogLabels == null) {
			throw new Exception("You must open file and read freame first");
		}
		if (!_analogLabelsToId.containsKey(key)) {
			throw new Exception("Analog data label " + key
					+ " doesn't exist in the 3D point data section");
		}
		return _analogData[_analogLabelsToId.get(key)][0];
	}
}
