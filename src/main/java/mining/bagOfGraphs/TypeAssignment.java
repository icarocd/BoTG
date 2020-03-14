package mining.bagOfGraphs;

import java.util.Collection;
import java.util.List;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import util.dataStructure.FlexibleMatrix;

public enum TypeAssignment {
    /** Produz uma matriz correlacionando cada subgraph (linha) àquele codeword do codebook (coluna) que o subgraph mais se assemelhe */
    HARD {
        @Override
        public FlexibleMatrix assign(Collection<SampleSubgraph> sampleSubGraphs, List<SampleSubgraph> codebook)
        {
            int rows = sampleSubGraphs.size();
            int columns = codebook.size();

            FlexibleMatrix assign = new FlexibleMatrix(rows, columns);

            int i = 0;
            for (SampleSubgraph sampleSubgraph : sampleSubGraphs) {
                int idxClosestCodeword = 0;
                float minValue = Float.MAX_VALUE;
                for (int j = 0; j < columns; j++) {
                    float distance = sampleSubgraph.calculateDistance(codebook.get(j));
                    if(distance < minValue){
                        minValue = distance;
                        idxClosestCodeword = j;
                    }
                }
                assign.setValue(i, idxClosestCodeword, 1F);
                i++;
            }

            return assign;
        }
    },

    /** Produz uma matriz correlacionando cada subgraph (linha) a seus graus de pertinencia nos codewords (colunas) do codebook */
    SOFT {
        @Override
        public FlexibleMatrix assign(Collection<SampleSubgraph> sampleSubGraphs, List<SampleSubgraph> codebook)
        {
        	//int sigma = 1; //TODO rever

        	//double temp1 = 1.0/( Math.sqrt(2*Math.PI) * sigma);
            //double temp2 = -1.0 / (2.0*(sigma*sigma));
            //double beta_ = ( -1.0 / ( 2 * Math.PI * (sigma * sigma) ) );

            FlexibleMatrix assign = new FlexibleMatrix(sampleSubGraphs.size(), codebook.size());
            {
                int i = 0;
                for (SampleSubgraph sampleSubgraph : sampleSubGraphs) {
                    for (int j = 0; j < codebook.size(); j++) {
                        float v = sampleSubgraph.calculateDistance(codebook.get(j));

                        //implementacao da formulacao original:
                        //    ( 1/(sqrt(2*pi) * sigma) ) * exp( -1/(2 * sigma^2) * v^2 )
                        //v = temp1 * Math.exp( (v*v) / temp2 );

                        //implementacao original do codigo de fernanda:
                        //    exp ( -1/(2*pi*sigma^2) * v^2 )
                        //v = (float) Math.exp(beta_ * (v * v));

                        //formulacao icaro: (assignment linear: apenas considerar a similaridade, e depois dividir pela soma...)
                        v = 1 - v;

                        assign.setValue(i, j, v);
                    }
                    i++;
                }
            }

            // UNC = K(D(w,ri))/ sum for all w [ K(D(w,ri) ]
            for (int i = 0; i < assign.getLineNumber(); i++) {
                float lineSum = assign.getLineSum(i); // sum the kernel of a feature for all codewords
                if(lineSum != 0F){
                	assign.divideLineValues(i, lineSum);
                }
            }

            return assign;
        }
    };

    public abstract FlexibleMatrix assign(Collection<SampleSubgraph> sampleSubGraphs, List<SampleSubgraph> codebook);
}