package common;

import java.io.File;

public interface CircuitParser<E> extends CircuitProvider<E> {
	public File getCircuitFile();
}
