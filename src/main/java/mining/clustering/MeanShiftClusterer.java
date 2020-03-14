package mining.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import com.google.common.base.Preconditions;
import util.DataStructureUtils;
import util.Logs;
import util.MathUtils;
import util.Pair;
import util.RandomFactory;
import util.TimeWatcher;
import util.dataStructure.Matrix;

public class MeanShiftClusterer extends Clusterer {

	private final Matrix<Float> distanceMatrix;
	private final float percentageElementsForSeeds;
	private final int maxIterations;
	private final float bandwidth;
	private Map<Integer,Pair<ArrayList<Integer>,Integer>> indicesOfNeighborsWithinRadius;

	/**
	 * Perform MeanShift Clustering of data using a flat kernel.
	 * Returns indices of samples from input matrix, where each index relates to a sample that was considered a cluster.
	 *
	 * @param distanceMatrix
	 * @param percentageElementsForSeeds   the maximum percentage of elements to be used as seeds. Use -1 to use all elements as seeds, otherwise something between ]0,1].
	 * @param maxIterations                the maximum number of iteration steps used by seed convergence. In doubt, use 300.
	 * @param bandwidth                    optional. If not set, it will be calculated based on quantile and distance matrix.
	 * @param quantile                     used then bandwidth not set. Should be between [0, 1]. 0.5 means that the median of all pairwise distances is used. In doubt, use around 0.3.
	 */
	public MeanShiftClusterer(Matrix<Float> distanceMatrix, float percentageElementsForSeeds, int maxIterations, Float bandwidth, Float quantile) {
		this.distanceMatrix = distanceMatrix;
		this.percentageElementsForSeeds = percentageElementsForSeeds;
		this.maxIterations = maxIterations;
		this.bandwidth = bandwidth != null ? bandwidth : estimateBandwidth(distanceMatrix, quantile);
	}

	private static float estimateBandwidth(Matrix<Float> distanceMatrix, float quantile) {
        Logs.finest("[MeanShiftClusterer#estimateBandwidth] Starting...");
        Preconditions.checkArgument(quantile > 0F && quantile <= 1F);
        TimeWatcher timeWatcher = new TimeWatcher();

        int numLines = distanceMatrix.getLineNumber();

        int k = (int) (numLines * quantile);

        float sumDistanceKNearestNeighbor = distanceMatrix.getSumKthLowestValueInLines(k);

        float bandwidth = sumDistanceKNearestNeighbor / numLines;

        Logs.finer("MeanShift estimated bandwidth: "+bandwidth+". After "+timeWatcher);

        return bandwidth;
    }

	@Override
	public List<Integer> cluster() {
		final Map<Integer, Integer> centersWithIntensities = new HashMap<>();
		{
		    float stop_thresh = 1e-3F * bandwidth; // when mean has converged

		    List<Integer> seeds = MathUtils.range(distanceMatrix.getLineNumber());
		    if (percentageElementsForSeeds > 0 && percentageElementsForSeeds < 1) {
		    	int maxSeeds = (int)Math.ceil(distanceMatrix.getLineNumber() * percentageElementsForSeeds);
		        DataStructureUtils.reduceRandomly(seeds, maxSeeds, RandomFactory.create());
		        Logs.finest("[MeanShiftClusterer] " + seeds.size()+" of "+distanceMatrix.getLineNumber()+" elements picked as seeds");
		    }

		    //optional: let's pre-compute some required data before their use
		    computeInitialIndicesOfNeighborsWithinRadius(seeds);

		    // For each seed, climb gradient until convergence or max_iterations
		    final int numElements = seeds.size();
		    Stream<Integer> forSeeds = distanceMatrix.isParallelSupported() ? seeds.parallelStream() : seeds.stream();
		    TimeWatcher logPooler = new TimeWatcher(0);
	        AtomicInteger completed = new AtomicInteger(0);
		    forSeeds.forEach(seed -> {
		        converge(stop_thresh, centersWithIntensities, seed);
		        completed.incrementAndGet();
		        if(logPooler.checkSecondsSpent(60))
		            Logs.finest("[MeanShiftClusterer] concluded for " + completed + " of " + numElements + " seeds");
		    });
		}

	    // POST PROCESSING: remove near duplicate points
		// If the distance between two kernels is less than the bandwidth,
		// remove one because it is a duplicate: remove the one with fewer points.
		Logs.finer("[MeanShiftClusterer] Removing near duplicate centers, from " + centersWithIntensities.size());
		List<Integer> sortedCenters = DataStructureUtils.getMapKeysSortedByValueAsList(centersWithIntensities, false, -1);
		boolean[] uniqueCenters = DataStructureUtils.newArrayTrue(sortedCenters.size());
		for (int i = 0; i < sortedCenters.size(); i++) {
			int center = sortedCenters.get(i);
			if (uniqueCenters[i]) {
				List<Integer> neighborCenters = indicesOfNeighborsWithinRadius(center, sortedCenters);
				DataStructureUtils.setValue(uniqueCenters, neighborCenters, false);
				uniqueCenters[i] = true; // leave the current point as unique
			}
		}
		return DataStructureUtils.collect(sortedCenters, uniqueCenters);
	}

    private void converge(float stop_thresh, Map<Integer, Integer> intensityByCenter, int seed) {
        int completedIterations = 0;
        do {
        	int previousMeanPoint = seed;

        	// Find mean of points within bandwidth
        	Pair<ArrayList<Integer>,Integer> pointsWithinRadiusAndMeanPoint = indicesOfNeighborsWithinRadius(seed);
        	ArrayList<Integer> pointsWithinRadius = pointsWithinRadiusAndMeanPoint.getA();
			if(pointsWithinRadius.isEmpty())
        		break; // Depending on seeding strategy this condition may occur

        	seed = pointsWithinRadiusAndMeanPoint.getB();

        	// If converged or at max_iterations, add the cluster
        	if(distanceMatrix.getValue(seed, previousMeanPoint) < stop_thresh || completedIterations == maxIterations){
        	    int intensity = pointsWithinRadius.size();
                synchronized (intensityByCenter) {
                    Integer previousIntensity = intensityByCenter.putIfAbsent(seed, intensity);
                    if(previousIntensity != null && intensity > previousIntensity){
                        intensityByCenter.put(seed, intensity);
                    }
                }
        	    break;
        	}
        	completedIterations++;
		} while (true);
    }

    private void computeInitialIndicesOfNeighborsWithinRadius(List<Integer> seeds) {
        Logs.finest("[computeInitialIndicesOfNeighborsWithinRadius] starting for "+seeds.size()+" seeds...");
        TimeWatcher time = new TimeWatcher();
        Stream<Integer> forSeeds;
        if (distanceMatrix.isParallelSupported()) {
            indicesOfNeighborsWithinRadius = new ConcurrentHashMap<>();
            forSeeds = seeds.parallelStream();
        } else {
            indicesOfNeighborsWithinRadius = new HashMap<>();
            forSeeds = seeds.stream();
        }
        TimeWatcher logPooler = new TimeWatcher(0);
        AtomicInteger completed = new AtomicInteger(0);
        forSeeds.forEach(elementIdx -> {
            computeIndicesOfNeighborsWithinRadius(elementIdx);
            completed.incrementAndGet();
            if(logPooler.checkSecondsSpent(60))
                Logs.finest("Completed for " + completed + " of " + seeds.size() + " elements");
        });
        Logs.finest("[computeInitialIndicesOfNeighborsWithinRadius] ended after "+time);
    }

    private Pair<ArrayList<Integer>,Integer> computeIndicesOfNeighborsWithinRadius(Integer elementIdx) {
        ArrayList<Integer> list = distanceMatrix.getColumnIndicesOfValuesLowerThan(elementIdx, bandwidth);
        Integer meanPoint = list.isEmpty() ? null : getMeanObject(list, distanceMatrix);
        Pair<ArrayList<Integer>, Integer> pair = new Pair<>(list, meanPoint);
		indicesOfNeighborsWithinRadius.put(elementIdx, pair);
        return pair;
    }

	/**
	 * Finds the neighbors within a given radius of a point. Returns indices of the neighbors.
	 * The points compared to the point are all possible points.
	 */
	private Pair<ArrayList<Integer>,Integer> indicesOfNeighborsWithinRadius(int elementIdx) {
	    //PS: as bandwidth is fixed, we don't need to compute elements' neighbors more than once!
		Pair<ArrayList<Integer>,Integer> cache = indicesOfNeighborsWithinRadius.get(elementIdx);
		if(cache != null)
		    return cache;
		return computeIndicesOfNeighborsWithinRadius(elementIdx);
	}

	/**
	 * Finds the neighbors within a given radius of a point. Returns indices of the neighbors.
	 * The points compared to the point are either those from 'points' argument.
	 */
	private List<Integer> indicesOfNeighborsWithinRadius(int elementIdx, List<Integer> points){
		List<Integer> indicesOfValuesLowerThanRadius = new ArrayList<>();
		for(int idx = 0; idx < points.size(); idx++) {
			int point = points.get(idx);
			if(distanceMatrix.getValue(elementIdx, point) < bandwidth)
				indicesOfValuesLowerThanRadius.add(idx);
		}
		return indicesOfValuesLowerThanRadius;
	}
}
