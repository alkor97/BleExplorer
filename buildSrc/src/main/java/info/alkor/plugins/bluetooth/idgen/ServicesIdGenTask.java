package info.alkor.plugins.bluetooth.idgen;

import com.bluetooth.Service;
import com.sun.codemodel.JClassAlreadyExistsException;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.*;

public class ServicesIdGenTask extends AbstractIdGenTask<Service> {

    @Inject
    public ServicesIdGenTask(File path, String fullyQualifiedGeneratedClassName, File output) throws JAXBException, JClassAlreadyExistsException {
        super(path, Service.class, fullyQualifiedGeneratedClassName, output);
    }

    @Override
    protected String getName(Service instance) {
        return instance != null ? instance.getName() : null;
    }

    @Override
    protected String getUuid(Service instance) {
        return instance != null ? instance.getUuid() : null;
    }
}
