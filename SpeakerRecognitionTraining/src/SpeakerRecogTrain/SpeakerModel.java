package SpeakerRecogTrain;

import java.io.Serializable;
import java.util.ArrayList;

public class SpeakerModel implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double[][] means=null;
	private double[][] covars=null;
	private double[] weights = null;
	private String name = null;
	private ArrayList<String> test_nums;
	
	/*public Speaker_model(double[][] means, double[][] covars, double[] weights, String name, int[] test_nums){
		this.means = means;
		this.covars=covars;
		this.weights=weights;
		this.name = name;
		this.test_nums = test_nums;
	}*/
	
	public SpeakerModel(double[][] means, double[][] covars, double[] weights, String name, ArrayList<String> test_nums){
		this.means = means;
		this.covars=covars;
		this.weights=weights;
		this.name = name;
		this.test_nums = test_nums;
	}
	
	public double[][] getMeans(){
		return this.means;
	}
	
	public double[][] getCovars(){
		return this.covars;
	}
	
	public double[] getWeights(){
		return this.weights;
	}
	
	public String getName(){
		return this.name;
	}
	
	public ArrayList<String> getTestNums(){
		return this.test_nums;
	}
	
	public double getScore(double[][] data){
		double score = 0;
		double[] logprob = null;
		
		try{
			double[][] lpr = log_multivariate_normal_density(data, this.means, this.covars);
			lpr = Matrixes.addValue(lpr, Matrixes.makeLog(this.weights));
			logprob = Matrixes.logsumexp(lpr);
			score = Statistics.getMean(logprob);
		}
		catch(Exception myEx)
        {
            //System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);
        }
		
		return score;
	}
	
	private double[][] log_multivariate_normal_density(double[][] data, double[][] means, double[][] covars){
		//diagonal type
		double[][] lpr = new double[data.length][means.length];
		//int n_samples = data.length;
		int n_dim = data[0].length;
		
		try{
			double[] sumLogCov = Matrixes.sum(Matrixes.makeLog(covars), 1); //np.sum(np.log(covars), 1)
			double[] sumDivMeanCov = Matrixes.sum(Matrixes.divideElements(Matrixes.power(this.means, 2), this.covars),1); //np.sum((means ** 2) / covars, 1)
			double[][] dotXdivMeanCovT = Matrixes.multiplyByValue(Matrixes.multiplyByMatrix(data, Matrixes.transpose(Matrixes.divideElements(means, covars))), -2); //- 2 * np.dot(X, (means / covars).T)
			//double[][] q1 = Matrixes.divideElements(means, covars);
			//double[][] q2 = Matrixes.transpose(Matrixes.divideElements(means, covars));
			//double[][] q3 = Matrixes.multiplyByMatrix(data, q2);
			//double[][] q4 = Matrixes.multiplyByValue(q3, -2);
			double[][] dotXdivOneCovT = Matrixes.multiplyByMatrix(Matrixes.power(data,  2), Matrixes.transpose(Matrixes.invertElements(covars)));
			
			
			sumLogCov = Matrixes.addValue(sumLogCov,n_dim * Math.log(2*Math.PI)); //n_dim * np.log(2 * np.pi) + np.sum(np.log(covars), 1)
			sumDivMeanCov = Matrixes.addMatrixes(sumDivMeanCov, sumLogCov); // n_dim * np.log(2 * np.pi) + np.sum(np.log(covars), 1) + np.sum((means ** 2) / covars, 1)
			dotXdivOneCovT = Matrixes.sum(dotXdivOneCovT, dotXdivMeanCovT); //- 2 * np.dot(X, (means / covars).T) + np.dot(X ** 2, (1.0 / covars).T)
			dotXdivOneCovT = Matrixes.addValue(dotXdivOneCovT, sumDivMeanCov); // (n_dim * np.log(2 * np.pi) + np.sum(np.log(covars), 1) + np.sum((means ** 2) / covars, 1) - 2 * np.dot(X, (means / covars).T) + np.dot(X ** 2, (1.0 / covars).T))
			lpr = Matrixes.multiplyByValue(dotXdivOneCovT, -0.5);
		}
		catch(Exception myEx)
        {
			System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);		            
        }
		
		return lpr;
	}

}
