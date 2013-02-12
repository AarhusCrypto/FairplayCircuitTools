package impl;
import java.io.File;

import common.CircuitParser;
import common.CommonUtilities;

public class VerilogToFairplayConverter implements Runnable {
	private CircuitParser circuitParser;
	private File outputFile;

	public VerilogToFairplayConverter(CircuitParser circuitParser, File outputFile){
		this.circuitParser = circuitParser;
		this.outputFile = outputFile;
	}

	@Override
	public void run() {
		CommonUtilities.outputFairplayCircuit(circuitParser.getGates(), 
				outputFile, circuitParser.getHeaders());
	}
}
