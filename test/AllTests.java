
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import output.CircuitEvaluator;
import parsers.CUDAParser;
import parsers.FairplayParser;
import parsers.SCDParser;
import parsers.SPACLParser;
import common.CircuitConverter;
import common.CircuitParser;
import common.CircuitProvider;
import common.CommonUtilities;
import common.Driver;
import common.Gate;
import converters.ListToLayersConverter;
import converters.FairplayToSPACLConverter;
import static org.junit.Assert.*;

public class AllTests {
	
	@Test
	public void assertCircuitEvaluatorCUDA(){
		File outDir = new File("test/data/out/");
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		
		File inputFile = new File("test/data/input/aes_input_0.bin");
		File outputFile = new File("test/data/out/out.bin");
		File circuitFile = new File("test/data/aes_cuda_nigel.txt");

		CUDAParser cudaCircuitParser = 
				new CUDAParser(circuitFile);
		
		List<List<Gate>> cudaGates = cudaCircuitParser.getGates();
		String header = cudaCircuitParser.getHeaders()[0];
		
		CircuitEvaluator eval = new CircuitEvaluator(inputFile, outputFile, cudaGates, 
				header, Driver.EVAL_FAIRPLAY_REVERSED);
		eval.run();

		File expectedResultFile = new File("test/data/input/aes_expected_0.bin");

		boolean res = false;
		try {
			res = FileUtils.contentEquals(expectedResultFile, 
					outputFile);
		} catch (IOException e) {
		}
		outputFile.delete();
		assertTrue("The circuit did not evaluate correctly", res);
	}
	
	@Test
	public void assertCircuitEvaluatorNigel(){
		File outDir = new File("test/data/out/");
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		File inputFile = new File("test/data/input/aes_input_0.bin");
		File outputFile = new File("test/data/out/out1.bin");
		File circuitFile = new File("test/data/nigel/AES-non-expanded.txt");

		FairplayParser fairplayCircuitParser = 
				new FairplayParser(circuitFile, false);
		ListToLayersConverter circuitConverter = 
				new ListToLayersConverter(fairplayCircuitParser);
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, circuitConverter.getGates(), 
				circuitConverter.getHeaders()[0], Driver.EVAL_FAIRPLAY_REVERSED);
		eval.run();

		File expectedResultFile = new File("test/data/input/aes_expected_0.bin");

		boolean res = false;
		try {
			res = FileUtils.contentEquals(expectedResultFile, 
					outputFile);
		} catch (IOException e) {
		}
		outputFile.delete();
		assertTrue(res);
	}

	@Test
	public void assertAESCircuitConverted() {
		File circuitFile = new File("test/data/aes_fairplay.txt");
		File circuitOutputFile = new File("test/data/out/aes_cuda_tmp.txt");
		File outDir = new File("test/data/out/");
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		ListToLayersConverter circuitConverter = 
				new ListToLayersConverter(circuitParser);
		CommonUtilities.outputCUDACircuit(circuitConverter, circuitOutputFile);
		
		CircuitParser<List<Gate>> cudaCircuitParser = 
				new CUDAParser(circuitOutputFile);
		checkWithEvaluator(cudaCircuitParser, 4, "test/data/input/aes_input_", 
				"test/data/input/aes_expected_", Driver.EVAL_FAIRPLAY);
	}
//	
//	/*
//	 * For testing the SPACL parser
//	 */
//	@Test
//	public void assertAESCircuit() {
//		File spaclFile = new File("test/data/aes_spacl.spaclc");
//		SPACLParser spaclCircuitParser = 
//				new SPACLParser(spaclFile);
//		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/aes_input_", 
//				"test/data/input/aes_expected_", Driver.EVAL_FAIRPLAY_REVERSED);
//	}
//
	@Test
	public void assertMD5Circuit() {
		File circuitFile = new File("test/data/nigel/md5.txt");
		checkNigelCircuit(circuitFile, "test/data/input/md5_input_", "test/data/input/md5_expected_");
	}
	
	@Test
	public void assertSHA1Circuit() {
		File circuitFile = new File("test/data/nigel/sha-1.txt");
		checkNigelCircuit(circuitFile, "test/data/input/sha1_input_", "test/data/input/sha1_expected_");
	}
	
	@Test
	public void assertSHA256Circuit() {
		File circuitFile = new File("test/data/nigel/sha-256.txt");
		checkNigelCircuit(circuitFile, "test/data/input/sha256_input_", "test/data/input/sha256_expected_");
	}
	
//	@Test
//	public void assertSCD() {
//		File circuitFile = new File("test/data/nigel/AES-non-expanded.txt");
//		File circuitOutputFile = new File("test/data/out/aes.scd");
//		File outDir = new File("test/data/out/");
//		if (!outDir.isDirectory()) {
//			outDir.mkdir();
//		}
//
//		FairplayParser circuitParser = 
//				new FairplayParser(circuitFile, true);
//		ListToLayersConverter circuitConverter = 
//				new ListToLayersConverter(circuitParser);
//		CommonUtilities.outputSCDCircuit(circuitConverter, circuitOutputFile);
//		
//		File inputFile = new File("test/data/input/aes_input_0.bin");
//		File outputFile = new File("test/data/out.bin");
//
//		SCDParser parser = new SCDParser(circuitOutputFile);
//		CircuitEvaluator eval = new CircuitEvaluator(
//				inputFile, outputFile, parser.getGates(), 
//				parser.getHeaders()[0], Driver.EVAL_FAIRPLAY_REVERSED);
//		eval.run();
//
//		File expectedResultFile = new File("test/data/input/aes_expected_0.bin");
//
//		boolean res = false;
//		try {
//			res = FileUtils.contentEquals(expectedResultFile, 
//					outputFile);
//		} catch (IOException e) {
//		}
//		outputFile.delete();
//		assertTrue(res);
//	}
	
	private void checkNigelCircuit(File circuitFile, String inputPrefix, String outputPrefix) {
		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		CircuitConverter<List<Gate>, Gate> circuitConverter = 
				new ListToLayersConverter(circuitParser);


		FairplayToSPACLConverter spaclConverter = 
				new FairplayToSPACLConverter(circuitConverter);
		checkWithEvaluator(spaclConverter, 4, inputPrefix, 
				outputPrefix, Driver.EVAL_FAIRPLAY);
	}
	
	private void checkWithEvaluator(CircuitProvider<List<Gate>> circuitParser, 
			int numberOfTests, String inputPrefix, String outputPrefix, String evalType){
		//Checks that the converted circuit is correct
		boolean res = true;
		File outDir = new File("test/data/out/");
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		for (int i = 0; i < numberOfTests; i++) {
			File inputFile = new File(inputPrefix + i + ".bin");
			File outputFile = new File("test/data/out/out.bin");
			
			List<List<Gate>> gates = circuitParser.getGates();
			String header = circuitParser.getHeaders()[0];
			
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, gates, 
					header, evalType);
			eval.run();

			File expectedResultFile = new File(outputPrefix + i +
					".bin");
			try {
				res = res && FileUtils.contentEquals(expectedResultFile, 
						outputFile);
				outputFile.delete();
			} catch (IOException e) {
			}
		}
		assertTrue("The converted circuit did not evaluate correctly", 
				res);
	}
} 