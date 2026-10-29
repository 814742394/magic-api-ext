package org.ssssssss.magicapi.idea.lang;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Comment support: {@code //} line comments and {@code /* ... *}{@code /} block
 * comments, exactly the two forms accepted by the magic-script tokenizer.
 */
public class MagicScriptCommenter implements Commenter {

    @Override
    public @Nullable String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public List<String> getLineCommentPrefixes() {
        return List.of("//");
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return "/*";
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return "*/";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }
}
