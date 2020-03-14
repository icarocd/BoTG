package mining.clustering;

import java.util.List;
import util.MathUtils;
import util.dataStructure.Matrix;

public abstract class Clusterer {

	public abstract List<Integer> cluster();

	/** Returns, from objects, that one which has the lowest sum of distances to the others, i.e. the object that best centers all of them */
	protected int getMeanObject(List<Integer> objects, Matrix<Float> distanceMatrix) {
		int n = objects.size();
		if(n == 1)
			return objects.get(0);
		//Logs.finest("starting getMeanObject()");
        //TimeWatcher timeWatcher = new TimeWatcher().start();
		/*
        Integer pointWithMinSumDistance = null;
        float minSumDistance = Float.MAX_VALUE;
        for (Integer point : objects) {
        	float sumDistance = distanceMatrix.getLineSum(point, objects);
        	if(sumDistance < minSumDistance){
        		minSumDistance = sumDistance;
        		pointWithMinSumDistance = point;
        	}
        }
        return pointWithMinSumDistance;
		*/
        //implementacao, otimizada do codigo comentado acima, mas que gasta mais memoria:
        float[] objectDistances = new float[n];
        for (int i = 0; i < n; i++) {
        	int objI = objects.get(i);
        	for (int j = i+1; j < n; j++) {
        		int objJ = objects.get(j);
        		float v = distanceMatrix.getValue(objI, objJ);
        		objectDistances[i] += v;
        		objectDistances[j] += v;
        	}
        }
        return objects.get(MathUtils.argmin(objectDistances));
        //Logs.finest("ending getMeanObject() after "+timeWatcher);
	}
}
