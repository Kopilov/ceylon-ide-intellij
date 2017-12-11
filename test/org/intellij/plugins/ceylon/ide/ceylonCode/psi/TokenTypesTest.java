package org.eclipse.ceylon.ide.intellij.psi;

import org.eclipse.ceylon.ide.intellij.psi.CeylonTokens;
import org.eclipse.ceylon.ide.intellij.psi.CeylonTypes;
import org.eclipse.ceylon.ide.intellij.psi.TokenTypes;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Matija Mazi <br/>
 */
public class TokenTypesTest {
    @Test
    public void testTokens() throws Exception {
        Assert.assertTrue(Integer.toString(TokenTypes.byIElementType.size()), TokenTypes.byIElementType.size() >= 129);
        Assert.assertTrue(Integer.toString(TokenTypes.byIElementType.size()), TokenTypes.byIElementType.size() < 200);
        Assert.assertTrue(Integer.toString(TokenTypes.byIndex.size()), TokenTypes.byIndex.size() >= 129);
        Assert.assertTrue(Integer.toString(TokenTypes.byIndex.size()), TokenTypes.byIndex.size() < 200);

        assert TokenTypes.get(CeylonTokens.IMPORT) == TokenTypes.IMPORT;
        try {
            TokenTypes.get(CeylonTypes.IMPORT);
            Assert.assertTrue("An exception should be thrown.", false);
        } catch (IllegalArgumentException expected) { }
    }
}
