/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	MaestroCheck is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	MaestroCheck is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with MaestroCheck. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

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
	private static final long serialVersionUID = 1L;
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
