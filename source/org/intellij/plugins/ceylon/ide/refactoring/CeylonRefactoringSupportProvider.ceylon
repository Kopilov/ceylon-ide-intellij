import com.intellij.lang.refactoring {
    RefactoringSupportProvider
}
import com.intellij.psi {
    PsiElement
}

import org.eclipse.ceylon.ide.intellij.refactoring {
    CeylonChangeSignatureHandler,
    ExtractFunctionHandler,
    ExtractValueHandler
}

shared class CeylonRefactoringSupportProvider() extends RefactoringSupportProvider() {

    isInplaceRenameAvailable(PsiElement element, PsiElement context)
            => false; // CeylonVariableRenameHandler handles this

    changeSignatureHandler => CeylonChangeSignatureHandler();

    introduceVariableHandler => ExtractValueHandler();

    extractMethodHandler => ExtractFunctionHandler();
}
