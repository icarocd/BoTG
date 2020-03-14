package util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

import org.apache.commons.lang3.mutable.MutableInt;
import com.google.common.base.Preconditions;

public class MathUtils
{
	private static final int ONE_MEGA_BYTE_IN_BYTES = 1024 * 1024;
    private static DecimalFormat percentFormatter;

	public static BigDecimal asBigDecimal(Object value) {
	    if (value instanceof BigDecimal) {
	        return (BigDecimal) value;
	    }
	    if (value instanceof Double) {
	        return new BigDecimal(Double.toString((Double) value));
	    }
	    if (value instanceof Number) {
	        return new BigDecimal(((Number) value).doubleValue());
	    }
		if (value != null) {
			String s = value.toString();
			if (!s.isEmpty()) {
				return new BigDecimal(s);
			}
		}
	    return null;
	}

	public static BigInteger asBigInteger(Object value) {
	    if (value instanceof BigInteger) {
	        return (BigInteger) value;
	    }
	    if (value instanceof BigDecimal) {
	        return ((BigDecimal) value).toBigInteger();
	    }
	    if (value instanceof Double) {
	        return new BigDecimal(Double.toString((Double) value)).toBigInteger();
	    }
	    if (value instanceof Number) {
	        return BigInteger.valueOf(((Number) value).longValue());
	    }
	    if (value != null) {
			String s = value.toString();
			if (!s.isEmpty()) {
				return new BigInteger(s);
			}
		}
	    return null;
	}

	public static int asIntDefault0(MutableInt mutableInt) {
        return mutableInt == null ? 0 : mutableInt.intValue();
    }

	/** Extracts a integer value from the input, accepting Number, String etc */
    public static Integer asInteger(Object value) {
        return asInteger(value, null);
    }

	/** Extracts a integer value from the input, accepting Number, String etc */
	public static Integer asInteger(Object value, Integer defaultValue) {
	    if (value instanceof Number) {
	        return ((Number) value).intValue();
	    }
        if (value != null) {
            try {
                String s = value.toString();
                if(StringUtils.isNotBlank(s)){
                    return Integer.valueOf(s);
                }
            } catch (Exception e) {}
        }
        return defaultValue;
	}

	/** Extracts a long value from the input, accepting Number, String etc */
	public static Long asLong(Object value) {
	    if (value instanceof Number) {
	        return ((Number) value).longValue();
	    }
	    try {
	        return StringUtils.isBlank(value.toString()) ? null : Long.valueOf(value.toString());
	    } catch (Exception e) {
	        return null;
	    }
	}

	public static float asFloatDefault0(MutableInt mutableInt) {
        return mutableInt == null ? 0 : mutableInt.floatValue();
    }

	/** Extracts a float value from the input, accepting Number, String etc */
	public static Float asFloat(Object value) {
	    if (value instanceof Number) {
	        return ((Number) value).floatValue();
	    }
	    try {
	        return StringUtils.isBlank(value.toString()) ? null : Float.valueOf(value.toString());
	    } catch (Exception e) {
	        return null;
	    }
	}

	public static Float[] asFloatArray(String[] values) {
	    return asFloatArray(values, 0, values.length - 1);
	}
	public static Float[] asFloatArray(String[] values, int init, int end) {
		Float[] floats = new Float[end - init + 1];
		int idx = 0;
		while(init <= end) {
			floats[idx++] = asFloat(values[init++]);
		}
		return floats;
	}
	public static float[] asFloatArrayPrimitive(String[] values) {
        return asFloatArrayPrimitive(values, 0, values.length - 1);
    }
	public static float[] asFloatArrayPrimitive(String[] values, int init, int end) {
        float[] floats = new float[end - init + 1];
        int idx = 0;
        while(init <= end) {
            floats[idx++] = Float.parseFloat(values[init++]);
        }
        return floats;
    }

	public static <T> float[] asFloatArray(T[] values, ToFloatFunction<T> transformer) {
		float[] floats = new float[values.length];
        for(int i = 0; i < values.length; i++)
        	floats[i] = transformer.applyAsFloat(values[i]);
        return floats;
	}
	public static <T> float[] asFloatArray(Collection<T> values, ToFloatFunction<T> transformer) {
        float[] floats = new float[values.size()];
        int idx = 0;
        for(T v : values)
            floats[idx++] = transformer.applyAsFloat(v);
        return floats;
    }

	public static <T extends Number> double[] asDoubleArray(Collection<T> collection){
		return collection.stream().mapToDouble(Number::doubleValue).toArray();
	}

	public static boolean isZero(float value) {
		return value > -0.00001F && value < 0.00001F;
	}

	public static boolean isNotZero(float value) {
		return value <= -0.00001F || value >= 0.00001F;
	}

	public static Double round(Double v, int scale, RoundingMode roundingMode) {
        return v == null ? null : round(asBigDecimal(v), scale, roundingMode).doubleValue();
    }
	public static Float round(Float v, int scale, RoundingMode roundingMode) {
        return v == null ? null : round(asBigDecimal(v), scale, roundingMode).floatValue();
    }
    public static BigDecimal round(BigDecimal v, int scale, RoundingMode roundingMode) {
        return v == null ? null : v.setScale(scale, roundingMode);
    }

	/**
	 * Generates an array from 0 to length-1, inclusive. Exemple for length 4: [0 1 2 3].
	 */
	public static List<Integer> range(int length) {
		List<Integer> range = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			range.add(i);
		}
		return range;
	}

	/** An Iterable for interval [0, to[ */
	public static Iterable<Integer> rangeIterable(int to){
		return rangeIterable(0, to);
	}

	/** An Iterable for interval [from, to[ */
	public static Iterable<Integer> rangeIterable(int from, int to){
		return () -> new Iterator<Integer>() {
		    private int current = from;
		    public boolean hasNext() {
		        return current < to;
		    }
		    public Integer next() {
		        return current++;
		    }
		    public void remove() {
		        throw new UnsupportedOperationException();
		    }
		};
	}

	/** Returns the logarithm of a for base 2 */
	public static double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

	/** use lucene's version */
	@Deprecated
	public static double logANaBaseB(double a, double b)
	{
		if (b == Math.E) { //desnecessario fazer conversao de base neste caso
			return Math.log(a);
		}

		//log a na base b = log a na base W dividido por log b na base W (pra qualquer W)
		return Math.log(a) / Math.log(b);
	}

	/** Returns the minimum value in the array a[], +infinity if no such value */
	public static double min(double[] elements) {
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < elements.length; i++) {
			if(elements[i] < min)
                min = elements[i];
		}
		return min;
	}
	public static double min(Collection<Double> elements) {
		double min = Double.POSITIVE_INFINITY;
		for (Double a : elements) {
			if(a < min)
                min = a;
		}
		return min;
	}

	/** Returns the maximum value in the array a[], -infinity if no such value */
	public static double max(double[] elements) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < elements.length; i++) {
			if(elements[i] > max)
                max = elements[i];
		}
		return max;
	}
	public static float max(float[] elements) {
		float max = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < elements.length; i++) {
			if(elements[i] > max)
                max = elements[i];
		}
		return max;
	}
	public static <T> double max(Iterable<T> elements, Function<T,Double> transformer) {
        double max = Double.NEGATIVE_INFINITY;
        for(T el : elements){
            double v = transformer.apply(el);
            if(v > max)
                max = v;
        }
        return max;
    }
	public static <T> float maxFloat(Iterable<T> elements, Function<T,Float> transformer) {
        float max = Float.NEGATIVE_INFINITY;
        for(T el : elements){
            float v = transformer.apply(el);
            if(v > max)
                max = v;
        }
        return max;
    }
	public static <T> int maxInt(Iterable<T> elements, Function<T,Integer> transformer) {
        int max = Integer.MIN_VALUE;
        for(T el : elements){
            int v = transformer.apply(el);
            if(v > max)
                max = v;
        }
        return max;
    }

	public static Pair<Double,Double> minMax(Iterable<Double> elements) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double v : elements) {
			if(v < min)
                min = v;
            if(v > max)
				max = v;
		}
		return new Pair(min,max);
	}
	public static Pair<Double,Double> minMax(double[] elements) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : elements) {
            if(v < min)
                min = v;
            if(v > max)
                max = v;
        }
        return new Pair(min,max);
    }
	public static <T> Pair<Double,Double> minMax(Iterable<T> elements, Function<T,Double> transformer) {
	    double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (T el : elements) {
            double v = transformer.apply(el);
            if(v < min)
                min = v;
            if(v > max)
                max = v;
        }
        return new Pair(min,max);
	}
	public static <T> Pair<Float,Float> minMaxFloat(Iterable<T> elements, Function<T,Float> transformer) {
	    float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (T el : elements) {
            float v = transformer.apply(el);
            if(v < min)
                min = v;
            if(v > max)
                max = v;
        }
        return new Pair(min,max);
	}

	/** Returns the maximum value in the collection, or null if no such value. */
	public static <T> T max(T[] elements) {
		Comparable max = null;
		for (T a : elements) {
			Comparable el = (Comparable)a;
			if (max==null || el.compareTo(max) > 0) {
                max = el;
            }
		}
		return (T) max;
	}

	/** Returns the maximum value in the collection, or null if no such value. */
	public static <T> T max(Collection<T> elements) {
		Comparable max = null;
		for (T a : elements) {
			Comparable el = (Comparable)a;
			if (max==null || el.compareTo(max) > 0) {
                max = el;
            }
		}
		return (T) max;
	}

	/* value' = (max'-min')/(max-min)*(value-max)+max'
	 * optimizing:
	 * a = (max'-min')/(max-min)
	 * b = max' - a*max
	 * value' = a*value - a*max + max' = a*value + (max' - a*max) = a*value + b
	 */
	public static <T> void normalizeFloat(Iterable<T> elements, Function<T,Float> getter, BiConsumer<T,Float> setter) {
		normalizeFloat(elements, getter, setter, 0, 1);
	}
	public static <T> void normalizeFloat(Iterable<T> elements, Function<T,Float> getter, BiConsumer<T,Float> setter, float min_new, float max_new) {
		Pair<Float,Float> minMax = minMaxFloat(elements, getter);
		float min = minMax.getA(), max = minMax.getB(), range = max - min;
		if(range == 0){
            for(T element : elements)
            	setter.accept(element, max);
        }else{
        	float a = (max_new - min_new) / range;
        	if(a == 1) return; //already in desired interval
        	float b = max_new - a*max;
			for(T element : elements)
                setter.accept(element, a*getter.apply(element) + b);
        }
	}
	public static <T> void normalize(Iterable<T> elements, Function<T,Double> getter, BiConsumer<T,Double> setter, double min_new, double max_new) {
		Pair<Double,Double> minMax = minMax(elements, getter);
		double min = minMax.getA(), max = minMax.getB(), range = max - min;
		if(range == 0){
            for(T element : elements)
            	setter.accept(element, max);
        }else{
        	double a = (max_new - min_new) / range;
        	if(a == 1) return; //already in desired interval
        	double b = max_new - a*max;
			for(T element : elements)
                setter.accept(element, a*getter.apply(element) + b);
        }
	}
	public static void normalize(double[] elements, double min_new, double max_new) {
        Pair<Double,Double> minMax = minMax(elements);
        double min = minMax.getA(), max = minMax.getB(), range = max - min;
        if(range == 0){
            for (int i = 0; i < elements.length; i++)
                elements[i] = max;
        }else{
            double a = (max_new - min_new) / range;
            if(a == 1) return; //already in desired interval
            double b = max_new - a*max;
            for (int i = 0; i < elements.length; i++)
                elements[i] = a*elements[i] + b;
        }
    }

	/** 2ab/(a+b). We return 0 when 2ab = 0 */
	public static float harmonicMean(float a, float b) {
	    float temp = 2F*a*b;
	    if(temp == 0F) //treatment to avoid division by 0
	        return 0F;
        return temp / (a+b);
    }

	public static float[] harmonicMeans(float[] a, float[] v) {
        Preconditions.checkArgument(a.length == v.length);
        float[] harmonicMeans = new float[a.length];
        for (int i = 0; i < harmonicMeans.length; i++)
            harmonicMeans[i] = harmonicMean(a[i], v[i]);
        return harmonicMeans;
    }

	public static long mean(long[] values) {
	    return sum(values) / values.length;
	}
	public static float mean(float[] values) {
		return sum(values) / values.length;
	}
	public static float mean(Float[] values) {
		return sum(values) / values.length;
	}
	public static double mean(double[] values) {
        return sum(values) / values.length;
    }
	public static <T> float mean(Collection<T> values, Function<T, Float> transformer) {
	    return sumFloat(values, transformer) / values.size();
	}
    public static double mean(Collection<Double> values) {
        return sum(values) / values.size();
    }
    public static float meanFloat(Collection<Float> values) {
        return sumFloat(values) / values.size();
    }

    public static long sum(long[] values) {
	    long total = 0;
        for (long element : values) {
            total += element;
        }
        return total;
    }
    public static int sum(Collection<MutableInt> values) {
        int total = 0;
        for (MutableInt element : values) {
            total += element.intValue();
        }
        return total;
    }
	public static float sum(float[] values) {
		float total = 0;
		for (float element : values) {
			total += element;
		}
		return total;
	}
	public static float sum(Float[] values) {
		float total = 0;
		for (float element : values) {
			total += element;
		}
		return total;
	}
	public static double sum(double[] values) {
        double total = 0;
        for(double element : values)
            total += element;
        return total;
    }
	public static <T> double sum(Collection<T> values, Function<T, Double> transformer) {
        double total = 0;
        for(T element : values)
            total += transformer.apply(element);
        return total;
    }
	public static <T> float sumFloat(Collection<T> values, Function<T, Float> transformer) {
	    float total = 0;
        for(T element : values)
            total += transformer.apply(element);
        return total;
	}
	public static double sum(Iterable<Double> values) {
        double total = 0;
        for(Double element : values)
            total += element;
        return total;
    }
	public static float sumFloat(Iterable<Float> values) {
        float total = 0;
        for(Float element : values)
            total += element;
        return total;
    }

	public static double variance(Collection<Double> values) {
		return variance(values, mean(values));
	}
	public static double variance(float[] values) {
	    return variance(values, mean(values));
	}
	public static double variance(Float[] values) {
        return variance(values, mean(values));
    }
	public static double variance(double[] values) {
        return variance(values, mean(values));
    }

	public static double variance(float[] values, float mean) {
	    float temp = 0;
        for (float a : values) {
            float diff = a - mean;
            temp += diff * diff;
        }
        return temp / values.length;
    }
	public static double variance(Float[] values, float mean) {
        float temp = 0;
        for (float a : values) {
            float diff = a - mean;
            temp += diff * diff;
        }
        return temp / values.length;
    }
	public static double variance(double[] values, double mean) {
        double temp = 0;
        for (double a : values) {
            double diff = a - mean;
            temp += diff * diff;
        }
        return temp / values.length;
    }
	public static double varianceFloat(Collection<Float> values, float mean) {
		float temp = 0;
		for (Float a : values) {
			float diff = a - mean;
			temp += diff * diff;
		}
		return temp / values.size();
	}
	public static <T> double variance(Collection<T> values, Function<T, Float> transformer, float mean) {
	    float temp = 0;
        for (T a : values) {
            float diff = transformer.apply(a) - mean;
            temp += diff * diff;
        }
        return temp / values.size();
	}
	private static double variance(Collection<Double> values, double mean) {
		double temp = 0;
		for (Double a : values) {
			double diff = a - mean;
			temp += diff * diff;
		}
		return temp / values.size();
	}


	public static double standardDeviation(float[] values) {
	    return standardDeviation(variance(values));
	}
	public static double standardDeviation(Float[] values) {
        return standardDeviation(variance(values));
    }
	public static double standardDeviation(double[] values) {
        return standardDeviation(variance(values));
    }
	public static double standardDeviation(Collection<Double> values) {
        return standardDeviation(variance(values));
    }

	public static double standardDeviation(double variance) {
		return Math.sqrt(variance);
	}

	public static double squareSum(float[] values) {
	    double s = 0;
	    for (int i = 0; i < values.length; i++)
	        s += values[i] * values[i];
	    return s;
	}

	public static double squareSum(Collection<Float> values) {
	    double s = 0;
        for (Float v : values)
            s += v * v;
        return s;
	}

	public static double norm(float[] values) {
		return Math.sqrt(squareSum(values));
    }

	public static double norm(Collection<Float> values) {
		return Math.sqrt(squareSum(values));
	}

	public static <T> List<Integer> indicesOfValuesLowerThan(T[] array, T value) {
		List<Integer> indices = new ArrayList<>();
		Comparable value_ = (Comparable) value;

		for (int i = 0; i < array.length; i++) {
			if(value_.compareTo(array[i]) >= 0){
				indices.add(i);
			}
		}
		return indices;
	}

	public static int argmin(float[] dist) {
		int argmin = 0;
		float min = Float.MAX_VALUE;
		for (int i = 0; i < dist.length; i++) {
			float v = dist[i];
			if(v < min){
				argmin = i;
				min = v;
			}
		}
		return argmin;
	}

	public static float[] getMaxValuesFromRows(float[][] matrix) {
		float[] maxValuesInRows = new float[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			maxValuesInRows[i] = max(matrix[i]);
		}
		return maxValuesInRows;
	}

	public static float euclideanSimilarity(FloatVector p1, FloatVector p2) {
        return 1F - euclideanDistance(p1, p2);
    }
	public static float euclideanDistance(FloatVector p1, FloatVector p2) {
		if(p1 instanceof FloatVectorSparse)
		    return euclideanDistanceSparse((FloatVectorSparse)p1, (FloatVectorSparse)p2);
		double sum = 0, temp;
		for (int i = 0; i < p1.length(); i++) {
			temp = p1.get(i) - p2.get(i);
			sum += temp * temp;
		}
		return (float) Math.sqrt(sum);
	}
	public static float euclideanDistanceSparse(FloatVectorSparse p1, FloatVectorSparse p2) { //an improved implementation to avoid some operations when the vectors are sparse
        double sum = 0, temp;
        //primeiro itera pelos elementos contidos em p1, vs p2, depois o contrario porem desconsiderando os contidos os contidos em p1 pra nao repetir
        for(Entry<Integer,Float> idx_value1 : p1.getValues().entrySet()){
            temp = idx_value1.getValue() - p2.get(idx_value1.getKey());
            sum += temp * temp;
        }
        for(Entry<Integer,Float> idx_value2 : p2.getValues().entrySet()){
            if(p1.getInternal(idx_value2.getKey()) == null){
                temp = idx_value2.getValue(); //0 - v2, which will be use for (-v2)*(-v2), so the signal can be ignored
                sum += temp * temp;
            }
        }
        return (float) Math.sqrt(sum);
    }

	public static float squareDiference(float value1, float value2, float range) {
        if (range == 0) {
            range = 1;
        }
        float aux = (value1 - value2) / range;
        return aux * aux;
    }

    public static int overlapDistance(Object l1, Object l2) {
        return l1.equals(l2) ? 0 : 1;
    }

    public static float manhattanDistance(FloatVector a, FloatVector b){
        double dist = 0;
        for (int i = 0; i < b.length(); i++) {
            dist += Math.abs(a.get(i) - b.get(i));
        }
        return (float) dist;
    }

    public static float cosineDistance(FloatVector vectorA, FloatVector vectorB) {
        return 1.0F - cosineSimilarity(vectorA, vectorB);
    }

    //cosine(theta) = (A . B) / ( ||A|| ||B|| )
    //For a vector A = (a1, a2, ..., an):
    //    A.B is the dot product = summation(ai * bi), for all indices i
    //    ||A|| = sqrt(a1^2 + a2^2 + ... + an^2)
    public static float cosineSimilarity(FloatVector a, FloatVector b) {
    	if(a instanceof FloatVectorSparse){ //an improved implementation to avoid some operations when the vectors are sparse
    		Map<Integer,Float> a_ = ((FloatVectorSparse)a).getValues(), b_ = ((FloatVectorSparse)b).getValues();
    		if (a_.size() > b_.size()) { //swap, so we iterate over the shortest collection
    			Map<Integer,Float> temp = a_;
    			a_ = b_;
    			b_ = temp;
    		}
    		double dotProduct = 0;
    		double squareNormA = 0;
    		for(Entry<Integer,Float> aiEntry : a_.entrySet()){
    			Float ai = aiEntry.getValue();
    			Float bi = b_.get(aiEntry.getKey());
    			if(bi != null){ dotProduct += ai * bi; }
    			squareNormA += ai * ai;
    		}
    		if(squareNormA == 0D)
    			return 0F;
    		double normB = norm(b_.values());
    		if(normB == 0D)
    			return 0F;
    		return (float) (dotProduct / (Math.sqrt(squareNormA) * normB));
		} else {
    		double dotProduct = 0;
    		double squareNormA = 0;
    		double squareNormB = 0;
    		for (int i = 0; i < a.length(); i++) {
    			float ai = a.get(i);
    			float bi = b.get(i);
    			dotProduct += ai * bi;
    			squareNormA += ai * ai;
    			squareNormB += bi * bi;
    		}
    		if(squareNormA == 0D || squareNormB == 0D)
    			return 0F;
    		return (float) (dotProduct / (Math.sqrt(squareNormA) * Math.sqrt(squareNormB)));
    	}
	}

	//Ioffe, Sergey. "Improved consistent sampling, weighted minhash and l1 sketching." Data Mining (ICDM), 2010 IEEE 10th International Conference on. IEEE, 2010.
    public static float jaccardDistance(FloatVector vectorA, FloatVector vectorB) {
    	return 1.0F - jaccardSimilarity(vectorA, vectorB);
    }

    //somatorio( min(ai, bi) ) / somatorio( max(ai, bi) ), variando i para todo indice dos vetores
    public static float jaccardSimilarity(FloatVector vectorA, FloatVector vectorB) {
    	if(vectorA instanceof FloatVectorSparse)
    	    return jaccardSimilaritySparse((FloatVectorSparse)vectorA, (FloatVectorSparse)vectorB);
		double sumMins = 0, sumMaxs = 0;
		float ai, bi;
		for (int i = 0; i < vectorA.length(); i++) {
			ai = vectorA.get(i); bi = vectorB.get(i);
			if(ai < bi){
				sumMins += ai; sumMaxs += bi;
			}else{
				sumMins += bi; sumMaxs += ai;
			}
		}
		return sumMaxs != 0D ? (float)(sumMins / sumMaxs) : 0F;
    }
	public static float jaccardSimilaritySparse(FloatVectorSparse p1, FloatVectorSparse p2) { //an improved implementation to avoid some operations when the vectors are sparse
	    double sumMins = 0, sumMaxs = 0;
        float ai, bi;
        for(Entry<Integer,Float> idx_value1 : p1.getValues().entrySet()){
            ai = idx_value1.getValue(); bi = p2.get(idx_value1.getKey());
            if(ai < bi){
                sumMins += ai; sumMaxs += bi;
            }else{
                sumMins += bi; sumMaxs += ai;
            }
        }
        for(Entry<Integer,Float> idx_value2 : p2.getValues().entrySet()){ //itera para todos p2, vs p1, mas desconsiderando as entradas de p2 consideradas no loop anterior. ps: aqui, p1[i] é sempre 0, então codigo está simplificado...
            if(p1.getInternal(idx_value2.getKey()) == null){
                bi = idx_value2.getValue();
                if(0F < bi){
                    sumMaxs += bi;
                }else{
                    sumMins += bi;
                }
            }
        }
        return sumMaxs != 0D ? (float)(sumMins / sumMaxs) : 0F;
    }

    public static Pair<Float,Double> meanAndStandardDeviation(Collection<Float> values) {
    	float mean = meanFloat(values);
        double variance = varianceFloat(values, mean);
        double stdDev = standardDeviation(variance);
        return new Pair<>(mean, stdDev);
	}

    public static String printMeanAndStandardDeviation(float[] values, boolean asPercent) {
        float mean = mean(values);
        double variance = variance(values, mean);
        double stdDev = standardDeviation(variance);
        return printMeanAndStandardDeviation(mean, stdDev, asPercent);
    }
    public static String printMeanAndStandardDeviation(double[] values, boolean asPercent) {
        double mean = mean(values);
        double variance = variance(values, mean);
        double stdDev = standardDeviation(variance);
        return printMeanAndStandardDeviation(mean, stdDev, asPercent);
    }
    public static String printMeanAndStandardDeviationFloat(Collection<Float> values, boolean asPercent) {
    	float mean = meanFloat(values);
        double variance = varianceFloat(values, mean);
        double stdDev = standardDeviation(variance);
        return printMeanAndStandardDeviation(mean, stdDev, asPercent);
    }
    public static <T> String printMeanAndStandardDeviation(Collection<T> values_, boolean asPercent, ToFloatFunction<T> transformer) {
        return printMeanAndStandardDeviation(asFloatArray(values_, transformer), asPercent);
    }
    public static String printMeanAndStandardDeviation(double mean, double stdDev, boolean asPercent) {
        if(asPercent)
            return printPercent(mean) + " +- " + printPercent(stdDev);
        else
            return print(mean) + " +- " + print(stdDev);
    }

    public static String printPercent(double percent) {
        return getDecimalFormatter().format(percent * 100D);
    }
    public static String print(double v) {
        return getDecimalFormatter().format(v);
    }

    private static synchronized NumberFormat getDecimalFormatter() {
        if (percentFormatter == null) {
            percentFormatter = new DecimalFormat();
            percentFormatter.setMinimumFractionDigits(2);
            percentFormatter.setMaximumFractionDigits(2);
            percentFormatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
            percentFormatter.setRoundingMode(RoundingMode.UP);
        }
        return percentFormatter;
    }

    public static long bytesToMB(long bytes) {
		return bytes / ONE_MEGA_BYTE_IN_BYTES;
	}

    /**
     * @return true if minMax null or min <= value <= max
     */
	public static boolean isInRange(int value, Pair<Integer, Integer> minMax) {
		return minMax == null || (value >= minMax.getA().intValue() && value <= minMax.getB().intValue());
	}

	public static IntPredicate asIntPredicateTrue() {
		return k -> true;
	}
	public static IntPredicate asIntPredicateTrueLessThan(int limit) {
		return k -> k < limit;
	}
	public static IntPredicate asIntPredicate(boolean... array) {
		return k -> array[k];
	}
}
