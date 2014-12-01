package common;


public class Gate {

	private int counter;
	private int leftWireIndex;
	private int rightWireIndex;
	private int outputWireIndex;
	private String boolTable;
	private int gateNumber;
	private int numberOfInputWires;
	private int numberOfOutputWires;
	private int layer;
	private int topologicalLayer;

	public Gate(String s, InputGateType type) {

		//Example string: 2 1 96 99 256 0110
		String[] split = s.split(" ");
		layer = -1;
		gateNumber = -1;
		if (type == InputGateType.FAIRPLAY) {
			if (split[0].startsWith("1")) {
				initGate(Integer.parseInt(split[0]), 
						Integer.parseInt(split[1]), Integer.parseInt(split[2]),
						Integer.MIN_VALUE, Integer.parseInt(split[3]), split[4]);
			} else {
				initGate(Integer.parseInt(split[0]), 
						Integer.parseInt(split[1]), Integer.parseInt(split[2]),
						Integer.parseInt(split[3]), Integer.parseInt(split[4]), split[5]);
			}
		} else {
			initGate(2, Integer.parseInt(split[1]), Integer.parseInt(split[2]),
					Integer.parseInt(split[3]), Integer.parseInt(split[4]), split[5]);
			layer = Integer.parseInt(split[0]);
		}
	}

	public void initGate(int numberOfInputWires, int numberOfOutputWires,
			int leftWireIndex, int rightWireIndex, int outputWireIndex, String boolTable) {
		this.counter = numberOfInputWires;
		this.numberOfInputWires = numberOfInputWires;
		this.numberOfOutputWires = numberOfOutputWires;
		this.leftWireIndex = leftWireIndex;
		this.rightWireIndex = rightWireIndex;
		
		this.outputWireIndex = outputWireIndex;
		this.boolTable = boolTable; //.replaceFirst("^0*", ""); //Removes leading 0's. Uncommented since it just adds work for the evaluator
	}

	public int getLeftWireIndex()  {
		return leftWireIndex;
	}

	public int getRightWireIndex() {
		return rightWireIndex;
	}

	public int getOutputWireIndex() {
		return outputWireIndex;
	}

	public int getCounter() {
		return counter;
	}

	public void decCounter() {
		counter--;
	}

	public String getBoolTable() {
		return boolTable;
	}

	public int getNumberOfInputWires() {
		return numberOfInputWires;
	}

	public String toFairPlayString() {
		if (getNumberOfInputWires() == 2) {
			return numberOfInputWires + " " +  numberOfOutputWires + " " + getLeftWireIndex() + " " +
					getRightWireIndex() + " " + getOutputWireIndex() + " " + getBoolTable();
		} else return numberOfInputWires + " " +  numberOfOutputWires + " " + getLeftWireIndex() + " " +
		getOutputWireIndex() + " " + getBoolTable();
	}

	public String toCUDAString() {
		return getGateNumber() + " " + getLeftWireIndex() + " " + getRightWireIndex() +
				" " + getOutputWireIndex() + " " + getBoolTable();
	}

	public String toString() {
		return toFairPlayString();
	}

	public boolean isXOR() {
		if (boolTable.matches("0110") || boolTable.matches("110")) {
			return true;
		} else return false;
	}

	public boolean isAND() {
		if (boolTable.matches("0001") || boolTable.matches("1")) {
			return true;
		} else return false;
	}

	public boolean isINV() {
		if (boolTable.matches("-1")) {
			return true;
		} else return false;
	}

	public void setGateNumber(int gateNumber) {
		this.gateNumber = gateNumber;
	}

	public int getGateNumber() {
		return gateNumber;
	}

	public int getLayer() {
		return layer;
	}

	public void setLayer(int layer) {
		this.layer = Math.max(this.layer, layer);
	}

	public int getTopologicalLayer() {
		return topologicalLayer;
	}

	public void setTopologicalLayer(int topologicalLayer) {
		this.topologicalLayer = topologicalLayer;
	}

	public void setLeftWireIndex(int index) {
		leftWireIndex = index;
	}

	public void setRightWireIndex(int index) {
		rightWireIndex = index;
	}

	public void setOutputWireIndex(int index) {
		outputWireIndex = index;
	}
}
