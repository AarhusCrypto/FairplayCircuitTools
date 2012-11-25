import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;


public class VerilogToFairplayConverter implements Runnable {
	private File circuitFile;
	private File outputFile;
	private int numberOfInputs;
	private int numberOfOutputs;
	private String firstHeader;
	private String secondHeader;
	List<Gate> leftOutputGates;
	List<Gate> rightOutputGates;
	List<Gate> outputGates;
	int maxOutputWire;

	public VerilogToFairplayConverter(File circuitFile, File outputFile){
		this.circuitFile = circuitFile;
		this.outputFile = outputFile;
		leftOutputGates = new ArrayList<Gate>();
		rightOutputGates = new ArrayList<Gate>();
		outputGates = new ArrayList<Gate>();
		maxOutputWire = 0;

	}

	@Override
	public void run() {
		List<String> gateStrings = analyzeCircuit();
		List<Gate> res = getGates(gateStrings);
		incrementeGates(res);
		firstHeader = res.size() + " " + CommonUtilities.getWireCount(res);
		secondHeader = 0 + " " + numberOfInputs + " " + "0" + " " + numberOfOutputs;
		String[] headers = {firstHeader, secondHeader};
		CommonUtilities.outputFairplayCircuit(res, outputFile, headers);

	}

	private List<String> analyzeCircuit() {
		List<String> res = new ArrayList<String>();
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = "";
			while ((line = fbr.readLine()) != null) {
				line = StringUtils.trim(line);
				if (line.isEmpty() || line.startsWith("//") || line.startsWith("module")
						|| line.startsWith("endmodule")){
					continue;
				} else if (line.startsWith("input [")) {
					String[] split = line.split(" ");
					String inputInfo = split[1];
					String inputNumber = 
							inputInfo.substring(3, inputInfo.length() - 1);
					numberOfInputs = Integer.parseInt(inputNumber) + 1;
					continue;
				} else if (line.startsWith("output [")) {
					String[] split = line.split(" ");
					String outputInfo = split[1];
					String outputNumber = 
							outputInfo.substring(3, outputInfo.length() - 1);
					numberOfOutputs = Integer.parseInt(outputNumber) + 1;
					continue;
				} else {
					//TODO Analysis of the circuit, max outputgate, etc
					
					res.add(line);
				}
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return res;


	}

	private void incrementeGates(List<Gate> res) {
		int incNumber = maxOutputWire + 1;

		for (Gate g: leftOutputGates) {
			g.setLeftWireIndex(g.getLeftWireIndex() + incNumber);
		}
		for (Gate g: rightOutputGates) {
			g.setRightWireIndex(g.getRightWireIndex() + incNumber);
		}
		for (Gate g: outputGates) {
			g.setOutputWireIndex(g.getOutputWireIndex() + incNumber);
		}
	}

	public List<Gate> getGates(List<String> gates) {
		List<Gate> res = new ArrayList<Gate>();
		boolean[] blankWires;
		MultiValueMap leftMap = new MultiValueMap();
		MultiValueMap rightMap = new MultiValueMap();
		HashMap<Integer, Gate> outputMap = new HashMap<Integer, Gate>();
		boolean first = true;


		//Constructs the constant 1 at first wire after input
		if(first) {
			String nAND1 = "2 1 0 0 " + numberOfInputs + " 1110";
			String nAND2 = "2 1 0 " + numberOfInputs + " " + (numberOfInputs + 1) + " 1110";
			Gate g1 = new Gate(nAND1);
			Gate g2 = new Gate(nAND2);
			res.add(g1);
			res.add(g2);
			first = false;
		}
		for(String line: gates){


			//Parsing of gates begins here
			String[] split = line.split(" ");

			String inputs;
			String outputs = "1";
			if (split[1].endsWith("I")) {
				inputs = "1";
			} else {
				inputs = "2";
			}

			String leftWire = "";
			String rightWire = "";
			String outputWire = "";
			String boolTable = "";
			if(split[0].equals("XOR2HS")) {
				boolTable = "0110";
			}
			if (split[0].equals("AN2")) {
				boolTable = "0001";
			}
			if (split[0].equals("INV1S")) {
				inputs = "2";
				boolTable = "0110";
				leftWire = getWire(split[2]);
				rightWire = Integer.toString(numberOfInputs + 1);
				outputWire = getOutputWire(split[4]);
			} else {
				leftWire = getWire(split[2]);
				rightWire = getWire(split[4]);
				outputWire = getOutputWire(split[6]);
			}

			boolean leftOutputFlag = false;
			boolean rightOutputFlag = false;
			boolean outputFlag = false;

			if (leftWire.startsWith("o")) {
				leftWire = leftWire.substring(1);
				leftOutputFlag = true;
			}
			if (rightWire.startsWith("o")) {
				rightWire = rightWire.substring(1);
				rightOutputFlag = true;
			}
			if (outputWire.startsWith("o")) {
				outputWire = outputWire.substring(1);
				outputFlag = true;
			}

			String gateString = inputs + " " + outputs + " " + leftWire +
					" " + rightWire + " " + outputWire + " " + boolTable;

			Gate g = new Gate(gateString);

			maxOutputWire = Math.max(maxOutputWire, g.getOutputWireIndex());

			res.add(g);
			if (leftOutputFlag) {
				leftOutputGates.add(g);
			}
			if (rightOutputFlag) {
				rightOutputGates.add(g);
			}
			if (outputFlag) {
				outputGates.add(g);
			}

			blankWires = new boolean[maxOutputWire + 1];
			for(Gate g0: res) {
				int leftIndex = g0.getLeftWireIndex();
				int rightIndex = g0.getRightWireIndex();
				int outputIndex = g0.getOutputWireIndex();

				leftMap.put(leftIndex, g0);
				rightMap.put(rightIndex, g0);
				outputMap.put(g0.getOutputWireIndex(), g0);
				blankWires[leftIndex] = true;
				blankWires[rightIndex] = true;
				blankWires[outputIndex] = true;	
			}

			//Strip blank wires
			for(int i =  maxOutputWire; i >= 0; i--){
				boolean b = blankWires[i];
				if(!b){
					for(int j = i; j <= maxOutputWire; j++){
						Gate outputG = outputMap.get(j);
						if (outputG != null){
							outputG.setOutputWireIndex(outputG.getOutputWireIndex() - 1);
						}

						Collection<Gate> leftWires = leftMap.getCollection(j);
						if (leftWires != null){
							for(Gate leftG: leftWires){
								leftG.setLeftWireIndex(leftG.getLeftWireIndex() - 1);
							}
						}

						Collection<Gate> rightWires = rightMap.getCollection(j);
						if (rightWires != null){
							for(Gate rightG: rightWires){
								rightG.setRightWireIndex(rightG.getRightWireIndex() - 1);
							}
						}
					}
				}
			}
			maxOutputWire = 0;
			for(Gate g2: res) {
				maxOutputWire = Math.max(maxOutputWire, g2.getOutputWireIndex());
			}
		}

		return res;
	}

	private String getOutputWire(String s) {
		if (s.startsWith("(output_o")) {
			return "o" + s.substring(10, s.length() - 4);
		} else {
			int i = Integer.parseInt(s.substring(3, s.length() - 3));
			return Integer.toString(i + numberOfInputs + 2);
		}
	}

	private String getWire(String s) {
		if (s.startsWith("(input_i")) {
			return s.substring(9, s.length() - 3);
		} else if (s.startsWith("(output_o")) {
			return "o" + s.substring(10, s.length() - 3);
		} else {
			int i = Integer.parseInt(s.substring(3, s.length() - 2));
			return Integer.toString(i + numberOfInputs + 2);
		}
	}
}
