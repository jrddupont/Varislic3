package code;

import java.util.HashMap;

public class CLIArgumentParser {
	public static HashMap<String, String> pareseCLI(String[] args){
		if(args.length % 2 != 0){
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
