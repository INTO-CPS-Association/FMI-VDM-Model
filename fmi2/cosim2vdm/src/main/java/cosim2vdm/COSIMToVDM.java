/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	cosim2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	cosim2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with cosim2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

/**
 * A simple JSON parser to turn INTO-CPS configuration files into VDM-SL
 */

package cosim2vdm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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

		JSONReader reader = new JSONReader(new FileReader(args[0]));
		processConfiguration(reader.readObject());
		System.out.println(");");
	}
	
	private static void error(String message)
	{
		System.err.println(message);
		System.exit(1);
	}
	
	private static void processConfiguration(JSONObject map)
	{
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
		
		if (map.containsKey("parameters"))
		{
			processParameters(map.get("parameters"));
		}
		else
		{
			error("Configuration does not define parameters");
		}
	}
	
	private static Map<String, Integer> FMURoot = new TreeMap<String, Integer>();
	private static Map<String, Integer> FMUIndex = new TreeMap<String, Integer>();
	private static Map<Integer, String> FMUspec = new TreeMap<Integer, String>();

	private static void processFMUs(JSONObject map)
	{
		int index = 0;
		System.out.println("\t-- FMUs\n\t[");
		String sep = "";
		
		for (String fmu: map.keySet())
		{
			FMURoot.put(fmu, ++index);							// eg. "{wt}" = 1
			
			String raw = fmu.substring(1, fmu.length() - 1);	// eg. "wt"
			FMUspec.put(index, raw + ".vdmsl");					// eg. "wt.vdmsl"
			
			System.out.print(sep + "\t\t" + raw );
			sep = ",\n";
		}

		System.out.println("\n\t],\n");
	}

	private static void processConnections(JSONObject map)
	{
		System.out.println("\t-- Connections\n\t{");
		String sep = "";
		
		// Collect unique FMU stems first
		for (String source: map.keySet())
		{
			String[] parts = source.split("\\.");
			String key = (parts.length > 1 ? parts[0] + "." + parts[1] : parts[0]);
			
			if (!FMUIndex.containsKey(key))
			{
				FMUIndex.put(key, FMURoot.get(parts[0]));
			}

			JSONArray list = map.get(source);
			
			for (Object obj: list)
			{
				String name = (String)obj;
				parts = name.split("\\.");
				key = (parts.length > 1 ? parts[0] + "." + parts[1] : parts[0]);
				
				if (!FMUIndex.containsKey(key))
				{
					FMUIndex.put(key, FMURoot.get(parts[0]));
				}
			}
		}
		
		for (String source: map.keySet())
		{
			System.out.println(sep + "\t\t" + fmuVariable(source) + " |->\n\t\t{");
			JSONArray list = map.get(source);
			String sep2 = "";
			
			for (Object obj: list)
			{
				String name = (String)obj;
				System.out.println(sep2 + "\t\t\t" + fmuVariable(name));
				sep2 = ",\n";
			}
			
			System.out.print("\t\t}");
			sep = ",\n\n";
		}

		System.out.println("\n\t},\n");
	}
	
	private static void processParameters(JSONObject map)
	{
		System.out.println("\t-- Parameters\n\t{");
		String sep = "";
		
		for (String source: map.keySet())
		{
			System.out.print(sep + "\t\t" + fmuVariable(source) + " |-> " + map.get(source));
			sep = ",\n";
		}

		System.out.println("\n\t}");
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
}
