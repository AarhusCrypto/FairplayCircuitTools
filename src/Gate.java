

public class Gate {

	private static final int INPUT_WIRES = 2;

	private int counter;
	private int time;
	private int leftWireIndex;
	private int rightWireIndex;
	private int outputWireIndex;
	private String boolTable;
	private int gateNumber;
	private int numberOfInputWires;
	private int numberOfOutputWires;

	public Gate(String s) {

		//Example string: 2 1 96 99 256 0110
		String[] split = s.split(" ");
		counter = INPUT_WIRES;
		time = -1;
		numberOfInputWires = Integer.parseInt(split[0]);
		numberOfOutputWires = Integer.parseInt(split[1]);
		leftWireIndex = Integer.parseInt(split[2]);
		rightWireIndex = Integer.parseInt(split[3]);
		outputWireIndex = Integer.parseInt(split[4]);

		boolTable = split[5].replaceFirst("^0*", ""); //Removes leading 0's
		gateNumber = -1;
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

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = Math.max(this.time, time);
	}

	public String getBoolTable() {
		return boolTable;
	}

	public String toFairPlayString() {
		return numberOfInputWires + " " +  numberOfOutputWires + " " + getLeftWireIndex() + " " + getRightWireIndex() +
				" " + getOutputWireIndex() + " " + getBoolTable();
	}

	public String toCUDAString() {
		return getGateNumber() + " " + getLeftWireIndex() + " " + getRightWireIndex() +
				" " + getOutputWireIndex() + " " + getBoolTable();
	}

	public String toString() {
		return toFairPlayString();
	}

	public boolean isXOR() {
		if (boolTable.matches("110")) {
			return true;
		} else return false;
	}

	public void setGateNumber(int gateNumber) {
		this.gateNumber = gateNumber;
	}

	public int getGateNumber() {
		return gateNumber;
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
