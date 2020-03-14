package mining.textMining.parsing;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import mining.textMining.TextSample;
import util.StringUtils;

public class TextSection {

    public static final String SECTION_TITLE = "title";
    public static final String SECTION_BODY = "body";

    private String id;
    private List<TokenList> subSections;

    public TextSection(String id, List<TokenList> subSections) {
        this.id = id;
        this.subSections = subSections;
    }

    public String getId() {
        return id;
    }

    public List<TokenList> getSubSections() {
        if(subSections == null){
            subSections = new ArrayList<>();
        }
        return subSections;
    }

    public static List<TextSection> loadSections(TokenExtractor tokenExtractor, TextSample textSample) {
        List<TextSection> sections = new ArrayList<>();
        addSection(tokenExtractor, sections, TextSection.SECTION_TITLE, textSample.getTitle());
        if (ArrayUtils.isNotEmpty(textSample.getTags())) {
            String tags = StringUtils.join(textSample.getTags(), '.');
            addSection(tokenExtractor, sections, TextSection.SECTION_BODY, StringUtils.join(new String[]{textSample.getBody(), tags}, '.'));
        } else {
            addSection(tokenExtractor, sections, TextSection.SECTION_BODY, textSample.getBody());
        }
        return sections;
    }
    private static void addSection(TokenExtractor tokenExtractor, List<TextSection> sections, String sectionId, String textSection) {
        if (StringUtils.isNotBlank(textSection)) {
            TokenListsCollector tokenListCollector = new TokenListsCollector();
            tokenExtractor.extract(textSection, tokenListCollector);
            sections.add(new TextSection(sectionId, tokenListCollector.getTokenLists()));
        }
    }
}
