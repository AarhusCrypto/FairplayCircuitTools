package output;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import parsers.FairplayParser;

import common.Gate;
import converters.FairplayToCUDAConverter;

public class FairplayToSPACL implements Runnable {

	private FairplayToCUDAConverter circuitConverter;
	private File outputFile;
	private String circuitName;

	public FairplayToSPACL(FairplayToCUDAConverter circuitConverter, 
			File outputFile, String circuitName) {
		this.circuitConverter = circuitConverter;
		this.outputFile = outputFile;
		this.circuitName = circuitName;
	}


	public void run() {
		List<List<Gate>> gates = circuitConverter.getGates();
		FairplayParser circuitParser = circuitConverter.getParser();

		int sizeOfKey = circuitParser.getNumberOfP1Inputs();
		int sizeOfPlaintext = circuitParser.getNumberOfP2Inputs();
		int sizeOfCiphertext = circuitParser.getNumberOfOutputs();

		int heapSize = getNumberOfGates(gates);
		int[] widthSize = getWidthSizes(gates);

		outputCircuit(sizeOfKey, sizeOfPlaintext, sizeOfCiphertext,
				heapSize, widthSize);
	}


	public String[] getHeaders() {
		return circuitConverter.getHeaders();
	}

	private int getNumberOfGates(List<List<Gate>> gates) {
		int size = 0;
		for (List<Gate> list: gates) {
			size += list.size();
		}
		return size;
	}

	private int[] getWidthSizes(List<List<Gate>> gates) {
		int[] res = new int[3]; //XOR = 0, AND = 1, NAND = 2
		for (List<Gate> list: gates) {
			Gate tester = list.get(0);
			if (tester.isXOR()) {
				res[0] = Math.max(res[0], list.size());
			} else if (tester.isAND()) {
				res[1] = Math.max(res[1], list.size());
			} else res[2] = Math.max(res[2], list.size());
		}
		return res;
	}

	private void outputCircuit(int sizeOfKey, int sizeOfPlaintext, 
			int sizeOfCiphertext, int heapSize, int[] widthSize) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));

			fbw.write("spacl " + circuitName + "(");
			fbw.newLine();
			fbw.write("private_common_in key[" + sizeOfKey + "],");
			fbw.newLine();
			fbw.write("public_common_in plaintext[" + sizeOfPlaintext + "],");
			fbw.newLine();
			fbw.write("public_common_out ciphertext[" + sizeOfCiphertext + "]) {");
			
			fbw.newLine();
			fbw.newLine();
			
			fbw.write("size_of_heap(" + heapSize + ");");
			
			fbw.newLine();
			fbw.newLine();
			
			//continue

		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			try {
				fbw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String common_load(String access, String type, int i1, int i2, int i3) {
		return access + "_common_load(" + type + "[" + i1 + "]," + i2 + "," + i3 + ");";
	}
}
