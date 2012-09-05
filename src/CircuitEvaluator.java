import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * @author Roberto Trifiletti
 *
 */
public class CircuitEvaluator implements Runnable {

	private static final int BYTESIZE = 8;
	private File inputFile;
	private File outputFile;
	private boolean verify;
	private List<List<Gate>> layersOfGates;

	private int inputSize;
	private int outputSize;
	@SuppressWarnings("unused")
	private int numberOfWires;

	/**
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @param parseStrategy
	 */
	public CircuitEvaluator(File inputFile, File outputFile,
			List<List<Gate>> layersOfGates, String header, boolean verify){
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.verify = verify;
		this.layersOfGates = layersOfGates;
		
		String[] split = header.split(" ");

		inputSize = Integer.parseInt(split[0]);
		outputSize = Integer.parseInt(split[1]);
		numberOfWires = Integer.parseInt(split[2]);
	}

	@Override
	public void run() {
		if(inputFile.length() != inputSize/BYTESIZE){
			System.out.println("Input mismatch, check inputfile");
			return;
		}

		byte[] bytesRead = getBytesFromFile();

		BitSet bitset = bitsetToByteArray(bytesRead);

		BitSet result = evalCircuit(layersOfGates, bitset);

		writeCircuitOutput(result);

		if (verify){
			verifyOutput();
		}
	}

	/**
	 * 
	 * @return the contents of the inputFile as a byte array.
	 */
	public byte[] getBytesFromFile() {
		byte[] bytesRead = null;
		try {
			RandomAccessFile f = new RandomAccessFile(inputFile, "r");
			bytesRead = new byte[(int)f.length()];
			f.read(bytesRead);
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytesRead;
	}

	/**
	 * Method for converting a byte[] to a BitSet
	 * @param bytes
	 * @return BitSet corresponding to the byte[], in little endian form
	 */
	public BitSet bitsetToByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i=0; i<bytes.length*8; i++) {
			if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
				bits.set((bytes.length*8 - 1) - i);
			}
		}
		return bits;
	}

	/**
	 * Method for evaluating the given circuit on the given input bits
	 * @param layersOfGates
	 * @param inputs
	 * @return the resulting output of the circuit on the given input
	 */
	public BitSet evalCircuit(List<List<Gate>> layersOfGates, BitSet inputs) {
		BitSet result = new BitSet();

		// Construct and fill up initial evaluation map with the inputs
		HashMap<Integer, Boolean> evals = new HashMap<Integer, Boolean>();
		for(int i = 0; i < inputSize; i++){
			evals.put(i, inputs.get(i));
		}

		int maxGateNumber = 0;
		for(List<Gate> layer: layersOfGates){
			for(Gate g: layer){
				maxGateNumber = Math.max(maxGateNumber, g.getOutputWireIndex());
				String gate = g.getGate();
				char[] gateArray = gate.toCharArray();

				boolean leftInput = evals.get(g.getLeftWireIndex());
				boolean rightInput = evals.get(g.getRightWireIndex());

				if (leftInput == false &&
						rightInput == false){
					if(gateArray.length < 4){
						evals.put(g.getOutputWireIndex(), false);
						continue;
					}
					if (gateArray[gateArray.length - 4] == '1'){
						evals.put(g.getOutputWireIndex(), true);
					}
					else evals.put(g.getOutputWireIndex(), false);
				}
				if(leftInput == false
						&& rightInput == true){
					if(gateArray.length < 3){
						evals.put(g.getOutputWireIndex(), false);
						continue;
					}
					if (gateArray[gateArray.length - 3] == '1'){
						evals.put(g.getOutputWireIndex(), true);
					}
					else evals.put(g.getOutputWireIndex(), false);

				}
				if(leftInput == true &&
						rightInput == false){
					if(gateArray.length < 2){
						evals.put(g.getOutputWireIndex(), false);
						continue;
					}
					if (gateArray[gateArray.length - 2] == '1'){
						evals.put(g.getOutputWireIndex(), true);
					}
					else evals.put(g.getOutputWireIndex(), false);
				}
				if(leftInput == true &&
						rightInput == true){
					if(gateArray.length < 1){
						evals.put(g.getOutputWireIndex(), false);
						continue;
					}
					if (gateArray[gateArray.length - 1] == '1'){
						evals.put(g.getOutputWireIndex(), true);
					}
					else evals.put(g.getOutputWireIndex(), false);
				}
			}
		}
		int outputCounter = outputSize;
		for(int i = maxGateNumber; outputCounter > 0; i--){
			boolean res;
			if (evals.containsKey(i)){
				 res = evals.get(i);
				 outputCounter--;
				 result.set(outputCounter, res);
			}
			else {
				continue;
			}
		}

		return result;
	}

	/**
	 * Method for outputting the computed result to a file
	 * @param result
	 */
	public void writeCircuitOutput(BitSet result) {
		//Convert to big endian for correct output format
		byte[] out = toByteArray(littleEndianToBigEndian(result));
		try {
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			outputStream.write(out);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to convert a BitSet to a byte[]
	 * @param bits
	 * @return the corresponding byte[]
	 */
	public byte[] toByteArray(BitSet bits) {
		byte[] bytes = new byte[bits.size()/8];
		for (int i=0; i<bits.size(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length-i/8-1] |= 1<<(i%8);
			}
		}
		return bytes;
	}

	/**
	 * Method for converting a BitSet from little endian format to big endian
	 * @param bitset
	 * @return
	 */
	public BitSet littleEndianToBigEndian(BitSet bitset){
		BitSet result = new BitSet(bitset.size());
		for(int i = 0; i < bitset.size(); i++){
			result.set((result.size() - 1) - i, bitset.get(i));
		}
		return result;
	}

	/**
	 * Method for verifying the test vectors of AES from
	 * http://www.inconteam.com/software-development/41-encryption/55-aes-test-vectors#aes-ecb-128
	 */
	public void verifyOutput() {
		File expectedResultFile = null;
		if(inputFile.getName().equals("input0.bin")){
			expectedResultFile = new File("data/expected0.bin");
		}
		else if(inputFile.getName().equals("input1.bin")){
			expectedResultFile = new File("data/expected1.bin");
		}
		else if(inputFile.getName().equals("input2.bin")){
			expectedResultFile = new File("data/expected2.bin");
		}
		else if(inputFile.getName().equals("input3.bin")){
			expectedResultFile = new File("data/expected3.bin");
		}

		try {
			if(FileUtils.contentEquals(expectedResultFile, outputFile)){
				System.out.println("Circuit evaluated correctly");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method for visual representation of the given bitset
	 * @param bitset
	 * @return a string corresponding to the given bitset
	 */
	public String bitsetToBitString(BitSet bitset) {
		String res = "";
		for(int i = 0; i < bitset.size(); i++){
			if (i != 0 && i % 8 == 0){
				res += " ";
			}
			if(bitset.get(i)){
				res += '1';
			}
			else res += '0';
		}
		return res;
	}
}
