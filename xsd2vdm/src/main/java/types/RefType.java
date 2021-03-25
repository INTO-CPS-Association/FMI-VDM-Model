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

import org.xml.sax.Locator;

import values.VDMValue;

/**
 * RefTypes are added to the "converted" map early as a placeholder for recursive
 * element definitions. They allow a type to be updated and referred to by a
 * consistent reference.
 */
public class RefType extends Type
{
	private Type ref;
	
	public RefType(Type ref)
	{
		this.ref = ref;
	}
	
	public void set(Type type)
	{
		this.ref = type;
	}

	public Type deref()
	{
		Type t = ref;
		
		while (t instanceof RefType)
		{
			t = ((RefType)t).ref;
		}
		
		return t;
	}
	
	@Override
	public boolean matches(Type type)
	{
		return ref.matches(type);
	}

	@Override
	protected String signature()
	{
		return ref.signature();
	}
	
	@Override
	public String toString()
	{
		return ref.toString();
	}
	
	@Override
	public int hashCode()
	{
		return ref.hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		return ref.equals(other);
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		return ref.valueOf(avalue, locator);
	}
}
