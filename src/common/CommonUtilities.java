package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

import converters.FairplayToSPACLConverter;


public class CommonUtilities {
	public static void outputFairplayCircuit(CircuitProvider<Gate> circuitParser,
			File outputFile) {
		List<Gate> circuit = circuitParser.getGates();
		String[] headers = circuitParser.getHeaders();
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));
			fbw.write(headers[0]);
			fbw.newLine();
			fbw.write(headers[1]);
			fbw.newLine();
			fbw.newLine();

			for(Gate g: circuit) {
				fbw.write(g.toFairPlayString());
				fbw.newLine();
			}
			fbw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void outputCUDACircuit(CircuitProvider<List<Gate>> circuitParser, 
			File outputFile) {
		List<List<Gate>> layersOfGates = circuitParser.getGates();
		String header = circuitParser.getHeaders()[0];
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));

			fbw.write(header);
			fbw.newLine();

			/*
			 * Write the gates the the file, one layer at a time
			 */
			for (List<Gate> l: layersOfGates) {
				// Write the size of the current layer
				fbw.write("*" + l.size()); 
				fbw.newLine();

				// Write the gates in this layer
				for (Gate g: l) {
					String gateString = layersOfGates.indexOf(l) + " " + 
							g.toCUDAString();
					fbw.write(gateString);
					fbw.newLine();
				}
			}
			fbw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void outputSPACLCircuit(FairplayToSPACLConverter circuitConverter,
			File outputFile, String circuitName) {
		List<List<Gate>> gates = circuitConverter.getGates();
		int[] headers = circuitConverter.getCircuitInfo();
		
		int sizeOfKey = headers[0];
		int sizeOfPlaintext = headers[1];
		int sizeOfCiphertext = headers[2];
		int heapSize = headers[3];
		int maxXOR = headers[4];
		int maxINV = headers[5];
		int maxAND = headers[6];

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));

			// Function header
			bw.write("spacl " + circuitName + "(");
			bw.newLine();
			bw.write("  private_common_in key[" + sizeOfKey + "],");
			bw.newLine();
			bw.write("  public_common_in plaintext[" + sizeOfPlaintext + "],");
			bw.newLine();
			bw.write("  public_common_out ciphertext[" + sizeOfCiphertext + "]) {");
			bw.newLine();
			bw.newLine();

			// Size of heap
			bw.write("  size_of_heap(" + heapSize + ");");
			bw.newLine();
			bw.newLine();

			// Max_width specifications
			bw.write(max_width("xor", maxXOR));
			bw.newLine();
			bw.write(max_width("and", maxINV));
			bw.newLine();
			bw.write(max_width("inv", maxAND));
			bw.newLine();
			bw.write(max_width("private_common_load", sizeOfKey));
			bw.newLine();
			bw.write(max_width("public_common_load", sizeOfPlaintext));
			bw.newLine();
			bw.write(max_width("public_common_out", sizeOfCiphertext));
			bw.newLine();
			bw.newLine();

			// Init key
			bw.write(begin_layer("private_common_load", sizeOfKey));
			bw.newLine();
			for (int i = 0; i < sizeOfKey; i++) { //Check which is key and which is plaintext
				bw.write("    private_common_load(key[" + i + "]," + i + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer("private_common_load", sizeOfKey));
			bw.newLine();
			bw.newLine();

			// Init plaintext
			bw.write(begin_layer("public_common_load", sizeOfPlaintext));
			bw.newLine();
			for (int i = 0; i < sizeOfPlaintext; i++) { //TODO Check which is key and which is plaintext
				bw.write("    public_common_load(plaintext[" + i + "]," + (sizeOfKey + i) + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer("public_common_load", sizeOfPlaintext));
			bw.newLine();
			bw.newLine();

			// The layers
			int index = 0;
			for (List<Gate> list: gates) {
				Gate tester = list.get(0);
				int j = 0;
				String layerString = "";
				if (tester.isXOR()) {
					layerString = "xor";

				} else if (tester.isAND()) {
					layerString = "and";
				} else {
					layerString = "inv";
				}
				bw.write(begin_layer(layerString, list.size()));
				bw.newLine();
				for (Gate g: list) {
					if (g.isAND()) {
						bw.write(getGateString(g, layerString, j++, index++));
					} else {
						bw.write(getGateString(g, layerString, j++));
					}
					bw.newLine();
				}
				bw.write(end_layer(layerString, list.size()));
				bw.newLine();
				bw.newLine();
			}

			// Write output
			bw.write(begin_layer("public_common_out", sizeOfCiphertext));
			bw.newLine();
			for (int i = 0; i < sizeOfCiphertext; i++) {
				bw.write("    public_common_out(ciphertext[" + (heapSize - sizeOfCiphertext + i)  + "]," + i + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer("public_common_out", sizeOfCiphertext));
			bw.newLine();

			bw.write("}");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String max_width(String suffix, int index) {
		return "  max_width_" + suffix + "(" + index + ");";
	}

	private static String begin_layer(String suffix, int index) {
		return "  begin_layer_" + suffix + "(" + index + ");";
	}

	private static String end_layer(String suffix, int index) {
		return "  end_layer_" + suffix + "(" + index + ");";
	}

	private static String getGateString(Gate g, String gateType, int index, int gateNumber) {
		return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
				g.getLeftWireIndex() + "," + g.getRightWireIndex()
				+ "," + index + "," + gateNumber + ");";
	}

	private static String getGateString(Gate g, String gateType, int index) {
		if (gateType.equals("xor")) {
			return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
					g.getLeftWireIndex() + "," + g.getRightWireIndex()
					+ "," + index + ");";
		} else return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
		g.getLeftWireIndex() + "," + index + ");";
	}

	public static int getWireCount(List<Gate> gates) {
		HashSet<Integer> hs = new HashSet<Integer>();
		for (Gate g: gates) {
			int leftIndex = g.getLeftWireIndex();
			int rightIndex = g.getRightWireIndex();
			int outputIndex = g.getOutputWireIndex();
			if (leftIndex != Integer.MIN_VALUE) {
				hs.add(g.getLeftWireIndex());
			}
			if (rightIndex != Integer.MIN_VALUE) {
				hs.add(g.getRightWireIndex());
			}
			if (outputIndex != Integer.MIN_VALUE) {
				hs.add(g.getOutputWireIndex());
			}
		}
		return hs.size();
	}

	public static int getWireCountList(List<List<Gate>> gates) {
		HashSet<Integer> hs = new HashSet<Integer>();
		for (List<Gate> list: gates) {
			for (Gate g: list) {
				int leftIndex = g.getLeftWireIndex();
				int rightIndex = g.getRightWireIndex();
				int outputIndex = g.getOutputWireIndex();
				if (leftIndex != Integer.MIN_VALUE) {
					hs.add(g.getLeftWireIndex());
				}
				if (rightIndex != Integer.MIN_VALUE) {
					hs.add(g.getRightWireIndex());
				}
				if (outputIndex != Integer.MIN_VALUE) {
					hs.add(g.getOutputWireIndex());
				}
			}
		}
		return hs.size();
	}

	public static void showBlankWires(List<Gate> gates, int size) {
		boolean[] wires = new boolean[size];
		for (Gate g: gates) {
			wires[g.getLeftWireIndex()] =  true;
			wires[g.getRightWireIndex()] =  true;
			wires[g.getOutputWireIndex()] =  true;
		}
		for (int i = 0; i < wires.length; i++){
			if (!wires[i]) {
				System.out.println(i);
			}
		}
	}
}
