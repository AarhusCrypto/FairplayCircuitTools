package common;

import java.util.Comparator;

public class TopoTypeComparator implements Comparator<Gate> {

	@Override
	public int compare(Gate g1, Gate g2) {
		int topological = g1.getTopologicalLayer() - g2.getTopologicalLayer();
        if (topological != 0) {
            return topological;
        }
        
        if (!g1.isAND() && !g2.isAND()) {
        	int layer = g1.getLayer() - g2.getLayer();
            if (layer != 0) {
            	return layer;
            }
        }

        if (g1.isXOR()) {
        	if (g2.isXOR()){
        		return 0;
        	}
        	if (g2.isINV()) {
        		return -1;
        	}
        	if (g2.isAND()) {
        		return -1;
        	}
        }
        if (g1.isINV()) {
        	if (g2.isXOR()){
        		return 1;
        	}
        	if (g2.isINV()) {
        		return 0;
        	}
        	if (g2.isAND()) {
        		return -1;
        	}
        }
        if (g1.isAND()) {
        	if (g2.isXOR()){
        		return 1;
        	}
        	if (g2.isINV()) {
        		return 1;
        	}
        	if (g2.isAND()) {
        		return 0;
        	}
        }
        System.out.println("unreachable!");
        return 0;
	}

}
