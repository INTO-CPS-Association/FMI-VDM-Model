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

public class MaestroCheckFMI3
{
	/**
	 * Main class for testing. Maestro will use the default constructor.
	 */
	public static void main(String[] args) throws Exception
	{
		MaestroCheckFMI3 checker = new MaestroCheckFMI3();

		for (OnFailError err : checker.check(new File(args[0])))
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
		copyStream(xmlStream, xmlfile.getParentFile(), xmlfile.getName());
		return check(xmlfile);
	}

	/**
	 * Run model checks on the XML passed as a file. This can be a
	 * modelDescription.xml, buildDescription.xml or terminalsAndIcons.xml
	 * source.
	 */
	public List<OnFailError> check(File xmlFile) throws Exception
	{
		List<OnFailError> errors = new Vector<>();

		File fmi3 = Files.createTempDirectory("fmi3", new FileAttribute[0]).toFile();
		fmi3.deleteOnExit();

		File fmi3schema = new File(fmi3, "fmi3schema");
		fmi3schema.mkdir();
		fmi3schema.deleteOnExit();

		copyResources(fmi3,
			"/fmi3schema/fmi3Annotation.xsd",
			"/fmi3schema/fmi3AttributeGroups.xsd",
			"/fmi3schema/fmi3BuildDescription.xsd",
			"/fmi3schema/fmi3InterfaceType.xsd",
			"/fmi3schema/fmi3ModelDescription.xsd",
			"/fmi3schema/fmi3TerminalsAndIcons.xsd",
			"/fmi3schema/fmi3Terminal.xsd",
			"/fmi3schema/fmi3Type.xsd",
			"/fmi3schema/fmi3Unit.xsd",
			"/fmi3schema/fmi3VariableDependency.xsd",
			"/fmi3schema/fmi3Variable.xsd",
			"/fmi3schema/fmi3.xsd");

		File xsdFile = new File(fmi3schema, "fmi3.xsd");
		OnFailError validation = validate(xmlFile, xsdFile);

		if (validation != null)
		{
			errors.add(validation);
		}
		else
		{
			File vdmsl = new File(fmi3, "vdmsl");
			vdmsl.mkdir();
			vdmsl.deleteOnExit();

			File vdmFile = new File(vdmsl, "model.vdmsl");
			vdmFile.deleteOnExit();

			Xsd2VDM converter = new Xsd2VDM();
			Xsd2VDM.loadProperties(xsdFile);
			Map<String, Type> vdmSchema = converter.createVDMSchema(xsdFile, xmlFile, false, true);
			converter.createVDMValue(vdmSchema, vdmFile, xmlFile, "model");

			if (vdmFile.exists()) // Means successful?
			{
				copyResources(vdmsl,
					"/fmi3-rule-model/Rules/BuildConfiguration.adoc",
					"/fmi3-rule-model/Rules/CoSimulation.adoc",
					"/fmi3-rule-model/Rules/DefaultExperiment.adoc",
					"/fmi3-rule-model/Rules/FMI3Rules.adoc",
					"/fmi3-rule-model/Rules/FmiModelDescription.adoc",
					"/fmi3-rule-model/Rules/LogCategories.adoc",
					"/fmi3-rule-model/Rules/ModelExchange.adoc",
					"/fmi3-rule-model/Rules/ModelStructure.adoc",
					"/fmi3-rule-model/Rules/ModelVariables.adoc",
					"/fmi3-rule-model/Rules/ScheduledExecution.adoc",
					"/fmi3-rule-model/Rules/Terminals.adoc",
					"/fmi3-rule-model/Rules/TypeDefinitions.adoc",
					"/fmi3-rule-model/Rules/UnitDefinitions.adoc",
					"/fmi3-rule-model/Annotations.vdmsl",
					
					"/fmi3-rule-model/BuildConfiguration.vdmsl",
					"/fmi3-rule-model/Common.vdmsl",
					"/fmi3-rule-model/CoSimulation.vdmsl",
					"/fmi3-rule-model/DefaultExperiment.vdmsl",
					"/fmi3-rule-model/EffectiveVariables.vdmsl",
					"/fmi3-rule-model/FMI3Schema.vdmsl",
					"/fmi3-rule-model/FMIModelDescription.vdmsl",
					"/fmi3-rule-model/InvariantSupport.vdmsl",
					"/fmi3-rule-model/LogCategories.vdmsl",
					"/fmi3-rule-model/ModelExchange.vdmsl",
					"/fmi3-rule-model/ModelStructure.vdmsl",
					"/fmi3-rule-model/ModelVariables.vdmsl",
					"/fmi3-rule-model/ScheduledExecution.vdmsl",
					"/fmi3-rule-model/Support.vdmsl",
					"/fmi3-rule-model/Terminals.vdmsl",
					"/fmi3-rule-model/TypeDefinitions.vdmsl",
					"/fmi3-rule-model/UnitDefinitions.vdmsl",
					"/fmi3-rule-model/Validation.vdmsl",
					"/fmi3-rule-model/VariableNaming.vdmsl",
					"/fmi3-rule-model/XSD.vdmsl");

				Properties.init();
				Settings.annotations = true;
				ASTModuleList ast = new ASTModuleList();
				readDirectory(ast, vdmsl, errors);

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
					Value result = interpreter.execute("isValidFMIConfiguration(model)");

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
			return new OnFailError(0, "XML validation: " + e); // Raw exception
																// gives
																// file/line/col
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

		for (String resource : resources)
		{
			InputStream is = MaestroCheckFMI3.class.getResourceAsStream(resource);

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
		File targetFile = new File(target.getAbsolutePath() + file);
		targetFile.getParentFile().mkdirs();
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));

		while (data.available() > 0)
		{
			bos.write(data.read());
		}

		bos.close();
		targetFile.deleteOnExit(); // Note! All files temporary
	}
}
