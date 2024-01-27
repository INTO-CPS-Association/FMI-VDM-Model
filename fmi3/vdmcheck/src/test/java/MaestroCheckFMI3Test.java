import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import maestro.MaestroCheckFMI3;
import maestro.OnFailError;

/**
 * If you want to unit test this in Eclipse, you have to add the rule-model
 * jar explicitly, rather than just adding the project to the JUnit classpath.
 * This is because Eclipse assumes that the project's target/classes folder will
 * contain the resources, rather than .generated.
 */
public class MaestroCheckFMI3Test
{
	@Test
	public void test() throws Exception
	{
		MaestroCheckFMI3 checker = new MaestroCheckFMI3();

		List<OnFailError> errors = null;
		File model = Paths.get("src", "test", "resources", "modelDescription.xml").toFile();
		File build = Paths.get("src", "test", "resources", "sources", "buildDescription.xml").toFile();
		File terms = Paths.get("src", "test", "resources", "terminalsAndIcons", "terminalsAndIcons.xml").toFile();
		File nosuch = Paths.get("src", "test", "resources", "noSuchFile.xml").toFile();
								
		errors = checker.check(model, null, null);
		Assert.assertEquals(35, errors.size());
		Assert.assertEquals(
				"validMEModelIdentifier: \"Test FMU\" not valid C variable name at modelDescription.xml:4",
				errors.get(0).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(1, errors.get(0).doclinks.size());
		Assert.assertEquals(
				"<FMI3_STANDARD>#modelIdentifier\n<FMI3_STANDARD>#header-files-and-naming-of-functions\n",
				errors.get(0).doclinks.get(0).toString());

		errors = checker.check(null, build, null);
		Assert.assertEquals(0, errors.size());

		errors = checker.check(model, null, terms);
		Assert.assertEquals(39, errors.size());
		Assert.assertEquals(
				"validTerminalMemberVariables: Terminal member variable \"vname\" not declared at terminalsAndIcons.xml:15",
				errors.get(34).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(1, errors.get(0).doclinks.size());
		Assert.assertEquals(
				"<FMI3_STANDARD>#section-terminalvars\n",
				errors.get(34).doclinks.get(0).toString());

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
