import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import maestro.MaestroCheckFMI2;
import maestro.OnFailError;

/**
 * If you want to unit test this in Eclipse, you have to add the rule-model
 * jar explicitly, rather than just adding the project to the JUnit classpath.
 * This is because Eclipse assumes that the project's target/classes folder will
 * contain the resources, rather than .generated.
 */
public class MaestroCheckFMI2Test
{
	@Test
	public void test() throws Exception
	{
		MaestroCheckFMI2 checker = new MaestroCheckFMI2();

		List<OnFailError> errors = null;
		
		File model = Paths.get("src", "test", "resources", "modelDescription.xml").toFile();
		File build = Paths.get("src", "test", "resources", "sources", "buildDescription.xml").toFile();
		File terms = Paths.get("src", "test", "resources", "icons", "terminalsAndIcons.xml").toFile();
		File nosuch = Paths.get("src", "test", "resources", "noSuchFile.xml").toFile();
				
		errors = checker.check(model, null, null);
		Assert.assertEquals(2, errors.size());
		Assert.assertEquals(
				"validInitialUnknowns: InitialUnknowns must include: {3, 4}",
				errors.get(0).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(
				"<FMI2_STANDARD>#_definition_of_the_model_structure_modelstructure\n",
				errors.get(0).doclinks.get(0).toString());

		errors = checker.check(null, build, null);
		Assert.assertEquals(0, errors.size());

		errors = checker.check(model, null, terms);
		Assert.assertEquals(6, errors.size());
		Assert.assertEquals(
				"validTerminalMemberVariables: Terminal member variable \"vname\" not declared at terminalsAndIcons.xml:15",
				errors.get(1).message);
		Assert.assertTrue(errors.get(1).doclinks != null);
		Assert.assertEquals(
				"<FMI2_STANDARD>#section-terminalvars\n",
				errors.get(1).doclinks.get(0).toString());

		errors = checker.check(nosuch, null, null);
		Assert.assertEquals(1, errors.size());
		Assert.assertEquals(0, errors.get(0).errno);
		System.err.println(errors.get(0).message);
		Assert.assertTrue(
			// Linux systems
			errors.get(0).message.equals("XML validation: src/test/resources/noSuchFile.xml (No such file or directory)")
			// Windows systems
		||	errors.get(0).message.equals("XML validation: src\\test\\resources\\noSuchFile.xml (The system cannot find the file specified)")
		);
	}
}
