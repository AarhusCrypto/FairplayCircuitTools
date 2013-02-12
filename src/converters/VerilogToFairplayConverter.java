package converters;
import java.io.File;

import common.CircuitParser;
import common.CommonUtilities;
import common.Gate;

public class VerilogToFairplayConverter implements Runnable {
	private CircuitParser<Gate> circuitParser;
	private File outputFile;

	public VerilogToFairplayConverter(CircuitParser<Gate> circuitParser, File outputFile){
		this.circuitParser = circuitParser;
		this.outputFile = outputFile;
	}

	@Override
	public void run() {
		CommonUtilities.outputFairplayCircuit(circuitParser.getGates(), 
				outputFile, circuitParser.getHeaders());
	}
}
