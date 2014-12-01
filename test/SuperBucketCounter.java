import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.map.MultiValueMap;

import common.Gate;
import common.InputGateType;

import parsers.FairplayParser;
import converters.ListToLayersConverter;

/*
 * Circuit must only consist of AND, XOR and INV gates
 */
public class SuperBucketCounter {

	public static void main(String[] args) {
//		File circuitFile = new File("test/data/nigel/AES-non-expanded.txt");
		File circuitFile = new File("test/data/Fanin-test-circuit2.txt");
		FairplayParser circuitParser = 
				new FairplayParser(circuitFile, false);
		ListToLayersConverter circuitConverter = 
				new ListToLayersConverter(circuitParser);
		
		List<List<Gate>> layersOfGates = circuitConverter.getGates();

		MultiValueMap outMap = new MultiValueMap();
		MultiValueMap leftInMap = new MultiValueMap();
		MultiValueMap rightInMap = new MultiValueMap();
		Map<Integer, Integer> accumulatedInputs = new TreeMap<Integer, Integer>();

		// Initialize auxiliary maps
		for (List<Gate> l: layersOfGates) {
			for (Gate g: l) {
				leftInMap.put(g.getLeftWireIndex(), g);
				rightInMap.put(g.getRightWireIndex(), g);
				outMap.put(g.getOutputWireIndex(), g);
			}
		}
		
		// Fill in dummy XOR gates on input wires. Will make the below code not count them.
		for (int i = 0; i < circuitParser.getNumberOfInputs(); i++) {
			Gate g = new Gate("2 1 -1 -1 " + i + " 0110", InputGateType.FAIRPLAY);
			accumulatedInputs.put(g.getOutputWireIndex(), 0);
			outMap.put(g.getOutputWireIndex(), g);
		}
		
		// Start producing output map
		Map<Integer, Integer> outputs = new TreeMap<Integer, Integer>();
		for (List<Gate> l: layersOfGates) {
			for (Gate g: l) {
				if (!g.isAND()) {
					int oIndex = g.getOutputWireIndex();
					int lIndex = g.getLeftWireIndex();
					int rIndex = g.getRightWireIndex();
					
					Collection<Gate> leftList = outMap.getCollection(lIndex);
					Collection<Gate> rightList = outMap.getCollection(rIndex);
					Collection<Gate> outLeftList = leftInMap.getCollection(oIndex);
					Collection<Gate> outRightList = rightInMap.getCollection(oIndex);
					
					// Figure out if any children are AND gates  
					boolean leftContainsAND = false;
					boolean leftNull = false;
					if (outLeftList != null) {
						for (Gate gl: outLeftList) {
							if (gl.isAND()) {
								leftContainsAND = true;
							}
						}
					} else {
						leftNull = true;
					}
					
					boolean rightContainsAND = false;
					boolean rightNull = false;
					if (outRightList != null) {
						for (Gate gr: outRightList) {
							if (gr.isAND()) {
								rightContainsAND = true;
							}
						}
					} else {
						rightNull = true;
					}
					boolean outputContainsAND = leftContainsAND || rightContainsAND;
					
					// The left and right input gate to this XOR/INV gate
					Gate leftInputGate = (Gate) leftList.toArray()[0];
					Gate rightInputGate = null;
					if (!g.isINV()){
						rightInputGate = (Gate) rightList.toArray()[0];
					}
					// Count number of actual inputs into this XOR/INV gate
					int totalInputs = 0;
					if (g.isXOR()) {
						if (!leftInputGate.isAND() && !rightInputGate.isAND()) {
							totalInputs = accumulatedInputs.get(lIndex) + accumulatedInputs.get(rIndex);
						} else if (!leftInputGate.isAND() && rightInputGate.isAND()) {
							totalInputs = accumulatedInputs.get(lIndex) + 1;
						} else if (leftInputGate.isAND() && !rightInputGate.isAND()) {
							totalInputs = 1 + accumulatedInputs.get(rIndex);
						} else if (leftInputGate.isAND() && rightInputGate.isAND()) {
							totalInputs = 2;
						}
					} else { // g is INV gate and thus does not have a right input
						if (leftInputGate.isAND()) {
							totalInputs = 1;
						} else {
							totalInputs = accumulatedInputs.get(lIndex);
						}
					}
					
					// If one of the children is an AND gate or this is an output gate,
					// then count this gate to have fan-in $totalinputs, and let the output
					// be set to 1 (for eventual XOR children).
					
					// Else if there are only XOR children, simply accumulate the inputs
					// and do not count any input for this gate
					if (outputContainsAND || (leftNull && rightNull)) {
						outputs.put(oIndex, totalInputs);
						accumulatedInputs.put(oIndex, 1);
					} else {
						accumulatedInputs.put(oIndex, totalInputs);
					}
				}
			}
		}
		Map<Integer, Integer> count = new TreeMap<Integer, Integer>();
		for (Integer i: outputs.values()) {
			if (!count.containsKey(i)) {
				count.put(i, 1);
			} else {
				int current = count.get(i) + 1;
				count.put(i, current);
			}
		}
		System.out.println(circuitFile.getName());
		int sum = 0;
		for (Integer i: count.keySet()) {
			int n = count.get(i);
			if (i > 2) {
				sum += n;
				System.out.println("Fan-in " + i + ": " + n);
			}
			
		}
		System.out.println("Fan-in more than 2 in total: " + sum);
	}
}
		
//				Collection<Gate> outLeftList = leftMap.getCollection(i);
//				Collection<Gate> outRightList = rightMap.getCollection(i);
				
//				boolean isLeftXOR = false;
//				if (outLeftList != null) {
//					for (Gate gl: outLeftList) {
//						isLeftXOR = isLeftXOR || gl.isXOR();
//					}
//				}
//				boolean isRightXOR = false;
//				if (outRightList != null) {
//					for (Gate gr: outRightList) {
//						isRightXOR = isRightXOR || gr.isXOR();
//					}
//				}
//				boolean isFinalXOR = !isLeftXOR && !isRightXOR;
//				boolean isLeftOnlyXOR = true;
//				if (outLeftList != null) {
//					for (Gate gl: outLeftList) {
//						if (gl.isAND() || gl.isINV()) {
//							isLeftOnlyXOR = false;
//						}
//					}
//				}
//				boolean isRightOnlyXOR = true;
//				if (outRightList != null) {
//					for (Gate gr: outRightList) {
//						if (gr.isAND() || gr.isINV()) {
//							isRightOnlyXOR = false;
//						}
//					}
//				}
//				
//				boolean isOutputOnlyXOR = isLeftOnlyXOR && isRightOnlyXOR;
				
//				int q = 0;
//				int numberOfInputs = inputs.get(i);
//				if (numberOfInputs > 2) {
//					if (!isOutputOnlyXOR) {
//						if (!outputs.containsKey(numberOfInputs)) {
//							outputs.put(numberOfInputs, 1);
//						} else {
//							int currentInput = outputs.get(numberOfInputs);
//							outputs.put(numberOfInputs, currentInput++);
//						}
//						inputs.put(i, 1);
//					} else {
//						
//					}
//				}
//				for (int j = 3; j < 10000; j++) {
//					if (numberOfInputs == j && !isOutputOnlyXOR) {
//						if (!outputs.containsKey(j)) {
//							outputs.put(j, inputs.get(i))
//						}
//						q++;
//						inputs.put(i, 1);
//					}
//				}
//				if (q != 0) {
//					
//				}
			

//			if (outLeftList != null || outRightList != null){
//				for (Gate gl: leftList){
//					for (Gate gr: rightList){
//						if (!gl.isXOR() && !gr.isXOR()) {
//							inputs.put(oIndex, totalInputs);
//						} else {
//							inputs.put(oIndex, g.getNumberOfInputWires());
//						}
//					}
//				}
		
//				

//					
////					

//		
//		for (Integer index: count.keySet()) {
//			
//		}
//		for (HashSet<Integer> hs: count.values()){
//			if(hs.size() > 2){
//				System.out.println(hs.size());
//			}
//			
//		}

