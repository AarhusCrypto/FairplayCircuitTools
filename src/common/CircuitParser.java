package common;

import java.util.List;

public interface CircuitParser<E> {
	public List<E> getGates();
	public String[] getHeaders();
}
