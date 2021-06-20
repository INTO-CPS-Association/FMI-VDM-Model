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

package maestro;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.values.Value;

import annotations.in.INOnFailAnnotation;
import types.Type;
import xsd2vdm.Xsd2VDM;

public class MaestroCheck
{
	/**
	 * Main class for testing. Maestro will use the default constructor.
	 */
	public static void main(String[] args) throws Exception
	{
		MaestroCheck checker = new MaestroCheck();
		
		for (OnFailError err: checker.check(new File(args[0])))
		{
			System.out.println(err);
		}
	}
	
	public List<OnFailError> check(File modelDescriptionFile) throws Exception
	{
		List<OnFailError> errors = new Vector<>();

		File fmi2 = Files.createTempDirectory("fmi2", new FileAttribute[0]).toFile();
		fmi2.deleteOnExit();
		
		File schema = new File(fmi2, "schema");
		schema.mkdir();
		schema.deleteOnExit();
		
		copyResources(fmi2,
			"/schema/fmi2Annotation.xsd",
			"/schema/fmi2AttributeGroups.xsd",
			"/schema/fmi2ModelDescription.xsd",
			"/schema/fmi2ScalarVariable.xsd",
			"/schema/fmi2Type.xsd",
			"/schema/fmi2Unit.xsd",
			"/schema/fmi2VariableDependency.xsd",
			"/schema/xsd2vdm.properties");
		
		File xsdFile = new File(schema, "fmi2ModelDescription.xsd");
		OnFailError validation = validate(modelDescriptionFile, xsdFile);
		
		if (validation != null)
		{
			errors.add(validation);
		}
		else
		{
			File vdmsl = new File(fmi2, "vdmsl");
			vdmsl.mkdir();
			vdmsl.deleteOnExit();
			
			File vdmFile = new File(vdmsl, "model.vdmsl");
			vdmFile.deleteOnExit();

			Xsd2VDM converter = new Xsd2VDM();
			Xsd2VDM.loadProperties(xsdFile);
			Map<String, Type> vdmSchema = converter.createVDMSchema(xsdFile, modelDescriptionFile, false, true);
			converter.createVDMValue(vdmSchema, vdmFile, modelDescriptionFile, "model");
			
			if (vdmFile.exists())	// Means successful?
			{
				copyResources(vdmsl,
					"/CoSimulation_4.3.1.vdmsl",
					"/DefaultExperiment_2.2.5.vdmsl",
					"/FMI2Schema.vdmsl",
					"/FMIModelDescription_2.2.1.vdmsl",
					"/LogCategories_2.2.4.vdmsl",
					"/Misc.vdmsl",
					"/ModelExchange_3.3.1.vdmsl",
					"/ModelStructure_2.2.8.vdmsl",
					"/ModelVariables_2.2.7.vdmsl",
					"/TypeDefinitions_2.2.3.vdmsl",
					"/UnitDefinitions_2.2.2.vdmsl",
					"/VariableNaming_2.2.9.vdmsl",
					"/VendorAnnotations_2.2.6.vdmsl");
				
				Settings.annotations = true;
				ASTModuleList ast = new ASTModuleList();
				
				for (File sl: vdmsl.listFiles())
				{
					LexTokenReader ltr = new LexTokenReader(sl, Dialect.VDM_SL);
					ModuleReader mreader = new ModuleReader(ltr);
					ast.addAll(mreader.readModules());
					
					for (VDMError err: mreader.getErrors())
					{
						errors.add(new OnFailError(err.number, err.message));
					}
				}
				
				if (!errors.isEmpty())
				{
					errors.add(new OnFailError(1, "Syntax errors in VDMSL?"));
				}
				else
				{
					TCModuleList tc = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(ast);
					tc.combineDefaults();
					TypeChecker tchecker = new ModuleTypeChecker(tc);
					tchecker.typeCheck();
					
					if (TypeChecker.getErrorCount() > 0)
					{
						for (VDMError err: TypeChecker.getErrors())
						{
							errors.add(new OnFailError(err.number, err.message));
						}

						errors.add(new OnFailError(2, "Type errors in VDMSL?"));
					}
					
					INModuleList in = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tc);
					Interpreter interpreter = new ModuleInterpreter(in, tc);
					interpreter.init();
					INOnFailAnnotation.setErrorList(errors);
					Value result = interpreter.execute("isValidFMIModelDescription(model)");
					
					if (!result.boolValue(null))
					{
						errors.add(new OnFailError(3, "Errors found"));
					}
				}
			}
		}
		
		return errors;
	}
	
	private OnFailError validate(File xml, File xsd)
	{
		try
		{
			// Note that we pass a stream to allow the validator to determine the
			// encoding, rather than passing a File, which seems to use default encoding.
			Source xmlFile = new StreamSource(new FileInputStream(xml));
			xmlFile.setSystemId(xml.toURI().toASCIIString());
			Source xsdFile = new StreamSource(xsd);
			xsdFile.setSystemId(xsd.toURI().toASCIIString());
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			Schema schema = schemaFactory.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
			
			return null;
		}
		catch (SAXException e)
		{
			return new OnFailError(0, "XML validation: " + e);		// Raw exception gives file/line/col
		}
		catch (Exception e)
		{
			return new OnFailError(0, "XML validation: " + e.getMessage());
		}
	}

	private void copyResources(File target, String... resources) throws Exception
	{
		if (!target.exists())
		{
			throw new Exception("Target of copyResources does not exist: " + target);
		}
		
		for (String resource: resources)
		{
			InputStream is = MaestroCheck.class.getResourceAsStream(resource);
			
			if (is == null)
			{
				throw new Exception("Cannot load resource " + resource);
			}
			
			copyStream(is, target, resource);
			is.close();
		}
	}

	private void copyStream(InputStream data, File target, String file) throws IOException
	{
		File targetFile = new File(target, file);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
		
		while (data.available() > 0)
		{
			bos.write(data.read());
		}
		
		bos.close();
		targetFile.deleteOnExit();		// Note! All files temporary
	}
}
