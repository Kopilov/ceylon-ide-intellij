import com.intellij.openapi.\imodule {
    Module
}
import com.intellij.openapi.vfs {
    VirtualFile
}
import com.intellij.psi {
    PsiClass,
    PsiMethod
}
import org.eclipse.ceylon.ide.common.model {
    CrossProjectBinaryUnit
}
import org.eclipse.ceylon.model.typechecker.model {
    Package
}

shared class IdeaCrossProjectBinaryUnit(PsiClass cls,
    String filename,
    String relativePath,
    String fullPath,
    Package pkg)
        extends CrossProjectBinaryUnit<Module,VirtualFile,VirtualFile,VirtualFile,PsiClass,PsiClass|PsiMethod>
        (cls, filename, relativePath, fullPath, pkg)
        satisfies IdeaJavaModelAware {
    
}
