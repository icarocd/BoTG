package mining.textMining.parsing;

import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.mutable.MutableBoolean;
import mining.textMining.Stemmer;
import util.StringUtils;

public class TokenExtractor {

    public static final String TOKEN_SEPARATORS_LIST_EXTENDED   = "\\s+|\u00A0|,|;|:|\\+|<|>|\\(|\\)|\"|'|´|`|/|\\[|\\]|\\||\\{|\\}|=|#|&|@|\\$|%|_|-|~|\\^|\\*|\\\\";
	private boolean removeEmails;
    private final Stemmer stemmer;
    private final Set<String> stopList;
    private final String tokenSeparators;
    private final boolean retainAcronyms;
	private final boolean discardSequencesOfSingleChar;

    /**
     * @param stripChars caso null, faz strip (limpeza no inicio e fim) nos tokens por todo caractere que não seja numero ou letra
     */
    public TokenExtractor(Stemmer stemmer, Set<String> stopList, String tokenSeparators, boolean retainAcronyms,
    	boolean discardSequencesOfSingleChar)
    {
        this.stemmer = stemmer;
        this.stopList = stopList != null ? stopList : Collections.EMPTY_SET;
        this.tokenSeparators = tokenSeparators;
        this.retainAcronyms = retainAcronyms;
        this.discardSequencesOfSingleChar = discardSequencesOfSingleChar;
    }

    public TokenExtractor(Stemmer stemmer, Set<String> stopList) {
        this(stemmer, stopList, TokenExtractor.TOKEN_SEPARATORS_LIST_EXTENDED, false, true);
    }

    public void setRemoveEmails(boolean removeEmails) {
        this.removeEmails = removeEmails;
    }

    public void extract(String text, TokenCollector tokenCollector) {
        if (text == null) {
            return;
        }

        //interessante remover acentos pois: termos iguais a menos dos acentos tornam-se iguais, facilita stemming, ...
        text = StringUtils.replaceAccents(text);

        if (removeEmails) {
            text = removeEmails(text);
        }

        String[] tokens = splitInTokens(text);

        MutableBoolean firedNewSubSectionAsLastEvent = new MutableBoolean(false);
        for (String token : tokens) {
            analyzeToken(token, firedNewSubSectionAsLastEvent, tokenCollector);
        }
    }

    private static String removeEmails(String text) {
        return text.replaceAll("[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})", "");
    }

    private void analyzeToken(String token, MutableBoolean firedNewSubSectionAsLastEvent, TokenCollector tokenCollector) {
        token = token.trim().toLowerCase();

        if (token.isEmpty()) {
            return;
        }

        //detecta inicio/termino de subSecoes no começo/termino do token, mas tratando de nao confundir ultimo ponto de sigla como indicativo de sub-secao
        //quebras de sub-secao: ponto/interrogação/exclamação
        boolean subSectionStarted = false;
        boolean subSectionEnded = false;
        boolean keepAcronym = false;
        {
            //analisa se inicio do token contem indicativo de sub-secao:
            String newToken = StringUtils.stripStart(token, ".!?");
            if (!token.equals(newToken)) {
                subSectionStarted = true;
                token = newToken;
            }

            //analisa se fim do token contem indicativo de sub-secao:
            if (!token.isEmpty()) {
                newToken = StringUtils.stripEnd(token, ".!?");

                if(retainAcronyms && StringUtils.isAcronym(newToken + ".")){
                    //OBS: pontos de fim de sigla nao devem ser confundidos com fim de sub-secao
                    token = newToken + ".";
                    keepAcronym = true;
                }
                else if (!token.equals(newToken)) {
                    subSectionEnded = true;
                    token = newToken;
                }
            }
        }

        if (subSectionStarted) {
            announceNewSubSection(tokenCollector, firedNewSubSectionAsLastEvent);
        }

        //detecta ocorrencias de sub-secoes no meio do token
        String[] subTokens;
        if (keepAcronym) {
            subTokens = new String[] { token };
        } else {
            subTokens = token.split("\\.|!|\\?");
        }

        for (int i = 0; i < subTokens.length; i++) {
            if(i > 0){
                announceNewSubSection(tokenCollector, firedNewSubSectionAsLastEvent);
            }
            addToken(subTokens[i], tokenCollector, firedNewSubSectionAsLastEvent);
        }

        if (subSectionEnded) {
            announceNewSubSection(tokenCollector, firedNewSubSectionAsLastEvent);
        }
    }

    private void addToken(String token, TokenCollector tokenCollector, MutableBoolean firedNewSubSectionAsLastEvent) {
        //remover quaisquer simbolos pois atrapalham match de termo
        //fazemos isso apos ter removido acentos, pois caracteres acentuados terao sido covertidos em nao acentuados e dai nao haverá remocao
        token = StringUtils.removeNonPrintableAndNonBasicLatinCharacters(token);

        token = StringUtils.stripDirtyFromEdges(token);

        if (isAcccepted(token)) {
            //OBS: mas isto nao vai fazer sentido apos a aplicação de stemming que gere como stems palavras inexistentes!
            //somente antes pois apos o stemming a palavra pode ter vindo de uma aceita e passar a ser nao mais aceita

            boolean tokenAccepted = true;

            if (stemmer != null) {
                String stem = stemmer.getStem(token);
                if (stem != null && !stem.equals(token)) {
                    if (!isAcccepted(stem)) {
                        tokenAccepted = false;
                    }
                    token = stem;
                }
            }

            if (tokenAccepted) {
                tokenCollector.collect(token);
                firedNewSubSectionAsLastEvent.setFalse();
            }
        }
    }

    private void announceNewSubSection(TokenCollector tokenCollector, MutableBoolean firedNewSubSectionAsLastEvent) {
        if (firedNewSubSectionAsLastEvent.isFalse()) {
            tokenCollector.subSectionStarted();
            firedNewSubSectionAsLastEvent.setTrue();

            /* OBS: controle acima é pra nao lancarmos mais de um evento seguido de subSectionStarted(),
             * pra evitar que o listener crie subSecoes vazias. So enviaremos tal evento apos um envio se houver um evento de token apos o 1º.
             */
        }
    }

    private String[] splitInTokens(String text) {
        //remove numeros, pois:
        //  -sao geralmente descartaveis para efeito de texto
        //  -e porque fazendo isto as ocorrencias de virgula e ponto remanescentes poderao ser consideradas corretamente como simbolos de frases
        text = StringUtils.removeNumbers(text);

        return text.split(tokenSeparators);
    }

    private boolean isAcccepted(String token) {
		if (token.length() < 3) {
			return false;
		}
		if (!StringUtils.hasLetter(token)) {
			return false;
		}
		if (isStopWord(token)) {
			return false;
		}
		if (discardSequencesOfSingleChar && StringUtils.isSequenceOfSingleChar(token)) {
			return false;
		}
        return true;
    }

    private boolean isStopWord(String term) {
        return stopList.contains(term);
    }
}
