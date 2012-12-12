/**
 * Small class for representing inputs and output as a bit string
 * @author Roberto
 *
 */
public class BitString {
	private final boolean[] bitset;
	
	public BitString(int size) {
		bitset = new boolean[size];
	}
	
	public void set(int i) {
		if (i > bitset.length - 1 || i < 0) {
			throw new IllegalArgumentException();
		} else {
			bitset[i] = true;
		}
	}
	
	public void clear(int i) {
		if (i > bitset.length - 1 || i < 0) {
			throw new IllegalArgumentException();
		} else {
			bitset[i] = false;
		}
	}
	
	public boolean get(int i) {
		if (i > bitset.length - 1 || i < 0) {
			throw new IllegalArgumentException();
		} else {
			return bitset[i];
		}
	}
	
	public int length() {
		return bitset.length;
	}
	
	public String toString(){
		String res = "";
		for (int i = 0; i < bitset.length; i++) {
			if (i != 0 && i % 8 == 0){
				res += " ";
			}
			if (get(i) == true) {
				res += '1';
			} else res += '0';
		}
		return res;
	}
	
	public BitString getIA32BitString() {
		BitString res = new BitString(bitset.length);
		int BYTESIZE = 8;
		int m = BYTESIZE - 1;
		for (int i = 0; i < bitset.length; i++) {
			int offset = i % BYTESIZE;
			if (offset == 0 && i != 0) {
				m += BYTESIZE;
			}
			if (bitset[i]) {
				res.set(m - offset);
			}
		}
		return res;
	}
	
	public BitString getMirroredBitString() {
		BitString res = new BitString(bitset.length);
		
		for (int i = 0; i < bitset.length; i++) {
			boolean b = bitset[bitset.length - 1 - i];
			if (b) {
				res.set(i);
			}
		}
		return res;
	}
	
	public BitString getReverseOrder() {
		BitString res = new BitString(bitset.length);
		
		for (int i = 0; i < bitset.length; i++) {
			if (bitset[(bitset.length/2 + i) % bitset.length]) {
				res.set(i);
			}
		}
		
		return res;
	}
}
