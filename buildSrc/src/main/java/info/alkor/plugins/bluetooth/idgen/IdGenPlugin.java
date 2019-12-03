package info.alkor.plugins.bluetooth.idgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

public class IdGenPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        final File projectDir = project.getProjectDir();
        final File servicesDir = new File(projectDir, "src/main/services");
        final File characteristicsDir = new File(projectDir, "src/main/characteristics");

        final File outputDir = new File(project.getBuildDir(), "my-generated");
        outputDir.mkdirs();

        project.getTasks().create("genServicesIds", ServicesIdGenTask.class, servicesDir, "com.bluetooth.tools.Service", outputDir);
        project.getTasks().create("genCharacteristicsIds", CharacteristicsIdGenTask.class, characteristicsDir, "com.bluetooth.tools.Characteristic", outputDir);
    }
}
