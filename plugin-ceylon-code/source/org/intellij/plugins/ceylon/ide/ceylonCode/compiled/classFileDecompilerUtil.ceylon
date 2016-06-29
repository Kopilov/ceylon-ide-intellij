import ceylon.interop.java {
    createJavaObjectArray
}

import com.intellij.ide.highlighter {
    JavaClassFileType
}
import com.intellij.openapi.util {
    Key,
    Ref
}
import com.intellij.openapi.vfs {
    VirtualFile
}

import org.intellij.plugins.ceylon.ide.ceylonCode.model {
    Annotations
}
import org.jetbrains.org.objectweb.asm {
    ClassReader {
        skipCode,
        skipDebug,
        skipFrames
    },
    ClassVisitor,
    Opcodes,
    AnnotationVisitor,
    Attribute
}

"Helper to determine if a .class file was generated by the Ceylon compiler."
shared object classFileDecompilerUtil {
    shared Key<CeylonBinaryData> ceylonBinaryDataKey = Key<CeylonBinaryData>("CEYLON_BINARY_DATA_KEY");

    shared Boolean isCeylonCompiledFile(VirtualFile file) {
        if (file.extension != JavaClassFileType.instance.defaultExtension) {
            return false;
        }
        if (!file.\iexists() || file.length == 0) {
            return false;
        }

        return getCeylonBinaryData(file).ceylonCompiledFile;
    }

    String ann(String className) => "L" + className.replace(".", "/") + ";";

    shared CeylonBinaryData getCeylonBinaryData(VirtualFile file) {
        if (exists userData = file.getUserData(ceylonBinaryDataKey),
            userData.timestamp == file.timeStamp) {

            return userData;
        }

        value isCeylon = Ref(false);
        value isIgnore = Ref(false);
        value isInner = Ref(false);
        value reader = ClassReader(file.contentsToByteArray());
        value className = reader.className;

        reader.accept(object extends ClassVisitor(Opcodes.asm5) {

            shared actual AnnotationVisitor? visitAnnotation(String desc, Boolean visible) {
                if (desc == ann(Annotations.ceylon.className)) {
                    isCeylon.set(true);
                }
                if (desc == ann(Annotations.ignore.className)) {
                    isIgnore.set(true);
                }
                return super.visitAnnotation(desc, visible);
            }

            visitOuterClass(String owner, String name, String desc) => isInner.set(true);

            shared actual void visitInnerClass(String name, String \iouter, String inner, Integer access) {
                if (className.equals(name)) {
                    isInner.set(true);
                }
            }
        }, createJavaObjectArray<Attribute>(empty), skipCode.or(skipDebug).or(skipFrames));

        value data = CeylonBinaryData(file.timeStamp, isCeylon.get(), isIgnore.get(), isInner.get());
        file.putUserData(ceylonBinaryDataKey, data);
        return data;
    }

    shared Boolean hasValidCeylonBinaryData(VirtualFile? file) {
        if (!exists file) {
            return false;
        }
        return file.getUserData(ceylonBinaryDataKey)?.ceylonBinary else false;
    }
}