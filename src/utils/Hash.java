package utils;

public class Hash {
	/*
	 * comment from C&C: shift value to multiply by with each additional
	 * component of the hash function; this number is normally selected to be
	 * a smallish prime; 31 and 37 seem to work well
	 */
	private final static long INC = 92821;
	private long hash;

	public Hash(Hash other) {
		hash = other.value();
	}

	public Hash(long hash) {
		this.hash = hash;
	}

	public Hash(byte hash) {
		this.hash = (hash);
	}

	public long value() {
		return hash;
	}

	public void plusEqual(long value) {
		// += in the C&C code
		hash *= INC;
		hash += value;
	}

	public void barEqual(long value) {
		// |= in the C&C code
		hash += value;
	}

	public void timesEqual(long value) {
		// *= in the C&C code
		hash *= value;
	}
}
