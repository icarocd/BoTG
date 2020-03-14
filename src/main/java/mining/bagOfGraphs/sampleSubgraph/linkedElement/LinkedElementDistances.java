package mining.bagOfGraphs.sampleSubgraph.linkedElement;

import java.util.Map;
import java.util.Set;
import com.google.common.base.Preconditions;
import mining.distance.graphDistance.GraphDistanceType;
import util.DataStructureUtils;
import util.StringUtils;

public enum LinkedElementDistances {

	/*DIST1 {
		protected float calc(LinkedElement a, LinkedElement b) {
			if(!a.getElement().equals(b.getElement()))
	            return 1;
	        float dist = Math.abs(a.termWeight - b.termWeight);
	        int numComparisons = 1;
	        Map<String, Float> edgesWeights1 = a.edgesWeights;
	        Map<String, Float> edgesWeights2 = b.edgesWeights;
	        int numEdges1 = edgesWeights1.size();
	        int numEdges2 = edgesWeights2.size();
	        if(numEdges1 > 0 || numEdges2 > 0){
	            int missingComparisons;
	            if(numEdges1 == 0)
	                missingComparisons = numEdges2;
	            else if(numEdges2 == 0)
	                missingComparisons = numEdges1;
	            else{
	                Set<String> commonNeighborTerms = DataStructureUtils.intersection(edgesWeights1.keySet(), edgesWeights2.keySet());
	                for(String commonNeighborTerm : commonNeighborTerms){
	                    dist += Math.abs(edgesWeights1.get(commonNeighborTerm) - edgesWeights2.get(commonNeighborTerm));
	                    numComparisons++;
	                }
	                missingComparisons = numEdges1 + numEdges2 - commonNeighborTerms.size(); //deveria ser [VIDE DIST1_FIX]: numEdges1 + numEdges2 - 2*commonNeighborTerms.size()
	            }
	            dist += missingComparisons;
	            numComparisons += missingComparisons;
	        }
	        return dist / numComparisons;
		}
	},*/
	DIST1_FIX { //corrige o problema com o calculo de 'missingComparisons' de DIST1
		protected float calc(LinkedElement a, LinkedElement b) {
			if(!a.getElement().equals(b.getElement()))
	            return 1;
	        float dist = Math.abs(a.elementWeight - b.elementWeight);
	        int numComparisons = 1;
	        Map<String, Float> edgesWeights1 = a.edgesWeights;
	        Map<String, Float> edgesWeights2 = b.edgesWeights;
	        int numEdges1 = edgesWeights1.size();
	        int numEdges2 = edgesWeights2.size();
	        if(numEdges1 > 0 || numEdges2 > 0){
	            int missingComparisons;
	            if(numEdges1 == 0)
	                missingComparisons = numEdges2;
	            else if(numEdges2 == 0)
	                missingComparisons = numEdges1;
	            else{
	                Set<String> commonNeighborTerms = DataStructureUtils.intersection(edgesWeights1.keySet(), edgesWeights2.keySet());
	                for(String commonNeighborTerm : commonNeighborTerms){
	                    dist += Math.abs(edgesWeights1.get(commonNeighborTerm) - edgesWeights2.get(commonNeighborTerm));
	                    numComparisons++;
	                }
	                missingComparisons = numEdges1 + numEdges2 - 2*commonNeighborTerms.size();
	            }
	            dist += missingComparisons;
	            numComparisons += missingComparisons;
	        }
	        return dist / numComparisons;
		}
	},
	DIST1B {
		//reformulacao de DIST1_FIX compondo 50% de peso no calculo de dist do termo central e 50% aos vizinhos
		//	(DIST1_FIX tende a considerar mais a disparidade de vizinhos conforme maior qtd de vizinhos os grafos tenham)
		//problema com DIST1B: g1 e g2 com termos iguais e vizinhos (abd) e vazio deveriam ser mais parecidos que g1 e g3 onde vizinhos de g3 sao (e), no entanto DIST1B dá mesma distancia

		protected float calc(LinkedElement a, LinkedElement b) {
			if(!a.getElement().equals(b.getElement()))
	            return 1;
	        float d1 = Math.abs(a.elementWeight - b.elementWeight);

	        Map<String, Float> edgesWeights1 = a.edgesWeights;
	        Map<String, Float> edgesWeights2 = b.edgesWeights;
	        int numEdges1 = edgesWeights1.size();
	        int numEdges2 = edgesWeights2.size();
	        float d2 = 0;
	        if(numEdges1 > 0 || numEdges2 > 0){
		        int n = numEdges1 + numEdges2;
	            if(numEdges1 == 0 || numEdges2==0) //conjuntos sao disjuntos
	            	d2 = 1;
	            else {
	            	Set<String> commonNeighborTerms = DataStructureUtils.intersection(edgesWeights1.keySet(), edgesWeights2.keySet());
	            	for(String commonNeighborTerm : commonNeighborTerms)
	            		d2 += Math.abs(edgesWeights1.get(commonNeighborTerm) - edgesWeights2.get(commonNeighborTerm));
	            	d2 += (n - 2*commonNeighborTerms.size()); //a presença de cada termo nao comum contribui em 1 no calculo
	            	d2 = d2/n;
	            }
	        }
	        return (d1 + d2) / 2F;
		}
	},
	DIST2 {
		protected float calc(LinkedElement a, LinkedElement b) {
			//TODO testar tambem usando os pesos

	    	//similarity = beta * similarityTerm(a, b) + (1 - beta) * similarityNeighbors(a, b)
	        if(a.getElement().equals(b.getElement())){ //similarityTerm = 1
	        	return 1F - (0.5F + 0.5F * similarityNeighbors(a, b));
	        }else{
	        	return 1F - (0.5F * similarityNeighbors(a, b));
	        }
		}
	},
	MCS {
		protected float calc(LinkedElement a, LinkedElement b) {
			return GraphDistanceType.MCS.calculateDistance(a, b);
		}
	},
	WGU {
		protected float calc(LinkedElement a, LinkedElement b) {
			return GraphDistanceType.WGU.calculateDistance(a, b);
		}
	};

	private static LinkedElementDistances CURRENT;

	protected abstract float calc(LinkedElement a, LinkedElement b);

	public static LinkedElementDistances get(String functionName, LinkedElementDistances defaultValue) {
		if(StringUtils.isNotBlank(functionName))
			return LinkedElementDistances.valueOf(functionName.toUpperCase());
		return defaultValue;
	}

	public static void initialize(LinkedElementDistances f) {
	    CURRENT = f;
	}

	public static float calculateDistance(LinkedElement a, LinkedElement b) {
        return CURRENT.calc(a, b);
	}

	public static boolean isNeighborNodeWeightsRequired() {
        Preconditions.checkNotNull(CURRENT, "LinkedElementDistances was not initialized");
        return CURRENT == MCS || CURRENT == WGU;
    }

    private static float similarityNeighbors(LinkedElement a, LinkedElement b) {
        Set<String> neighborsA = a.edgesWeights.keySet();
        Set<String> neighborsB = b.edgesWeights.keySet();
        //return DataStructureUtils.intersectionUnionRatio(neighborsA, neighborsB);
        return DataStructureUtils.intersectionMaxRatio(neighborsA, neighborsB);
    }
}
