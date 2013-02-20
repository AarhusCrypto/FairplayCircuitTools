
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import output.CircuitEvaluator;

import parsers.CUDAParser;
import parsers.FairplayParser;
import parsers.SPACLParser;

import common.CircuitConverter;
import common.CircuitParser;
import common.CircuitProvider;
import common.CommonUtilities;
import common.Driver;
import common.Gate;
import converters.FairplayToCUDAConverter;
import converters.FairplayToSPACLConverter;

import static org.junit.Assert.*;

public class AllTests {
	
	@Test
	public void assertCircuitEvaluator(){
		File inputFile = new File("test/data/input/aes_input_0.bin");
		File outputFile = new File("test/data/out.bin");
		File circuitFile = new File("test/data/aes_cuda.txt");

		CUDAParser cudaCircuitParser = 
				new CUDAParser(circuitFile);
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, cudaCircuitParser.getGates(), 
				cudaCircuitParser.getHeaders()[0], Driver.EVAL_FAIRPLAY);
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

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		CommonUtilities.outputCUDACircuit(circuitConverter, circuitOutputFile);
		
		CircuitParser<List<Gate>> cudaCircuitParser = 
				new CUDAParser(circuitOutputFile);
		checkWithEvaluator(cudaCircuitParser, 4, "test/data/input/aes_input_", 
				"test/data/input/aes_expected_", Driver.EVAL_FAIRPLAY);
	}
	
	/*
	 * For testing the SPACL parser
	 */
	@Test
	public void assertAESCircuit() {
		File spaclFile = new File("test/data/out/aes_spacl.spaclc");
		SPACLParser spaclCircuitParser = 
				new SPACLParser(spaclFile);
		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/aes_input_", 
				"test/data/input/aes_expected_", Driver.EVAL_FAIRPLAY_REVERSED);
	}

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
	
	private void checkNigelCircuit(File circuitFile, String inputPrefix, String outputPrefix) {
		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		CircuitConverter<List<Gate>, Gate> circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);


		FairplayToSPACLConverter spaclConverter = 
				new FairplayToSPACLConverter(circuitConverter);
		checkWithEvaluator(spaclConverter, 4, inputPrefix, 
				outputPrefix, Driver.EVAL_FAIRPLAY);
	}
	
	private void checkWithEvaluator(CircuitProvider<List<Gate>> circuitParser, 
			int numberOfTests, String inputPrefix, String outputPrefix, String evalType){
		//Checks that the converted circuit is correct
		boolean res = true;
		for (int i = 0; i < numberOfTests; i++) {
			File inputFile = new File(inputPrefix + i + ".bin");
			File outputFile = new File("test/data/out/out.bin");
			
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, circuitParser.getGates(), 
					circuitParser.getHeaders()[0], evalType);
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