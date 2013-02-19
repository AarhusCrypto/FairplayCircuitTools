package parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import common.CircuitParser;
import common.Gate;
import common.InputGateType;


public class CUDAParser implements CircuitParser<List<Gate>> {

	private File circuitFile;
	private String headerLine;

	public CUDAParser(File circuitFile) {
		this.circuitFile = circuitFile;
	}

	public List<List<Gate>> getGates() {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();
		boolean firstLine = true;

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line;
			//hack to skip first line
			List<Gate> currentLayer = null;
			while ((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
				} else if (firstLine) {
					headerLine = line;
					firstLine = false;
				} else if (line.startsWith("*")) {
					currentLayer = new ArrayList<Gate>();
					layersOfGates.add(currentLayer);
				} else {
					// Parse each gate line and count numberOfNonXORGates
					Gate g = new Gate(line, InputGateType.CUDA);
					currentLayer.add(g);
				}
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return layersOfGates;
	}

	public String[] getHeaders() {
		return new String[]{headerLine};
	}

	public File getCircuitFile() {
		return circuitFile;
	}

}
