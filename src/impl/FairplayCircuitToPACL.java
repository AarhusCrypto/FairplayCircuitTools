package impl;
import java.io.File;
import java.util.List;

import common.CircuitParser;
import common.Gate;

public class FairplayCircuitToPACL implements Runnable{

	private CircuitParser circuitParser;
	private FairplayCircuitConverter circuitConverter;
	private File outputFile;
	
	public FairplayCircuitToPACL(CircuitParser circuitParser, 
			FairplayCircuitConverter circuitConverter, File outputFile) {
		this.circuitParser = circuitParser;
		this.outputFile = outputFile;
	}
	
	@Override
	public void run() {
		List<Gate> gates = circuitParser.getGates();
		
		
	}


}
