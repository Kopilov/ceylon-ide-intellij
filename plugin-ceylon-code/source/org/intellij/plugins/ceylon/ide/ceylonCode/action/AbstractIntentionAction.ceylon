import com.intellij.openapi.actionSystem {
    AnAction,
    AnActionEvent,
    CommonDataKeys
}
import com.intellij.openapi.application {
    Result
}
import com.intellij.openapi.command {
    WriteCommandAction
}

import org.intellij.plugins.ceylon.ide.ceylonCode.correct {
    AbstractIntention
}
import org.intellij.plugins.ceylon.ide.ceylonCode.psi {
    CeylonFile
}

"An action that wraps an [[AbstractIntention]]."
shared abstract class AbstractIntentionAction() extends AnAction() {
    
    shared actual void update(AnActionEvent evt) { 
        evt.presentation.enabled = evt.getData(CommonDataKeys.\iPSI_FILE) is CeylonFile;
    }
    
    shared actual void actionPerformed(AnActionEvent evt) {
        if (exists project = evt.project,
            exists editor = evt.getData(CommonDataKeys.\iEDITOR),
            is CeylonFile psiFile = evt.getData(CommonDataKeys.\iPSI_FILE)) {
            
            value intention = createIntention();
            
            if (intention.isAvailable(project, editor, psiFile)) {
                value p = project;
                value cn = commandName;
                
                object extends WriteCommandAction<Nothing>(p, cn) {
                    shared actual void run(Result<Nothing> result) {
                        intention.invoke(project, editor, psiFile);
                    }
                }.execute();
            }
        }
    }
    
    shared formal AbstractIntention createIntention();
    
    shared formal String commandName;
}