package code;

public class Range {
	double start;
	double end;
	double layerHeight;
	public Range(double start, double end, double layerHeight){
		this.start = start;
		this.end = end;
		this.layerHeight = layerHeight;
	}
	public Range(Range r){
		this(r.start, r.end, r.layerHeight);
	}
	public double getSize(){
		return end - start;
	}
	public String toString(){
		return "range[" + start + ", " + end + ", " + layerHeight + "]";
	}
	public void set(Range r){
		this.start = r.start;
		this.end = r.end;
		this.layerHeight = r.layerHeight;
	}
}
