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

package fmureader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fujitsu.vdmj.lex.ExternalFormatReader;

import xsd2vdm.Xsd2VDM;

public class FMUReader implements ExternalFormatReader
{
	private File fmuFile;
	
	@Override
	public char[] getText(File fmuFile, String charset) throws IOException
	{
		this.fmuFile = fmuFile;
		
		String modelDescription = readFile("modelDescription.xml");
		String buildDescription = readFile("sources/buildDescription.xml");
		String terminalsAndIcons = readFile("terminalsAndIcons/terminalsAndIcons.xml");
		
		System.out.println(modelDescription);
		System.out.println(buildDescription);
		System.out.println(terminalsAndIcons);
		
//		Xsd2VDM converter = new Xsd2VDM();
//		converter.createVDMSchema(xsdFile, vdmFile, writeVDM, noWarn);
//		converter.createVDMValue(schema, vdmFile, xmlFile, varName);
		
		return null;
	}
	
	private String readFile(String xmlName) throws IOException
	{
		ZipInputStream zip = new ZipInputStream(new FileInputStream(fmuFile));
		ZipEntry ze = zip.getNextEntry();
		StringBuilder sb = new StringBuilder();
		InputStreamReader utf = new InputStreamReader(zip, "utf8");
		
		while (ze != null)
		{
			if (ze.getName().equals(xmlName))
			{
				int c = utf.read();
				
				while (c > 0)
				{
					sb.append((char)c);
					c = utf.read();
				}
			}
			
			ze = zip.getNextEntry();
		}
		
		utf.close();	// closes zip
		
		return sb.length() == 0 ? null : sb.toString();
	}
}
