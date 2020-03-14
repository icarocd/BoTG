package mining.textMining.parsing;

public interface TokenCollector {

    void collect(String token);

    void subSectionStarted();
}
