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

package annotations.in;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

import maestro.OnFailError;

public class INOnFailAnnotation extends INAnnotation
{
	private static List<OnFailError> errorList = new Vector<>();

	public INOnFailAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	public void inAfter(INExpression exp, Value rv, Context ctxt)
	{
		try
		{
			if (!rv.boolValue(ctxt))	// ONLY if we fail!
			{
				int errno = 0;
				int offset = 1;			// By default, args(1) is the 1st
				
				if (args.get(0) instanceof INIntegerLiteralExpression)
				{
					INIntegerLiteralExpression num = (INIntegerLiteralExpression)args.get(0);
					errno = (int)num.value.value;
					offset = 2;
				}
				
				Object[] values = new Value[args.size() - offset];
				
				for (int p = offset; p < args.size(); p++)
				{
					values[p - offset] = args.get(p).eval(ctxt);
				}
				
				INStringLiteralExpression fmt = (INStringLiteralExpression)args.get(offset - 1);
				String format = fmt.value.value;
				String location = "";
				
				if (format.endsWith("$"))	// Add @OnFail location to output
				{
					 location = name.getLocation().toString();
					 format = format.substring(0, format.length() - 1);
				}
							
				errorList.add(new OnFailError(errno, String.format(format + location, values)));
			}
		}
		catch (ValueException e)
		{
			// Doesn't happen
		}
	}

	public static void setErrorList(List<OnFailError> errors)
	{
		errorList = errors;
	}
}
