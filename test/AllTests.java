
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import output.CircuitEvaluator;

import parsers.CUDAParser;
import parsers.FairplayParser;

import common.CommonUtilities;
import common.Driver;
import common.Gate;
import converters.FairplayToCUDAConverter;

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
		File circuitOutputFile = new File("test/data/aes_cuda_tmp.txt");

		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, true);
		FairplayToCUDAConverter circuitConverter = 
				new FairplayToCUDAConverter(circuitParser, false);
		List<List<Gate>> layersOfGates = circuitConverter.getGates();
		String header[] = circuitConverter.getHeaders();
		CommonUtilities.outputCUDACircuit(layersOfGates, circuitOutputFile, header[0]);
		
		checkWithEvaluator(circuitOutputFile);

	}
	
	private void checkWithEvaluator(File circuitOutputFile){
		//Checks that the converted circuit is correct
		boolean res = true;
		for(int i = 0; i < 4; i++){
			File inputFile = new File("test/data/input/input" + i + ".bin");
			File outputFile = new File("test/data/out.bin");
			CUDAParser cudaCircuitParser = 
					new CUDAParser(circuitOutputFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, cudaCircuitParser.getGates(), 
					cudaCircuitParser.getHeaders()[0], Driver.FAIRPLAY_EVALUATOR);
			eval.run();

			File expectedResultFile = new File("test/data/input/expected" + i +
					".bin");
			try {
				res = res && FileUtils.contentEquals(expectedResultFile, 
						outputFile);
				outputFile.delete();
			} catch (IOException e) {
			}
		}
		circuitOutputFile.delete();
		assertTrue("The converted circuit did not evaluate correctly", 
				res);
	}
} 