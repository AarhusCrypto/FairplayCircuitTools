import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import static org.junit.Assert.*;

public class AllTests {

	//@Test
	public void assertAESCircuitAugMultipleOutput(){
		File circuitFile = new File("test/data/aes_multiple_fairplay.txt");
		File circuitOutputFile = new File("data/aug_multiple_aes_fairplay.txt");
		
		FairplayCircuitParser circuitParser = 
				new FairplayCircuitParser(circuitFile);
		FairplayCircuitAugMultipleOutputs am = 
				new FairplayCircuitAugMultipleOutputs(circuitParser, circuitOutputFile);
		am.run();
		

		circuitParser = 
				new FairplayCircuitParser(circuitOutputFile);
		
		File convertedCircuit = new File("data/aug_multiple_aes_cuda.txt");
		FairplayCircuitConverter circuitConverter = 
				new FairplayCircuitConverter(circuitParser, 
						convertedCircuit, false);
		circuitConverter.run();
		circuitOutputFile.delete();
		
		checkWithEvaluator(convertedCircuit, 
				new File("test/data/aug_multiple_aes_input.bin"));
	}
	
	@Test
	public void assertCircuitEvaluator(){
		File inputFile = new File("test/data/input0.bin");
		File outputFile = new File("data/out.bin");
		File circuitFile = new File("test/data/aes_cuda.txt");

		CUDACircuitParser cudaCircuitParser = 
				new CUDACircuitParser(circuitFile);
		CircuitEvaluator eval = new CircuitEvaluator(
				inputFile, outputFile, cudaCircuitParser.getGates(), 
				cudaCircuitParser.getCUDAHeader());
		eval.run();

		File expectedResultFile = new File("test/data/expected0.bin");

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
		File circuitOutputFile = new File("data/aes_cuda.txt");

		FairplayCircuitParser circuitParser = 
				new FairplayCircuitParser(circuitFile);
		FairplayCircuitConverter circuitConverter = 
				new FairplayCircuitConverter(circuitParser, 
						circuitOutputFile, false);
		circuitConverter.run();
		
		checkWithEvaluator(circuitOutputFile);

	}

	@Test
	public void assertAESCircuitAugChecksum(){

		File circuitFile = new File("test/data/aes_fairplay.txt");
		File circuitOutputFile = new File("data/aug_checksum_aes_fairplay.txt");
		
		FairplayCircuitParser circuitParser = 
				new FairplayCircuitParser(circuitFile);
		FairplayCircuitAugChecksum ac = 
				new FairplayCircuitAugChecksum(circuitParser, circuitOutputFile);
		ac.run();
		

		circuitParser = 
				new FairplayCircuitParser(circuitOutputFile);
		
		File convertedCircuit = new File("data/aug_checksum_aes_cuda.txt");
		FairplayCircuitConverter circuitConverter = 
				new FairplayCircuitConverter(circuitParser, 
						convertedCircuit, false);
		circuitConverter.run();
		circuitOutputFile.delete();
		
		checkWithEvaluator(convertedCircuit, 
				new File("test/data/aug_checksum_aes_input.bin"));
	}
	
	private void checkWithEvaluator(File circuitOutputFile){
		//Checks that the converted circuit is correct
		boolean res = true;
		for(int i = 0; i < 4; i++){
			File inputFile = new File("test/data/input" + i + ".bin");
			File outputFile = new File("data/out.bin");
			CUDACircuitParser cudaCircuitParser = 
					new CUDACircuitParser(circuitOutputFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, cudaCircuitParser.getGates(), 
					cudaCircuitParser.getCUDAHeader());
			eval.run();

			File expectedResultFile = new File("test/data/expected" + i +
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
	
	private void checkWithEvaluator(File circuitOutputFile, File inputFile){
		boolean res = true;
			File outputFile = new File("data/out.bin");
			CUDACircuitParser cudaCircuitParser = 
					new CUDACircuitParser(circuitOutputFile);
			CircuitEvaluator eval = new CircuitEvaluator(
					inputFile, outputFile, cudaCircuitParser.getGates(), 
					cudaCircuitParser.getCUDAHeader());
			eval.run();

			File expectedResultFile = new File("test/data/expected0.bin");
			try {
				res = res && FileUtils.contentEquals(expectedResultFile, 
						outputFile);
				outputFile.delete();
			} catch (IOException e) {
			}
		circuitOutputFile.delete();
		assertTrue("The converted circuit did not evaluate correctly", 
				res);
	}

} 