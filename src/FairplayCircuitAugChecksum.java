
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class FairplayCircuitAugChecksum implements Runnable {

	private File outputFile;
	private FairplayCircuitParser circuitParser;
	private int numberOfNonXORGatesAdded;
	private int largestOutputGate;
	private List<Gate> outputGates;

	public FairplayCircuitAugChecksum(FairplayCircuitParser circuitParser, File outputFile){
		this.outputFile = outputFile;
		this.circuitParser = circuitParser;
		outputGates = new ArrayList<Gate>();
		numberOfNonXORGatesAdded = 0;
		largestOutputGate = 0;
	}

	@Override
	public void run() {
		List<Gate> parsedGates = circuitParser.getGates();
		int numberOfInputs = circuitParser.getTotalNumberOfInputs();
		List<Gate> augGates = 
				getAugGates(numberOfInputs);
		List<Gate> augCircuit = new ArrayList<Gate>();

		List<Gate> incrementedGates = 
				getIncrementedGates(parsedGates, augGates.size(), numberOfInputs);
		List<Gate> augOutputGates = getOutputGates();//Must be called after getIncrementedGates()
		augCircuit.addAll(augGates);
		augCircuit.addAll(incrementedGates);
		augCircuit.addAll(augOutputGates);

		writeOutput(augCircuit);
	}

	private List<Gate> getAugGates(int numberOfStandardInputs) {

		List<Gate> res = new ArrayList<Gate>();
		int t_a = circuitParser.getNumberOfAliceInputs();
		int totalInputSize = numberOfStandardInputs * 2;
		int gateNumber = 0;
		int r = totalInputSize - t_a;
		List<Gate> andGates = new ArrayList<Gate>();

		/**
		 * Add all the AND gates. We assume r is the 3rd input
		 */
		for(int s = t_a; s < numberOfStandardInputs; s++){
			for(int x = 0; x < t_a; x++){
				int shift = s - t_a;
				int leftWire = x;
				int rightWire = (shift + x + r) % 
						totalInputSize;
				if (rightWire < numberOfStandardInputs){
					rightWire += r;
				}
				int outputWire = shift * t_a + (x + totalInputSize);

				Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0001");
				g.setGateNumber(gateNumber);
				gateNumber++;
				andGates.add(g);
			}
			res.addAll(andGates);
			numberOfNonXORGatesAdded += andGates.size();
			andGates.clear();
		}

		/**
		 * Add all the XOR gates.
		 */
		List<Gate> xorGates = new ArrayList<Gate>();
		//How far we filled up the res array with and gates
		int xorGateStart = res.size() + totalInputSize;

		for (int s = t_a; s < numberOfStandardInputs; s++){
			int multCounter = s - t_a;
			int priorOutputWire = 0;
			int numberOfXORs = t_a - 1;
			for (int i = 0; i < numberOfXORs; i++){
				int leftWire;
				int rightWire;
				if (i == 0){
					leftWire = totalInputSize + t_a * multCounter;
					rightWire = totalInputSize + t_a * multCounter + 1;
				}
				else{
					leftWire = priorOutputWire;
					rightWire = totalInputSize + t_a * multCounter + 1 + i;
				}
				int outputWire = xorGateStart + i + 
						(s - t_a) * numberOfXORs;
				priorOutputWire = outputWire;

				// We make each xor dependant on the following xor, thus
				// creating a chain structure. The last gate in this list is the
				// output gate.
				Gate g = new Gate("2 1 " + leftWire + " " +
						rightWire + " " + outputWire + " 0110");
				xorGates.add(g);
			}

			//We identify the last xor-gate of the chain and xor this
			//with the current s bit. The output of this xor is the s'th
			//output bit of the augmentation output.
			Gate xorOutputGate = xorGates.get(xorGates.size() - 1);
			int xorOutputWireIndex = xorOutputGate.getOutputWireIndex();

			Gate outputGate = new Gate("2 1 " + xorOutputWireIndex +" " +
					s + " " + multCounter + " 0110");

			outputGates.add(outputGate);
			res.addAll(xorGates);
			xorGates.clear();
		}

		return res;

	}

	private List<Gate> getIncrementedGates(List<Gate> parsedGates, 
			int numberOfAddedGates, int numberOfInputs){
		List<Gate> res = new ArrayList<Gate>();
		int incNumber = numberOfAddedGates + numberOfInputs;
		int numberOfAliceInputs = numberOfInputs/2;
		
		for(Gate g: parsedGates){
			int leftWireIndex = g.getLeftWireIndex();
			int rightWireIndex = g.getRightWireIndex();
			if(!g.isXOR()){
				g.setGateNumber(g.getGateNumber() + numberOfNonXORGatesAdded);
			}
			
			//If input is t_b, increment it
			if(leftWireIndex < numberOfInputs &&
					leftWireIndex > numberOfAliceInputs){
				g.setLeftWireIndex(leftWireIndex + numberOfAliceInputs);
			}
			if(rightWireIndex < numberOfInputs &&
					rightWireIndex > numberOfAliceInputs){
				g.setRightWireIndex(rightWireIndex + numberOfAliceInputs);
			}
					
			// Increment wires with new gate size
			if(leftWireIndex > numberOfInputs){
				int newIndex = leftWireIndex + incNumber;
				g.setLeftWireIndex(newIndex);
			}
			if(rightWireIndex > numberOfInputs){
				int newIndex = rightWireIndex + incNumber;
				g.setRightWireIndex(newIndex);
			}
			
			
			//Outputwires should always be incremented
			int newIndex = g.getOutputWireIndex() + incNumber;
			g.setOutputWireIndex(newIndex);
			largestOutputGate = Math.max(largestOutputGate, g.getOutputWireIndex());
			res.add(g);
		}
		return res;
	}

	/**
	 * @param layersOfGates
	 * Writes the given lists of lists to a file
	 */
	private void writeOutput(List<Gate> augCircuit) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));
			String[] headers = circuitParser.getNewFairplayHeader(augCircuit);
			fbw.write(headers[0]);
			fbw.newLine();
			fbw.write(headers[1]);
			fbw.newLine();
			fbw.newLine();

			for(Gate g: augCircuit){
				fbw.write(g.toFairPlayString());
				fbw.newLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			try {
				fbw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<Gate> getOutputGates(){
		int startIndex = largestOutputGate + 1;
		for(Gate g: outputGates){
			g.setOutputWireIndex(startIndex);
			startIndex++;
		}
		return outputGates;
	}
}

