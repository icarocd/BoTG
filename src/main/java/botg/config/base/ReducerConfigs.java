package botg.config.base;

import java.util.Set;
import mining.DatasetCrossFold;
import mining.textMining.PorterStemmer;
import mining.textMining.Stemmer;
import mining.textMining.StopList;
import mining.textMining.TextSampleFilter;
import mining.textMining.parsing.TokenExtractor;
import util.Params;

public class ReducerConfigs extends DatasetConfigs {

    private final boolean removeEmails;

    private final Set<String> stopList = StopList.STOP_LIST;
    protected final Stemmer stemmer;

    //OBS: importante gerar a distribuicao uma vez, e usar a mesma em todos os modelos a ser comparados!
    private final boolean usePriorDatasetCrossFoldDistribution;

    public final TextSampleFilter textSampleFilter;

    public ReducerConfigs(Params params) {
        super(params);
        removeEmails = params.getBoolean("removeEmails", false);

        usePriorDatasetCrossFoldDistribution = params.getBoolean("usePriorDatasetCrossFoldDistribution", true);

        String stemming = params.get("stemming", "porter");
        if ("porter".equalsIgnoreCase(stemming)) {
            stemmer = new PorterStemmer();
        } else {
            stemmer = null;
        }

        textSampleFilter = usePriorDatasetCrossFoldDistribution ? null : new TextSampleFilter(true, true, true);
    }

    public TokenExtractor createTokenExtractor() {
        TokenExtractor tokenExtractor = new TokenExtractor(stemmer, stopList);
        tokenExtractor.setRemoveEmails(removeEmails);
        return tokenExtractor;
    }

    public DatasetCrossFold getFoldDistributionToUseInReduction() {
		return usePriorDatasetCrossFoldDistribution ? getDatasetCrossFold() : null;
	}
}
