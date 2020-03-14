package util;

import java.util.Random;

/**
 * Centraliza a criação de Random pra permitir o controle global de seeds
 */
public class RandomFactory {

	private static Long seed;

	public static void setSeed(Long seed) {
		RandomFactory.seed = seed;
	}

    public static synchronized Random create() {
        if(seed == null){
        	setSeed(0L);
        	Logs.warn("Random seed was not set. Forcing "+seed);
        	return new Random();
        }
        return new Random(seed);
    }
}
