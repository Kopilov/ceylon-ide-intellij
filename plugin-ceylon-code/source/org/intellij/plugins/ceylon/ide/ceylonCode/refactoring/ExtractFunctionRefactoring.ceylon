import ceylon.interop.java {
    javaClass,
    javaString
}

import com.intellij.openapi.application {
    Result
}
import com.intellij.openapi.command {
    WriteCommandAction
}
import com.intellij.openapi.editor {
    Editor
}
import com.intellij.openapi.project {
    Project
}
import com.intellij.openapi.util {
    TextRange,
    Pass,
    Condition
}
import com.intellij.psi {
    PsiFile,
    PsiDocumentManager,
    PsiElement
}
import com.intellij.refactoring {
    IntroduceTargetChooser
}
import com.intellij.util {
    Function
}
import com.redhat.ceylon.compiler.typechecker.tree {
    Visitor,
    Tree
}
import com.redhat.ceylon.ide.common.refactoring {
    createExtractFunctionRefactoring,
    DefaultRegion
}

import java.lang {
    JString=String
}
import java.util {
    List,
    ArrayList
}

import org.intellij.plugins.ceylon.ide.ceylonCode.platform {
    IdeaDocument,
    IdeaTextChange,
    IdeaCompositeChange
}
import org.intellij.plugins.ceylon.ide.ceylonCode.psi {
    CeylonFile,
    CeylonPsi,
    descriptions
}
import org.intellij.plugins.ceylon.ide.ceylonCode.resolve {
    FindMatchingPsiNodeVisitor
}
import com.intellij.psi.util {
    PsiTreeUtil
}

shared class ExtractFunctionHandler() extends AbstractExtractHandler() {

    List<CeylonPsi.DeclarationPsi> toPsi(PsiFile file, List<Tree.Declaration> elements) {
        value psi = ArrayList<CeylonPsi.DeclarationPsi>();
        for (term in elements) {
            value visitor = FindMatchingPsiNodeVisitor(term, javaClass<CeylonPsi.DeclarationPsi>());
            visitor.visitFile(file);

            if (is CeylonPsi.DeclarationPsi element = visitor.psi) {
                psi.add(element);
            } else {
                print("Couldn't find PSI node for Node " + term.string);
            }
        }
        return psi;
    }

    function findContainer(CeylonPsi.DeclarationPsi element) {
        assert (is CeylonPsi.DeclarationPsi? result 
            = PsiTreeUtil.findFirstParent(element,
                    object satisfies Condition<PsiElement> {
                        \ivalue(PsiElement cand)
                                => cand is CeylonPsi.DeclarationPsi
                                && cand!=element;
                    }));
        return result;
    }
    
    function containerLabel(CeylonPsi.DeclarationPsi element)
            => if (exists container = findContainer(element),
                   exists desc
                       = descriptions.descriptionForPsi {
                           element = container;
                           includeReturnType = false;
                       })
            then javaString(desc)
            else javaString("package " + element.ceylonNode.unit.\ipackage.qualifiedNameString);

    shared actual void extractToScope(Project project, Editor editor, CeylonFile file, TextRange range) {

        value declarations = ArrayList<Tree.Declaration>();
        
        file.compilationUnit.visit(object extends Visitor() {
            shared actual void visit(Tree.Declaration that) {
                if (that.startIndex.intValue()<=range.startOffset,
                    that.endIndex.intValue()>=range.endOffset) {
                    super.visit(that);
                    if (!that is Tree.AttributeDeclaration) {
                        declarations.add(that);
                    }
                }
            }
        });
        
        if (declarations.size()>1) {
            IntroduceTargetChooser.showChooser(editor,
                toPsi(file, declarations),
                object extends Pass<CeylonPsi.DeclarationPsi>() {
                    pass(CeylonPsi.DeclarationPsi selectedValue)
                            => createAndIntroduceValue { 
                                proj = project;
                                editor = editor;
                                file = file;
                                range = range;
                                ceylonNode = selectedValue.ceylonNode;
                            };
                },
                object satisfies Function<CeylonPsi.DeclarationPsi,JString> {
                    fun(CeylonPsi.DeclarationPsi element) => containerLabel(element);
                },
                "Select target scope");
        }
        else {
            super.extractToScope(project, editor, file, range);
        }
        
    }
    
    shared actual default [TextRange+]? extract(Project proj, Editor editor, CeylonFile file, TextRange range, Tree.Declaration? scope) {
        assert(exists localAnalysisResult = file.localAnalysisResult);
        assert(exists phasedUnit = localAnalysisResult.lastPhasedUnit);
        
        value refactoring = if (exists typecheckedRootNode = localAnalysisResult.typecheckedRootNode)
        then createExtractFunctionRefactoring {
            doc = IdeaDocument(editor.document);
            selectionStart = range.startOffset;
            selectionEnd = range.endOffset;
            rootNode = typecheckedRootNode;
            tokens = localAnalysisResult.tokens;
            target = scope;
            moduleUnits = empty;
            vfile = phasedUnit.unitFile;
        }
        else null;

        if (exists refactoring) {

            return object extends WriteCommandAction<[TextRange+]?>(proj, file) {
                shared actual void run(Result<[TextRange+]?> result) {

                    switch (change = refactoring.build())
                    case (is IdeaTextChange) {
                        change.applyOnProject(proj);
                    }
                    case (is IdeaCompositeChange) {
                        change.applyChanges(proj);
                    }
                    else {}

                    function textRange(DefaultRegion r) => TextRange.from(r.start, r.length);

                    if (exists dec = refactoring.decRegion,
                        exists ref = refactoring.refRegion) {
                        editor.selectionModel.setSelection(dec.start, dec.end);
                        value newId = editor.document.getText(textRange(dec));

                        for (dupe in refactoring.dupeRegions) {
                            editor.document.replaceString(
                                dupe.start, 
                                dupe.start + dupe.length, 
                                JString(newId));
                        }

                        result.setResult([dec, ref, for (dupe in refactoring.dupeRegions) dupe].collect(textRange));
                    }

                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                }
            }.execute().resultObject;
        }

        return null;
    }
}
