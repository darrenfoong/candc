package utils;

public class Benchmark {
	public static long getTime() {
		return System.nanoTime();
	}

	public static long getMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	public static void printTime(String description, long startTime, long endTime) {
		long elapsedTime = endTime - startTime;
		double elapsedTimeSecs = elapsedTime/Math.pow(10,9);
		double elapsedTimeMins = elapsedTimeSecs/60;
		System.out.println("# TIME # " + description + ": " + elapsedTime + " ns (" + elapsedTimeSecs + " s) (" + elapsedTimeMins + " mins)");
	}

	public static void printMemory(String description, long startMemory, long endMemory) {
		long usedMemory = endMemory - startMemory;
		double usedMemoryMega = usedMemory/(1024L * 1024L);
		System.out.println("# MEMORY # " + description + ": " + usedMemory + " B (" + usedMemoryMega + " MB)");
	}
}
