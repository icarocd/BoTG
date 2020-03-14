package mining.textMining;

public interface Stemmer
{
	String getStem(String term);

	boolean stemOnlyToValidWords();
}
