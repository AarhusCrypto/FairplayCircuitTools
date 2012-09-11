import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
		
		int newNumberOfInputs = n1 + 3*m1 + n2;
		int startOfC = n1+ 2*m1;
		incrementOriginalGates(parsedGates, 3*m1);
		int newNumberOfOutputs = 2*m1 + m2;
		
		List<List<Gate>> outputGates = getOutputGates(parsedGates, m1, m2);
		for(Gate g: outputGates.get(0)){
			System.out.println(g.toFairPlayString());
		}
		for(Gate g: outputGates.get(1)){
			System.out.println(g.toFairPlayString());
		}
		
//		List<Gate> e = getEncryptedP1Output(outputGates.get(0), startOfC);
		
	}

	private void incrementOriginalGates(List<Gate> gates, int i) {
		System.out.println(i);
		for(Gate g: gates){
			g.setLeftWireIndex(g.getLeftWireIndex() + i);
			g.setRightWireIndex(g.getRightWireIndex() + i);
			g.setOutputWireIndex(g.getOutputWireIndex() + i);
		}
		
	}

//	private List<Gate> getEncryptedP1Output(List<Gate> p1Output, int startOfC,
//			int ) {
//		List<Gate> res = new ArrayList<Gate>();
//		
//		
//		
//		for(Gate g: p1Output){
//			Gate xor = new Gate("2 1 "+ startOfC++ + " " + g.getOutputWireIndex() +
//						" " +  + " 0001"))
//		}
//		
//		
//		return res;
//	}

	private List<List<Gate>> getOutputGates(List<Gate> parsedGates, int p1Outputs, 
			int p2Outputs) {
		List<List<Gate>> res = new ArrayList<List<Gate>>();
		List<Gate> p1OutputGates = new ArrayList<Gate>();
		List<Gate> p2OutputGates = new ArrayList<Gate>();
		int actualNumberOfWires = getNewWireCount(parsedGates);
		int totalOutputs = p1Outputs + p2Outputs;
		
		for(Gate g: parsedGates){
			
			if (g.getOutputWireIndex() >= actualNumberOfWires - totalOutputs &&
					g.getOutputWireIndex() < actualNumberOfWires - p2Outputs){
				p1OutputGates.add(g);
			}
			else if(g.getOutputWireIndex() >= actualNumberOfWires - totalOutputs
					&& g.getOutputWireIndex() >= actualNumberOfWires - p2Outputs){
				p2OutputGates.add(g);
			}
		}
		res.add(p1OutputGates);
		res.add(p2OutputGates);
		return res;
	}
	
	
	private int getNewWireCount(List<Gate> gates){
		HashSet<Integer> hs = new HashSet<Integer>();
		for(Gate g: gates){
			hs.add(g.getLeftWireIndex());
			hs.add(g.getRightWireIndex());
			hs.add(g.getOutputWireIndex());
		}
		return hs.size();
	}
}
