package mining.textMining.parsing;

import java.util.ArrayList;
import java.util.List;

public class TokenList {

    private List<String> tokens;

    public TokenList() {
        this(new ArrayList<>());
    }

    public TokenList(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return tokens.toString();
    }
}
