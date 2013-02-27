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

	private int numberOfInputs;
	private int numberOfOutputs;

	private int numberOfNonXORGates;

	private int numberOfWires;

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
					String[] split = headerLine.split(" ");
					numberOfInputs = Integer.parseInt(split[0]);
					numberOfInputs = Integer.parseInt(split[1]);
					numberOfWires = Integer.parseInt(split[2]);
					numberOfNonXORGates = Integer.parseInt(split[5]);


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

	@Override
	public int getNumberOfInputs() {
		return numberOfInputs;
	}

	@Override
	public int getNumberOfOutputs() {
		return numberOfOutputs;
	}

	@Override
	public int getNumberOfNonXORGates() {
		return numberOfNonXORGates;
	}

	/*
	 * Unavailible
	 */
	@Override
	public int getNumberOfP1Inputs() {
		return 0;
	}

	/*
	 * Unavailible
	 */
	@Override
	public int getNumberOfP2Inputs() {
		return 0;
	}

	@Override
	public int getNumberOfWires() {
		return numberOfWires;
	}

}
