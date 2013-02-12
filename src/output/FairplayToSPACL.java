package output;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;

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
				heapSize, widthSize, gates);
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
			int sizeOfCiphertext, int heapSize, int[] widthSize,
			List<List<Gate>> gates) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));

			// Function header
			fbw.write("spacl " + circuitName + "(");
			fbw.newLine();
			fbw.write("  private_common_in key[" + sizeOfKey + "],");
			fbw.newLine();
			fbw.write("  public_common_in plaintext[" + sizeOfPlaintext + "],");
			fbw.newLine();
			fbw.write("  public_common_out ciphertext[" + sizeOfCiphertext + "]) {");
			fbw.newLine();
			fbw.newLine();
			
			// Size of heap
			fbw.write("  size_of_heap(" + heapSize + ");");
			fbw.newLine();
			fbw.newLine();
			
			// Max_width specifications
			fbw.write(max_width("xor", widthSize[0]));
			fbw.newLine();
			fbw.write(max_width("and", widthSize[1]));
			fbw.newLine();
			fbw.write(max_width("nand", widthSize[2]));
			fbw.newLine();
			fbw.write(max_width("private_common_load", sizeOfKey));
			fbw.newLine();
			fbw.write(max_width("public_common_load", sizeOfPlaintext));
			fbw.newLine();
			fbw.write(max_width("public_common_out", sizeOfCiphertext));
			fbw.newLine();
			fbw.newLine();
			
			// Init key
			fbw.write(begin_layer("private_common_load", sizeOfKey));
			fbw.newLine();
			for (int i = 0; i < sizeOfKey; i++) { //Check which is key and which is plaintext
				fbw.write("    private_common_load(key[" + i + "]," + i + "," + i + ");");
				fbw.newLine();
			}
			fbw.write(end_layer("  private_common_load", sizeOfKey));
			fbw.newLine();
			fbw.newLine();
			
			// Init plaintext
			fbw.write(begin_layer("public_common_load", sizeOfPlaintext));
			fbw.newLine();
			for (int i = 0; i < sizeOfPlaintext; i++) { //TODO Check which is key and which is plaintext
				fbw.write("  public_common_load(plaintext[" + (sizeOfKey + i) + "]," + i + "," + i + ");");
				fbw.newLine();
			}
			fbw.write(end_layer("public_common_load", sizeOfPlaintext));
			fbw.newLine();
			fbw.newLine();
			
			// The layers
			for (List<Gate> list: gates) {
				fbw.write(gates.indexOf(list) + ""); //TODO Debugging
				fbw.newLine(); //TODO Debugging
				Gate tester = list.get(0);
				int j = 0;
				if (tester.isXOR()) {
					fbw.write(begin_layer("xor", list.size()));
					fbw.newLine();
					for (Gate g: list) {
						fbw.write(gateString(g, "xor", j++));
						fbw.newLine();
					}
					fbw.write(end_layer("xor", list.size()));
					fbw.newLine();
				} else if (tester.isAND()) {
					fbw.write(begin_layer("and", list.size()));
					fbw.newLine();
					for (Gate g: list) {
						fbw.write(gateString(g, "and", j++));
						fbw.newLine();
					}
					fbw.write(end_layer("and", list.size()));
					fbw.newLine();
				} else {
					fbw.write(begin_layer("nand", list.size()));
					fbw.newLine();
					for (Gate g: list) {
						fbw.write(gateString(g, "nand", j++));
						fbw.newLine();
					}
					fbw.write(end_layer("nand", list.size()));
					fbw.newLine();
				}
				fbw.newLine();
			}
			
			fbw.write("}");
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
	
	private String max_width(String suffix, int index) {
		return "  max_width_" + suffix + "(" + index + ");";
	}
	
	private String begin_layer(String suffix, int index) {
		return "  begin_layer_" + suffix + "(" + index + ");";
	}
	
	private String end_layer(String suffix, int index) {
		return "  end_layer_" + suffix + "(" + index + ");";
	}
	
	private String gateString(Gate g, String gateType, int index) {
		return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
				g.getLeftWireIndex() + "," + g.getRightWireIndex()
				+ "," + index + ");";
	}
}
