package converters;
import java.util.ArrayList;
import java.util.List;

import parsers.FairplayParser;

import common.CircuitConverter;
import common.CommonUtilities;
import common.Gate;
import common.InputGateType;


public class FairplayToAugMultipleConverter implements CircuitConverter<Gate> {

	private FairplayParser circuitParser;
	private List<Gate> multipleOutputCircuit;

	public FairplayToAugMultipleConverter(FairplayParser circuitParser) {
		this.circuitParser = circuitParser;
	}
	
	@Override
	public List<Gate> getGates() {
		List<Gate> parsedGates = circuitParser.getGates();

		int n1 = circuitParser.getNumberOfP1Inputs();
		int n2 = circuitParser.getNumberOfP2Inputs();
		int m1 = circuitParser.getNumberOfP1Outputs();
		int m2 = circuitParser.getNumberOfP2Outputs();
		
		int addedInput = 3 * m1;
		int gatesToBeAddedForM = (m1 * m1) + (m1 * m1);
		int gatesToBeAddedForE = m1;
		int totalGatesToBeAdded = gatesToBeAddedForM + gatesToBeAddedForE;
		
		int originalNumberOfWires = 
				circuitParser.getNumberOfWiresParsed();
		int newNumberOfWires = originalNumberOfWires + addedInput;

		int startOfAInput = n1;
		int startOfBInput = n1 + m1;
		int startOfCInput = n1 + 2*m1;

		int startOfF1 = originalNumberOfWires - n1 - n2;
		int startOfF2 = originalNumberOfWires - n2;

		int startOfEOutput = newNumberOfWires + totalGatesToBeAdded - m2 - 2*m1;
		int startOfMComputation = newNumberOfWires - m2;
		int startOfM = newNumberOfWires + totalGatesToBeAdded - m2 - m1;

		multipleOutputCircuit = 
				getPreparedCircuit(parsedGates, n1, addedInput, 
						startOfF2, m2, totalGatesToBeAdded);

		int newStartOfF1 = startOfF1 + addedInput;
		List<Gate> eGates = getEGates(newStartOfF1,
				startOfEOutput, startOfCInput, m1);
		
		List<Gate> mGates = getMGates(startOfEOutput, 
				startOfAInput, startOfBInput, m1, startOfMComputation, startOfM);

		multipleOutputCircuit.addAll(eGates);
		multipleOutputCircuit.addAll(mGates);
		
		return multipleOutputCircuit;
	}

	@Override
	public String[] getHeaders() {
		String[] res = new String[2];
		String[] inputOutputInfo = 
				circuitParser.getHeaders();

		int m1 = Integer.parseInt(inputOutputInfo[2]);
		res[0] = multipleOutputCircuit.size() + " " + CommonUtilities.getWireCount(multipleOutputCircuit);

		int newP1Input = Integer.parseInt(inputOutputInfo[0]) + 3 * m1;
		int newP2Input = Integer.parseInt(inputOutputInfo[1]);
		int newP1Output = Integer.parseInt(inputOutputInfo[2]) + m1;
		int newP2Output = Integer.parseInt(inputOutputInfo[3]);

		res[1] = newP1Input + " " + newP2Input + " " + newP1Output + " " +
				newP2Output;

		return res;
	}

	private List<Gate> getPreparedCircuit(List<Gate> gates, int n1, 
			int addedInputs, int startOfP2Output, int m2, int gatesToBeAdded) {
		for (Gate g: gates) {
			int leftIndex = g.getLeftWireIndex();
			int rightIndex = g.getRightWireIndex();
			int outputIndex = g.getOutputWireIndex();

			if (outputIndex >= startOfP2Output) {
				g.setOutputWireIndex(outputIndex + gatesToBeAdded);
			}

			outputIndex = g.getOutputWireIndex();

			if (leftIndex >= n1) {
				g.setLeftWireIndex(leftIndex + addedInputs);
			}
			if (rightIndex >= n1) {
				g.setRightWireIndex(rightIndex + addedInputs);
			}
			if (outputIndex >= n1) {
				g.setOutputWireIndex(outputIndex + addedInputs);
			}
		}
		return gates;
	}

	private List<Gate> getEGates(int startOfP1Outputs, int startOfE, int startOfC, int m1) {
		List<Gate> res = new ArrayList<Gate>();

		for (int i = 0; i < m1; i++) {
			int leftWire = startOfP1Outputs + i;
			int rightWire = startOfC + i;
			int outputWire = startOfE + i;
			Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
					" " + outputWire + " 0110", InputGateType.FAIRPLAY);
			res.add(g);
		}

		return res;
	}

	private List<Gate> getMGates(int startOfE, int startOfA, int startOfB, int m1,
			int startOfMComputation, int startOfM) {
		List<Gate> res = new ArrayList<Gate>();

		//Construct the AND and XOR gates
		int priorOutput = 0;
		int m1Squared = m1 * m1;
		int[] aConvolutedE = new int[m1];
		for (int j = 0; j < m1; j++) {
			int index = 0;
			for (int i = 0; i < m1; i++) {
				int leftWire = startOfA + i;
				int rightWire = startOfE + ((i + j) % m1);
				index = m1 * j + i;
				int outputWire = startOfMComputation + index;
				Gate andGate = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0001", InputGateType.FAIRPLAY);
				res.add(andGate);
				/*****************************************************/
				if (i == m1 - 1) {
					continue;
				}
				index = j * m1 + i;
				if (i == 0) {
					leftWire = startOfMComputation + index;
					rightWire = startOfMComputation + index + 1;
				} else {
					leftWire = startOfMComputation + index + 1;
					rightWire = priorOutput;
				}
				index = index - j;

				outputWire = priorOutput = 
						startOfMComputation + index + m1Squared;
				Gate orGate = new Gate("2 1 "+ leftWire + " " + rightWire +
						" " + outputWire + " 0110", InputGateType.FAIRPLAY);
				res.add(orGate);

			}
			aConvolutedE[j] = startOfMComputation + index + m1Squared - 1;
		}
		
		//Construct final XOR gates 
		for (int i = 0; i < m1; i++) {
			int leftWire = aConvolutedE[i];
			int rightWire = startOfB + i;
			int outputWire = startOfM + i;
			Gate g = new Gate("2 1 "+ leftWire + " " + rightWire +
					" " + outputWire + " 0110", InputGateType.FAIRPLAY);
			res.add(g);
		}
		return res;
	}
}
