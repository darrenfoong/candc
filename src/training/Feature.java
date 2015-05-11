package training;

import utils.NumericalFunctions;

public class Feature {
	private int ID;
	private double lambda;
	// feature weight
	private double lambdaUpdate;
	// used for the (hidden) perceptron
	private double cumulativeLambda;
	// for the averaged perceptron
	private double expectedValue;
	// estimated feature expectation (for the log-linear model)
	private double empiricalValue;
	// empirical feature expectation (for the log-linear model)
	private double sumGradSquared;
	// keeps track of squared gradients for adaGrad
	private int lastNumTrainInstances;
	// for the fast averaged perceptron

	public Feature(int ID) {
		this.ID = ID;
		this.lambda = 0.0;
		this.lambdaUpdate = 0.0;
		this.cumulativeLambda = 0.0;
		this.expectedValue = 0.0;
		this.empiricalValue = 0.0;
		this.sumGradSquared = 0.0;
		this.lastNumTrainInstances = 1;
	}

	public Feature(int ID, double lambda) {
		this.ID = ID;
		this.lambda = lambda;
		this.lambdaUpdate = 0.0;
		this.cumulativeLambda = lambda;
		this.expectedValue = 0.0;
		this.empiricalValue = 0.0;
		this.sumGradSquared = 0.0;
		this.lastNumTrainInstances = 1;
	}

	public int getID() {
		return ID;
	}

	public double getLambda() {
		return lambda;
	}

	public double getLambdaUpdate() {
		return lambdaUpdate;
	}

	public double getCumulativeLambda() {
		return cumulativeLambda;
	}

	public double getExpectedValue() {
		return expectedValue;
	}

	public double getEmpiricalValue() {
		return empiricalValue;
	}

	public void setID(int iD) {
		this.ID = iD;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	public void setLambdaUpdate(double lambdaUpdate) {
		this.lambdaUpdate = lambdaUpdate;
	}

	public void setExpectedValue(double expectedValue) {
		this.expectedValue = expectedValue;
	}

	public void setEmpiricalValue(double empiricalValue) {
		this.empiricalValue = empiricalValue;
	}

	public void resetExpValues() {
		this.expectedValue = 0.0;
		this.empiricalValue = 0.0;
	}

	public void perceptronUpdate() {
		lambda += lambdaUpdate;
		lambdaUpdate = 0.0;

		cumulativeLambda += lambda;
	}

	public void perceptronUpdateFast(int numTrainInstances) {
		double oldLambda = lambda;
		lambda += lambdaUpdate;
		lambdaUpdate = 0.0;

		cumulativeLambda += oldLambda * (numTrainInstances - lastNumTrainInstances - 1) + lambda;
		lastNumTrainInstances = numTrainInstances;
	}

	public void decrementLambdaUpdate() {
		this.lambdaUpdate--;
	}

	public void incrementLambdaUpdate() {
		this.lambdaUpdate++;
	}

	public void adaGradUpdate(double learningRate) {
		double gradient = empiricalValue - expectedValue;
		sumGradSquared += gradient * gradient;
		lambda += NumericalFunctions.adaGradUpdate(gradient, sumGradSquared, learningRate);
	}
}
