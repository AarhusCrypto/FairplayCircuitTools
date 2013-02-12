package impl;
import java.util.List;

import common.CircuitConverter;
import common.Gate;

public class FairplayCircuitToSPACL implements CircuitConverter<List<Gate>>{

	private FairplayCircuitConverter circuitConverter;
	
	public FairplayCircuitToSPACL(FairplayCircuitConverter circuitConverter) {
		this.circuitConverter = circuitConverter;
	}

	@Override
	public List<List<Gate>> getGates() {
		List<List<Gate>> gates = circuitConverter.getGates();
		String[] headers = circuitConverter.getHeaders();
		return gates;
	}

	@Override
	public String[] getHeaders() {
		return circuitConverter.getHeaders();
	}


}
