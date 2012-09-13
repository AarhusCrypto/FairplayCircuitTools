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
		int actualNumberOfWires = 
				circuitParser.getWireCountFromSingleList(parsedGates);
		int newNumberOfInputs = n1 + 3*m1 + n2;
		int newNumberOfOutputs = 2*m1 + m2;

		List<Gate> parsedGatesAddedInputs = 
				getAdditionalInputWires(parsedGates, n1, 3*m1);
		
		int startOfCInput = n1 + 2*m1;
		int startOfP1Output = actualNumberOfWires - n1 - n2 + 3*m1; //to account for additional inputs
		int startOfP2Output = actualNumberOfWires - n2 + 3*m1; //to account for additional inputs

//		writeOutput(parsedGatesAddedInputs);
		
		List<Gate> nonOutputPositionedEGates = getEGates(startOfP1Output, startOfCInput, m1);
		
		String[] headers = circuitParser.getNewFairplayHeader(nonOutputPositionedEGates);
		CommonUtilities.outputFairplayCircuit(nonOutputPositionedEGates, 
				outputFile, headers);
	}

	private List<Gate> getAdditionalInputWires(List<Gate> gates, int n1, 
			int addedInputs) {
		for(Gate g: gates){
			int leftIndex = g.getLeftWireIndex();
			int rightIndex = g.getRightWireIndex();
			int outputIndex = g.getOutputWireIndex();
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

	private List<Gate> getEGates(int startOfP1Outputs, int startOfC, int m1) {
		List<Gate> res = new ArrayList<Gate>();
		
		for(int i = 0; i < m1; i++){
			int leftWire = startOfP1Outputs + i;
			int rightWire = startOfC + i;
			int tmpOutputWire = i;
			Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
					" " + tmpOutputWire + " 0110");
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
