package code;

/**	
 *	A simple range from A to B with an importance or precedence which in this case is layer height. Lower height is higher precedence 
 */
public class Range {
	double start;
	double end;
	double layerHeight;
	/**
	 * A simple range from A to B with an importance or precedence which in this case is layer height. Lower height is higher precedence
	 * 
	 * @param start	The start of the range, typically lower than end
	 * @param end	The end of the range, typically higher than start
	 * @param layerHeight	The layer heightto be printed at. 
	 */
	public Range(double start, double end, double layerHeight){
		this.start = start;
		this.end = end;
		this.layerHeight = layerHeight;
	}
	/**
	 * Overloaded constructor for coping a range
	 * 
	 * @param r The range to be copied
	 */
	public Range(Range r){
		this(r.start, r.end, r.layerHeight);
	}
	/**
	 * Gets the size of the range 
	 * 
	 * @return How large the range is.
	 */
	public double getSize(){
		return end - start;
	}
	public String toString(){
		return "range[" + start + ", " + end + ", " + layerHeight + "]";
	}
	/**
	 * Copies a given range into this object
	 * 
	 * @param r	The range to copy
	 */
	public void set(Range r){
		this.start = r.start;
		this.end = r.end;
		this.layerHeight = r.layerHeight;
	}
}
