package util;

public class FloatVectorFactory {

	public static FloatVector create(int length) {
		return /*denseVectors ? new FloatVectorDense(length) :*/ new FloatVectorSparse(length);
	}

    public static FloatVector concat(FloatVector v1, FloatVector v2) {
        return new FloatVectorCompound(v1, v2);
    }
}
