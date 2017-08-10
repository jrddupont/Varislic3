package code;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import stl.STLParser;
import stl.Triangle;
import stl.Vec3d;

public class VariSlic3 {
	
	static final String MIN_LAYER_KEY = "min";
	static final String MAX_LAYER_KEY = "max";
	static final String STEPS_KEY = "steps";
	static final String NEGATIVE_KEY = "negatives";
	static final String FILE_KEY = "file";
	
	static double minLayerHeight = 0.1;	// In mm
	static double maxLayerHeight = 0.3;	// In mm
	static int numberOfSteps = 10;	// As an int
	static boolean considerNegatives = true;	// Should consider overhangs in this calculation
	static double stepSize = 0;
	public static void main(String[] args) throws IOException{
		
		File file = null;
		if(inConsole()){
			try{
				HashMap<String, String> arguments = CLIArgumentParser.pareseCLI(args);
				if(arguments == null){
					printHelp();
					return;
				}
				if(arguments.containsKey(FILE_KEY)){
					file = new File(arguments.get(FILE_KEY));
				}else{
					System.out.println("Please include a STL file path.");
					printHelp();
					return;
				}
				if(arguments.containsKey(MIN_LAYER_KEY)){
					minLayerHeight = Double.parseDouble(arguments.get(MIN_LAYER_KEY));
				}else{
					minLayerHeight = 0.1;
				}
				if(arguments.containsKey(MAX_LAYER_KEY)){
					maxLayerHeight = Double.parseDouble(arguments.get(MAX_LAYER_KEY));
				}else{
					maxLayerHeight = 0.3;
				}
				if(arguments.containsKey(STEPS_KEY)){
					numberOfSteps = Integer.parseInt(arguments.get(STEPS_KEY));
				}else{
					numberOfSteps = 10;
				}
				if(arguments.containsKey(NEGATIVE_KEY)){
					considerNegatives = Boolean.parseBoolean(arguments.get(NEGATIVE_KEY));
				}else{
					considerNegatives = true;
				}
			}catch(Exception e){
				System.out.println("Error parsing input.");
				printHelp();
			}
 		}else{
			file = new File("bulb.stl");
			considerNegatives = true;
			minLayerHeight = 0.1;
			maxLayerHeight = 0.3;
			numberOfSteps = 10;
		}
		
		// Options are read in, building the table...
		
		stepSize = (maxLayerHeight - minLayerHeight) / (numberOfSteps - 1);
		List<Triangle> mesh = STLParser.parseSTLFile(file.toPath());
		ArrayList<Range> ranges = new ArrayList<Range>();
		
		int numberOfPolys = mesh.size();
		int printEvery = 0;
		if(numberOfPolys < 20){
			printEvery = 1;
		}else{
			printEvery = numberOfPolys / 20;
		}
		
		double upperBound = 0;	// Find global upper bound
		
		for(Triangle t : mesh){
			for(Vec3d point : t.getVertices()){
				if(point.z > upperBound){
					upperBound = point.z;
				}
			}
		}
		ranges.add(new Range(0, upperBound, maxLayerHeight));
		
		int counter = 0;
		System.out.print("Building table: ");
		outer:
		for(Triangle t : mesh){	// Looping through each triangle in the mesh
			//validateRanges(ranges);
			counter++;
			if(counter >= printEvery){
				counter = 0;
				System.out.print("*");
			}
			
			double maxHeight = 0;	// Max height of the current triangle
			double minHeight = upperBound;	// Min height of the current triangle
			for(Vec3d point : t.getVertices()){	// For each point in the current triangle, update min/max
				if(point.z > maxHeight){
					maxHeight = point.z;
				}
				if(point.z < minHeight){
					minHeight = point.z;
				}
			}
			// The min and max values are now set and accurate
			
			double decimals = 100;
			maxHeight = (int)((maxHeight * decimals)) / decimals;
			minHeight = (int)((minHeight * decimals)) / decimals;
			
			if(minHeight < 0){
				continue;
			}
			
			// This loop is trying to find a place for the new range we created to go.
			Range newRange = new Range(minHeight, maxHeight, getLayerHeight(t));
			if(newRange.layerHeight == 0){
				continue;
			}
			
			for(Range r : ranges){
				if(r.end > newRange.start){ // If the range we are looking at's upper bound is greater than the new range's lower bound then we know it is the start
					int startIndex = ranges.indexOf(r);	// Setting the index of the top
					int endIndex = startIndex;	// The end index must be >= start index, so setting it appropriately 
					while(true){
						if(ranges.get(endIndex).end >= newRange.end){ // If suspected end range's top is >= the new range's top, then we have found end  
							break;
						}else{
							endIndex++;
						}
					}
					// We should now have start and end ranges, even if they are the same range
					// Handle start and finish
					if(startIndex > endIndex){
						System.out.println("Critical error, please send me the STL you tried to proccess. 0x0");
						return;
					}
					if(startIndex == endIndex){	// If the range we are inserting only intersects one existing range
						Range currentRange = ranges.get(startIndex);
						if(newRange.layerHeight >= currentRange.layerHeight){ // If the new range's layer height is larger than the existing, just skip this triangle all together
							continue outer;
						}
						
						Range rightSplitRange = new Range(currentRange);
						currentRange.end = newRange.start;
						rightSplitRange.start = newRange.end;
						
						ranges.add(startIndex + 1, newRange);
						ranges.add(startIndex + 2, rightSplitRange);
						if(newRange.getSize() == 0){
							ranges.remove(newRange);
						}
						if(rightSplitRange.getSize() == 0){
							ranges.remove(rightSplitRange);
						}
						if(currentRange.getSize() == 0){
							ranges.remove(currentRange);
						}
					}else{
						
						Range startRange = ranges.get(startIndex);
						Range endRange = ranges.get(endIndex);
						if(startRange.layerHeight >= newRange.layerHeight){	// If start has lower priority than new. It's backwards because larger layer height means less priority
							startRange.end = newRange.start;
						}else if(startRange.layerHeight < newRange.layerHeight){
							newRange.start = startRange.end;
						}
						if(endRange.layerHeight >= newRange.layerHeight){
							endRange.start = newRange.end;
						}else if(endRange.layerHeight < newRange.layerHeight){
							newRange.end = endRange.start;
						}
						
						if(newRange.getSize() == 0){
							continue;
						}
						
						ranges.add(startIndex + 1, newRange);
						Range curMiddleRange = ranges.get(startIndex + 2);
						
						Stack<Range> removeStack = new Stack<Range>();
						while(curMiddleRange != endRange){
							if(curMiddleRange.layerHeight >= newRange.layerHeight){
								removeStack.push(curMiddleRange);
								curMiddleRange = ranges.get(ranges.indexOf(curMiddleRange) + 1);
							}else{
								Range temp = new Range(newRange);
								temp.start = curMiddleRange.end;
								newRange.end = curMiddleRange.start;
								
								ranges.add(ranges.indexOf(curMiddleRange) + 1, temp);
								curMiddleRange = ranges.get(ranges.indexOf(curMiddleRange) + 2);

								if(temp.getSize() == 0){
									removeStack.push(temp);
								}
								if(newRange.getSize() == 0){
									removeStack.push(newRange);
								}
								newRange = temp;
							}
							
						}
						if(startRange.getSize() == 0){
							ranges.remove(startRange);
						}
						if(endRange.getSize() == 0){
							ranges.remove(endRange);
						}
						while(!removeStack.isEmpty()){
							Range rem = removeStack.pop();
							ranges.remove(rem);
						}
					}
					continue outer;
				}
			}
			// Didn't find range to start on.
			System.out.println("Critical error, please send me the STL you tried to proccess. 0x1");
			return;
		}
		System.out.println("\nDone building table, purging table...");
		System.out.print("Reduced ranges from " + ranges.size());
		int curRangeIndex = 0;
		while(curRangeIndex < ranges.size() - 1){
			Range thisRange = ranges.get(curRangeIndex);
			Range nextRange = ranges.get(curRangeIndex + 1);
			if(thisRange.layerHeight == nextRange.layerHeight){
				thisRange.end = nextRange.end;
				ranges.remove(nextRange);
			}else{
				curRangeIndex++;
			}
		}
		System.out.println(" to " + ranges.size());
		saveRanges(ranges);
		System.out.println("Table saved to 'output.txt'");
	}
	
	/**
	 * Simply prints a help page
	 */
	private static void printHelp() {
		System.out.println("VariSlic3 is a program that automatically generates a table of layer heights that Slic3r accepts and saves the output to 'output.txt' in the same directory as the jar file ");
		System.out.println("Usage:");
		System.out.println("	'-file file_path'	The STL file location.");
		System.out.println("	'-min layer_height'	Optional, the minimum desired layer height as a double. Defaults to 0.1");
		System.out.println("	'-max layer_height'	Optional, the maximum desired layer height as a double. Defaults to 0.3");
		System.out.println("	'-steps number_of_steps'	Optional, the number of different layer heights as an integer. Defaults to 10.");
		System.out.println("	'-negatives true_or_false'	Optional, should negative slopes (overhangs) effect the layer height, as a boolean. Defaults to true.");
		System.out.println();
		System.out.println("Basic usage: ");
		System.out.println("	'varislic3.jar -file C:/objects/fox.stl -min 0.1 -max 0.3'");
		System.out.println("	'varislic3.jar -file C:/objects/fox.stl -min 0.1 -max 0.3 -steps 3'	Generates the table only using 0.1, 0.2, and 0.3 as possible layer heights.");
		System.out.println("	'varislic3.jar -file C:/objects/fox.stl -min 0.1 -max 0.3 -negatives false'	Does not consider overhanges");
		System.out.println();
		System.out.println();
	}

	/**
	 * Saves the ranges into a table separated by tabs
	 * 
	 * ╔═══════╦═════╦═════════════╗
	 * ║ start ║ end ║ layerHeight ║
	 * ╚═══════╩═════╩═════════════╝
	 * 
	 * @param ranges
	 */
	private static void saveRanges(ArrayList<Range> ranges) {
		try{
			FileWriter fw = new FileWriter("output.txt");
			String newLine = System.getProperty("line.separator");
			for(Range r : ranges){
				fw.write(r.start + "	" + r.end + "	" + r.layerHeight + newLine);
			}
			fw.close();
		}catch(IOException e){
			System.out.println("Error in saving file");
		}
	}

	/**
	 * Gets the angle of a vector relative to a horizontal plane.
	 * 
	 * @param vector The normal of a triangle
	 * @return The angle in radians of the normal
	 */
	private static double getAngle(Vec3d vector){
		// 90 degrees means flat, lower layer height
		// 0  degrees means vertical, higher layer height
		return Math.asin(vector.x); // x is vertical for some reason
	}
	
	/**
	 * Gets what layer height that the printer should print at for a given triangle (Polygon). If the "considerNegatives" flag is false,
	 * it will return 0 causing the main loop to continue. Always returns between minLayerHeight and maxLayerHeight inclusive. 
	 * 
	 * @param triangle The triangle to check
	 * @return 0 if it is a negative and 'considerNegatives' is false and a layer height otherwise
	 */
	private static double getLayerHeight(Triangle triangle){
		double angle = getAngle(triangle.getNormal());
		if(!considerNegatives && angle < 0){
			return 0;
		}
		if(considerNegatives && angle < 0){
			angle *= -1;
		}
		double layerHeight = minLayerHeight + (stepSize * (numberOfSteps - (int)(numberOfSteps * (angle / 1.5708))));
		return ((int)(layerHeight * 1000)) / 1000.0;
	}
	
	/**
	 * Loop through a set of ranges and check if the end of the current one matches the start of the next. For example [0-10] next to [12-15] is invalid.
	 * 
	 * @param ranges The ranges to validate  
	 */
	private static void validateRanges(ArrayList<Range> ranges){
		for(int i = 0; i < ranges.size() - 1; i++){
			if(ranges.get(i).end != ranges.get(i + 1).start){
				System.out.println();
				System.out.println();
				printRanges(ranges);
				System.out.println("Error in validating ranges: " + ranges.get(i) + " " + ranges.get(i + 1));
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Prints all the ranges in order
	 * 
	 * @param ranges
	 */
	private static void printRanges(ArrayList<Range> ranges) {
		for(Range trange : ranges){
				System.out.print("["+trange.start+"("+trange.layerHeight+")"+trange.end+"]");
		}
		System.out.println();
	}
	/**
	 * Checks if the program was run from eclipse or a console
	 * @return
	 */
	private static boolean inConsole(){
		return System.console() != null;
	}
}
