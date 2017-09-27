import org.eclipse.ceylon.ide.common.correct {
    joinDeclarationQuickFix
}
import org.eclipse.ceylon.ide.common.util {
    nodes
}

import org.eclipse.ceylon.ide.intellij.psi {
    CeylonFile
}

shared class JoinDeclarationIntention() extends AbstractIntention() {

    familyName => "Join declaratation";
    
    shared actual void checkAvailable(IdeaQuickFixData data, CeylonFile file, Integer offset) {
        value statement = nodes.findStatement(data.rootNode, data.node);
        joinDeclarationQuickFix.addJoinDeclarationProposal(data, statement);
    }
}
