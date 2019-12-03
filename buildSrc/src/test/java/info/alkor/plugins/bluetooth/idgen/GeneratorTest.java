package info.alkor.plugins.bluetooth.idgen;

import com.sun.codemodel.JClassAlreadyExistsException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class GeneratorTest {

    @Test
    public void test() throws JClassAlreadyExistsException, IOException {
        final File output = new File(new File(".", "build"), "my-generated");

        Generator generator = new Generator(output);
        generator.beginEnum("com.bluetooth.Services");
        generator.addMapping("Battery Service", "180F");

        if (!output.exists()) {
            assertTrue(output.mkdirs());
        }
        generator.finalizeEnum(output);
    }
}
