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

	public CUDAParser(File circuitFile) {
		this.circuitFile = circuitFile;
	}

	public List<List<Gate>> getGates() {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = fbr.readLine();
			//hack to skip first line
			List<Gate> currentLayer = null;
			while((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}

				if(line.startsWith("*")){
					currentLayer = new ArrayList<Gate>();
					layersOfGates.add(currentLayer);
					continue;
				}

				// Parse each gate line and count numberOfNonXORGates
				Gate g = new Gate(line, InputGateType.CUDA);
				currentLayer.add(g);
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return layersOfGates;
	}

	// TODO: Fix this to not read the file each time
	public String[] getHeaders() {
		BufferedReader fbr = null;
		String line = null;
		try {
			fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			line = fbr.readLine();
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String[]{line};
	}

}
