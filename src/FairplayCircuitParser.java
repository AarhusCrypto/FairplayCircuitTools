
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
	private int numberOfAliceInputs;
	private int numberOfBobInputs;
	private int numberOfNonXORGates;
	private int totalNumberOfInputs;
	
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
		ArrayList<Gate> res = new ArrayList<Gate>();
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
					counter = true;
					continue;
				}

				/*
				 * Parse number of input bits
				 */
				if (counter == true){
					secondHeader = line;
					String[] split = line.split(" ");
					numberOfAliceInputs = Integer.parseInt(split[0]);
					numberOfBobInputs = Integer.parseInt(split[1]);
					totalNumberOfInputs = numberOfAliceInputs +
							numberOfBobInputs;
					counter = false;
					continue;
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new Gate(line);
				
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
		int greatestGateNumber = 0;
		for(Gate g: res){
			greatestGateNumber = Math.max(greatestGateNumber, g.getLeftWireIndex());
			greatestGateNumber = Math.max(greatestGateNumber, g.getRightWireIndex());
			greatestGateNumber = Math.max(greatestGateNumber, g.getOutputWireIndex());
		}
		
		blankWires = new boolean[greatestGateNumber + 1];
		for(Gate g: res){
			blankWires[g.getLeftWireIndex()] = true;
			blankWires[g.getRightWireIndex()] = true;
			blankWires[g.getOutputWireIndex()] = true;			
		}


		MultiValueMap leftMap = new MultiValueMap();
		MultiValueMap rightMap = new MultiValueMap();
		HashMap<Integer, Gate> outputMap = new HashMap<Integer, Gate>();
		
		for(Gate g: res){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
			outputMap.put(g.getOutputWireIndex(), g);
		}
		
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
	
	public String getCUDAHeader(List<List<Gate>> layersOfGates){
		int totalNumberOfOutputs = totalNumberOfInputs/2;
		int numberOfWires = getActualWireCount(layersOfGates);
		int numberOfLayers = layersOfGates.size();

		int maxLayerWidth = 0;

		/*
		 * We have to figure out the max layer size before writing to the file.
		 */
		for(List<Gate> l: layersOfGates){
			maxLayerWidth = Math.max(maxLayerWidth, l.size());
		}
		
		return totalNumberOfInputs + " " + totalNumberOfOutputs + " " +
		numberOfWires + " " + numberOfLayers + " " + maxLayerWidth + " " +
		numberOfNonXORGates;
		
		
	}
	
	public String[] getNewFairplayHeader(List<Gate> augCircuit){
		String[] res = new String[2];
		
		List<List<Gate>> wrapList = new ArrayList<List<Gate>>();
		wrapList.add(augCircuit);
		int totalNumberOfWires = 
				getActualWireCount(wrapList);
		
		res[0] = augCircuit.size() + " " +  totalNumberOfWires;
		
		String[] inputOutputInfo = secondHeader.split(" ");
		int newAliceInput = Integer.parseInt(inputOutputInfo[0]) *2;
		int newBobInput = Integer.parseInt(inputOutputInfo[1]) *2;
		int newOutput = Integer.parseInt(inputOutputInfo[1]) *2;
		
		res[1] = newAliceInput + " " + newBobInput + " " + inputOutputInfo[4] + " " +
		newOutput;
		
		return res;
	}

	public int getTotalNumberOfInputs(){
		return totalNumberOfInputs;
	}
	
	public int getNumberOfAliceInputs(){
		return numberOfAliceInputs;
	}

	/**
	 * @param multiTimedGates
	 * @return
	 */
	private int getActualWireCount(List<List<Gate>> multiTimedGates) {
		HashSet<Integer> hs = new HashSet<Integer>();
		for(List<Gate> l: multiTimedGates){
			for(Gate g: l){
				hs.add(g.getLeftWireIndex());
				hs.add(g.getRightWireIndex());
				hs.add(g.getOutputWireIndex());
			}
		}
		return hs.size();
	}
}
