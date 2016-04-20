import com.intellij.openapi.application {
    Result
}
import com.intellij.openapi.command {
    WriteCommandAction
}
import com.intellij.openapi.editor {
    Editor
}
import com.intellij.psi {
    PsiElement,
    PsiFile,
    PsiNamedElement
}
import com.intellij.refactoring.rename.inplace {
    VariableInplaceRenameHandler,
    VariableInplaceRenamer
}

import org.intellij.plugins.ceylon.ide.ceylonCode.psi {
    CeylonFile
}
import org.intellij.plugins.ceylon.ide.ceylonCode.psi.impl {
    DeclarationPsiNameIdOwner
}

shared class CeylonVariableRenameHandler() extends VariableInplaceRenameHandler() {

    shared actual VariableInplaceRenamer createRenamer(PsiElement elementToRename, Editor editor) {
        value file = elementToRename.containingFile;

        assert(is PsiNamedElement elementToRename);

        return object extends VariableInplaceRenamer(elementToRename, editor) {
            shared actual void finish(Boolean success) {
                super.finish(success);

                if (success, is CeylonFile file) {
                    object extends WriteCommandAction<Nothing>(myProject, file) {
                        shared actual void run(Result<Nothing> result) {
                            file.forceReparse();
                        }
                    }.execute();
                }
            }
        };
    }

    shared actual Boolean isAvailable(PsiElement? element, Editor editor, PsiFile file) {
        if (exists context = file.findElementAt(editor.caretModel.offset),
            exists element,
            context.containingFile != element.containingFile) {

            return false;
        }

        return element is DeclarationPsiNameIdOwner;
    }
}
