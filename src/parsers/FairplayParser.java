package parsers;

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

import common.CircuitParser;
import common.CommonUtilities;
import common.Gate;

import org.apache.commons.collections.map.MultiValueMap;


public class FairplayParser implements CircuitParser<Gate> {

	private File circuitFile;
	private int originalNumberOfWires;
	private int numberOfWiresParsed;

	private int numberOfP1Inputs;
	private int numberOfP2Inputs;
	private int numberOfP1Outputs;
	private int numberOfP2Outputs;
	private int numberOfNonXORGates;
	private int totalNumberOfInputs;
	private int totalNumberOfOutputs;

	private boolean[] blankWires;
	MultiValueMap leftMap;
	MultiValueMap rightMap;
	HashMap<Integer, Gate> outputMap;

	private String secondHeader;
	private int addedWires;
	private boolean stripWires;

	public FairplayParser(File circuitFile, boolean stripWires){
		this.circuitFile = circuitFile;
		this.stripWires = stripWires;
		this.addedWires = 0;
		this.leftMap = new MultiValueMap();
		this.rightMap = new MultiValueMap();
		this.outputMap = new HashMap<Integer, Gate>();
	}

	/**
	 * @return A list of gates in the given circuitFile
	 */
	public List<Gate> getGates() {
		boolean secondLine = false;
		boolean constantGateCounter = false;
		List<Gate> res = new ArrayList<Gate>();

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = "";
			while((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}

				/*
				 * Parse meta-data info
				 */
				if(line.matches("[0-9]* [0-9]*")){
					String[] sizeInfo = line.split(" ");
					originalNumberOfWires = Integer.parseInt(sizeInfo[1]);
					blankWires = new boolean[originalNumberOfWires];
					secondLine = true;

					continue;
				}

				/*
				 * Parse number of input bits
				 */
				if (secondLine){
					secondHeader = line;

					String[] split = getHeaderArray(line);

					numberOfP1Inputs = Integer.parseInt(split[0]);
					numberOfP2Inputs = Integer.parseInt(split[1]);
					if (split.length < 4) {
						numberOfP1Outputs = 0;
						numberOfP2Outputs = Integer.parseInt(split[2]);
					} else {
						numberOfP1Outputs = Integer.parseInt(split[2]);
						numberOfP2Outputs = Integer.parseInt(split[3]);
					}

					totalNumberOfInputs = numberOfP1Inputs + numberOfP2Inputs;
					totalNumberOfOutputs = numberOfP1Outputs + numberOfP2Outputs;
					secondLine = false;
					continue;
				}

				// If format
				if (!constantGateCounter && (line.endsWith("INV") || 
						line.endsWith("XOR") || line.endsWith("AND"))) {

					//Constructs the constant 1 wire at second first wire after input
					String nAND1 = "2 1 0 0 " + totalNumberOfInputs + " 1110";
					String nAND2 = "2 1 0 " + totalNumberOfInputs + " " + (totalNumberOfInputs + 1) + " 1110";
					Gate g1 = new Gate(nAND1);
					Gate g2 = new Gate(nAND2);

					res.add(g1);
					res.add(g2);
					addedWires = 2;
					originalNumberOfWires += addedWires;
					
					// blankWires must be overwritten before adding g1 and g2
					blankWires = new boolean[originalNumberOfWires];
					addToWireInfo(g1);
					addToWireInfo(g2);
					constantGateCounter = true;
				}

				// Transforms format from http://www.cs.bris.ac.uk/Research/CryptographySecurity/MPC/
				// to standard Fairplay.
				String[] split = line.split(" ");
				if (line.endsWith("INV")) {
					line = "2 " + split[1] + " " + getWire(split[2]) + " " + 
							(totalNumberOfInputs + 1) + " " + getWire(split[3]) + " " + "0110";
				} else if (line.endsWith("XOR")) {
					line = split[0] + " " + split[1] + " " + getWire(split[2]) + " " + 
							getWire(split[3]) + " " + getWire(split[4]) + " " + "0110";
				} else if (line.endsWith("AND")) {
					line = split[0] + " " + split[1] + " " + getWire(split[2]) + " " + 
							getWire(split[3]) + " " + getWire(split[4]) + " " + "0001";
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new Gate(line);
				addToWireInfo(g);


				if (!g.isXOR()){
					g.setGateNumber(numberOfNonXORGates);
					numberOfNonXORGates++;
				}
				res.add(g);
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//This is the number of unique wires parsed
		numberOfWiresParsed = CommonUtilities.getWireCount(res);
		if (stripWires) {
			stripBlankWires(res);
		} else {
			int nonStrippedWires = 0;
			for (boolean b: blankWires) {
				if (!b) {
					nonStrippedWires++;
				}
			}
			numberOfWiresParsed += nonStrippedWires;
		}	

		return res;
	}
	
	private int getWire(String s) {
		int wire = Integer.parseInt(s);
		if (wire < totalNumberOfInputs) {
			return wire;
		} else {
			return wire + addedWires;
		}
	}

	private void addToWireInfo(Gate g) {
		int leftIndex = g.getLeftWireIndex();
		int rightIndex = g.getRightWireIndex();
		int outputIndex = g.getOutputWireIndex();

		// Accumulate information for later usage
		leftMap.put(leftIndex, g);
		rightMap.put(rightIndex, g);
		outputMap.put(outputIndex, g);
		blankWires[leftIndex] = true;
		blankWires[rightIndex] = true;
		blankWires[outputIndex] = true;	

	}

	@SuppressWarnings("unchecked")
	private void stripBlankWires(List<Gate> res) {

		// We now strip the blank wires
		// false means blank
		// Runs from top to bottom, decrementing the appropriate wires
		// Is a bit funky since we cannot guarantee the input circuit
		// is sorted by output wires
		for (int i =  originalNumberOfWires - 1; i >= 0; i--) {
			boolean b = blankWires[i];
			if(!b){
				for (int j = i; j <= originalNumberOfWires; j++) {
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
	}

	public String[] getHeaders() {
		return getHeaderArray(secondHeader);
	}

	public int getNumberOfInputs() {
		return totalNumberOfInputs;
	}
	
	public int getNumberOfOutputs() {
		return totalNumberOfOutputs;
	}
	
	public int getNumberOfNonXORGates() {
		return numberOfNonXORGates;
	}

	public int getNumberOfP1Inputs() {
		return numberOfP1Inputs;
	}

	public int getNumberOfP2Inputs() {
		return numberOfP2Inputs;
	}

	public int getNumberOfP1Outputs() {
		return numberOfP1Outputs;
	}

	public int getNumberOfP2Outputs() {
		return numberOfP2Outputs;
	}

	public int getOriginalNumberOfWires() {
		return originalNumberOfWires;
	}

	public int getNumberOfWiresParsed() {
		return numberOfWiresParsed;
	}

	private String[] getHeaderArray(String line) {
		char[] lineArray = line.toCharArray();
		String tmp = "";
		String[] split = new String[line.length() + 1/2]; //Cannot be greater
		int i = 0;
		// We here build all the strings to go into our array, if we meet a
		// ' ' we stop and store our accumulated string in the array.
		for(char c: lineArray){
			if (c != ' '){
				tmp += c;
			}
			else {
				if(!tmp.equals("")){
					split[i++] = tmp;
					tmp = "";
				}
			}
		}
		// We check if there is something left in tmp
		if(!tmp.equals("")){
			split[i++] = tmp;
		}
		// Finally we fill the built strings into an array of exact size.
		String[] res = new String[i];
		for(int j = 0; j < i; j++){
			res[j] = split[j];
		}

		return res;
	}
}
