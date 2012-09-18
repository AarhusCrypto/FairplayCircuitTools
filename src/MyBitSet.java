
public class MyBitSet {
	private final boolean[] bitset;
	
	public MyBitSet(int size){
		bitset = new boolean[size];
	}
	
	public void set(int i){
		if(i > bitset.length - 1 || i < 0){
			throw new IllegalArgumentException();
		}
		else{
			bitset[i] = true;
		}
	}
	
	public void clear(int i){
		if(i > bitset.length - 1 || i < 0){
			throw new IllegalArgumentException();
		}
		else{
			bitset[i] = false;
		}
	}
	
	public boolean get(int i){
		if(i > bitset.length - 1 || i < 0){
			throw new IllegalArgumentException();
		}
		else{
			return bitset[i];
		}
	}
	
	public int length(){
		return bitset.length;
	}
	
	public String toString(){
		String res = "";
		for(int i = 0; i < bitset.length; i++){
			if (i != 0 && i % 8 == 0){
				res += " ";
			}
			if(get(i) == true){
				res += '1';
			}
			else res += '0';
		}
		return res;
	}
}
