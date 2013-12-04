import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ServerUtil {

	public static Map<String, String[]> readConfiguration(String[] args)
			throws InvalidConfigException {
		Map<String, String[]> vars = readVars(args, "-");
		String[] names;
		if ((names = vars.get("config")) != null) {
			String fname = names[0];
			if (fname != null) {
				try {
					File f = new File(fname);
					Scanner s = new Scanner(f);
					String line;
					while (s.hasNextLine()) {
						line = s.nextLine();
						String[] tokens = line.split(" ");
						if (!(tokens.length > 1))
							continue; // throw new InvalidConfigException();
						if (vars.get(tokens[0]) == null) // Command Line
															// arguments
															// overwrite config
															// file arguments
							vars.put(tokens[0], Arrays.copyOfRange(tokens, 1,
									tokens.length));
					}

				} catch (IOException e) {
					System.err.println("Config File not Found.");
					e.printStackTrace();
					throw new InvalidConfigException();
				}
			} 
		} else
			throw new InvalidConfigException();
		return vars;
	}

	public static Map<String, String[]> readVars(String[] args, String del) {
		Map<String, String[]> vars = new HashMap<String, String[]>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].regionMatches(0, del, 0, del.length())) {
				ArrayList<String> vals = new ArrayList<String>();
				String argName = args[i].substring(1);
				i++;
				while (i < args.length
						&& !args[i].regionMatches(0, del, 0, del.length()))
					vals.add(args[i++]);
				vars.put(argName, (String[]) vals.toArray(new String[1]));
				i--;
			}
		}
		return vars;
	}

}
