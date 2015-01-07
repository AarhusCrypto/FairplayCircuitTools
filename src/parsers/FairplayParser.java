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
import common.InputGateType;

import org.apache.commons.collections.map.MultiValueMap;


public class FairplayParser implements CircuitParser<Gate> {

	private File circuitFile;
	
	private int numberOfInputs;
	private int numberOfOutputs;
	
	private int numberOfANDGates;
	private int numberOfP1Inputs;
	private int numberOfP2Inputs;
	
	private int numberOfWires;
	private int numberOfGates;
	
	private int originalNumberOfWires;
	private int numberOfP1Outputs;
	private int numberOfP2Outputs;

	private boolean[] blankWires;
	MultiValueMap leftMap;
	MultiValueMap rightMap;
	HashMap<Integer, Gate> outputMap;

	private String secondHeader;
	private boolean stripWires;

	public FairplayParser(File circuitFile, boolean stripWires) {
		this.circuitFile = circuitFile;
		this.stripWires = stripWires;
		this.leftMap = new MultiValueMap();
		this.rightMap = new MultiValueMap();
		this.outputMap = new HashMap<Integer, Gate>();
	}

	/**
	 * @return A list of gates in the given circuitFile
	 * @Override
	 */
	public List<Gate> getGates() {
		boolean secondLine = false;
		List<Gate> res = new ArrayList<Gate>();

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = "";
			while ((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}

				/*
				 * Parse meta-data info
				 */
				if (line.matches("[0-9]* [0-9]*")) {
					String[] sizeInfo = line.split(" ");
					numberOfGates = Integer.parseInt(sizeInfo[0]);
					originalNumberOfWires = Integer.parseInt(sizeInfo[1]);
					blankWires = new boolean[originalNumberOfWires];
					secondLine = true;

					continue;
				}

				/*
				 * Parse number of input bits
				 */
				if (secondLine) {
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

					numberOfInputs = numberOfP1Inputs + numberOfP2Inputs;
					numberOfOutputs = numberOfP1Outputs + numberOfP2Outputs;
					secondLine = false;
					continue;
				}

				// Transforms format from http://www.cs.bris.ac.uk/Research/CryptographySecurity/MPC/
				// to standard Fairplay.
				String[] split = line.split(" ");
				if (line.endsWith("INV")) {
					line = "1 " + split[1] + " " +  split[2] + " " + 
							split[3] + " -1";
				} else if (line.endsWith("XOR")) {
					line = split[0] + " " + split[1] + " " + split[2] + " " + 
							split[3] + " " + split[4] + " " + "0110";
				} else if (line.endsWith("AND")) {
					line = split[0] + " " + split[1] + " " + split[2] + " " + 
							split[3] + " " + split[4] + " " + "0001";
				}

				/*
				 * Construct each gate and count number of AND gates
				 */
				Gate g = new Gate(line, InputGateType.FAIRPLAY);
				addToWireInfo(g);

				if (!(g.isXOR() || g.isINV())){
					g.setGateNumber(numberOfANDGates++);
				}
				res.add(g);
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//This is the number of unique wires parsed
		numberOfWires = CommonUtilities.getWireCount(res);
		if (stripWires) {
			stripBlankWires(res);
		} else {
			int nonStrippedWires = 0;
			for (boolean b: blankWires) {
				if (!b) {
					nonStrippedWires++;
				}
			}
			numberOfWires += nonStrippedWires;
		}	

		return res;
	}

	@Override
	public String[] getHeaders() {
		return getHeaderArray(secondHeader);
	}
	
	@Override
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
	public int getNumberOfANDGates() {
		return numberOfANDGates;
	}

	@Override
	public int getNumberOfP1Inputs() {
		return numberOfP1Inputs;
	}

	@Override
	public int getNumberOfP2Inputs() {
		return numberOfP2Inputs;
	}

	@Override
	public int getNumberOfWires() {
		return numberOfWires;
	}
	
	public int getNumberOfP1Outputs() {
		return numberOfP1Outputs;
	}

	public int getNumberOfP2Outputs() {
		return numberOfP2Outputs;
	}
	
	private void addToWireInfo(Gate g) {
		int leftIndex = g.getLeftWireIndex();
		int rightIndex = g.getRightWireIndex();
		int outputIndex = g.getOutputWireIndex();

		// Accumulate information for later usage
		if (!g.isINV()) {
		leftMap.put(leftIndex, g);
		rightMap.put(rightIndex, g);
		outputMap.put(outputIndex, g);
		blankWires[leftIndex] = true;
		blankWires[rightIndex] = true;
		blankWires[outputIndex] = true;	
		} else {
			leftMap.put(leftIndex, g);
			outputMap.put(outputIndex, g);
			blankWires[leftIndex] = true;
			blankWires[outputIndex] = true;	
		}

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
