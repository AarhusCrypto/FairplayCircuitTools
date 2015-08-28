package converters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import common.CircuitConverter;
import common.CircuitParser;
import common.Gate;
import common.InputGateType;

public class FairplayToTinyLegoConverter implements CircuitConverter<List<Gate>, Gate> {

	private CircuitParser<Gate> circuitParser;
	private CircuitParser<Gate> newCircuitParser;
	private ListToLayersConverter listConverter;
	private List<Gate> gates;

	public FairplayToTinyLegoConverter(CircuitParser<Gate> circuitParser) {
		this.circuitParser = circuitParser;
		gates = circuitParser.getGates();
		int numOfWires = circuitParser.getNumberOfWires();
		int numOfOutputs = circuitParser.getNumberOfOutputs();
		int numOfNonOutputs = numOfWires - numOfOutputs;
		int numberOfAndGates = circuitParser.getNumberOfANDGates();
		List<Gate> identityGates = new ArrayList<Gate>();
		for (Gate g: gates) {
			int currentOutputNumber = g.getOutputWireIndex() - numOfNonOutputs;
			if (currentOutputNumber >= 0) {
				int newOutputIndex = currentOutputNumber + numOfWires;
				Gate gIdentity = new Gate("2 1 " + g.getOutputWireIndex() + " " + 
						g.getOutputWireIndex() + " " + newOutputIndex + " 0001", InputGateType.FAIRPLAY);
				gIdentity.setGateNumber(numberOfAndGates+currentOutputNumber);
				identityGates.add(gIdentity);
			}
		}
		gates.addAll(identityGates);
	}
	@Override
	public List<List<Gate>> getGates() {
		
		newCircuitParser = new CircuitParser<Gate>() {

			@Override
			public File getCircuitFile() {
				return circuitParser.getCircuitFile();
			}

			@Override
			public List<Gate> getGates() {
				return gates;
			}

			@Override
			public String[] getHeaders() {
				return circuitParser.getHeaders();
				
			}

			@Override
			public int getNumberOfInputs() {
				return circuitParser.getNumberOfInputs();
			}

			@Override
			public int getNumberOfOutputs() {
				return circuitParser.getNumberOfOutputs();
			}

			@Override
			public int getNumberOfANDGates() {
				return circuitParser.getNumberOfANDGates() + circuitParser.getNumberOfOutputs();
			}

			@Override
			public int getNumberOfP1Inputs() {
				return circuitParser.getNumberOfP1Inputs();
			}

			@Override
			public int getNumberOfP2Inputs() {
				return circuitParser.getNumberOfP2Inputs();
			}

			@Override
			public int getNumberOfWires() {
				return circuitParser.getNumberOfWires() + circuitParser.getNumberOfOutputs();
			}
		};
		
		listConverter = new ListToLayersConverter(newCircuitParser);
		return listConverter.getGates();
//		return gates;
	}

	@Override
	public String[] getHeaders() {
//		int newNumberOfWires = circuitParser.getNumberOfWires() + circuitParser.getNumberOfOutputs(); 
//		int numberOfGates = newNumberOfWires - circuitParser.getNumberOfInputs();
//		String header0 = numberOfGates + " " + newNumberOfWires;
//		String[] currentHeaders = circuitParser.getHeaders();
//		String header1 = currentHeaders[0] + " " + currentHeaders[1] + " " + currentHeaders[2];
//		return new String[]{header0, header1};
		return listConverter.getHeaders();
	}

	@Override
	public CircuitParser<Gate> getCircuitParser() {
		return circuitParser;
	}

}
