package org.ssssssss.magicapi.idea;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.Assert;
import org.junit.Test;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;
import org.ssssssss.magicapi.idea.lang.lexer.MagicScriptLexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure lexer tests: no IDE application is started, so these run in a plain JVM test.
 */
public class LexerTest {

    private static List<IElementType> tokenTypes(String text) {
        Lexer lexer = new MagicScriptLexer();
        lexer.start(text);
        List<IElementType> types = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                types.add(lexer.getTokenType());
            }
            lexer.advance();
        }
        return types;
    }

    private static List<String> tokenTexts(String text) {
        Lexer lexer = new MagicScriptLexer();
        lexer.start(text);
        List<String> texts = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                texts.add(text.substring(lexer.getTokenStart(), lexer.getTokenEnd()));
            }
            lexer.advance();
        }
        return texts;
    }

    @Test
    public void lexesKeywordsAndIdentifiers() {
        List<IElementType> types = tokenTypes("var x = 1;");
        Assert.assertEquals(List.of(
                MagicScriptTokenType.IDENTIFIER,
                MagicScriptTokenType.IDENTIFIER,
                MagicScriptTokenType.ASSIGN,
                MagicScriptTokenType.INT_LITERAL,
                MagicScriptTokenType.SEMICOLON
        ), types);
    }

    @Test
    public void lexesTripleQuotedSqlAsOneToken() {
        String text = "var sql = \"\"\"\n  select * from t\n\"\"\";";
        List<String> texts = tokenTexts(text);
        Assert.assertTrue("SQL block must be a single token", texts.contains("\"\"\"\n  select * from t\n\"\"\""));
    }

    @Test
    public void lexesEmbeddedLanguageBlock() {
        String text = "var v = ```javascript\n  name + ' ~ world'\n```;";
        List<String> texts = tokenTexts(text);
        Assert.assertTrue("embedded block must be a single token",
                texts.contains("```javascript\n  name + ' ~ world'\n```"));
        Assert.assertTrue(texts.contains(";"));
    }

    @Test
    public void lexesTemplateString() {
        String text = "var s = `hello ${name} world`;";
        List<String> texts = tokenTexts(text);
        Assert.assertTrue(texts.contains("`hello ${name} world`"));
    }

    @Test
    public void distinguishesRegexFromDivision() {
        List<IElementType> regex = tokenTypes("var p = /\\d+/g;");
        Assert.assertTrue(regex.contains(MagicScriptTokenType.REGEXP_LITERAL));

        List<IElementType> division = tokenTypes("var a = 1 / 2;");
        Assert.assertFalse(division.contains(MagicScriptTokenType.REGEXP_LITERAL));
    }

    @Test
    public void lexesComments() {
        List<IElementType> types = tokenTypes("// line\n/* block */ x");
        Assert.assertTrue(types.contains(MagicScriptTokenType.LINE_COMMENT));
        Assert.assertTrue(types.contains(MagicScriptTokenType.BLOCK_COMMENT));
    }

    @Test
    public void lexesNumericSuffixes() {
        List<IElementType> types = tokenTypes("1b 1s 1l 1f 1d 1m 1");
        Assert.assertEquals(List.of(
                MagicScriptTokenType.BYTE_LITERAL,
                MagicScriptTokenType.SHORT_LITERAL,
                MagicScriptTokenType.LONG_LITERAL,
                MagicScriptTokenType.FLOAT_LITERAL,
                MagicScriptTokenType.DOUBLE_LITERAL,
                MagicScriptTokenType.DECIMAL_LITERAL,
                MagicScriptTokenType.INT_LITERAL
        ), types);
    }

    @Test
    public void lexesLambdaAndSafeAccess() {
        List<IElementType> types = tokenTypes("var f = (a) => a?.b;");
        Assert.assertTrue(types.contains(MagicScriptTokenType.LAMBDA));
        Assert.assertTrue(types.contains(MagicScriptTokenType.SAFE_DOT));
    }

    @Test
    public void lexesAtPrefixedIdentifiers() {
        List<String> texts = tokenTexts("@PathVariable id");
        Assert.assertTrue(texts.contains("@PathVariable"));
    }

    @Test
    public void everyOffsetIsLexable() {
        String text = "var a = \"\"\"select #{id} from t\"\"\";\nvar r = /x/g;\nreturn a;";
        Lexer lexer = new MagicScriptLexer();
        String[] arrows = {"->", "=>"};
        for (int i = 0; i < text.length(); i++) {
            lexer.start(text, i, text.length(), 0);
            int guard = 0;
            while (lexer.getTokenType() != null && guard++ < 10000) {
                Assert.assertTrue("token must make progress at " + i,
                        lexer.getTokenEnd() > lexer.getTokenStart() || lexer.getTokenEnd() == text.length());
                lexer.advance();
            }
            Assert.assertTrue("guard tripped at offset " + i, guard < 10000);
        }
    }
}
