package mining.textMining.parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * TokenCollector that puts the received tokens into token lists,
 * which are separated based on 'subSectionStarted' events receivement.
 */
public class TokenListsCollector implements TokenCollector {

    private List<TokenList> tokenLists;

    private TokenList currentTokenList;

    public TokenListsCollector() {
        tokenLists = new ArrayList<>();
    }

    @Override
    public void collect(String token) {
        if (currentTokenList == null) {
            currentTokenList = new TokenList();
            tokenLists.add(currentTokenList);
        }
        currentTokenList.getTokens().add(token);
    }

    @Override
    public void subSectionStarted() {
        currentTokenList = null;
    }

    public List<TokenList> getTokenLists() {
        return tokenLists;
    }
}
