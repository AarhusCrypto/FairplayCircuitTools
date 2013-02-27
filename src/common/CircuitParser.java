package common;

import java.io.File;
import java.util.List;

public interface CircuitParser<E> extends CircuitProvider<E> {
	public File getCircuitFile();
	public List<E> getGates();
	public String[] getHeaders();
	public int getNumberOfInputs();
	public int getNumberOfOutputs();
	public int getNumberOfNonXORGates();
	public int getNumberOfP1Inputs();
	public int getNumberOfP2Inputs();
	public int getNumberOfWires();
}
