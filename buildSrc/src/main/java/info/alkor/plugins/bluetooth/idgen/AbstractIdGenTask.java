package info.alkor.plugins.bluetooth.idgen;

import com.sun.codemodel.JClassAlreadyExistsException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;

abstract class AbstractIdGenTask<E> extends DefaultTask {

    private final File sourcePath;
    private final Class<E> clazz;
    private final JAXBContext context;
    private final Unmarshaller unmarshaller;
    private final Generator generator;

    AbstractIdGenTask(File path, Class<E> clazz, String fullyQualifiedGeneratedClassName, File output) throws JAXBException, JClassAlreadyExistsException {
        this.sourcePath = path;
        this.clazz = clazz;
        this.context = JAXBContext.newInstance(clazz);
        this.unmarshaller = context.createUnmarshaller();
        this.generator = new Generator(output);
        generator.beginEnum(fullyQualifiedGeneratedClassName);
    }

    @TaskAction
    public void run() throws IOException, JAXBException {
        if (sourcePath.isFile()) {
            processFile(sourcePath);
        } else if (sourcePath.isDirectory()) {
            final File[] files = sourcePath.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.canRead()) {
                        processFile(f);
                    }
                }
            }
        }
        generator.finalizeEnum();
    }

    private void processFile(File f) throws IOException, JAXBException {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(f))) {
            final E instance = parse(stream, clazz);
            generator.addMapping(getName(instance), getUuid(instance), clean(getAbstract(instance)));
            System.out.printf("\t%s: %s\n", getName(instance), getUuid(instance));
        }
    }

    private E parse(InputStream stream, Class<E> clazz) throws JAXBException {
        Object o = unmarshaller.unmarshal(stream);
        return o != null ? (E) o : null;
    }

    protected abstract String getName(E instance);

    protected abstract String getUuid(E instance);

    protected abstract String getAbstract(E instance);

    private String clean(String text) {
        if (text == null) {
            return null;
        }

        return text.replaceAll("\\s+", " ").trim();
    }
}
