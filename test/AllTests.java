import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import static org.junit.Assert.*;

public class AllTests {

	@Before
	public void setUp() {

	}

	@Test
	public void assertAESCircuitConvertedProperly() {
		
		//Runs the converter
		File circuitFile = new File("data/aes_fairplay.txt");
		File circuitOutputFile = new File("data/fairplay_to_cuda.txt");
		FairplayCircuitParser circuitParser = 
				new FairplayCircuitParser(circuitFile);
		FairplayCircuitConverter circuitConverter = 
				new FairplayCircuitConverter(circuitParser, 
						circuitOutputFile, false);
		circuitConverter.run();

		//Checks that the converted circuit is correct
		boolean results = true;
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
				results = results && FileUtils.contentEquals(expectedResultFile, 
						outputFile);
				outputFile.delete();
			} catch (IOException e) {
			}
		}
		circuitOutputFile.delete();
		assertTrue("The converted circuit did not evaluate correctly", 
				results);


	}

} 