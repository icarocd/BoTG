package mining.bagOfGraphs;

import util.FloatVector;
import util.FloatVectorFactory;
import util.dataStructure.FlexibleMatrix;

public enum TypePooling {

	/**
	 * Cada codeword do BoW resultante receberá, como peso, a soma dos pesos que lhe foram atributos por sampleSubgraph
	 * PS: a reason to use SUM instead of AVG is that the number of subgraphs of a graph is a relevant information that should be encoded in the bag representation
	 */
	SUM {
		@Override
		public FloatVector pooling(FlexibleMatrix assign) {
			int nColumns = assign.getColumnNumber();
			FloatVector bag = FloatVectorFactory.create(nColumns);

            assign.forEachNonZero((i,j,value) -> {
                bag.add(j, value);
            });
            //for (int i = 0; i < assign.getLineNumber(); i++)
            //    for (int j = 0; j < nColumns; j++)
            //        bag.add(j, assign.getValue(i, j));

	        return bag;
		}
	},

	/** Cada codeword do BoW resultante receberá, como peso, a média dos pesos que lhe foram atributos por sampleSubgraph */
	AVG {
		@Override
		public FloatVector pooling(FlexibleMatrix assign) {
		    FloatVector bag = SUM.pooling(assign);

		    //for each attribute counting, normalize the counting by the number of subgraphs from the sample:
		    int nRows = assign.getLineNumber();
		    int nColumns = assign.getColumnNumber();
			for(int i = 0; i < nColumns; i++) {
			    bag.divideBy(i, nRows);
			}

			return bag;
		}
	},

	/** Cada codeword do BoW resultante receberá, como peso, o maior peso que lhe foi atribuído por sampleSubgraph */
	MAX {
		@Override
		public FloatVector pooling(FlexibleMatrix assign) {
			int nColumns = assign.getColumnNumber();
	        FloatVector bag = FloatVectorFactory.create(nColumns);

            assign.forEachNonZero((i,j,value) -> {
                if(value > bag.get(j))
                    bag.set(j, value);
            });
            //for (int i = 0; i < assign.getLineNumber(); i++) {
            //    for (int j = 0; j < nColumns; j++) {
            //        float value = assign.getValue(i, j);
            //        if (value > bag.get(j))
            //            bag.set(j, value);
            //    }
            //}

	        return bag;
		}
	};

	public abstract FloatVector pooling(FlexibleMatrix assign);
}