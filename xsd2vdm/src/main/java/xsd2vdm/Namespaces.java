/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	xsd2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	xsd2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with xsd2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/
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
