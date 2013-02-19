
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import output.CircuitEvaluator;

import parsers.CUDAParser;
import parsers.FairplayParser;
import parsers.SPACLParser;

import common.CircuitParser;
import common.CommonUtilities;
import common.Driver;
import common.Gate;
import converters.FairplayToCUDAConverter;
import converters.SPACLOutputter;

import static org.junit.Assert.*;

public class AllTests {
	
	@Test
	public void assertCircuitEvaluator(){
		File inputFile = new File("test/data/input/input0.bin");
		File outputFile = new File("test/data/out.bin");
		File circuitFile = new File("test/data/aes_cuda.txt");

		CUDAParser cudaCircuitParser = 
				new CUDAParser(circuitFile);
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, cudaCircuitParser.getGates(), 
				cudaCircuitParser.getHeaders()[0], Driver.FAIRPLAY_EVALUATOR);
		eval.run();

		File expectedResultFile = new File("test/data/input/expected0.bin");

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
		List<List<Gate>> layersOfGates = circuitConverter.getGates();
		String header[] = circuitConverter.getHeaders();
		CommonUtilities.outputCUDACircuit(layersOfGates, circuitOutputFile, header[0]);
		
		CircuitParser<List<Gate>> cudaCircuitParser = 
				new CUDAParser(circuitOutputFile);
		checkWithEvaluator(cudaCircuitParser, 4, "test/data/input/aes_input_", 
				"test/data/input/aes_expected_", Driver.FAIRPLAY_EVALUATOR);
	}
	
	@Test
	public void assertAESCircuit() {
		//parse MD5
		File circuitFile = new File("test/data/nigel/AES-non-expanded.txt");
		File spaclFile = new File("test/data/out/aes_spacl.txt");

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		//output spacl
		SPACLOutputter spacl = new SPACLOutputter(circuitConverter, spaclFile, "md5");
		spacl.run();
		SPACLParser spaclCircuitParser = 
				new SPACLParser(spaclFile);
		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/aes_input_", 
				"test/data/input/aes_expected_", Driver.FAIRPLAY_EVALUATOR_REVERSED);
	}

	@Test
	public void assertMD5Circuit() {
		//parse MD5
		File circuitFile = new File("test/data/nigel/md5.txt");
		File spaclFile = new File("test/data/out/md5_spacl_tmp.txt");

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		//output spacl
		SPACLOutputter spacl = new SPACLOutputter(circuitConverter, spaclFile, "md5");
		spacl.run();
		SPACLParser spaclCircuitParser = 
				new SPACLParser(spaclFile);
		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/md5_input_", 
				"test/data/input/md5_expected_", Driver.FAIRPLAY_EVALUATOR);
	}
	
	@Test
	public void assertSHA1Circuit() {
		//parse MD5
		File circuitFile = new File("test/data/nigel/sha-1.txt");
		File spaclFile = new File("test/data/out/sha-1_spacl_tmp.txt");

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		//output spacl
		SPACLOutputter spacl = new SPACLOutputter(circuitConverter, spaclFile, "sha1");
		spacl.run();
		SPACLParser spaclCircuitParser = 
				new SPACLParser(spaclFile);
		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/sha1_input_", 
				"test/data/input/sha1_expected_", Driver.FAIRPLAY_EVALUATOR);
	}
	
	@Test
	public void assertSHA256Circuit() {
		//parse MD5
		File circuitFile = new File("test/data/nigel/sha-256.txt");
		File spaclFile = new File("test/data/out/sha-256_spacl_tmp.txt");

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser);
		//output spacl
		SPACLOutputter spacl = new SPACLOutputter(circuitConverter, spaclFile, "sha1");
		spacl.run();
		SPACLParser spaclCircuitParser = 
				new SPACLParser(spaclFile);
		checkWithEvaluator(spaclCircuitParser, 4, "test/data/input/sha256_input_", 
				"test/data/input/sha256_expected_", Driver.FAIRPLAY_EVALUATOR);
	}
	
	private void checkWithEvaluator(CircuitParser<List<Gate>> circuitParser, 
			int numberOfTests, String inputPrefix, String outputPrefix, String evalType){
		//Checks that the converted circuit is correct
		boolean res = true;
		for(int i = 0; i < numberOfTests; i++){
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
		circuitParser.getCircuitFile().delete();
		assertTrue("The converted circuit did not evaluate correctly", 
				res);
	}
} 