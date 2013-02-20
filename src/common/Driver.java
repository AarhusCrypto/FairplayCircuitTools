package common;


import java.io.File;
import java.util.List;

import output.CircuitEvaluator;
import parsers.CUDAParser;
import parsers.FairplayParser;
import parsers.SPACLParser;
import parsers.VerilogParser;
import converters.FairplayToAugConverter;
import converters.FairplayToAugMultipleConverter;
import converters.FairplayToCUDAConverter;
import converters.FairplayToSPACLConverter;


public class Driver {

	// Converters
	public static final String CONVERT_FAIRPLAY_TO_CUDA = "-fc";
	public static final String CONVERT_FAIRPLAY_TO_SPACL = "-spacl";
	public static final String CONVERT_VERILOG_TO_FAIRPLAY = "-vc";
	public static final String AUG_FAIRPLAY_CHECKSUM = "-ac";
	public static final String AUG_FAIRPLAY_MULTIOUTPUT = "-am";

	// Evaluators
	public static final String EVAL_FAIRPLAY = "-fe";
	public static final String EVAL_FAIRPLAY_IA32 = "-fe32";
	public static final String EVAL_FAIRPLAY_MIRRORED = "-feMI";
	public static final String EVAL_FAIRPLAY_REVERSED = "-feRE";
	public static final String EVAL_CUDA = "-ce";
	public static final String EVAL_SPACL = "-se";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean stripWires = true;

		String mode = args[0];
		// -fc circuitfile outputfile
		if (mode.equals(CONVERT_FAIRPLAY_TO_CUDA) && checkArgs(args, 3)) {
			convertFairplayToCUDA(args, stripWires);
		}
		// -spacl circuitfile outputfile
		else if (mode.equals(CONVERT_FAIRPLAY_TO_SPACL) && checkArgs(args, 3)) {
			convertFairplayToSPACL(args, stripWires);
		}
		// -vc circuitfile outputfile
		else if (mode.equals(CONVERT_VERILOG_TO_FAIRPLAY) && checkArgs(args, 3)) {
			convertVerilogToFairplay(args);
		}
		// -ac circuitfile outputfile l (int)
		else if (mode.equals(AUG_FAIRPLAY_CHECKSUM) && checkArgs(args, 4)) {
			augmentFairplayChecksum(args, stripWires);
		}
		// -am circuitfile outputfile
		else if (mode.equals(AUG_FAIRPLAY_MULTIOUTPUT) && checkArgs(args, 3)) {
			augmentFairplayMultiOutput(args, stripWires);
		}

		// --------------------------------------------------------------------
		// -fe, -fe32, -feMI, -feRE inputfile circuitfile outputfile
		else if ((mode.equals(EVAL_FAIRPLAY) || 
				mode.equals(EVAL_FAIRPLAY_IA32) || 
				mode.equals(EVAL_FAIRPLAY_MIRRORED) || 
				mode.equals(EVAL_FAIRPLAY_REVERSED)) && checkArgs(args, 4)) {	
			evalFairplay(args, stripWires, mode);
		}
		// -ce inputfile circuitfile outputfile
		else if (mode.equals(EVAL_CUDA) && checkArgs(args, 4)) {
			evalCUDA(args, mode);
		}
		// -se inputfile outputfile
		else if (mode.equals(EVAL_SPACL) && checkArgs(args, 3)) {
			evalSPACL(args, mode);
		}
		else {
			System.out.println(
					"Your request could not be identified, please " +
					"use one of the following prefixes for your request:");
			System.out.println(CONVERT_FAIRPLAY_TO_CUDA + ": Fairplay to CUDA format");
			System.out.println(CONVERT_FAIRPLAY_TO_SPACL + ": Fairplay to SPACL format");
			System.out.println(CONVERT_VERILOG_TO_FAIRPLAY + ": Verilog to Fairplay format");
			System.out.println(AUG_FAIRPLAY_CHECKSUM + ": Fairplay checkum augmentation");
			System.out.println(AUG_FAIRPLAY_MULTIOUTPUT + ": Fairplay multi-output augmentation");
			System.out.println(EVAL_FAIRPLAY + ": Fairplay evaluation");
			System.out.println(EVAL_CUDA + ": CUDA evaluation");
			System.out.println(EVAL_SPACL + ": SPACL evaluation");
		}
	}
	
	private static void convertFairplayToCUDA(String[] args, boolean stripWires) {
		FairplayParser circuitParser = new FairplayParser(new File(args[1]), stripWires);
		FairplayToCUDAConverter circuitConverter = new FairplayToCUDAConverter(
				circuitParser);
		CommonUtilities.outputCUDACircuit(circuitConverter, new File(args[2]));
	}
	
	private static void convertFairplayToSPACL(String[] args, boolean stripWires) {
		String outputFileName = args[2];
		int dotIndex = outputFileName.lastIndexOf('.');
		if (dotIndex >= 0) { // to prevent exception if there is no dot
			outputFileName = outputFileName.substring(0, dotIndex);
		}

		FairplayParser circuitParser = 
				new FairplayParser(new File(args[1]), stripWires);
		FairplayToCUDAConverter circuitConverter =
				new FairplayToCUDAConverter(circuitParser);
		FairplayToSPACLConverter fairplayToSPACL = 
				new FairplayToSPACLConverter(circuitConverter);

		CommonUtilities.outputSPACLCircuit(fairplayToSPACL, outputFileName);
	}
	
	private static void convertVerilogToFairplay(String[] args) {
		CircuitParser<Gate> circuitParser = 
				new VerilogParser(new File(args[1]));
		CommonUtilities.outputFairplayCircuit(circuitParser, 
				new File(args[2]));
	}

	private static void augmentFairplayChecksum(String[] args,
			boolean stripWires) {
		int l = Integer.parseInt(args[3]);

		FairplayParser circuitParser = new FairplayParser(new File(args[1]), stripWires);
		CircuitParser<Gate> circuitConverter = new FairplayToAugConverter(circuitParser, l);
		executeConverter(circuitConverter, new File(args[2]));
	}
	
	private static void augmentFairplayMultiOutput(String[] args,
			boolean stripWires) {
		FairplayParser circuitParser = new FairplayParser(new File(args[1]), stripWires);
		CircuitParser<Gate> circuitConverter = 
				new FairplayToAugMultipleConverter(circuitParser);
		executeConverter(circuitConverter, new File(args[2]));
	}
	
	private static void evalFairplay(String[] args, boolean stripWires,
			String mode) {
		FairplayParser circuitParser = 
				new FairplayParser(new File(args[2]), stripWires);
		CircuitConverter<List<Gate>, Gate> circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		evaluate(new File(args[1]), new File(args[3]), circuitConverter, mode);
	}
	
	private static void evalCUDA(String[] args, String mode) {
		CircuitParser<List<Gate>> circuitParser = new CUDAParser(new File(args[2]));
		evaluate(new File(args[1]), new File(args[3]), circuitParser, mode);
	}
	
	private static void evalSPACL(String[] args, String mode) {
		CircuitParser<List<Gate>> circuitParser = new SPACLParser(new File(args[2]));
		evaluate(new File(args[1]), new File(args[3]), circuitParser, mode);
	}

	private static void evaluate(File inputFile, File outputFile, 
			CircuitProvider<List<Gate>> circuitParser, String mode) {
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, circuitParser.getGates(), 
				circuitParser.getHeaders()[0], mode);
		eval.run();
	}

	private static boolean checkArgs(String[] args, int expectedNumberOfArgs) {
		if(args.length != expectedNumberOfArgs){
			System.out.println("Incorrect number of argumens, expected: " + 
					expectedNumberOfArgs);
			return false;
		}
		else return true;

	}

	private static void executeConverter(CircuitParser<Gate> circuitConverter, 
			File outputFile) {
		CommonUtilities.outputFairplayCircuit(circuitConverter, outputFile);
	}
}
