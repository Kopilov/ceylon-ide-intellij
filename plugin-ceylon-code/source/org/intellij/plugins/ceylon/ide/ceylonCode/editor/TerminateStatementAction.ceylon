import com.intellij.codeInsight.editorActions.smartEnter {
    SmartEnterProcessor
}
import com.intellij.openapi.editor {
    Editor
}
import com.intellij.openapi.project {
    Project
}
import com.intellij.psi {
    PsiFile
}
import com.redhat.ceylon.compiler.typechecker.parser {
    CeylonParser,
    CeylonLexer
}
import com.redhat.ceylon.compiler.typechecker.tree {
    Tree
}
import com.redhat.ceylon.ide.common.editor {
    AbstractTerminateStatementAction
}
import com.redhat.ceylon.ide.common.util {
    unsafeCast
}

import java.util {
    List
}

import org.antlr.runtime {
    CommonToken,
    ANTLRStringStream,
    CommonTokenStream
}
import org.intellij.plugins.ceylon.ide.ceylonCode.correct {
    DocumentWrapper
}

shared class TerminateStatementAction() extends SmartEnterProcessor() {

    shared actual Boolean process(Project? project, Editor editor, PsiFile? psiFile) {
        value line = editor.document.getLineNumber(editor.caretModel.offset);
        value handler = object extends AbstractTerminateStatementAction<DocumentWrapper>() {
            shared actual [Tree.CompilationUnit, List<CommonToken>] parse(DocumentWrapper doc) {
                value stream = ANTLRStringStream(doc.doc.text);
                value lexer = CeylonLexer(stream);
                value tokenStream = CommonTokenStream(lexer);
                value parser = CeylonParser(tokenStream);
                value cu = parser.compilationUnit();
                value toks = unsafeCast<List<CommonToken>>(tokenStream.tokens);

                return [cu, toks];
            }
        };

        if (exists reg = handler.terminateStatement(DocumentWrapper(editor.document), line)) {
            editor.caretModel.moveToOffset(reg.start);
        }
        
        return true;
    }
}
