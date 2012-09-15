import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FairplayCircuitAugMultipleOutputs implements Runnable {

	private FairplayCircuitParser circuitParser;
	private File outputFile;
	private List<Gate> outputGates;
	private int numberOfNonXORGatesAdded;
	private int largestOutputWire = 0;

	public FairplayCircuitAugMultipleOutputs(FairplayCircuitParser circuitParser,
			File outputFile){
		this.circuitParser = circuitParser;
		this.outputFile = outputFile; 
		outputGates = new ArrayList<Gate>();
		numberOfNonXORGatesAdded = 0;
		largestOutputWire = 0;
	}

	@Override
	public void run() {
		List<Gate> parsedGates = circuitParser.getGates();

		int n1 = circuitParser.getNumberOfP1Inputs();
		int n2 = circuitParser.getNumberOfP2Inputs();
		int m1 = circuitParser.getNumberOfP1Outputs();
		int m2 = circuitParser.getNumberOfP2Outputs();
		int addedInput = 3 *m1;
		int gatesToBeAddedForM = (m1 * m1) + (m1 * m1);
		int gatesToBeAddedForE = m1;
		int totalGatesToBeAdded = gatesToBeAddedForM + gatesToBeAddedForE;
		int originalNumberOfWires = 
				circuitParser.getWireCountFromSingleList(parsedGates);
		int newNumberOfWires = originalNumberOfWires + addedInput;
		
		int newNumberOfInputs = n1 + addedInput + n2;
		int newNumberOfOutputs = 2*m1 + m2;

		int startOfAInput = n1;
		int startOfBInput = n1 + m1;
		int startOfCInput = n1 + 2*m1;
		
		int f1 = originalNumberOfWires - n1 - n2;
		int f2 = originalNumberOfWires - n2;
		
		int startOfEOutput = newNumberOfWires + totalGatesToBeAdded - m2 - 2*m1;
		int startOfMComputation = newNumberOfWires - m2;
		int startOfM = newNumberOfWires + totalGatesToBeAdded - m2 - m1;
		
		List<Gate> preparedCircuit = 
				getPreparedCircuit(parsedGates, n1, addedInput, 
						f2, m2, totalGatesToBeAdded);

		List<Gate> eGates = getEGates(f1,
				startOfEOutput, startOfCInput, m1);
		List<Gate> mGates = getMGates(startOfEOutput, 
				startOfAInput, startOfBInput, m1, startOfMComputation, startOfM);
		
		preparedCircuit.addAll(eGates);
		preparedCircuit.addAll(mGates);
		
		String[] headers = circuitParser.getNewFairplayHeader(preparedCircuit);
		CommonUtilities.outputFairplayCircuit(preparedCircuit, 
				outputFile, headers);
	}

	private List<Gate> getPreparedCircuit(List<Gate> gates, int n1, 
			int addedInputs, int startOfP2Output, int m2, int gatesToBeAdded) {
		for(Gate g: gates){
			int leftIndex = g.getLeftWireIndex();
			int rightIndex = g.getRightWireIndex();
			int outputIndex = g.getOutputWireIndex();
			
			if(outputIndex >= startOfP2Output){
				g.setOutputWireIndex(outputIndex + gatesToBeAdded);
			}
			
			outputIndex = g.getOutputWireIndex();
			
			if(leftIndex >= n1){
				g.setLeftWireIndex(leftIndex + addedInputs);
			}
			if(rightIndex >= n1){
				g.setRightWireIndex(rightIndex + addedInputs);
			}
			if(outputIndex >= n1){
				g.setOutputWireIndex(outputIndex + addedInputs);
			}
		}
		return gates;
	}

	private List<Gate> getEGates(int startOfP1Outputs, int startOfE, int startOfC, int m1) {
		List<Gate> res = new ArrayList<Gate>();

		for(int i = 0; i < m1; i++){
			int leftWire = startOfP1Outputs + i;
			int rightWire = startOfC + i;
			int outputWire = startOfE + i;
			Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
					" " + outputWire + " 0110");
			res.add(g);
		}

		return res;
	}
	
	private List<Gate> getMGates(int startOfE, int startOfA, int startOfB, int m1,
			int startOfMComputation, int startOfM){
		List<Gate> res = new ArrayList<Gate>();
		System.out.println("StartOfA: " + startOfA);
		System.out.println("StartOfB: " + startOfB);
		System.out.println("StartOfE: " + startOfE);
		int currentOutputIndex = startOfMComputation;
		//Construct all the AND gates
		for(int j = 0; j < m1; j++){
			for(int i = 0; i < m1; i++){
				int leftWire = startOfA + i;
				int rightWire = startOfE + (i + j % m1);
				int outputWire = currentOutputIndex++;
				Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0001");
				res.add(g);
			}
		}
		
		//Construct all XOR Gates
		int priorOutput = 0;
		int[] aConvolutedE = new int[m1];
		for(int j = 0; j < m1; j++){
			for(int i = 0; i < m1 - 1; i++){
				int leftWire = 0;
				int rightWire = 0;
				if(i == 0){
					leftWire = startOfMComputation + j * m1 + i;
					rightWire = startOfMComputation + j * m1 + i + 1;
				}
				else{
					leftWire = startOfMComputation + j * m1 + i + 1;
					rightWire = priorOutput;
				}

				int outputWire = priorOutput = currentOutputIndex++;
				Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0110");
				res.add(g);
			}
			aConvolutedE[j] = currentOutputIndex - 1;
		}
		
		for(int i = 0; i < m1; i++){
			int leftWire = aConvolutedE[i];
			int rightWire = startOfB + i;
			int outputWire = startOfM + i;
			Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
					" " + outputWire + " 0110");
			res.add(g);
		}
		return res;
	}

	//	private List<List<Gate>> getOriginalOutputGates(List<Gate> parsedGates, int p1Outputs, 
	//			int p2Outputs) {
	//		List<List<Gate>> res = new ArrayList<List<Gate>>();
	//		List<Gate> p1OutputGates = new ArrayList<Gate>();
	//		List<Gate> p2OutputGates = new ArrayList<Gate>();
	//		int actualNumberOfWires = circuitParser.getParsedWireCount();
	//		int totalOutputs = p1Outputs + p2Outputs;
	//
	//		for(Gate g: parsedGates){	
	//			if (g.getOutputWireIndex() >= actualNumberOfWires - totalOutputs &&
	//					g.getOutputWireIndex() < actualNumberOfWires - p2Outputs){
	//				p1OutputGates.add(g);
	//			}
	//			else if(g.getOutputWireIndex() >= actualNumberOfWires - totalOutputs
	//					&& g.getOutputWireIndex() >= actualNumberOfWires - p2Outputs){
	//				p2OutputGates.add(g);
	//			}
	//		}
	//		res.add(p1OutputGates);
	//		res.add(p2OutputGates);
	//		return res;
	//	}
}
