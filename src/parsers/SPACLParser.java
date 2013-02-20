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
import common.CommonUtilities;
import common.Gate;
import common.GateTypes;
import common.InputGateType;

public class SPACLParser implements CircuitParser<List<Gate>> {

	private File circuitFile;
	private String numberOfleftInput;
	private String numberOfrightInput;
	private String numberOfOutput;
	private String xorMaxlayerSize;
	private String invMaxlayerSize;
	private String andMaxlayerSize;
	private int numberOfLayers;
	private int numberOfNonXorGates;
	private int numberOfWires;

	public SPACLParser(File circuitFile) {
		this.circuitFile = circuitFile;
		numberOfNonXorGates = 0;
	}
	@Override
	public List<List<Gate>> getGates() {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(getCircuitFile()), Charset.defaultCharset()));
			String line = fbr.readLine();
			//hack to skip first line
			int i= -1;
			while((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}
				line = line.trim();
				if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.XOR)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.XOR).length() + 1;
					System.out.println(length);
					xorMaxlayerSize = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.INV)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.INV).length() + 1;
					invMaxlayerSize = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.AND)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.AND).length() + 1;
					andMaxlayerSize = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.PRIVATE_LOAD)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.PRIVATE_LOAD).length() + 1;
					numberOfleftInput = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.PUBLIC_LOAD)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.PUBLIC_LOAD).length() + 1;
					numberOfrightInput = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.MAX_WIDTH + CommonUtilities.PUBLIC_STORE)) {
					int length = (CommonUtilities.MAX_WIDTH + CommonUtilities.PUBLIC_STORE).length() + 1;
					numberOfOutput = line.substring(length, line.length() - 2);
				} else if (line.contains(CommonUtilities.BEGIN_LAYER + CommonUtilities.XOR) || 
						line.contains(CommonUtilities.BEGIN_LAYER + CommonUtilities.INV) ||
						line.contains(CommonUtilities.BEGIN_LAYER + CommonUtilities.AND)) {
					layersOfGates.add(new ArrayList<Gate>());
					i++;
				} else if (line.startsWith(CommonUtilities.XOR + "(")) {
					layersOfGates.get(i).add(getGate(line, GateTypes.XOR));
				} else if (line.startsWith(CommonUtilities.INV + "(")) {
					layersOfGates.get(i).add(getGate(line, GateTypes.INV));
					numberOfNonXorGates++;
				} else if (line.startsWith(CommonUtilities.AND + "(")) {
					layersOfGates.get(i).add(getGate(line, GateTypes.AND));
					numberOfNonXorGates++;
				}
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		numberOfLayers = layersOfGates.size();
		numberOfWires = CommonUtilities.getWireCountList(layersOfGates);

		return layersOfGates;
	}

	public File getCircuitFile() {
		return circuitFile;
	}

	private Gate getGate(String line, GateTypes type) {
		String[] split = line.split(",");
		if (type == GateTypes.INV) {
			String output = split[0].substring(4);
			String leftInput = split[1];
			String gateString = "1 1 " + leftInput + " " +
					output + " " + "-1";
			return new Gate(gateString, InputGateType.FAIRPLAY);
		} else {
			String gateType = "";
			String output = split[0].substring(4);
			String leftInput = split[1];
			String rightInput = split[2];
			
			if (type.equals(GateTypes.XOR)) {
				gateType = "0110";
			} else if (type.equals(GateTypes.AND)) {
				gateType = "0001";
			}

			String gateString = "2 1 " + leftInput + " " + rightInput + " " +
					output + " " + gateType;
			return new Gate(gateString, InputGateType.FAIRPLAY);
		}
	}

	@Override
	public String[] getHeaders() {
		int input = Integer.parseInt(numberOfleftInput) + Integer.parseInt(numberOfrightInput);
		int maxLayer = Math.max(Integer.parseInt(xorMaxlayerSize), Integer.parseInt(invMaxlayerSize));
		maxLayer = Math.max(maxLayer, Integer.parseInt(andMaxlayerSize));

		return new String[]{input + " " + numberOfOutput + " " + numberOfWires +
				" " + numberOfLayers + " " + maxLayer + " " + numberOfNonXorGates};
	}

}
