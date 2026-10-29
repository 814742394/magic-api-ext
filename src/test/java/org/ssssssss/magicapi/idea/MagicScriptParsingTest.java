package org.ssssssss.magicapi.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.ssssssss.magicapi.idea.lang.MagicScriptFileType;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;

/**
 * Parsing tests running inside the light platform: they check that representative
 * magic-api scripts produce the expected PSI structure without errors.
 */
public class MagicScriptParsingTest extends BasePlatformTestCase {

    private PsiFile parse(String text) {
        PsiFile file = PsiFileFactory.getInstance(getProject())
                .createFileFromText("demo.ms", MagicScriptFileType.INSTANCE, text);
        // Force a full parse.
        file.getNode();
        return file;
    }

    private void assertNoErrors(String text) {
        PsiFile file = parse(text);
        PsiElement[] errors = PsiTreeUtil.collectElements(file, element ->
                element.getNode() != null
                        && element.getNode().getElementType() == com.intellij.psi.TokenType.ERROR_ELEMENT);
        StringBuilder message = new StringBuilder();
        for (PsiElement error : errors) {
            message.append("\nERROR at '").append(error.getText()).append("'");
        }
        if (!message.isEmpty()) {
            fail("Parse errors in:\n" + text + message);
        }
    }

    public void testSimpleQueryScript() {
        assertNoErrors("""
                var sql = \"\"\"
                    select * from test_data
                \"\"\"
                return db.select(sql)
                """);
    }

    public void testFullFeaturedScript() {
        assertNoErrors("""
                import log;
                import request;
                import 'java.util.Date' as Date;

                var id = path.id;
                var name = param.name;

                if (id == null) {
                    exit 400, 'id is required';
                } else if (id < 0) {
                    exit 400, 'invalid id', {id: id};
                }

                var user = db.selectOne('select * from user where id = #{id}');

                for (var item in [1, 2, 3]) {
                    log.info('item {}', item);
                }

                var result = db.page('select * from user');

                db.transaction(() => {
                    db.update('update user set name = #{name} where id = #{id}');
                    return true;
                });

                var map = {key: 'value', [name]: 1};
                var list = [1, 2, 3];
                var sum = list.map(it => it * 2).sum();
                return {code: 1, data: result, sum: sum};
                """);
    }

    public void testForKeyValueIn() {
        assertNoErrors("""
                for (item in [1, 2, 3]) {
                    log.info('item {}', item);
                }
                for (key, value in {a: 1, b: 2}) {
                    log.info('{} = {}', key, value);
                }
                return true;
                """);
    }

    public void testDestructuringDeclaration() {
        assertNoErrors("""
                var {a, b} = {a: 1, b: 2};
                var [first, second] = [1, 2];
                var rest = ...{a: 1};
                return a;
                """);
    }

    public void testConstAndTypeWithoutKeyword() {
        assertNoErrors("""
                const MAX = 10;
                let count = 0;
                String name = 'x';
                return name;
                """);
    }

    public void testIfWithoutParentheses() {
        assertNoErrors("""
                var a = 1;
                if a == 1 {
                    return 1;
                }
                while a < 10 {
                    a = a + 1;
                }
                return a;
                """);
    }

    public void testLinqSelect() {
        assertNoErrors("""
                var list = select id, name from user where id > 1 limit 10;
                return list;
                """);
    }

    public void testTryCatchFinally() {
        assertNoErrors("""
                try {
                    assert is_null(x) : 400, 'x is null';
                    return db.select('select 1');
                } catch (e) {
                    log.error('failed', e);
                    throw e;
                } finally {
                    log.info('done');
                }
                """);
    }

    public void testEmbeddedLanguageAndTemplate() {
        assertNoErrors("""
                var name = "hello";
                var test = ```javascript
                    name + ' ~ world'
                ```;
                var greeting = `hello ${name}`;
                var re = /\\d+/g;
                return test();
                """);
    }

    public void testLexerProducesNoBadCharacters() {
        PsiFile file = parse("var a = 1; // ok\nreturn a;");
        PsiElement[] bad = PsiTreeUtil.collectElements(file, element ->
                element.getNode() != null
                        && element.getNode().getElementType() == MagicScriptTokenType.BAD_CHARACTER);
        if (bad.length != 0) {
            fail("unexpected bad characters: " + bad.length);
        }
    }
}
