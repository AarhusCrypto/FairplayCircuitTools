//TODO Check this and the evaluator, the problem must be here
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;

/**
 * @author Roberto Trifiletti
 *
 */
public class FairplayCircuitConverter implements Runnable {

	private static final int NEW_LAYER_THRESHOLD = 0;

	private File outputFile;
	private boolean sorted;

	private MultiValueMap leftMap;
	private MultiValueMap rightMap;
	private HashMap<Integer, Gate> outputMap;

	private FairplayCircuitParser circuitParser;

	/**
	 * @param circuitFile
	 * @param outputFile
	 */
	public FairplayCircuitConverter(FairplayCircuitParser circuitParser, File outputFile,
			boolean sorted) {
		this.outputFile = outputFile;
		this.sorted = sorted;
		this.circuitParser = circuitParser;

		leftMap = new MultiValueMap();
		rightMap = new MultiValueMap();
		outputMap = new HashMap<Integer, Gate>();
	}

	@Override
	public void run() {
		List<Gate> gates = circuitParser.getGates();

		List<List<Gate>> layersOfGates = getLayersOfGates(gates);

		if(sorted){
			layersOfGates = getXorSortedLayers(layersOfGates);
		}

		String header = getHeader(layersOfGates);
		CommonUtilities.outputCUDACircuit(layersOfGates, outputFile, header);
	}

	public String getHeader(List<List<Gate>> layersOfGates) {
		int actualNumberOfWires = circuitParser.getWireCountFromMultipleLists(layersOfGates);
		int numberOfLayers = layersOfGates.size();

		int maxLayerWidth = 0;

		/*
		 * We have to figure out the max layer size before writing to the file.
		 */
		for(List<Gate> l: layersOfGates){
			maxLayerWidth = Math.max(maxLayerWidth, l.size());
		}
		int[] CUDAHeaderInfo = circuitParser.getCUDAHeaderInfo();
		
		int totalNumberOfInputs = CUDAHeaderInfo[0];
		int totalNumberOfOutputs = CUDAHeaderInfo[1];
		int numberOfNonXORGates = CUDAHeaderInfo[2];

		return totalNumberOfInputs + " " + totalNumberOfOutputs + " " +
		actualNumberOfWires + " " + numberOfLayers + " " + maxLayerWidth + " " +
		numberOfNonXORGates;
	}

	/**
	 * @param gates
	 * @return A lists of lists where each list represents a layer of gates in
	 * the converted circuit
	 */
	@SuppressWarnings("unchecked")
	public List<List<Gate>> getLayersOfGates(List<Gate> gates) {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();
		initMaps(gates);

		int totalNumberOfInputs = circuitParser.getTotalNumberOfInputs();
		/*
		 * Loop to run through each list in our MultiMap, first runs through all
		 * gates with left input 0, 1, 2, ..., 255.
		 * For each of these "input" dependant gates, we visit them recursively
		 * and set a timestamp on each of these.
		 */
		for(int i = 0; i < totalNumberOfInputs; i++){
			Collection<Gate> leftList = leftMap.getCollection(i);
			if(leftList == null){
				continue;
			}
			for(Gate g: leftList){
				visitGate(g, 0, layersOfGates);
			}
		}

		/*
		 * Now that we've visited all gates which depends on a left input, we
		 * do the same for the right input and recursively visit them again.
		 * When we visit a gate which has already been visited we set a
		 * timestamp again to be the max og the current time and the timestamp
		 * of the gate. This value determines which layer the gate is to be
		 * placed in. 
		 */
		for(int i = 0; i < totalNumberOfInputs; i++){
			Collection<Gate> rightList = rightMap.getCollection(i);
			if(rightList == null){
				continue;
			}
			for(Gate g: rightList){
				layersOfGates = visitGate(g, 0, layersOfGates);
			}
		}
		return layersOfGates;
	}

	/*
	 * We fill up our auxiliary maps which will help us find gates which are
	 * depending on a given gate. These Maps are MultiValued, so if two
	 * elements have the same key a list is created to hold each value associated to this
	 * key.
	 */
	private void initMaps(List<Gate> gates){
		for(Gate g: gates){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
			outputMap.put(g.getOutputWireIndex(), g);
		}
	}

	/**
	 * @param g
	 * @param time
	 * @param layersOfGates
	 * @return A list of lists representing each layer in the converted circuit
	 */
	private List<List<Gate>> visitGate(Gate g, int time, List<List<Gate>> layersOfGates) {
		g.decCounter();
		g.setTime(time);
		if (g.getCounter() == 0){
			g.setTime(time);
			addToSublist(g, layersOfGates);
			for(Gate outputGate: getOutputGates(g)){
				visitGate(outputGate, g.getTime() + 1, layersOfGates);
			}
		}
		return layersOfGates;
	}

	/**
	 * @param g
	 * @return A list of all gates depending directly on the given gate
	 */
	@SuppressWarnings("unchecked")
	private List<Gate> getOutputGates(Gate g){
		List<Gate> res = new ArrayList<Gate>();
		int inputIndex = g.getOutputWireIndex();
		Collection<Gate> leftList = leftMap.getCollection(inputIndex);
		Collection<Gate> rightList = rightMap.getCollection(inputIndex);

		if (leftList != null){
			res.addAll(leftList);
		}

		if (rightList != null){
			res.addAll(rightList);
		}

		return res;
	}

	/**
	 * 
	 * @param layersOfGates
	 * @return A list of lists where all layers either are xor-only or not
	 * containing any xors at all
	 */
	private List<List<Gate>> getXorSortedLayers(List<List<Gate>> layersOfGates) {
		List<List<Gate>> res = new ArrayList<List<Gate>>();
		for(List<Gate> l: layersOfGates){
			List<Gate> xorLayer = new ArrayList<Gate>();
			List<Gate> nonXorLayer = new ArrayList<Gate>();
			for(Gate g: l){
				if(g.isXOR()){
					xorLayer.add(g);
				}
				else nonXorLayer.add(g);
			}
			res.add(xorLayer);
			/**
			 * Can now adjust how many nonXors there has to be to
			 * justify creating a new layer
			 */

			if(nonXorLayer.size() > NEW_LAYER_THRESHOLD){
				res.add(nonXorLayer);
			}
			else xorLayer.addAll(nonXorLayer);

		}
		return res;
	}

	/**
	 * @param g
	 * @param gates
	 * @return A List of lists where the given gate has been added to the
	 * correct sublists depending on it's timestamp
	 */
	private List<List<Gate>> addToSublist(Gate g, List<List<Gate>> layersOfGates){

		while(layersOfGates.size() <= g.getTime()){
			layersOfGates.add(new ArrayList<Gate>());
		}

		List<Gate> layer = layersOfGates.get(g.getTime());
		layer.add(g);

		return layersOfGates;
	}
}
