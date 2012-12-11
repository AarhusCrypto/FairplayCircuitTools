import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import static org.junit.Assert.*;

public class AllTests {
	
	@Test
	public void assertCircuitEvaluator(){
		File inputFile = new File("test/data/input/input0.bin");
		File outputFile = new File("test/data/out.bin");
		File circuitFile = new File("test/data/aes_cuda.txt");

		CUDACircuitParser cudaCircuitParser = 
				new CUDACircuitParser(circuitFile);
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, cudaCircuitParser.getGates(), 
				cudaCircuitParser.getCUDAHeader(), false);
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

		FairplayCircuitParser circuitParser = 
				new FairplayCircuitParser(circuitFile);
		FairplayCircuitConverter circuitConverter = 
				new FairplayCircuitConverter(circuitParser, 
						circuitOutputFile, false);
		circuitConverter.run();
		
		checkWithEvaluator(circuitOutputFile);

	}
	
	private void checkWithEvaluator(File circuitOutputFile){
		//Checks that the converted circuit is correct
		boolean res = true;
		for(int i = 0; i < 4; i++){
			File inputFile = new File("test/data/input/input" + i + ".bin");
			File outputFile = new File("test/data/out.bin");
			CUDACircuitParser cudaCircuitParser = 
					new CUDACircuitParser(circuitOutputFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, cudaCircuitParser.getGates(), 
					cudaCircuitParser.getCUDAHeader(), false);
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