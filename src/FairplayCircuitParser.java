import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class FairplayCircuitParser {

	private File circuitFile;
	private int originalNumberOfWires;
	private boolean[] blankWires;
	private int numberOfP1Inputs;
	private int numberOfBobInputs;
	private int numberOfNonXORGates;
	private int totalNumberOfInputs;
	
	private String firstHeader;
	private String secondHeader;

	public FairplayCircuitParser(File circuitFile){
		this.circuitFile = circuitFile;
	}

	/**
	 * @return A list of gates in the given circuitFile
	 */
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
					firstHeader = line;
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
					String[] split = line.split(" ");
					numberOfP1Inputs = Integer.parseInt(split[0]);
					numberOfBobInputs = Integer.parseInt(split[1]);
					totalNumberOfInputs = numberOfP1Inputs +
							numberOfBobInputs;
					counter = false;
					continue;
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new Gate(line);
				blankWires[g.getLeftWireIndex()] = true;
				blankWires[g.getRightWireIndex()] = true;
				blankWires[g.getOutputWireIndex()] = true;
				
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
	
	public String[] getNewFairplayHeader(int sizeOfCircuit, int nonXORGatesAdded){
		String[] res = new String[2];
		
		
		String[] split = firstHeader.split(" ");
		int numberOfNonXORGates = Integer.parseInt(split[1]);
		int totalNumberOfNonXORGates = numberOfNonXORGates+ nonXORGatesAdded;
		
		res[0] = sizeOfCircuit + " " +  totalNumberOfNonXORGates;
		
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
	
	public boolean[] getBlankWires(){
		return blankWires;
	}
	
	public int getNumberOfP1Inputs(){
		return numberOfP1Inputs;
	}
	
	public int getOriginalNumberOfWires(){
		return originalNumberOfWires;
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
