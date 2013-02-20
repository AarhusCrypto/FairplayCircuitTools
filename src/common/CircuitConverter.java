package common;

public interface CircuitConverter<E, T> extends CircuitProvider<E> {
	public CircuitParser<T> getCircuitParser();
}
