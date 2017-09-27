import com.intellij.codeInsight.editorActions.moveLeftRight {
    MoveElementLeftRightHandler
}
import com.intellij.psi {
    PsiElement
}

import org.eclipse.ceylon.ide.intellij.psi {
    CeylonPsi
}

shared class CeylonMoveLeftRightHandler() extends MoveElementLeftRightHandler() {

    getMovableSubElements(PsiElement element)
            => if (element is CeylonPsi.PositionalArgumentListPsi
                            | CeylonPsi.SequencedArgumentPsi
                            | CeylonPsi.ParameterListPsi
                            | CeylonPsi.ImportMemberOrTypeListPsi)
            then element.children
            else PsiElement.emptyArray;
}
