import maestro.MaestroCheck;
import maestro.OnFailError;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;

public class MaestroCheckTest {
    @Test
    public void test() throws Exception {

        MaestroCheck checker = new MaestroCheck();

        List<OnFailError> errors = checker.check(Paths.get("src", "test", "resources", "modelDescription.xml").toFile());
        Assert.assertEquals(2, errors.size());
        Assert.assertEquals(1029, errors.get(0).errno);
    }
}
