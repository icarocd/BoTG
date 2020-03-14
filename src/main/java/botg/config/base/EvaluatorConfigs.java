package botg.config.base;

import java.io.File;
import util.DateUtil;
import util.Params;

public class EvaluatorConfigs extends DatasetConfigs {

    public final int nFolds;

    public EvaluatorConfigs(Params params) {
        super(params);
        nFolds = params.getInt("nFolds", 10);
    }

    public File createNewOutputFolder(String prefixName) {
        //devido execucao concorrente, varias tasks poderiam estar tentando criar o mesmo diretorio de saida. portanto, aqui faz-se retentativas:
        String parentFolder = getDatasetResultsFolder();
        for (int i = 0; i < 10; i++) {
            String folderName = prefixName + "_" + DateUtil.formatDateTimeFull();
            File output = parentFolder != null ? new File(parentFolder, folderName) : new File(folderName);
	        if(output.mkdirs())
	        	return output;
	        DateUtil.sleepUpToOneSecond();
        }
	    throw new IllegalStateException("Output folder could not be created with prefix "+ prefixName);
	}
}
