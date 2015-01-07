package parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import common.CircuitParser;
import common.CommonUtilities;
import common.Gate;
import common.InputGateType;

public class SCDParser implements CircuitParser<List<Gate>> {
	
	private File circuitFile;

	private int numberOfInputs;
	private int numberOfOutputs;

	private int numberOfANDGates;

	private int numberOfWires;

	private String headerLine;
	
	public SCDParser(File circuitFile) {
		this.circuitFile = circuitFile;
	}

	@Override
	public File getCircuitFile() {
		return circuitFile;
	}

	@Override
	public List<List<Gate>> getGates() {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();
		List<Gate> gates = new ArrayList<Gate>();

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			
			String line = fbr.readLine();
			fbr.close();
			String[] split = line.split(" ");
			numberOfInputs = Integer.parseInt(split[0].substring(1, split[0].length()-1));
			numberOfOutputs = Integer.parseInt(split[1].substring(0, split[1].length()-1));
			int numberOfGates = Integer.parseInt(split[2].substring(0, split[2].length()-1));
			
			List<String> AList = getList(split[3]);
			List<String> BList = getList(split[4]);
			List<String> TList = getList(split[5]);
			List<String> OList = getList(split[6]);
			
			for (int i = 0; i < numberOfGates; i++) {
				String gate = "";
				int type = Integer.parseInt(TList.get(i));
				
				int outputWire = numberOfInputs + i;
				if (type == 6) { // XOR
					gate = "2 1 " + AList.get(i) + " " + BList.get(i) + " " + outputWire + " 0110";
				} else if (type == 3) { // INV
					gate = "1 1 " + AList.get(i) + " " + BList.get(i) + " " + outputWire + " -1";
					numberOfANDGates++;
				} else if (type == 8) { // AND
					gate = "2 1 " + AList.get(i) + " " + BList.get(i) + " " + outputWire + " 0001";
					numberOfANDGates++;
				}
				Gate g = new Gate(gate, InputGateType.FAIRPLAY);
				gates.add(g);
			}
			
			numberOfWires = CommonUtilities.getWireCount(gates);
			headerLine = numberOfInputs + " " + numberOfOutputs + " " + numberOfWires;
			
			Map<Integer, Integer> convertTo = new TreeMap<Integer, Integer>();
			Map<Integer, Integer> convertFrom = new TreeMap<Integer, Integer>();
			for (int i = 0; i < numberOfOutputs; i++) {
				int out = Integer.parseInt(OList.get(i));
				convertTo.put(out, numberOfWires-numberOfOutputs+i);
				convertFrom.put(numberOfWires-numberOfOutputs+i, out);
			}
			for (Gate g: gates) {
				int leftIndex = g.getLeftWireIndex();
				int rightIndex = g.getRightWireIndex();
				int outIndex = g.getOutputWireIndex();
				
				if (convertTo.containsKey(leftIndex)) {
					g.setLeftWireIndex(convertTo.get(leftIndex));
				}
				if (convertTo.containsKey(rightIndex)) {
					g.setRightWireIndex(convertTo.get(rightIndex));
				}
				if (convertTo.containsKey(outIndex)) {
					g.setOutputWireIndex(convertTo.get(outIndex));
				}
				
				//Other way
				
				if (convertFrom.containsKey(leftIndex)) {
					g.setLeftWireIndex(convertFrom.get(leftIndex));
				}
				if (convertFrom.containsKey(rightIndex)) {
					g.setRightWireIndex(convertFrom.get(rightIndex));
				}
				if (convertFrom.containsKey(outIndex)) {
					g.setOutputWireIndex(convertFrom.get(outIndex));
				}
			}
			
			layersOfGates.add(gates);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return layersOfGates;
	}
	
	private List<String> getList(String s) {
		return new ArrayList<String>(Arrays.asList(s.substring(1, s.length()-2).split(",")));
	}

	@Override
	public String[] getHeaders() {
		return new String[]{headerLine};
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
	public int getNumberOfANDGates() {
		return numberOfANDGates;
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
