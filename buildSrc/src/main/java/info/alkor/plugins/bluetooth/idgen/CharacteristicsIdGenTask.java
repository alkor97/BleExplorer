package info.alkor.plugins.bluetooth.idgen;

import com.bluetooth.Characteristic;
import com.sun.codemodel.JClassAlreadyExistsException;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.File;

public class CharacteristicsIdGenTask extends AbstractIdGenTask<Characteristic> {

    @Inject
    public CharacteristicsIdGenTask(File path, String fullyQualifiedGeneratedClassName, File output) throws JAXBException, JClassAlreadyExistsException {
        super(path, Characteristic.class, fullyQualifiedGeneratedClassName, output);
    }

    @Override
    protected String getName(Characteristic instance) {
        return instance != null ? instance.getName() : null;
    }

    @Override
    protected String getUuid(Characteristic instance) {
        return instance != null ? instance.getUuid() : null;
    }
}
