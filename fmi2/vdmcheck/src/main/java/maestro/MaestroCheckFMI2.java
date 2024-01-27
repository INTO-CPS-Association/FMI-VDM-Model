/******************************************************************************
 *
 *	Copyright (c) 2017-2024, INTO-CPS Association,
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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.config.Properties;
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
import org.xml.sax.SAXException;
import types.Type;
import vdmcheck.annotations.in.INOnFailAnnotation;
import xsd2vdm.Xsd2VDM;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MaestroCheckFMI2
{
	/**
	 * Main class for testing. Maestro will use the default constructor.
	 */
	public static void main(String[] args) throws Exception
	{
		MaestroCheckFMI2 checker = new MaestroCheckFMI2();

		for (OnFailError err : checker.check(new File(args[0]), new File(args[1]), new File(args[2])))
		{
			System.out.println(err.errno + " => " + err.message);
		}
	}

	static
	{
		final String key = "vdmj.mapping.search_path";
		String searchPath = System.getProperty(key);
		
		if (searchPath == null)
		{
			System.setProperty(key, "/annotations");
		}
		else
		{
			System.setProperty(key, searchPath + File.pathSeparator + "/annotations");
		}
	}

	/**
	 * Run model checks on the XML passed as a stream. This can be a
	 * modelDescription.xml, buildDescription.xml or terminalsAndIcons.xml
	 * source.
	 */
	public List<OnFailError> check(InputStream xmlStream) throws Exception
	{
		File xmlfile = Files.createTempFile("fmi", ".xml", new FileAttribute[0]).toFile();
		xmlfile.deleteOnExit();
		String varName = copyStream(xmlStream, xmlfile.getParentFile(), xmlfile.getName());
		
		switch (varName)
		{
			case "modelDescription":
				return check(xmlfile, null, null);
				
			case "buildDescription":
				return check(null, xmlfile, null);
				
			case "terminalsAndIcons":
				return check(null, null, xmlfile);
				
			default:
				throw new IOException("Unknown XML content");
		}
	}

	/**
	 * Run model checks on the XML passed as a file. This can be a
	 * modelDescription.xml, buildDescription.xml or terminalsAndIcons.xml
	 * source.
	 */
	public List<OnFailError> check(File modelFile, File buildFile, File termsFile) throws Exception
	{
		List<OnFailError> errors = new Vector<>();

		File maestro = Files.createTempDirectory("maestro", new FileAttribute[0]).toFile();
		maestro.deleteOnExit();

		File fmi2schema = new File(maestro, "fmi2schema");
		fmi2schema.mkdir();
		fmi2schema.deleteOnExit();

		copyResources(maestro,
			"/fmi2schema/fmi2Annotation.xsd",
			"/fmi2schema/fmi2AttributeGroups.xsd",
			"/fmi2schema/fmi2ModelDescription.xsd",
			"/fmi2schema/fmi2ScalarVariable.xsd",
			"/fmi2schema/fmi2Type.xsd",
			"/fmi2schema/fmi2Unit.xsd",
			"/fmi2schema/fmi2VariableDependency.xsd",
			"/fmi2schema/fmi2.xsd",
			"/fmi2schema/fmi3Annotation.xsd",
			"/fmi2schema/fmi3BuildDescription.xsd",
			"/fmi2schema/fmi3TerminalsAndIcons.xsd",
			"/fmi2schema/fmi3Terminal.xsd",
			"/fmi2schema/xsd2vdm.properties");

		File xsdFile = new File(fmi2schema, "fmi2.xsd");
		validate(modelFile, xsdFile, errors);
		validate(buildFile, xsdFile, errors);
		validate(termsFile, xsdFile, errors);

		if (errors.isEmpty())
		{
			File vdmsl = new File(maestro, "vdmsl");
			vdmsl.mkdir();
			vdmsl.deleteOnExit();

			File vdmFile = new File(vdmsl, "model.vdmsl");
			vdmFile.deleteOnExit();

			Xsd2VDM converter = new Xsd2VDM();
			Xsd2VDM.loadProperties(xsdFile);
			Map<String, Type> vdmSchema = converter.createVDMSchema(xsdFile, null, false, true);
			
			if (modelFile != null)
			{
				converter.createVDMValue(vdmSchema, vdmFile, modelFile, "modelDescription");
			}
			else
			{
				missingVariable("modelDescription", vdmFile);
			}
			
			if (buildFile != null)
			{
				converter.createVDMValue(vdmSchema, vdmFile, buildFile, "buildDescription", true);
			}
			else
			{
				missingVariable("buildDescription", vdmFile);
			}
			
			if (termsFile != null)
			{
				converter.createVDMValue(vdmSchema, vdmFile, termsFile, "terminalsAndIcons", true);
			}
			else
			{
				missingVariable("terminalsAndIcons", vdmFile);
			}

			if (vdmFile.exists()) // Means successful?
			{
				File frm = new File(vdmsl, "fmi2-rule-model");
				frm.deleteOnExit();
				frm.mkdir();
				
				File rules = new File(frm, "Rules");
				rules.deleteOnExit();
				rules.mkdir();
				
				copyResources(vdmsl,
					"/fmi2-rule-model/Rules/BuildConfiguration.adoc",
					"/fmi2-rule-model/Rules/CoSimulation.adoc",
					"/fmi2-rule-model/Rules/DefaultExperiment.adoc",
					"/fmi2-rule-model/Rules/FMI2Rules.adoc",
					"/fmi2-rule-model/Rules/FmiModelDescription.adoc",
					"/fmi2-rule-model/Rules/LogCategories.adoc",
					"/fmi2-rule-model/Rules/ModelExchange.adoc",
					"/fmi2-rule-model/Rules/ModelStructure.adoc",
					"/fmi2-rule-model/Rules/ModelVariables.adoc",
					"/fmi2-rule-model/Rules/Terminals.adoc",
					"/fmi2-rule-model/Rules/TypeDefinitions.adoc",
					"/fmi2-rule-model/Rules/UnitDefinitions.adoc",
					
					"/fmi2-rule-model/Annotations.vdmsl",
					"/fmi2-rule-model/BuildConfiguration.vdmsl",
					"/fmi2-rule-model/Common.vdmsl",
					"/fmi2-rule-model/CoSimulation.vdmsl",
					"/fmi2-rule-model/DefaultExperiment.vdmsl",
					"/fmi2-rule-model/EffectiveVariables.vdmsl",
					"/fmi2-rule-model/FMI2Schema.vdmsl",
					"/fmi2-rule-model/FMIModelDescription.vdmsl",
					"/fmi2-rule-model/InvariantSupport.vdmsl",
					"/fmi2-rule-model/LogCategories.vdmsl",
					"/fmi2-rule-model/ModelExchange.vdmsl",
					"/fmi2-rule-model/ModelStructure.vdmsl",
					"/fmi2-rule-model/ModelVariables.vdmsl",
					"/fmi2-rule-model/Support.vdmsl",
					"/fmi2-rule-model/Terminals.vdmsl",
					"/fmi2-rule-model/TypeDefinitions.vdmsl",
					"/fmi2-rule-model/UnitDefinitions.vdmsl",
					"/fmi2-rule-model/Validation.vdmsl",
					"/fmi2-rule-model/VariableNaming.vdmsl",
					"/fmi2-rule-model/XSD.vdmsl");

				Properties.init();
				Settings.annotations = true;
				ASTModuleList ast = new ASTModuleList();
				
				boolean saved = Properties.parser_merge_comments;
				Properties.parser_merge_comments = true;
				readDirectory(ast, vdmsl, errors);
				Properties.parser_merge_comments = saved;

				if (!errors.isEmpty())
				{
					errors.add(new OnFailError(1, "Syntax errors in VDMSL?"));
				}
				else
				{
					PrintStream dummy = new PrintStream(new ByteArrayOutputStream());
					ClassMapper instance = ClassMapper.getInstance(TCNode.MAPPINGS, dummy);
					TCModuleList tc = instance.init().convert(ast);
					tc.combineDefaults();
					TypeChecker tchecker = new ModuleTypeChecker(tc);
					tchecker.typeCheck();

					if (TypeChecker.getErrorCount() > 0)
					{
						for (VDMError err : TypeChecker.getErrors())
						{
							errors.add(new OnFailError(err.number, err.message));
						}

						errors.add(new OnFailError(2, "Type errors in VDMSL?"));
					}

					ClassMapper classMapper = ClassMapper.getInstance(INNode.MAPPINGS, dummy);
					INModuleList in = classMapper.init().convert(tc);
					Interpreter interpreter = new ModuleInterpreter(in, tc);
					interpreter.init();
					INOnFailAnnotation.setErrorList(errors);
					Value result = interpreter.execute("isValidFMIConfigurations(modelDescription, buildDescription, terminalsAndIcons)");

					if (!result.boolValue(null))
					{
						errors.add(new OnFailError(3, "Errors found"));
					}
				}
			}
		}

		return errors;
	}

	private void readDirectory(ASTModuleList ast, File dir, List<OnFailError> errors)
	{
		for (File sl : dir.listFiles())
		{
			if (sl.isDirectory())
			{
				readDirectory(ast, sl, errors);
			}
			else
			{
				LexTokenReader ltr = new LexTokenReader(sl, Dialect.VDM_SL);
				ModuleReader mreader = new ModuleReader(ltr);
				ast.addAll(mreader.readModules());

				for (VDMError err : mreader.getErrors())
				{
					errors.add(new OnFailError(err.number, err.message));
				}
			}
		}
	}

	private void validate(File xml, File xsd, List<OnFailError> errors)
	{
		if (xml == null)
		{
			return;	// Missing file
		}
		
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
		}
		catch (SAXException e)
		{
			errors.add(new OnFailError(0, "XML validation: " + e));
		}
		catch (Exception e)
		{
			errors.add(new OnFailError(0, "XML validation: " + e.getMessage()));
		}
	}

	private void copyResources(File target, String... resources) throws Exception
	{
		if (!target.exists())
		{
			throw new Exception("Target of copyResources does not exist: " + target);
		}

		for (String resource : resources)
		{
			InputStream is = MaestroCheckFMI2.class.getResourceAsStream(resource);

			if (is == null)
			{
				throw new Exception("Cannot load resource " + resource);
			}

			copyStream(is, target, resource);
			is.close();
		}
	}

	private String copyStream(InputStream data, File target, String file) throws IOException
	{
		File targetFile = new File(target.getAbsolutePath() + file);
		targetFile.deleteOnExit(); // Note! All files temporary
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(baos);

		while (data.available() > 0)
		{
			bos.write(data.read());
		}

		bos.close();
		
		String xmlContent = baos.toString("UTF-8");
		String varName = null;
		
		if (xmlContent.contains("<fmiModelDescription"))
		{
			varName = "modelDescription";
		}
		else if (xmlContent.contains("<fmiBuildDescription"))
		{
			varName = "buildDescription";
		}
		else if (xmlContent.contains("<fmiTerminalsAndIcons"))
		{
			varName = "terminalsAndIcons";
		}
		
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(targetFile));
		fos.write(xmlContent.getBytes("UTF-8"));
		fos.close();

		return varName;
	}
	
	private void missingVariable(String varName, File vdmFile) throws IOException
	{
		PrintStream output = new PrintStream(new FileOutputStream(vdmFile, true));
		
		output.println("/**");
		output.println(" * VDM value missing");
		output.println(" */");
		
		output.println("values");
		output.println("    " + varName + " = nil;\n");
		output.println("\n");
		
		output.close();
	}
}
