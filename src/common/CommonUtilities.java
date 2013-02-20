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
	public static final String PRIVATE_IN = "private_common_in";
	public static final String PUBLIC_IN = "public_common_in";
	public static final String PUBLIC_OUT = "public_common_store";
	public static final String INPUT_1 = "key";
	public static final String INPUT_2 = "plaintext";
	public static final String OUTPUT = "ciphertext";
	public static final String HEAP_SIZE = "size_of_heap";
	public static final String XOR = "XOR";
	public static final String INV = "INV";
	public static final String AND = "AND";

	public static final String PRIVATE_LOAD = "private_common_load";
	public static final String PUBLIC_LOAD = "public_common_load";
	public static final String PUBLIC_STORE = "public_common_store";

	public static final String MAX_WIDTH = "max_width_";

	public static final String BEGIN_LAYER = "begin_layer_";
	public static final String END_LAYER = "end_layer_";



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
			String outputFileName) {
		List<List<Gate>> gates = circuitConverter.getGates();
		int[] circuitInfo = circuitConverter.getCircuitInfo();

		outputMeta(outputFileName, circuitInfo);
		outputActualSPACLCircuit(gates, outputFileName, circuitInfo);
	}

	private static void outputMeta(String circuitName, int[] circuitInfo) {
		int sizeOfKey = circuitInfo[0];
		int sizeOfPlaintext = circuitInfo[1];
		int sizeOfCiphertext = circuitInfo[2];
		String circuitMetaName = circuitName + ".spaclv";

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new File(circuitMetaName)), Charset.defaultCharset()));
			bw.write(PRIVATE_IN + " " + INPUT_1 + "[" + sizeOfKey + "],");
			bw.newLine();
			bw.write(PUBLIC_IN + " " + INPUT_2 + "[" + sizeOfPlaintext + "],");
			bw.newLine();
			bw.write(PUBLIC_STORE + " " + OUTPUT + "[" + sizeOfCiphertext + "]){");

			bw.close();
		} catch (IOException e) {

		}		
	}

	private static void outputActualSPACLCircuit(List<List<Gate>> gates, 
			String circuitName, int[] circuitInfo) {
		int sizeOfKey = circuitInfo[0];
		int sizeOfPlaintext = circuitInfo[1];
		int sizeOfCiphertext = circuitInfo[2];
		int heapSize = circuitInfo[3];
		int maxXOR = circuitInfo[4];
		int maxINV = circuitInfo[5];
		int maxAND = circuitInfo[6];
		String circuitMetaName = circuitName + ".spaclc";


		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new File(circuitMetaName)), Charset.defaultCharset()));

			// Size of heap
			bw.write("  " + HEAP_SIZE + "(" + heapSize + ");");
			bw.newLine();
			bw.newLine();

			// Max_width specifications
			bw.write(max_width(XOR, maxXOR));
			bw.newLine();
			bw.write(max_width(INV, maxINV));
			bw.newLine();
			bw.write(max_width(AND, maxAND));
			bw.newLine();
			bw.write(max_width(PRIVATE_LOAD, sizeOfKey));
			bw.newLine();
			bw.write(max_width(PUBLIC_LOAD, sizeOfPlaintext));
			bw.newLine();
			bw.write(max_width(PUBLIC_STORE, sizeOfCiphertext));
			bw.newLine();
			bw.newLine();

			// Init key
			bw.write(begin_layer(PRIVATE_LOAD, sizeOfKey));
			bw.newLine();
			for (int i = 0; i < sizeOfKey; i++) { //Check which is key AND which is plaintext
				bw.write("    " + PRIVATE_LOAD + "(" + INPUT_1 +"[" + i + "]," + i + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer(PRIVATE_LOAD, sizeOfKey));
			bw.newLine();
			bw.newLine();

			// Init plaintext
			bw.write(begin_layer(PUBLIC_LOAD, sizeOfPlaintext));
			bw.newLine();
			for (int i = 0; i < sizeOfPlaintext; i++) { //TODO Check which is key AND which is plaintext
				bw.write("    " + PUBLIC_LOAD + "(" + INPUT_2 +"[" + i + "]," + (sizeOfKey + i) + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer(PUBLIC_LOAD, sizeOfPlaintext));
			bw.newLine();
			bw.newLine();

			// The layers
			int index = 0;
			for (List<Gate> list: gates) {
				Gate tester = list.get(0);
				int j = 0;
				String layerString = "";
				if (tester.isXOR()) {
					layerString = XOR;

				} else if (tester.isAND()) {
					layerString = AND;
				} else {
					layerString = INV;
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
			bw.write(begin_layer(PUBLIC_STORE, sizeOfCiphertext));
			bw.newLine();
			for (int i = 0; i < sizeOfCiphertext; i++) {
				bw.write("    " + PUBLIC_STORE + "(" + OUTPUT +"[" + (heapSize - sizeOfCiphertext + i)  + "]," + i + "," + i + ");");
				bw.newLine();
			}
			bw.write(end_layer(PUBLIC_STORE, sizeOfCiphertext));
			//			bw.newLine();
			//
			//			bw.write("}");

			bw.close();
		} catch (IOException e) {

		}	
	}

	private static String max_width(String suffix, int index) {
		return "  " + MAX_WIDTH + suffix + "(" + index + ");";
	}

	private static String begin_layer(String suffix, int index) {
		return "  " + BEGIN_LAYER + suffix + "(" + index + ");";
	}

	private static String end_layer(String suffix, int index) {
		return "  " + END_LAYER + suffix + "(" + index + ");";
	}

	private static String getGateString(Gate g, String gateType, int index, int gateNumber) {
		return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
				g.getLeftWireIndex() + "," + g.getRightWireIndex()
				+ "," + index + "," + gateNumber + ");";
	}

	private static String getGateString(Gate g, String gateType, int index) {
		if (gateType.equals(XOR)) {
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
