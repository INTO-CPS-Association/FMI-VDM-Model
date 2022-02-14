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

package values;

import java.util.List;
import java.util.Vector;

import org.xml.sax.Locator;

import types.Type;

public class SeqValue extends VDMValue
{
	private final List<VDMValue> values = new Vector<VDMValue>();

	public SeqValue(Type type, Locator locator)
	{
		super(type, locator);
	}

	@Override
	public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "[\n");
		String sep = "";
		
		for (VDMValue value: values)
		{
			sb.append(sep);
			sb.append(value.toVDM(indent + "    "));
			sep = ",\n";
		}
		
		sb.append("\n" + indent + "]");
		return sb.toString();
	}

	public void add(VDMValue value)
	{
		values.add(value);
	}
}
