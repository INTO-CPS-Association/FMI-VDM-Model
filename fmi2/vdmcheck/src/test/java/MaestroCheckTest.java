import maestro.MaestroCheck;
import maestro.OnFailError;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;

/**
 * If you want to unit test this in Eclipse, you have to add the static-model
 * jar explicitly, rather than just adding the project to the JUnit classpath.
 * This is because Eclipse assumes that the project's target/classes folder will
 * contain the resources, rather than .generated.
 */
public class MaestroCheckTest
{
	@Test
	public void test() throws Exception
	{
		MaestroCheck checker = new MaestroCheck();

		List<OnFailError> errors = null;
				
		errors = checker.check(Paths.get("src", "test", "resources", "modelDescription.xml").toFile());
		Assert.assertEquals(2, errors.size());
		Assert.assertEquals(1029, errors.get(0).errno);

		errors = checker.check(Paths.get("src", "test", "resources", "sources", "buildDescription.xml").toFile());
		Assert.assertEquals(0, errors.size());

		errors = checker.check(Paths.get("src", "test", "resources", "icons", "terminalsAndIcons.xml").toFile());
		Assert.assertEquals(0, errors.size());

		errors = checker.check(Paths.get("src", "test", "resources", "noSuchFile.xml").toFile());
		Assert.assertEquals(1, errors.size());
		Assert.assertEquals(0, errors.get(0).errno);
		System.err.println(errors.get(0).message);
		Assert.assertEquals(
			"XML validation: src/test/resources/noSuchFile.xml (No such file or directory)",
			errors.get(0).message);
	}
}
