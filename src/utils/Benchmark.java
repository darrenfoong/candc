package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Benchmark {
	public static final Logger logger = LogManager.getLogger(Benchmark.class);

	public static long getTime() {
		return System.nanoTime();
	}

	public static void printTime(String description, long startTime, long endTime) {
		long elapsedTime = endTime - startTime;
		double elapsedTimeSecs = elapsedTime/Math.pow(10,9);
		double elapsedTimeMins = elapsedTimeSecs/60;
		logger.info("# TIME # " + description + ": " + elapsedTime + " ns (" + elapsedTimeSecs + " s) (" + elapsedTimeMins + " mins)");
	}
}
