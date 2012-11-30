import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	private HashMap<String, Integer> stateMap;
	private HashMap<String, Integer> roundMap;
	private int stateValue;
	private int roundValue;

	List<Gate> leftOutputGates;
	List<Gate> rightOutputGates;
	List<Gate> outputGates;

	List<Gate> leftRoundGates;
	List<Gate> rightRoundGates;
	List<Gate> outputRoundGates;

	List<Gate> leftStateGates;
	List<Gate> rightStateGates;
	List<Gate> outputStateGates;

	int maxOutputWire;

	public VerilogToFairplayConverter(File circuitFile, File outputFile){
		this.circuitFile = circuitFile;
		this.outputFile = outputFile;
		stateMap = new HashMap<String, Integer>();
		roundMap = new HashMap<String, Integer>();

		leftOutputGates = new ArrayList<Gate>();
		rightOutputGates = new ArrayList<Gate>();
		outputGates = new ArrayList<Gate>();

		leftRoundGates = new ArrayList<Gate>();
		rightRoundGates = new ArrayList<Gate>();
		outputRoundGates = new ArrayList<Gate>();

		leftStateGates = new ArrayList<Gate>();
		rightStateGates = new ArrayList<Gate>();
		outputStateGates = new ArrayList<Gate>();

		maxOutputWire = 0;

	}

	@Override
	public void run() {
		List<Gate> gates = analyzeCircuit();
		List<Gate> res = getGates(gates);
		incrementGates(res);
		firstHeader = res.size() + " " + CommonUtilities.getWireCount(res);
		secondHeader = 0 + " " + numberOfInputs + " " + "0" + " " + numberOfOutputs;
		String[] headers = {firstHeader, secondHeader};
		//CommonUtilities.outputFairplayCircuit(res, outputFile, headers);

	}

	private List<Gate> analyzeCircuit() {
		List<Gate> res = new ArrayList<Gate>();
		boolean[] blankWires;
		MultiValueMap leftMap = new MultiValueMap();
		MultiValueMap rightMap = new MultiValueMap();
		HashMap<Integer, Gate> outputMap = new HashMap<Integer, Gate>();
		boolean first = true;
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = "";
			String lastLine = "";
			HashSet<String> hs = new HashSet<String>();
			while ((line = fbr.readLine()) != null) {
				line = StringUtils.trim(line);
				
				if (line.isEmpty() || line.startsWith("//") || line.startsWith("module")
						|| line.startsWith("endmodule") || line.startsWith("wire")){
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
					if (!line.endsWith(";")) {
						lastLine = lastLine + " " + line;
						continue;
					} else {
						line = lastLine + " " + line;
						line = StringUtils.trim(line);
						line = line.replace(" )", ")");
						line = line.replace("] [", "][");
						lastLine = "";
					}
					
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
						if (split[2].contains("\\round_data")) {
							hs.add(split[2]);
						}
						if (split[4].contains("\\round_data")) {
							hs.add(split[4]);
						}
					} else {
						leftWire = getWire(split[2]);
						rightWire = getWire(split[4]);
						outputWire = getOutputWire(split[6]);
						
						if (split[2].contains("\\round_data")) {
							hs.add(split[2]);
						}
						if (split[4].contains("\\round_data")) {
							hs.add(split[4]);
						}
						if (split[6].contains("\\round_data")) {
							hs.add(split[6]);
						}
					}
					if (split[2].contains("\\round_data")) {
						hs.add(split[2]);
					}
					if (split[4].contains("\\round_data")) {
						hs.add(split[4]);
					}
					if (split[4].contains("\\round_data")) {
						hs.add(split[4]);
					}

					boolean leftOutputFlag = false;
					boolean rightOutputFlag = false;
					boolean outputFlag = false;
					boolean leftRoundFlag = false;
					boolean rightRoundFlag = false;
					boolean outputRoundFlag = false;
					boolean leftStateFlag = false;
					boolean rightStateFlag = false;
					boolean outputStateFlag = false;

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
					if (leftWire.startsWith("r")) {
						leftWire = leftWire.substring(1);
						leftRoundFlag = true;
					}
					if (rightWire.startsWith("r")) {
						rightWire = rightWire.substring(1);
						rightRoundFlag = true;
					}
					if (outputWire.startsWith("r")) {
						outputWire = outputWire.substring(1);
						outputRoundFlag = true;
					}
					if (leftWire.startsWith("s")) {
						leftWire = leftWire.substring(1);
						leftStateFlag = true;
					}
					if (rightWire.startsWith("s")) {
						rightWire = rightWire.substring(1);
						rightStateFlag = true;
					}
					if (outputWire.startsWith("s")) {
						outputWire = outputWire.substring(1);
						outputStateFlag = true;
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

					if (leftRoundFlag) {
						leftRoundGates.add(g);
					}
					if (rightRoundFlag) {
						rightRoundGates.add(g);
					}
					if (outputRoundFlag) {
						outputRoundGates.add(g);
					}

					if (leftStateFlag) {
						leftStateGates.add(g);
					}
					if (rightStateFlag) {
						rightStateGates.add(g);
					}
					if (outputStateFlag) {
						outputStateGates.add(g);
					}
				}
			}
			System.out.println(hs.size());
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		for(Gate g: res) {
			maxOutputWire = Math.max(maxOutputWire, g.getOutputWireIndex());
		}
		return res;
	}

	private void incrementGates(List<Gate> res) {
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

	public List<Gate> getGates(List<Gate> gates) {
		List<Gate> res = new ArrayList<Gate>();
		HashMap<String, Integer> gateMap = new HashMap<String, Integer>();
		System.out.println(roundMap.size());
		
		// TODO run through the special lists and add the correct wire numbers. Should
		// start with maxOutputWire + 1. Use a HashMap<Integer,String> to keep track
		// on previous wire numbers, if the string is not in the map then we add set the
		// wire number to maxOutputWire + 1 and increment maxOutputWire accordingly.
		// When done we return and the incrementGates should incrment the output gates
		// to the new highest value.
		
		
		return res;
	}

	private String getOutputWire(String s) {
		if (s.startsWith("(\\round_data")){
			return "r" + putInRoundMap(s);
		} else if (s.startsWith("(\\next_state")) {
			return "s" + putInStateMap(s);
		} else if (s.startsWith("(output_o")) {
			return "o" + s.substring(10, s.length() - 4);
		} else {
			int i = Integer.parseInt(s.substring(3, s.length() - 3));
			return Integer.toString(i + numberOfInputs + 2);
		}
	}

	private String getWire(String s) {
		if (s.startsWith("(\\round_data")){
			return "r" + putInRoundMap(s);
		} else if (s.startsWith("(\\next_state")) {
			return "s" + putInStateMap(s);
		} else if (s.startsWith("(input_i")) {
			return s.substring(9, s.length() - 3);
		} else if (s.startsWith("(output_o")) {
			return "o" + s.substring(10, s.length() - 3);
		} else {
			int i = Integer.parseInt(s.substring(3, s.length() - 2));
			return Integer.toString(i + numberOfInputs + 2);
		}
	}
	
	private int putInStateMap(String s) {
		if(stateMap.get(s) != null) {
			return stateMap.get(s);
		} else {
			stateMap.put(s, stateValue++);
			return stateValue - 1;
		}
		
	}
	
	private int putInRoundMap(String s) {
		if(roundMap.get(s) != null) {
			return roundMap.get(s);
		} else {
			roundMap.put(s, roundValue++);
			return roundValue - 1;
		}
		
	}
}
