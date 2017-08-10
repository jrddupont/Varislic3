package code;

import java.util.HashMap;

public class CLIArgumentParser {
	/**	
	 * Returns a map of commands to arguments generated from standard java CLI. 
	 * For example, '-file fox.stl' would become "file" -> "fox.stl"
	 * @param args	The raw arguments from a main function
	 * @return	The map
	 */
	public static HashMap<String, String> pareseCLI(String[] args){
		if(args.length % 2 != 0){	// There are no commands that take more than one argument, so if the arguments are odd, then there is a problem
			return null;
		}
		
		HashMap<String, String> output = new HashMap<String, String>();
		for(int i = 0; i < args.length; i += 2){
			if(args[i].charAt(0) != '-'){ 
				return null;
			}
			output.put(args[i].substring(1), args[i + 1]);
		}
		return output;
	}
}
