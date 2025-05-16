package math;

import java.math.BigInteger;

public class CachedMath {
    private static final BigInteger[] FACTORIAL_CACHE = new BigInteger[21];

    static {
        FACTORIAL_CACHE[0] = BigInteger.ONE;
        for (int i = 1; i < FACTORIAL_CACHE.length; i++) {
            FACTORIAL_CACHE[i] = FACTORIAL_CACHE[i - 1].multiply(BigInteger.valueOf(i));
        }
    }

    public static BigInteger calculateFactorial(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }

        if (number < FACTORIAL_CACHE.length) {

            return FACTORIAL_CACHE[number];
        }

        BigInteger result = FACTORIAL_CACHE[FACTORIAL_CACHE.length - 1];
        for (int i = FACTORIAL_CACHE.length; i <= number; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }

        return result;
    }
}