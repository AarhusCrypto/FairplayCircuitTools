
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


public class FairplayCircuitParser {

	private File circuitFile;
	private int originalNumberOfWires;
	private boolean[] blankWires;
	private int numberOfP1Inputs;
	private int numberOfP2Inputs;
	private int numberOfP1Outputs;
	private int numberOfP2Outputs;
	private int numberOfNonXORGates;
	private int numberOfWiresParsed;
	private int totalNumberOfInputs;
	private int totalNumberOfOutputs;

	private String secondHeader;

	public FairplayCircuitParser(File circuitFile){
		this.circuitFile = circuitFile;
	}

	/**
	 * @return A list of gates in the given circuitFile
	 */
	@SuppressWarnings("unchecked")
	public List<Gate> getGates() {
		boolean counter = false;
		List<Gate> res = new ArrayList<Gate>();
		MultiValueMap leftMap = new MultiValueMap();
		MultiValueMap rightMap = new MultiValueMap();
		HashMap<Integer, Gate> outputMap = new HashMap<Integer, Gate>();
		HashSet<Integer> hs = new HashSet<Integer>();
		
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
					counter = true;
					continue;
				}
				

				/*
				 * Parse number of input bits
				 */
				if (counter == true){
					secondHeader = line;

					String[] split = getHeaderArray(line);

					numberOfP1Inputs = Integer.parseInt(split[0]);
					numberOfP2Inputs = Integer.parseInt(split[1]);
					numberOfP1Outputs = Integer.parseInt(split[2]);
					numberOfP2Outputs = Integer.parseInt(split[3]);
					totalNumberOfInputs = numberOfP1Inputs +
							numberOfP2Inputs;
					totalNumberOfOutputs = numberOfP1Outputs + numberOfP2Outputs;
					counter = false;
					continue;
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new Gate(line);
				int leftIndex = g.getLeftWireIndex();
				int rightIndex = g.getRightWireIndex();
				int outputIndex = g.getOutputWireIndex();
				
				// Accumulate information for later usage
				hs.add(leftIndex);
				hs.add(rightIndex);
				hs.add(outputIndex);
				leftMap.put(leftIndex, g);
				rightMap.put(rightIndex, g);
				outputMap.put(g.getOutputWireIndex(), g);
				blankWires[leftIndex] = true;
				blankWires[rightIndex] = true;
				blankWires[outputIndex] = true;	

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
		numberOfWiresParsed = hs.size();
		// We now strip the blank wires
		// false means blank
		// Runs from top to bottom, decrementing the appropriate wires
		// Is a bit funky since we cannot guarantee the input circuit
		// is sorted by output wires
		for(int i =  originalNumberOfWires - 1; i >= 0; i--){
			boolean b = blankWires[i];
			if(!b){
				for(int j = i; j <= originalNumberOfWires; j++){
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

		return res;
	}

	public int[] getCUDAHeaderInfo(){
		return new int[]{totalNumberOfInputs, totalNumberOfOutputs, numberOfNonXORGates};

	}

	public String[] getFairplayInputOutputHeader(){
		return getHeaderArray(secondHeader);
	}

	public int getNumberOfInputs(){
		return totalNumberOfInputs;
	}

	public int getNumberOfP1Inputs(){
		return numberOfP1Inputs;
	}

	public int getNumberOfP2Inputs(){
		return numberOfP2Inputs;
	}

	public int getNumberOfP1Outputs(){
		return numberOfP1Outputs;
	}

	public int getNumberOfP2Outputs(){
		return numberOfP2Outputs;
	}

	public int getOriginalNumberOfWires(){
		return originalNumberOfWires;
	}
	
	public int getParsedWireCount(){
		return numberOfWiresParsed;
	}
	
	private String[] getHeaderArray(String line){
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
