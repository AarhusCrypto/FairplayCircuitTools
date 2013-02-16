package output;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import parsers.FairplayParser;

import common.CurrentGate;
import common.Gate;
import common.LayerComparator;
import common.TopoTypeComparator;

import converters.FairplayToCUDAConverter;

public class FairplayToSPACL implements Runnable {

	private FairplayToCUDAConverter circuitConverter;
	private File outputFile;
	private String circuitName;
	private BufferedWriter bw;

	public FairplayToSPACL(FairplayToCUDAConverter circuitConverter, 
			File outputFile, String circuitName) {
		this.circuitConverter = circuitConverter;
		this.outputFile = outputFile;
		this.circuitName = circuitName;
	}

	public void run() {
		List<List<Gate>> list = circuitConverter.getGates();
		FairplayParser circuitParser = circuitConverter.getParser();
		int numberOfInputs = circuitParser.getNumberOfInputs();
		
		List<Gate> sortedGates = getLayeredGates(list, 
				numberOfInputs);
		List<List<Gate>> layeredGates = getSortedGates(sortedGates);


		int sizeOfKey = circuitParser.getNumberOfP1Inputs();
		int sizeOfPlaintext = circuitParser.getNumberOfP2Inputs();
		int sizeOfCiphertext = numberOfInputs;

		int heapSize = getNumberOfGates(layeredGates);
		int[] widthSize = getWidthSizes(layeredGates);

		outputCircuit(sizeOfKey, sizeOfPlaintext, sizeOfCiphertext,
				heapSize, widthSize, layeredGates);
	}

	private List<Gate> getLayeredGates(List<List<Gate>> gates, int inputSize) {
		List<Gate> res = new ArrayList<Gate>();

		// Init wireLayer map with input wires
		HashMap<Integer, Integer> wireLayers = new HashMap<Integer, Integer>();
		for (int i = 0; i < inputSize; i++) {
			wireLayers.put(i, 0);
		}
		
		for (List<Gate> list: gates) {
			for (Gate g: list) {
				int a = wireLayers.get(g.getLeftWireIndex());
				int b = wireLayers.get(g.getRightWireIndex());
				int layer;
				if (g.isAND()) {
					layer = Math.max(a, b) + 1;
					g.setTopologicalLayer(layer - 1);
				} else {
					layer = Math.max(a, b);
					g.setTopologicalLayer(layer);
				}
				wireLayers.put(g.getOutputWireIndex(), layer);
				
				res.add(g);
			}
		}
		Collections.sort(res, new TopoTypeComparator());
		return res;
	}

//		int limit = Math.max(nonAndMap.size(), andMap.size());
//		res.add(new ArrayList<Gate>());
//		for (int i = 0; i < limit; i++) {
//			Collection<Gate> xorList = nonAndMap.getCollection(i);
////			Collection<Gate> invList = invMap.getCollection(i);
//			Collection<Gate> andList = andMap.getCollection(i);
//			
//			if (xorList != null) {
//				int startIndex = res.size() - 1;
//				boolean addingXor = true;
//				int j = 0;
//				for (Gate g: xorList) {
//					if (i == 0)
////						System.out.println(g.toFairPlayString());
//					if (g.isXOR()) {
//						if (addingXor) {
//							res.get(startIndex + j).add(g);
//						} else {
//							addingXor = !addingXor;
//							List<Gate> tmp = new ArrayList<Gate>();
//							tmp.add(g);
//							res.add(tmp);
//							j++;
//						}
//					} else {
//						if (!addingXor) {
//							res.get(startIndex + j).add(g);
//						} else {
//							addingXor = !addingXor;
//							List<Gate> tmp = new ArrayList<Gate>();
//							tmp.add(g);
//							res.add(tmp);
//							j++;
//						}
//					}
//				}
//			}
//			if (andList != null) {
//				List<Gate> ands = new ArrayList<Gate>();
//				ands.addAll(andList);
//				res.add(ands);
//			}
//		}

	private List<List<Gate>> getSortedGates(List<Gate> gates) {
		List<List<Gate>> res = new ArrayList<List<Gate>>();
		CurrentGate current;
		int index = 0;
		Gate trial = gates.get(0);
		
		if (trial.isXOR()) {
			current = CurrentGate.XOR;
		} else if (trial.isAND()) {
			current = CurrentGate.AND;
		} else current = CurrentGate.INV;
		
		res.add(new ArrayList<Gate>());
		for (Gate g: gates) {
			if (equal(g, current)) {
				res.get(index).add(g);
			} else {
				res.add(new ArrayList<Gate>());
				index++;
				res.get(index).add(g);
				current = getNewCurrent(g);
			}
		}
		
		for (List<Gate> list: res) {
			Collections.sort(list, new LayerComparator());
		}
		return res;
	}

	private boolean equal(Gate g, CurrentGate current) {
		if (g.isXOR() && current == CurrentGate.XOR) {
			return true;
		} else if (g.isAND() && current == CurrentGate.AND) {
			return true;
		} else if (g.isINV() && current == CurrentGate.INV) {
			return true;
		} else return false;
	}
	
	private CurrentGate getNewCurrent(Gate g) {
		if (g.isXOR()) {
			return CurrentGate.XOR;
		} else if (g.isAND()) {
			return CurrentGate.AND;
		} else return CurrentGate.INV;
	}


	private int getNumberOfGates(List<List<Gate>> gates) {
		int size = 0;
		for (List<Gate> list: gates) {
			size += list.size();
		}
		return size;
	}

	private int[] getWidthSizes(List<List<Gate>> gates) {
		int[] res = new int[3]; //XOR = 0, AND = 1, INV = 2
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
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), Charset.defaultCharset()));

			// Function header
			write("spacl " + circuitName + "(");
			newLine();
			write("  private_common_in key[" + sizeOfKey + "],");
			newLine();
			write("  public_common_in plaintext[" + sizeOfPlaintext + "],");
			newLine();
			write("  public_common_out ciphertext[" + sizeOfCiphertext + "]) {");
			newLine();
			newLine();

			// Size of heap
			write("  size_of_heap(" + heapSize + ");");
			newLine();
			newLine();

			// Max_width specifications
			write(max_width("xor", widthSize[0]));
			newLine();
			write(max_width("and", widthSize[1]));
			newLine();
			write(max_width("inv", widthSize[2]));
			newLine();
			write(max_width("private_common_load", sizeOfKey));
			newLine();
			write(max_width("public_common_load", sizeOfPlaintext));
			newLine();
			write(max_width("public_common_out", sizeOfCiphertext));
			newLine();
			newLine();

			// Init key
			write(begin_layer("private_common_load", sizeOfKey));
			newLine();
			for (int i = 0; i < sizeOfKey; i++) { //Check which is key and which is plaintext
				write("    private_common_load(key[" + i + "]," + i + "," + i + ");");
				newLine();
			}
			write(end_layer("private_common_load", sizeOfKey));
			newLine();
			newLine();

			// Init plaintext
			write(begin_layer("public_common_load", sizeOfPlaintext));
			newLine();
			for (int i = 0; i < sizeOfPlaintext; i++) { //TODO Check which is key and which is plaintext
				write("  public_common_load(plaintext[" + i + "]," + (sizeOfKey + i) + "," + i + ");");
				newLine();
			}
			write(end_layer("public_common_load", sizeOfPlaintext));
			newLine();
			newLine();

			// The layers
			int i = 0;
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
				write(begin_layer(layerString, list.size()));
				newLine();
				for (Gate g: list) {
					if (g.isAND()) {
						write(getGateString(g, layerString, j++, i++));
					} else {
						write(getGateString(g, layerString, j++));
					}
					newLine();
				}
				write(end_layer(layerString, list.size()));
				newLine();
				newLine();
			}
			write("}");
		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void write(String s) throws IOException {
		bw.write(s);
	}

	private void newLine() throws IOException {
		bw.newLine();
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

	private String getGateString(Gate g, String gateType, int index, int gateNumber) {
		return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
				g.getLeftWireIndex() + "," + g.getRightWireIndex()
				+ "," + index + "," + gateNumber + ");";
	}

	private String getGateString(Gate g, String gateType, int index) {
		return "    " + gateType + "(" + g.getOutputWireIndex() + "," + 
				g.getLeftWireIndex() + "," + g.getRightWireIndex()
				+ "," + index + ");";
	}
}
