package impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;

import common.BitString;
import common.Gate;

/**
 * @author Roberto Trifiletti
 *
 */
public class CircuitEvaluator implements Runnable {

	private static final int BYTESIZE = 8;
	private File inputFile;
	private File outputFile;
	private List<List<Gate>> layersOfGates;
	String mode;

	private int inputSize;
	private int outputSize;
	private int numberOfWires;

	/**
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @param parseStrategy
	 */
	public CircuitEvaluator(File inputFile, File outputFile,
			List<List<Gate>> layersOfGates, String header, String mode){
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.layersOfGates = layersOfGates;
		this.mode = mode;

		String[] split = header.split(" ");

		inputSize = Integer.parseInt(split[0]);
		outputSize = Integer.parseInt(split[1]);
		numberOfWires = Integer.parseInt(split[2]);
	}

	@Override
	public void run() {
		if(inputFile.length() < inputSize/BYTESIZE){
			System.out.println("Input too short, check inputfile");
			return;
		}

		byte[] bytesRead = getBytesFromFile();

		BitString input = byteArrayToBitSet(bytesRead);
		if (mode.equals(Driver.FAIRPLAY_EVALUATOR_IA32)) {
			input = input.getIA32BitString();
		} else if (mode.equals(Driver.FAIRPLAY_EVALUATOR_MIRRORED)) {
			input = input.getMirroredBitString();
		} else if (mode.equals(Driver.FAIRPLAY_EVALUATOR_REVERSED)) {
			input = input.getReverseOrder();
		}

		// The result returned is in big endian, the evaluator flips the
		// ouput before returning
		BitString output = evalCircuit(layersOfGates, input);

		writeCircuitOutput(output);

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
	 * Method for converting a byte[] to a FairplayBitSet
	 * @param bytes
	 * @return FairplayBitSet corresponding to the byte[], in little endian form
	 */
	public BitString byteArrayToBitSet(byte[] bytes) {
		BitString bits = new BitString(bytes.length * 8);
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i/8-1]&(1<<(i%8))) > 0) {
				bits.set((bytes.length * 8 - 1) - i);
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
	public BitString evalCircuit(List<List<Gate>> layersOfGates,
			BitString inputs) {

		// Construct and fill up initial evaluation map with the inputs
		HashMap<Integer, Boolean> evals = new HashMap<Integer, Boolean>();
		for (int i = 0; i < inputSize; i++) {
			evals.put(i, inputs.get(i));
		}

		for (List<Gate> layer: layersOfGates) {
			for (Gate g: layer) {
				String boolTable = g.getBoolTable();
				for (int i = boolTable.length(); i < 4; i++) {
					boolTable = "0" + boolTable;
				}
				char[] boolTableArray = boolTable.toCharArray();
				
				assert(evals.get(g.getLeftWireIndex()) != null &&
						evals.get(g.getLeftWireIndex()) != null);
				
				boolean leftInput = evals.get(g.getLeftWireIndex());
				boolean rightInput = evals.get(g.getRightWireIndex());
				

				if (leftInput == false && rightInput == false && 
						boolTableArray[0] == '1') {
					evals.put(g.getOutputWireIndex(), true);
				} else if (leftInput == false && rightInput == true && 
						boolTableArray[1] == '1') {
					evals.put(g.getOutputWireIndex(), true);
				} else if (leftInput == true && rightInput == false && 
						boolTableArray[2] == '1') {
					evals.put(g.getOutputWireIndex(), true);
				} else if (leftInput == true &&	rightInput == true && 
						boolTableArray[3] == '1') {
					evals.put(g.getOutputWireIndex(), true);
				} else evals.put(g.getOutputWireIndex(), false);
			}
		}
		// Read output in little endian, but stores it in big endian
		int currentBit = outputSize - 1;
		BitString result = new BitString(outputSize);
		for(int i = numberOfWires - outputSize; i < numberOfWires; i++){
			boolean res;
			res = evals.get(i);
			if(res == true){
				result.set(currentBit);
			}
			currentBit--;
		}

		return result;
	}

	/**
	 * Method for outputting the computed result to a file
	 * @param result
	 */
	public void writeCircuitOutput(BitString result) {
		byte[] out = toByteArray(result);

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
	public byte[] toByteArray(BitString bits) {
		byte[] bytes = new byte[(bits.length() + 7) / 8];
		for (int i = 0; i < bits.length(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length-i/8-1] |= 1<<(i%8);
			}
		}
		return bytes;
	}
}
