package common;

import java.io.File;
import java.util.List;

public interface CircuitParser<E> {
	public List<E> getGates();
	public String[] getHeaders();
	public File getCircuitFile();
}
