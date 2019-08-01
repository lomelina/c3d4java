package be.etrovub.c3d4java.parameters;

import java.io.RandomAccessFile;
import java.util.HashSet;

import be.etrovub.c3d4java.parameters.Parameter;

public class ParameterGroup extends ParameterModel {
	public ParameterGroup() {
		_parameters = new HashSet<Parameter>();
	}

	private HashSet<Parameter> _parameters;

	public HashSet<Parameter> getParameters() {
		return _parameters;
	}

	public boolean HasParameter(String name) {
		for (Parameter p : _parameters) {
			if (p.getName() == name) {
				return true;
			}
		}
		return false;
	}

	public Parameter GetParameter(String name) {
		for (Parameter p : _parameters) {
			if (p.getName() == name) {
				return p;
			}
		}
		return null;
	}

	public void ResetOffsetInFile() {
		for (Parameter p : _parameters) {
			p.ResetOffsetInFile();
		}
	}

	@Override
	protected short GetContentLength() {
		return 0;// ParameterGroup doesn't have content (NOTE: Parameters are
					// children, not content)
	}

	@Override
	public void WriteContent(RandomAccessFile writer) {		
	}
}
