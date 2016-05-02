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
import com.redhat.ceylon.ide.common.model {
    JavaClassFile
}
import com.redhat.ceylon.model.typechecker.model {
    Package
}

class IdeaJavaClassFile(
    PsiClass cls,
    String filename,
    String relativePath,
    String fullPath,
    Package pkg)
        extends JavaClassFile<Module,VirtualFile,VirtualFile,PsiClass,PsiClass|PsiMethod>
        (cls, filename, relativePath, fullPath, pkg)
        satisfies IdeaJavaModelAware {
    
    javaClassRootToNativeFile(PsiClass javaClassRoot)
        => javaClassRoot.containingFile.virtualFile;
    
    javaClassRootToNativeRootFolder(PsiClass javaClassRoot)
        => javaClassRoot.containingFile.virtualFile.parent;
}
