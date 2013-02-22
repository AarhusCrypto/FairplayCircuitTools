package common;

import java.util.Comparator;

public class OutputWireComparator implements Comparator<Gate> {

	@Override
	public int compare(Gate g1, Gate g2) {
		return g1.getOutputWireIndex() - g2.getOutputWireIndex();
	}

}
