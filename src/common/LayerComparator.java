package common;

import java.util.Comparator;

public class LayerComparator implements Comparator<Gate> {

	@Override
	public int compare(Gate g1, Gate g2) {
      return g1.getLayer() - g2.getLayer();
	}

}
