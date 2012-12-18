import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class WireReuseCounter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File circuitFile = new File("test/data/md5_fairplay.txt");
		FairplayCircuitParser circuitParser = new FairplayCircuitParser(circuitFile, false);
		List<Gate> gates = circuitParser.getGates();
		
		List<Integer> wires = new ArrayList<Integer>();
		
		for(Gate g: gates) {
			wires.add(g.getLeftWireIndex());
			wires.add(g.getRightWireIndex());
		}
		System.out.println(wires.size());
		System.out.println(CommonUtilities.getWireCount(gates));

	}

}
