
import java.io.File;
import java.util.List;


public class Driver {

	public static final String FAIRPLAY_CONVERT_TO_CUDA = "-fc";
	public static final String AUG_CHECKSUM = "-ac";
	public static final String AUG_MULTI_OUTPUT = "-am";
	public static final String FAIRPLAY_EVALUATOR = "-fe";
	public static final String FAIRPLAY_EVALUATOR_IA32 = "-fe32";
	public static final String FAIRPLAY_EVALUATOR_MIRRORED = "-feMI";
	public static final String FAIRPLAY_EVALUATOR_REVERSED = "-feRE";
	public static final String CUDA_EVALUATOR = "-ce";
	public static final String VERILOG_CONVERT_TO_FAIRPLAY = "-vc";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = null;
		File circuitFile = null;
		File outputFile = null;
		boolean sorted = false;
		boolean stripWires = false;

		String mode = args[0];
		// -fc circuitfile outputfile strip
		if (mode.equals(FAIRPLAY_CONVERT_TO_CUDA) && checkArgs(args, 4)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			if (args[3].equals("strip")) {
				stripWires = true;
			}
			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, stripWires);
			FairplayCircuitConverter circuitConverter = new FairplayCircuitConverter(
					circuitParser, outputFile, sorted);
			circuitConverter.run();

		}
		// -ac circuitfile outputfile l (int) strip
		else if (mode.equals(AUG_CHECKSUM) && checkArgs(args, 5)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			int l = Integer.parseInt(args[3]);
			if (args[4].equals("strip")) {
				stripWires = true;
			}

			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, stripWires);
			FairplayCircuitAugChecksum ac = new FairplayCircuitAugChecksum(circuitParser, 
					outputFile, l);
			ac.run();
		}
		// -am circuitfile outputfile
		else if (mode.equals(AUG_MULTI_OUTPUT) && checkArgs(args, 4)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			if (args[3].equals("strip")) {
				stripWires = true;
			}

			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, stripWires);
			FairplayCircuitAugMultipleOutputs am = 
					new FairplayCircuitAugMultipleOutputs(circuitParser, outputFile);
			am.run();
		}

		// -fe, -fe32, -feMI, -feRE inputfile circuitfile outputfile
		else if ((mode.equals(FAIRPLAY_EVALUATOR) || 
				mode.equals(FAIRPLAY_EVALUATOR_IA32) || 
				mode.equals(FAIRPLAY_EVALUATOR_MIRRORED) || 
				mode.equals(FAIRPLAY_EVALUATOR_REVERSED)) && checkArgs(args, 5)) {	
			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			if (args[4].equals("strip")) {
				stripWires = true;
			}

			FairplayCircuitParser circuitParser = 
					new FairplayCircuitParser(circuitFile, stripWires);
			FairplayCircuitConverter circuitConverter = 
					new FairplayCircuitConverter(circuitParser, outputFile, 
							false);
			List<Gate> gates = circuitParser.getGates();
			List<List<Gate>> layersOfGates = 
					circuitConverter.getLayersOfGates(gates);

			CircuitEvaluator eval;
			eval = new CircuitEvaluator(inputFile, outputFile, layersOfGates, 
					circuitConverter.getHeader(layersOfGates), mode);

			eval.run();
		}
		// -ce inputfile circuitfile outputfile
		else if (mode.equals(CUDA_EVALUATOR) && checkArgs(args, 4)) {

			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			CUDACircuitParser circuitParser = new CUDACircuitParser(circuitFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, circuitParser.getGates(), 
					circuitParser.getCUDAHeader(), mode);
			eval.run();
		}
		// -vc circuitfile outputfile
		else if (mode.equals(VERILOG_CONVERT_TO_FAIRPLAY) && checkArgs(args, 3)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			VerilogToFairplayConverter verilogConverter = 
					new VerilogToFairplayConverter(circuitFile, outputFile);
			verilogConverter.run();
		}
		else {
			System.out.println(
					"Your request could not be identified, please " +
					"use one of the following prefixes for your invoke:");
			System.out.println(FAIRPLAY_CONVERT_TO_CUDA + ": Fairplay to CUDA format");
			System.out.println(AUG_CHECKSUM + ": Fairplay checkum augmentation");
			System.out.println(AUG_MULTI_OUTPUT + ": Fairplay multi-output augmentation");
			System.out.println(FAIRPLAY_EVALUATOR + ": Fairplay evaluation");
			System.out.println(CUDA_EVALUATOR + ": CUDA evaluation");
			System.out.println(VERILOG_CONVERT_TO_FAIRPLAY + ": Verilog to Fairplay format");
		}
	}


	private static boolean checkArgs(String[] args, int expectedNumberOfArgs) {
		if(args.length != expectedNumberOfArgs){
			System.out.println("Incorrect number of argumens, expected: " + 
					expectedNumberOfArgs);
			return false;
		}
		else return true;

	}
}
