package utils;

public class NumericalFunctions {
	public static double addLogs(double x, double y) {
		if (y <= x) {
			return x + Math.log1p(Math.exp(y - x));
		} else {
			return y + Math.log1p(Math.exp(x - y));
		}
	}

	/*
	 * e^x + e^y = e^z - need to solve for z
	 * 
	 * z = log(e^x + e^y) = log(e^x.(e^x + e^y)/e^x) = x + log(1 + e^(y-x))
	 * 
	 * useful since e^(y-x) is presumably easier to calculate than either e^x or
	 * e^y
	 */

	public static double adaGradUpdate(double gradient, double sumGradSquared, double learningRate) {
		double eps = 0.001;

		return learningRate * gradient / (Math.sqrt(sumGradSquared) + eps);
	}
}
