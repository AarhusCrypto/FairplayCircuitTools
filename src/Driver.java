
import java.io.File;
import java.util.List;


public class Driver {

	private static final String FAIRPLAY_CONVERT_TO_CUDA = "-fc";
	private static final String AUG_CHECKSUM = "-ac";
	private static final String AUG_MULTI_OUTPUT = "-am";
	private static final String FAIRPLAY_EVALUATOR = "-fe";
	private static final String CUDA_EVALUATOR = "-ce";
	private static final String VERILOG_CONVERT_TO_FAIRPLAY = "-vc";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = null;
		File circuitFile = null;
		File outputFile = null;
		boolean sorted = false;
		
		String operation = args[0];
		//-fc circuitfile outputfile
		if (operation.equals(FAIRPLAY_CONVERT_TO_CUDA) && checkArgs(args, 3)){
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile);
			FairplayCircuitConverter circuitConverter = new FairplayCircuitConverter(
					circuitParser, outputFile, sorted);
			circuitConverter.run();
			
		}
		//-ac circuitfile outputfile l (int)
		else if (operation.equals(AUG_CHECKSUM) && checkArgs(args, 4)){
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			int l = Integer.parseInt(args[3]);
			
			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile);
			FairplayCircuitAugChecksum ac = new FairplayCircuitAugChecksum(circuitParser, 
					outputFile, l);
			ac.run();
		}
		//-am circuitfile outputfile
		else if (operation.equals(AUG_MULTI_OUTPUT) && checkArgs(args, 3)){
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			
			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile);
			FairplayCircuitAugMultipleOutputs am = 
					new FairplayCircuitAugMultipleOutputs(circuitParser, outputFile);
			am.run();
		}
		
		//-fe inputfile circuitfile outputfile
		else if (operation.equals(FAIRPLAY_EVALUATOR) && checkArgs(args, 4)){
			
			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			
			FairplayCircuitParser circuitParser = 
					new FairplayCircuitParser(circuitFile);
			FairplayCircuitConverter circuitConverter = 
					new FairplayCircuitConverter(circuitParser, outputFile, 
							false);
			List<Gate> gates = circuitParser.getGates();
			List<List<Gate>> layersOfGates = 
					circuitConverter.getLayersOfGates(gates);
			
			CircuitEvaluator eval = 
					new CircuitEvaluator(inputFile, outputFile, layersOfGates, 
							circuitConverter.getHeader(layersOfGates));
			eval.run();
		}
		//-ce inputfile circuitfile outputfile
		else if (operation.equals(CUDA_EVALUATOR) && checkArgs(args, 4)){
			
			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			CUDACircuitParser circuitParser = new CUDACircuitParser(circuitFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, circuitParser.getGates(), 
					circuitParser.getCUDAHeader());
			eval.run();
		}
		// nc circuitfile outputfile
		else if (operation.equals(VERILOG_CONVERT_TO_FAIRPLAY) && checkArgs(args, 3)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			VerilogToFairplayConverter nistConverter = 
					new VerilogToFairplayConverter(circuitFile, outputFile);
			nistConverter.run();
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
