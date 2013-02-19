package common;

import java.util.List;

public interface CircuitProvider<E> {
	public List<E> getGates();
	public String[] getHeaders();
}
