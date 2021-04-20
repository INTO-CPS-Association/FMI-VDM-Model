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

package types;

import java.util.Set;

public class DigitsFacet extends Facet
{
	public DigitsFacet(String type, String value)
	{
		super(type, value);
	}
	
	@Override
	public String toVDM(String var, Type fieldtype)
	{
		String result = "";
		
		if (fieldtype.isOptional())
		{
			result = var + " <> nil => ";
			OptionalType otype = (OptionalType)fieldtype;
			fieldtype = otype.type;
		}
		
		if (fieldtype instanceof SeqType)
		{
			result = result + "forall " + var + "' in seq " + var + " & ";
			var = var + "'";
		}
		
		switch (kind)
		{
			case "xs:totalDigits":
				return result + "xsdTotalDigits(" + var + ") <= " + value;
				
			case "xs:fractionDigits":
				return result + "xsdFractionDigits(" + var + ") <= " + value;
				
			default:
				return "?";
		}
	}
	
	@Override
	public Set<String> getFunctions()
	{
		Set<String> all = super.getFunctions();
		all.add(functionFromType());
		return all;
	}
}
