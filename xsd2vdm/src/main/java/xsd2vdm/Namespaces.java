/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2021, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License are
 * obtained from the INTO-CPS Association, either from the above address, from
 * the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */
package xsd2vdm;

import java.util.HashMap;
import java.util.Map;

public class Namespaces
{
	private Map<String, String> namespaces = new HashMap<String, String>();	// URL -> prefix

	public void addNamespace(String namespace, String prefix)
	{
		namespaces.put(namespace, prefix);
	}
	
	public String addNamespaces(Map<String, String> attributes)
	{
		String targetNamespace = null;
		
		for (String attr: attributes.keySet())
		{
			switch (attr)
			{
				case "targetNamespace":
					targetNamespace = attributes.get(attr);
					break;
					
				default:
					if (attr.startsWith("xmlns:"))
					{
						String prefix = attr.substring(6);	// eg. "xs"
						String namespace = attributes.get(attr);
						addNamespace(namespace, prefix);
					}
					break;	// else ignore
			}
		}
		
		return getPrefix(targetNamespace);
	}

	public String getPrefix(String namespace)
	{
		String prefix = namespaces.get(namespace);
		return prefix == null ? "" : prefix;
	}

	public boolean contains(String namespace)
	{
		return namespaces.containsKey(namespace);
	}

	public void clear()
	{
		namespaces.clear();
	}
}
