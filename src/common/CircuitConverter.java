package common;
import java.util.List;


public interface CircuitConverter<E> {
	public List<E> getGates();
	public String[] getHeaders();
}
