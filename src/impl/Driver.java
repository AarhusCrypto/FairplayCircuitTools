package impl;

import java.io.File;
import java.util.List;

import common.CircuitConverter;
import common.CircuitParser;
import common.CommonUtilities;
import common.Gate;


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
	public static final String FAIRPLAY_TO_SPACL = "-spacl";


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
			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, stripWires);
			CircuitConverter<List<Gate>> circuitConverter = new FairplayCircuitConverter(
					circuitParser, false);
			
			List<List<Gate>> layersOfGates = circuitConverter.getGates();
			String[] headers = circuitConverter.getHeaders();
			CommonUtilities.outputCUDACircuit(layersOfGates, outputFile, headers[0]);

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
			CircuitConverter<Gate> circuitConverter = new FairplayCircuitAugChecksum(circuitParser, l);
			
			List<Gate> layersOfGates = circuitConverter.getGates();
			String[] headers = circuitConverter.getHeaders();
			CommonUtilities.outputFairplayCircuit(layersOfGates, outputFile, headers);
		}
		// -am circuitfile outputfile
		else if (mode.equals(AUG_MULTI_OUTPUT) && checkArgs(args, 4)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			if (args[3].equals("strip")) {
				stripWires = true;
			}

			FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, stripWires);
			CircuitConverter<Gate> circuitConverter = 
					new FairplayCircuitAugMultipleOutputs(circuitParser);
			
			List<Gate> layersOfGates = circuitConverter.getGates();
			String[] headers = circuitConverter.getHeaders();
			CommonUtilities.outputFairplayCircuit(layersOfGates, outputFile, headers);
		}

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

			FairplayCircuitParser circuitParser = 
					new FairplayCircuitParser(circuitFile, stripWires);
			FairplayCircuitConverter circuitConverter = 
					new FairplayCircuitConverter(circuitParser,	false);
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
			CircuitParser circuitParser = 
					new VerilogCircuitParser(circuitFile);
			
			CommonUtilities.outputFairplayCircuit(circuitParser.getGates(), 
					outputFile, circuitParser.getHeaders());
		}
		// -spacl circuitfile outputfile
		else if (mode.equals(FAIRPLAY_TO_SPACL) && checkArgs(args, 3)) {
			circuitFile = new File(args[1]);
			outputFile = new File(args[2]);
			String circuitName = outputFile.getName();
			FairplayCircuitParser circuitParser = 
					new FairplayCircuitParser(circuitFile, true);
			FairplayCircuitConverter circuitConverter =
					new FairplayCircuitConverter(circuitParser, true);
			FairplayCircuitToSPACL fairplaytoSpacl = 
					new FairplayCircuitToSPACL(circuitConverter, outputFile, circuitName);
			fairplaytoSpacl.run();
//			List<List<Gate>> gates = spaclCircuitConverter.getGates();
//			String[] headers = spaclCircuitConverter.getHeaders();
//			
//			//This circuit needs to be evaluated using FAIRPLAY_EVALUATOR_REVERSED
//			CommonUtilities.outputCUDACircuit(gates, outputFile, headers[0]);
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
