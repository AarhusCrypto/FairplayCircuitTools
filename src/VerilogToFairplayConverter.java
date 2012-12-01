import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class VerilogToFairplayConverter implements Runnable {
	private File circuitFile;
	private File outputFile;
	private int numberOfInputs;
	private int numberOfOutputs;
	private String firstHeader;
	private String secondHeader;
	private HashMap<String, Integer> stringMap;
	private int wireCount;

	List<Gate> leftOutputGates;
	List<Gate> rightOutputGates;
	List<Gate> outputGates;

	int maxOutputWire;

	public VerilogToFairplayConverter(File circuitFile, File outputFile){
		this.circuitFile = circuitFile;
		this.outputFile = outputFile;
		stringMap = new HashMap<String, Integer>();

		leftOutputGates = new ArrayList<Gate>();
		rightOutputGates = new ArrayList<Gate>();
		outputGates = new ArrayList<Gate>();

		maxOutputWire = 0;

	}

	@Override
	public void run() {
		List<String> gateStrings = getAnalyzedCircuit();
		List<Gate> res = getGates(gateStrings);
		incrementGates(res);
		firstHeader = res.size() + " " + CommonUtilities.getWireCount(res);
		secondHeader = 0 + " " + numberOfInputs + " " + "0" + " " + numberOfOutputs;
		String[] headers = {firstHeader, secondHeader};
		CommonUtilities.outputFairplayCircuit(res, outputFile, headers);

	}

	private List<String> getAnalyzedCircuit() {
		List<String> res = new ArrayList<String>();

		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), Charset.defaultCharset()));
			String line = "";
			String lastLine = "";
			while ((line = fbr.readLine()) != null) {
				line = StringUtils.trim(line);

				if (line.isEmpty() || line.startsWith("//") || line.startsWith("module")
						|| line.startsWith("endmodule") || line.startsWith("wire")){
					continue;
				} else if (line.startsWith("input [")) {
					String[] split = line.split(" ");
					String inputInfo = split[1];
					String inputNumber = 
							inputInfo.substring(3, inputInfo.length() - 1);
					numberOfInputs = Integer.parseInt(inputNumber) + 1;
					continue;
				} else if (line.startsWith("output [")) {
					String[] split = line.split(" ");
					String outputInfo = split[1];
					String outputNumber = 
							outputInfo.substring(3, outputInfo.length() - 1);
					numberOfOutputs = Integer.parseInt(outputNumber) + 1;
					continue;
				} else {
					if(!line.endsWith(";")) {
						if(lastLine.equals("")) {
							lastLine = line;
						} else {
							lastLine = lastLine + " "  + line;
						}
						continue;
					} else {
						if(!lastLine.equals("")) {
							line = lastLine + " " + line;
							lastLine = "";
						} 
					}
					res.add(line);
				}
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	private void incrementGates(List<Gate> res) {
		int incNumber = maxOutputWire + 1;

		for (Gate g: leftOutputGates) {
			g.setLeftWireIndex(g.getLeftWireIndex() + incNumber);
		}
		for (Gate g: rightOutputGates) {
			g.setRightWireIndex(g.getRightWireIndex() + incNumber);
		}
		for (Gate g: outputGates) {
			g.setOutputWireIndex(g.getOutputWireIndex() + incNumber);
		}
	}

	public List<Gate> getGates(List<String> gates) {
		List<Gate> res = new ArrayList<Gate>();

		//Constructs the constant 1 at first wire after input
		String nAND1 = "2 1 0 0 " + numberOfInputs + " 1110";
		String nAND2 = "2 1 0 " + numberOfInputs + " " + (numberOfInputs + 1) + " 1110";
		Gate g1 = new Gate(nAND1);
		Gate g2 = new Gate(nAND2);
		res.add(g1);
		res.add(g2);
		//Parsing of gates begins here
		/*
		 * TODO: Just use the HashMap to map the wire strings to numbers,
		 * this way it will not matter if it is of the form n_0011, \next_state or
		 * \next_round - as long as they get the same number all is fine.
		 * Will need to still remember the output wires though, as
		 * these are to be incremented at the end. A two pass run through could
		 * change this.
		 */
		for (String line: gates) {

			String[] split = line.split(" ");

			String inputs;
			String outputs = "1";
			if (split[1].endsWith("I")) {
				inputs = "1";
			} else {
				inputs = "2";
			}

			String leftWire = "";
			String rightWire = "";
			String outputWire = "";
			String boolTable = "";
			if(split[0].equals("XOR2HS")) {
				boolTable = "0110";
			}
			if (split[0].equals("AN2")) {
				boolTable = "0001";
			}
			if (split[0].equals("INV1S")) {
				inputs = "2";
				boolTable = "0110";
				leftWire = getWire(split[2]);
				rightWire = Integer.toString(numberOfInputs + 1);

				outputWire = getWire(split[4]);

			} else {
				leftWire = getWire(split[2]);
				rightWire = getWire(split[4]);
				outputWire = getWire(split[6]);
			}

			boolean leftOutputFlag = false;
			boolean rightOutputFlag = false;
			boolean outputFlag = false;

			if (leftWire.startsWith("o")) {
				leftWire = leftWire.substring(1);
				leftOutputFlag = true;
			}
			if (rightWire.startsWith("o")) {
				rightWire = rightWire.substring(1);
				rightOutputFlag = true;
			}
			if (outputWire.startsWith("o")) {
				outputWire = outputWire.substring(1);
				outputFlag = true;
			}

			String gateString = inputs + " " + outputs + " " + leftWire +
					" " + rightWire + " " + outputWire + " " + boolTable;

			Gate g = new Gate(gateString);
			maxOutputWire = Math.max(maxOutputWire, g.getOutputWireIndex());

			res.add(g);
			if (leftOutputFlag) {
				leftOutputGates.add(g);
			}
			if (rightOutputFlag) {
				rightOutputGates.add(g);
			}
			if (outputFlag) {
				outputGates.add(g);
			}
		}

		System.out.println(maxOutputWire);
		System.out.println(wireCount);
		System.out.println(maxOutputWire);

		return res;
	}

	private String getWire(String s) {
		s = s.replace(",", "");
		s = s.replace(");", "");
		if (s.startsWith("(input_i")) {
			return s.substring(9, s.length() - 2);
		} else if (s.startsWith("(output_o")) {
			System.out.println(s);
			return "o" + s.substring(10, s.length() - 2);
		} else {
			System.out.println(s);
			return Integer.toString(getWireNumber(s) + numberOfInputs + 2);
		}
	}

	private int getWireNumber(String s) {
		if(stringMap.get(s) != null) {
			return stringMap.get(s);
		} else {
			stringMap.put(s, wireCount++);
			return wireCount - 1;
		}
	}
}
