import com.intellij.psi {
    PsiElement
}
import org.eclipse.ceylon.compiler.typechecker.tree {
    Node
}

import java.lang {
    Class,
    Types
}

import org.eclipse.ceylon.ide.intellij.psi {
    CeylonCompositeElement,
    CeylonPsiVisitor
}

shared class FindMatchingPsiNodeVisitor(Node node, klass)
        extends CeylonPsiVisitor(true) {

    Class<out CeylonCompositeElement> klass;

    variable CeylonCompositeElement? result = null;

    shared CeylonCompositeElement? psi => result;

    shared actual void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element.textRange.equalsToRange(node.startIndex.intValue(), node.endIndex.intValue()),
            klass.isAssignableFrom(Types.classForInstance(element))) {
            assert (is CeylonCompositeElement element);
            result = element;
        }
    }

}
