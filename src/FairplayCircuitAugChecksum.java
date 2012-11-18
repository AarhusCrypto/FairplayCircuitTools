
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FairplayCircuitAugChecksum implements Runnable {

	private File outputFile;
	private FairplayCircuitParser circuitParser;
	private int numberOfNonXORGatesAdded;
	private int largestOutputWire;
	private int l;
	private List<Gate> outputGates;

	public FairplayCircuitAugChecksum(FairplayCircuitParser circuitParser,
			File outputFile, int l){
		this.outputFile = outputFile;
		this.circuitParser = circuitParser;
		this.l = l;
		outputGates = new ArrayList<Gate>();
		numberOfNonXORGatesAdded = 0;
		largestOutputWire = 0;
	}

	@Override
	public void run() {
		List<Gate> parsedGates = circuitParser.getGates();
		int numberOfOriginalInputs = circuitParser.getNumberOfInputs();
		int newNumberOfInputs = numberOfOriginalInputs + 
				circuitParser.getNumberOfP1Inputs() + 2 * l;

		List<Gate> augGates = 
				getAugGates(newNumberOfInputs);
		List<Gate> augCircuit = new ArrayList<Gate>();

		List<Gate> incrementedGates = 
				getIncrementedGates(parsedGates, augGates.size(), newNumberOfInputs);
		List<Gate> augOutputGates = getOutputGates();//Must be called after getIncrementedGates()
		augCircuit.addAll(augGates);
		augCircuit.addAll(incrementedGates);
		augCircuit.addAll(augOutputGates);

		String[] headers = getHeaders(augCircuit);

		CommonUtilities.outputFairplayCircuit(augCircuit, outputFile, headers);
	}

	private String[] getHeaders(List<Gate> augCircuit) {
		String[] res = new String[2];
		String[] inputOutputInfo = 
				circuitParser.getFairplayInputOutputHeader();

		int t_a = Integer.parseInt(inputOutputInfo[0]);

		res[0] = augCircuit.size() + " " + CommonUtilities.getWireCount(augCircuit);

		int newP1Input = Integer.parseInt(inputOutputInfo[0]) + l;
		int newP2Input = Integer.parseInt(inputOutputInfo[1]) + t_a + l;
		int newP1Output = Integer.parseInt(inputOutputInfo[2]);
		int newP2Output = Integer.parseInt(inputOutputInfo[3]) + l;

		res[1] = newP1Input + " " + newP2Input + " " + newP1Output + " " +
				newP2Output;

		return res;
	}

	private List<Gate> getAugGates(int newNumberOfInputs) {

		List<Gate> res = new ArrayList<Gate>();
		int t_a = circuitParser.getNumberOfP1Inputs();
		int gateNumber = 0;
		int r = newNumberOfInputs - t_a - l;
		List<Gate> andGates = new ArrayList<Gate>();

		/**
		 * Add all the AND gates. We assume r is the 3rd input
		 */
		for(int i = 0; i < l; i++){
			for(int j = 0; j < t_a; j++){
				int leftWire = r + i + j;
				int rightWire = j;
				int outputWire = newNumberOfInputs + i * t_a + j;

				Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0001");
				g.setGateNumber(gateNumber++);
				andGates.add(g);
			}
			res.addAll(andGates);

			numberOfNonXORGatesAdded += andGates.size();
			andGates.clear();
		}

		/**
		 * Add all the XOR gates (both for inner product and final xor).
		 */
		List<Gate> xorGates = new ArrayList<Gate>();
		//How far we filled up the res array with andGates
		int xorGateStart = res.size() + newNumberOfInputs;

		for (int i = 0; i < l; i++){
			//int multCounter = s - t_a;
			int priorOutputWire = 0;
			for (int x = 1; x < t_a; x++){
				int leftWire;
				int rightWire;
				if (x == 1){
					leftWire = newNumberOfInputs + t_a * i + x - 1;
					rightWire = newNumberOfInputs + t_a * i + x;
				}
				else{
					leftWire = priorOutputWire;
					rightWire = newNumberOfInputs + t_a * i + x;
				}
				int outputWire = xorGateStart + (x - 1) + i * (t_a - 1);
				priorOutputWire = outputWire;

				// We make each xor dependant on the following xor, thus
				// creating a chain structure. The last gate in this list is the
				// output gate.
				Gate g = new Gate("2 1 " + leftWire + " " +
						rightWire + " " + outputWire + " 0110");
				xorGates.add(g);
			}

			//We identify the last xor-gate of the chain and xor this
			//with the current bit of s. The output of this xor is the s'th
			//output bit of the augmentation output.
			Gate xorOutputGate = xorGates.get(xorGates.size() - 1);
			int xorOutputWireIndex = xorOutputGate.getOutputWireIndex();

			int s = t_a + i;
			Gate outputGate = new Gate("2 1 " + xorOutputWireIndex +" " +
					s + " " + i + " 0110");

			outputGates.add(outputGate);
			res.addAll(xorGates);
			xorGates.clear();
		}

		return res;

	}

	private List<Gate> getIncrementedGates(List<Gate> parsedGates, 
			int numberOfAddedGates, int newNumberOfInputs){
		List<Gate> res = new ArrayList<Gate>();
		int numberOfP1Inputs = circuitParser.getNumberOfP1Inputs();
		int originalNumberOfInputs = circuitParser.getNumberOfInputs();
		int incNumber = numberOfAddedGates + 2 *l + numberOfP1Inputs;


		for(Gate g: parsedGates){
			int leftWireIndex = g.getLeftWireIndex();
			int rightWireIndex = g.getRightWireIndex();
			if(!g.isXOR()){
				g.setGateNumber(g.getGateNumber() + numberOfNonXORGatesAdded);
			}

			//If input is t_b, increment it
			if(leftWireIndex < originalNumberOfInputs &&
					leftWireIndex > numberOfP1Inputs - 1){
				g.setLeftWireIndex(leftWireIndex + l);
			}
			if(rightWireIndex < originalNumberOfInputs &&
					rightWireIndex > numberOfP1Inputs - 1){
				g.setRightWireIndex(rightWireIndex + l);
			}

			// Increment wires with new gate size
			if(leftWireIndex > originalNumberOfInputs){
				int newIndex = leftWireIndex + incNumber;
				g.setLeftWireIndex(newIndex);
			}
			if(rightWireIndex > originalNumberOfInputs){
				int newIndex = rightWireIndex + incNumber;
				g.setRightWireIndex(newIndex);
			}


			//Outputwires should always be incremented
			int newIndex = g.getOutputWireIndex() + incNumber;
			//System.out.println(g.getOutputWireIndex());
			g.setOutputWireIndex(newIndex);
			largestOutputWire = Math.max(largestOutputWire, g.getOutputWireIndex());
			res.add(g);
		}
		return res;
	}

	private List<Gate> getOutputGates(){
		int startIndex = largestOutputWire + 1;
		for(Gate g: outputGates){
			g.setOutputWireIndex(startIndex);
			startIndex++;
		}
		return outputGates;
	}
}

