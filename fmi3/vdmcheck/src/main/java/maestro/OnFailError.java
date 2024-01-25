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

package maestro;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;

/**
 * Object for returning number/message pairs from @OnFail messages.
 */
public class OnFailError
{
	public final int errno;
	public final String message;
	public final INAnnotationList doclinks;
	
	public OnFailError(int errno, String message, INAnnotationList doclinks)
	{
		this.errno = errno;
		this.message = message;
		this.doclinks = doclinks;
	}
	
	public OnFailError(int errno, String message)
	{
		this(errno, message, null);
	}
	
	@Override
	public String toString()
	{
		return errno + ": " + message;
	}
	
	public List<String> getDocLinks()
	{
		List<String> links = null;
		
		if (doclinks != null)
		{
			links = new Vector<String>();
			
			for (INAnnotation doclink: doclinks)
			{
				for (INExpression arg: doclink.args)
				{
					INStringLiteralExpression s = (INStringLiteralExpression) arg;
					links.add(s.value.value);
				}
			}
		}
		
		return links;
	}
}


