/**
 * A simple JSON parser to turn INTO-CPS configuration files into VDM-SL
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class COSIMToVDM
{
	public static void main(String[] args) throws IOException
	{
		if (args.length != 2)
		{
			error("Usage: COSIMToVDM <json file> <VDM var name>");
		}
		
		JSONReader reader = new JSONReader();
		System.out.println("/**");
		System.out.println(" * VDM Model generated from " + args[0] + " on " + new Date());
		System.out.println(" */");

		System.out.println("-- functions");
		System.out.println("-- " + args[1] + ": () +> bool");
		System.out.println("-- " + args[1] + "() ==");
		System.out.println("-- \tisValidCoSimulation");

		System.out.println("values");
		System.out.println(args[1] + " = mk_CoSimulation");
		System.out.println("(");
		processConfiguration(reader.read(readFile(args[0])));
		System.out.println(");");
	}
	
	private static void error(String message)
	{
		System.err.println(message);
		System.exit(1);
	}
	
	private static void processConfiguration(Object object)
	{
		if (object instanceof Map<?,?>)
		{
			Map<String, Object> map = (Map<String, Object>)object;
			
			if (map.containsKey("fmus"))
			{
				processFMUs(map.get("fmus"));
			}
			else
			{
				error("Configuration does not define fmus");
			}
			
			if (map.containsKey("connections"))
			{
				processConnections(map.get("connections"));
			}
			else
			{
				error("Configuration does not define connections");
			}
		}
		else
		{
			error("JSON config is not an object");
		}
	}
	
	private static Map<String, Integer> FMUIndex = new HashMap<String, Integer>();
	private static Map<Integer, String> FMUspec = new HashMap<Integer, String>();

	private static void processFMUs(Object object)
	{
		if (object instanceof Map<?,?>)
		{
			Map<String, Object> map = (Map<String, Object>) object;
			int index = 0;
			System.out.println("\t-- FMUs\n\t[");
			String sep = "";
			
			for (String fmu: map.keySet())
			{
				String raw = fmu.substring(1, fmu.length() - 1);	// eg. "wt"
				String instance = fmu + "." + raw + "Instance";		// eg. "{wt}.wtInstance"
				FMUIndex.put(instance, ++index);
				FMUspec.put(index, raw + ".vdmsl");					// eg. "wt.vdmsl"
				
				System.out.print(sep + "\t\t" + raw );
				sep = ",\n";
			}

			System.out.println("\n\t],\n");
		}
		else
		{
			error("fmus entry is not an object");
		}
	}

	private static void processConnections(Object object)
	{
		if (object instanceof Map<?,?>)
		{
			Map<String, Object> map = (Map<String, Object>) object;
			System.out.println("\t-- Connections\n\t{");
			String sep = "";
			
			for (String source: map.keySet())
			{
				System.out.println(sep + "\t\t" + fmuVariable(source) + " |->\n\t\t{");
				Object dest = map.get(source);
				
				if (dest instanceof List)
				{
					List<String> list = (List<String>) dest;
					String sep2 = "";
					
					for (String name: list)
					{
						System.out.println(sep2 + "\t\t\t" + fmuVariable(name));
						sep2 = ",\n";
					}
				}
				else
				{
					error("Connection set is not a list");
				}
				
				System.out.print("\t\t}");
				sep = ",\n\n";
			}
	
			System.out.println("\n\t}");
		}
		else
		{
			error("connections entry is not an object");
		}
	}
	
	private static String fmuVariable(String varname)
	{
		int fmui = fmuIndexOf(varname).getValue();
		int vari = varIndexOf(varname);
		return "mk_FMUVariable(" + fmui + ", " + vari + ")";
	}
	
	private static Entry<String, Integer> fmuIndexOf(String varname)
	{
		for (Entry<String, Integer> fmu: FMUIndex.entrySet())
		{
			if (varname.startsWith(fmu.getKey()))
			{
				return fmu;
			}
		}
		
		error("Unknown FMU varname: " + varname);
		return null;
	}
	
	private static int varIndexOf(String varname)
	{
		Entry<String, Integer> fmu = fmuIndexOf(varname);
		String spec = FMUspec.get(fmu.getValue());
		varname = varname.substring(fmu.getKey().length() + 1);
		
		BufferedReader br = null;

		try
		{
			br = new BufferedReader(new FileReader(spec));
			String line = br.readLine();
			Pattern p = Pattern.compile(".*mk_ScalarVariable\\(\"([^\"]+)\",.*$");
			int index = 0;
			
			while (line != null)
			{
				Matcher m = p.matcher(line);
				
				if (m.matches())
				{
					index++;
					
					if (m.group(1).equals(varname))
					{
						return index;
					}
				}
				
				line = br.readLine();
			}
		}
		catch (IOException e)
		{
			error(e.getMessage());
		}
		finally
		{
			try
			{
				br.close();
			}
			catch (IOException e)
			{
				// ignore
			}
		}

		error("Could not find " + varname + " in " + spec);
		return 0;
	}

	private static String readFile(String filename) throws IOException
	{
		File file = new File(filename);
		char[] buffer = new char[(int) file.length()];
		BufferedReader ir = new BufferedReader(new FileReader(file));
		ir.read(buffer);
		ir.close();
		return new String(buffer);
	}
}
