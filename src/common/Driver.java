package common;


import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import output.CircuitEvaluator;
import parsers.CUDAParser;
import parsers.FairplayParser;
import parsers.SPACLParser;
import parsers.VerilogParser;
import converters.FairplayToAugConverter;
import converters.FairplayToAugMultipleConverter;
import converters.FairplayToCUDAConverter;
import converters.SPACLOutputter;


public class Driver {

	// Converters
	public static final String FAIRPLAY_CONVERT_TO_CUDA = "-fc";
	public static final String FAIRPLAY_AUG_CHECKSUM = "-ac";
	public static final String FAIRPLAY_AUG_MULTI_OUTPUT = "-am";
	public static final String VERILOG_TO_FAIRPLAY = "-vc";
	public static final String FAIRPLAY_TO_SPACL = "-spacl";

	// Evaluators
	public static final String FAIRPLAY_EVALUATOR = "-fe";
	public static final String FAIRPLAY_EVALUATOR_IA32 = "-fe32";
	public static final String FAIRPLAY_EVALUATOR_MIRRORED = "-feMI";
	public static final String FAIRPLAY_EVALUATOR_REVERSED = "-feRE";
	public static final String CUDA_EVALUATOR = "-ce";
	public static final String SPACL_EVALUATOR = "-se";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = null;
		File circuitFile = null;
		File outputFile = null;
		boolean stripWires = false;

		String mode = args[0];
		// -fc circuitfile outputfile strip
		if (mode.equals(FAIRPLAY_CONVERT_TO_CUDA) && checkArgs(args, 4)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			if (args[3].equals("strip")) {
				stripWires = true;
			}
			FairplayParser circuitParser = new FairplayParser(circuitFile, stripWires);
			CircuitConverter<List<Gate>> circuitConverter = new FairplayToCUDAConverter(
					circuitParser);

			List<List<Gate>> layersOfGates = circuitConverter.getGates();
			String[] headers = circuitConverter.getHeaders();
			CommonUtilities.outputCUDACircuit(layersOfGates, outputFile, headers[0]);

		}
		// -ac circuitfile outputfile l (int) strip
		else if (mode.equals(FAIRPLAY_AUG_CHECKSUM) && checkArgs(args, 5)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			int l = Integer.parseInt(args[3]);
			if (args[4].equals("strip")) {
				stripWires = true;
			}

			FairplayParser circuitParser = new FairplayParser(circuitFile, stripWires);
			CircuitConverter<Gate> circuitConverter = new FairplayToAugConverter(circuitParser, l);

			executeConverter(circuitConverter, outputFile);
		}
		// -am circuitfile outputfile strip
		else if (mode.equals(FAIRPLAY_AUG_MULTI_OUTPUT) && checkArgs(args, 4)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			if (args[3].equals("strip")) {
				stripWires = true;
			}

			FairplayParser circuitParser = new FairplayParser(circuitFile, stripWires);
			CircuitConverter<Gate> circuitConverter = 
					new FairplayToAugMultipleConverter(circuitParser);

			executeConverter(circuitConverter, outputFile);
		}
		// -vc circuitfile outputfile
		else if (mode.equals(VERILOG_TO_FAIRPLAY) && checkArgs(args, 3)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			CircuitParser<Gate> circuitParser = 
					new VerilogParser(circuitFile);

			CommonUtilities.outputFairplayCircuit(circuitParser.getGates(), 
					outputFile, circuitParser.getHeaders());
		}
		// -spacl circuitfile outputfile
		else if (mode.equals(FAIRPLAY_TO_SPACL) && checkArgs(args, 3)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			String circuitName = FilenameUtils.removeExtension(outputFile.getName());
			FairplayParser circuitParser = 
					new FairplayParser(circuitFile, true);
			FairplayToCUDAConverter circuitConverter =
					new FairplayToCUDAConverter(circuitParser);
			SPACLOutputter fairplayToSPACL = 
					new SPACLOutputter(circuitConverter, outputFile, circuitName);
			fairplayToSPACL.run(); //Needs FairplayEvaluator reversed mode to pass tests
		}
		// --------------------------------------------------------------------
		// -fe, -fe32, -feMI, -feRE inputfile circuitfile outputfile strip
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

			FairplayParser circuitParser = 
					new FairplayParser(circuitFile, stripWires);
			FairplayToCUDAConverter circuitConverter = 
					new FairplayToCUDAConverter(circuitParser);
			List<List<Gate>> layersOfGates = 
					circuitConverter.getGates();

			String[] headers = circuitConverter.getHeaders();
			CircuitEvaluator eval = new CircuitEvaluator(inputFile, outputFile,
					layersOfGates, headers[0], mode);

			eval.run();
		}
		// -ce inputfile circuitfile outputfile
		else if (mode.equals(CUDA_EVALUATOR) && checkArgs(args, 4)) {

			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			CUDAParser circuitParser = new CUDAParser(circuitFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, circuitParser.getGates(), 
					circuitParser.getHeaders()[0], mode);
			eval.run();
		}


		// -se inputfile outputfile
		else if (mode.equals(SPACL_EVALUATOR)) {
			inputFile = new File(args[1]);
			circuitFile = new File(args[2]);
			outputFile = new File(args[3]);
			SPACLParser circuitParser = new SPACLParser(circuitFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, circuitParser.getGates(), 
					circuitParser.getHeaders()[0], mode);
			eval.run();
		}
		else {
			System.out.println(
					"Your request could not be identified, please " +
					"use one of the following prefixes for your invoke:");
			System.out.println(FAIRPLAY_CONVERT_TO_CUDA + ": Fairplay to CUDA format");
			System.out.println(FAIRPLAY_AUG_CHECKSUM + ": Fairplay checkum augmentation");
			System.out.println(FAIRPLAY_AUG_MULTI_OUTPUT + ": Fairplay multi-output augmentation");
			System.out.println(FAIRPLAY_EVALUATOR + ": Fairplay evaluation");
			System.out.println(CUDA_EVALUATOR + ": CUDA evaluation");
			System.out.println(VERILOG_TO_FAIRPLAY + ": Verilog to Fairplay format");
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

	private static void executeConverter(CircuitConverter<Gate> circuitConverter, 
			File outputFile) {
		List<Gate> layersOfGates = circuitConverter.getGates();
		String[] headers = circuitConverter.getHeaders();
		CommonUtilities.outputFairplayCircuit(layersOfGates, outputFile, headers);
	}
}
