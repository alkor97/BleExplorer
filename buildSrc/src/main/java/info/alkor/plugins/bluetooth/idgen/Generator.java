package info.alkor.plugins.bluetooth.idgen;

import com.sun.codemodel.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class Generator {

    private final File output;
    private final JCodeModel model = new JCodeModel();
    private final Set<String> uniqueConstants = new HashSet<>();

    private JDefinedClass definedClass;

    Generator(File output) {
        this.output = output;
    }

    void beginEnum(String className) throws JClassAlreadyExistsException {
        definedClass = model._class(className, ClassType.ENUM);
    }

    void addMapping(String name, String shortUuid) {
        final String baseName = name.toUpperCase().replaceAll("[\\s-()]+", "_");
        String constantName = baseName;

        int i = 1;
        while (uniqueConstants.contains(constantName)) {
            constantName = baseName + "_" + (i++);
        }

        final JEnumConstant constant = definedClass.enumConstant(constantName);
        constant.arg(JExpr.lit(name)).arg(JExpr.lit(shortUuid));

        uniqueConstants.add(constantName);
    }

    void finalizeEnum() throws IOException {
        finalizeEnum(output);
    }

    void finalizeEnum(File f) throws IOException {
        // public final String fullName;definedClass.field(JMod.PUBLIC | JMod.FINAL, clazz, name);
        final JFieldVar fullName = definedClass.field(JMod.PUBLIC | JMod.FINAL, String.class, "fullName");
        // public final String shortUuidString;
        final JFieldVar shortUuidString = definedClass.field(JMod.PUBLIC | JMod.FINAL, String.class, "shortUuidString");
        // public final String uuidString
        final JFieldVar uuidString = definedClass.field(JMod.PUBLIC | JMod.FINAL, String.class, "uuidString");
        // public final UUID uuid;
        final JFieldVar uuid = definedClass.field(JMod.PUBLIC | JMod.FINAL, UUID.class, "uuid");

        // (constructor)(String fullName, String shortUuidString) {
        final JMethod constructor = definedClass.constructor(JMod.NONE);
        final JVar paramFullName = constructor.param(String.class, "fullName");
        final JVar paramShortUuidString = constructor.param(String.class, "shortUuidString");

        {
            final JBlock block = constructor.body();

            // this.fullName = fullName;
            block.assign(JExpr._this().ref(fullName), paramFullName);
            // this.shortUuid = shortUuid;
            block.assign(JExpr._this().ref(shortUuidString), paramShortUuidString);

            final JClass stringClass = model.ref(String.class);
            // this.uuidString = String.format("%1$8s-0000-1000-8000-00805f9b34fb", shortUuidString).replace(' ', '0');
            block.assign(JExpr._this().ref(uuidString),
                    stringClass.staticInvoke("format").arg("%1$8s-0000-1000-8000-00805f9b34fb").arg(paramShortUuidString)
                            .invoke("replace").arg(JExpr.lit(' ')).arg(JExpr.lit('0')));

            // this.uuid = UUID.fromString(this.uuidString);
            final JClass uuidClass = model.ref(UUID.class);
            block.assign(JExpr._this().ref(uuid), uuidClass.staticInvoke("fromString").arg(JExpr._this().ref(uuidString)));
        }
        // }

        // public static (class) fromUuid(UUID uuid) {
        final JMethod fromUuidMethod = definedClass.method(JMod.PUBLIC | JMod.STATIC, definedClass, "fromUuid");
        {
            final JVar paramUuid = fromUuidMethod.param(UUID.class, "uuid");
            final JBlock methodBody = fromUuidMethod.body();

            // for ((class) id : values()) {
            final JForEach forEach = methodBody.forEach(definedClass, "id", definedClass.staticInvoke("values"));
            final JBlock block = forEach.body();
            // if (id.uuid..equals(uuid)) return id;
            block._if(JExpr.ref(forEach.var(), "uuid").invoke("equals").arg(paramUuid))._then()._return(forEach.var());

            // return null;
            methodBody._return(JExpr._null());
        }
        // }

        // public static String getFullName(UUID uuid) {
        final JMethod getFullName = definedClass.method(JMod.PUBLIC | JMod.STATIC, String.class, "getFullName");
        {
            final JVar paramUuid = getFullName.param(UUID.class, "uuid");
            final JBlock methodBody = getFullName.body();

            // final (class) id = fromUuid(uuid);
            final JVar id = methodBody.decl(JMod.FINAL, definedClass, "id", definedClass.staticInvoke(fromUuidMethod).arg(paramUuid));

            // return id != null ? id.fullName : uuid.toString();
            methodBody._return(JOp.cond(id.ne(JExpr._null()), JExpr.ref(id, fullName), JExpr.invoke(paramUuid, "toString")));
        }

        model.build(f);
    }
}
