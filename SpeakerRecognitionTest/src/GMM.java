import java.math.*;

/*import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.distribution.fitting.*;
import org.apache.commons.math3.linear.EigenDecomposition.*;*/


// https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/distribution/fitting/MultivariateNormalMixtureExpectationMaximization.html
// https://www.ee.washington.edu/techsite/papers/documents/UWEETR-2010-0002.pdf

public class GMM {
	private static final double EPS = 2.2204460492503131e-16;
	private int n_init=10;
	private int n_iter = 10;
	private int numOfRows;
	private int numOfCols;
	private int maxIter;
	private double threshold;
	private int numOfComponents;
	private double[][] observations;
	private double min_covar = 0.001;
	private boolean converged = false; 
	private double current_log_likelihood = 0;
	private double prev_log_likelihood = Double.NaN;
	private double tol = 0.001;
	
	private double[] log_likelihoods = null;
	private double[][] responsibilities = null;
	
	private double[][] means = null;
	private double[] weights = null;
	private double[][] covars = null;
	
	private double[][] best_means = null;
	private double[] best_weights = null;
	private double[][] best_covars = null;
	
	//private MultivariateNormalMixtureExpectationMaximization gmm = null;
	
	
	GMM(double[][] data, int compNum){
		this.observations = data;
		this.numOfRows = data.length;
		this.numOfCols = data[0].length;
		this.numOfComponents = compNum;
		this.means = new double[compNum][data[0].length];
		this.weights = new double[data.length];
		this.covars = new double[compNum][data[0].length];
		//this.gmm = new MultivariateNormalMixtureExpectationMaximization(data);
	}
	
	GMM(double[][] data, int compNum, int maxIt){
		/*this.observations = data;
		this.numOfRows = data.length;
		this.numOfCols = data[0].length;
		this.numOfComponents = compNum;*/
		this(data, compNum);
		this.maxIter = maxIt;
		//this.gmm = new MultivariateNormalMixtureExpectationMaximization(data);
	}
	
	GMM(double[][] data, int compNum, int maxIt, double thr){
		/*this.observations = data;
		this.numOfRows = data.length;
		this.numOfCols = data[0].length;
		this.numOfComponents = compNum;*/
		this(data, compNum);
		this.maxIter = maxIt;
		this.threshold = thr;
		
		//this.gmm = new MultivariateNormalMixtureExpectationMaximization(data);
	}
	
	public void fit(){
		double change = 0;
		
		try{
		
			double[][] cv = new double[this.numOfCols][this.numOfCols];
			double max_log_prob = Double.NEGATIVE_INFINITY;
			
			for(int i=0;i<this.n_init;i++){
				KMeans kMeans = new KMeans(this.observations, this.numOfComponents);
				kMeans.fit();
				this.means = kMeans.get_centers();
				this.weights = Matrixes.fillWith(this.weights, (double)1/this.numOfComponents);
				
				this.covars = Matrixes.cov(Matrixes.transpose(this.observations)); //np.cov(X.T), gmm.py line 450
				cv = Matrixes.eye(this.observations[0].length, this.min_covar); //self.min_covar * np.eye(X.shape[1])
				this.covars = Matrixes.addMatrixes(this.covars, cv);
				this.covars = Matrixes.duplicate(Matrixes.chooseDiagonalValues(this.covars), this.numOfComponents);
	
				for(int j=0;j<this.n_iter;j++){
					prev_log_likelihood = current_log_likelihood;
					Score_samples score_samples = new Score_samples(this.observations, this.means, this.covars, this.weights);
					this.log_likelihoods = score_samples.getLogprob();
					this.responsibilities = score_samples.getResponsibilities();
					current_log_likelihood = Statistics.getMean(log_likelihoods);
					
					if(!Double.isNaN(prev_log_likelihood)){
						change = Math.abs(current_log_likelihood - prev_log_likelihood);
						if(change<this.tol){
							this.converged = true;
							break;
						}
							
					}
					
					/// do m-step - gmm.py line 509
					do_mstep(this.observations, this.responsibilities);
					
				}
				
				if (current_log_likelihood > max_log_prob){
					max_log_prob = current_log_likelihood;
					this.best_means = this.means;
					this.best_covars = this.covars;
					this.best_weights = this.weights;
					
				}
			}
			
			if(Double.isInfinite(max_log_prob))
				System.out.println("EM algorithm was never able to compute a valid likelihood given initial parameters");
		}
		catch(Exception myEx)
        {
            //System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);
        }
		/*gmm.fit(MultivariateNormalMixtureExpectationMaximization.estimate(this.observations, this.numOfComponents));
		MixtureMultivariateNormalDistribution x = gmm.getFittedModel();
		String xx = x.toString();
		System.out.println(xx);*/
				
	}
	
	public double[][] get_means(){
		return this.best_means;
	}
	
	public double[][] get_covars(){
		return this.best_covars;
	}
	
	public double[] get_weights(){
		return this.best_weights;
	}
	
	private void do_mstep(double[][] data, double[][] responsibilities){
		try{
			double[] weights = Matrixes.sum(responsibilities, 0);
			double[][] weighted_X_sum = Matrixes.multiplyByMatrix(Matrixes.transpose(responsibilities), data);
			double[] inverse_weights = Matrixes.invertElements(Matrixes.addValue(weights, 10*EPS));
			this.weights = Matrixes.addValue(Matrixes.multiplyByValue(weights, 1.0/(Matrixes.sum(weights)+10*EPS)), EPS);
			this.means = Matrixes.multiplyByValue(weighted_X_sum, inverse_weights);
			this.covars = covar_mstep_diag(this.means, data, responsibilities, weighted_X_sum, inverse_weights, this.min_covar);
		}
		catch(Exception myEx)
        {
			//System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);		            
        }
		
	}
	
	private double[][] covar_mstep_diag(double[][] means, double[][] X, double[][] responsibilities, double[][] weighted_X_sum, double[] norm, double min_covar){
		double[][] temp = null;
		try{
			double[][] avg_X2 = Matrixes.multiplyByValue(Matrixes.multiplyByMatrix(Matrixes.transpose(responsibilities), Matrixes.multiplyMatrixesElByEl(X, X)), norm);
			double[][] avg_means2 = Matrixes.power(means, 2);
			double[][] avg_X_means = Matrixes.multiplyByValue(Matrixes.multiplyMatrixesElByEl(means, weighted_X_sum),norm);
			temp = Matrixes.addValue(Matrixes.addMatrixes(Matrixes.substractMatrixes(avg_X2, Matrixes.multiplyByValue(avg_X_means, 2)), avg_means2), min_covar);
		}
		catch(Exception myEx)
        {
			System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);		            
        }
		return temp;
	}
	
	private class Score_samples{
		private double[][] data = null;
		private double[] log_likelihoods = null; 
		private double[][] means = null;
		private double[][] covars = null;
		private double[] weights = null;
		/////out matrixes////
		private double[] logprob = null;
		private double[][] responsibilities = null;
		/////////////////////
		
		
		Score_samples(double[][] X, double[][] means, double[][] covars, double[] weights){
			this.data = X;
			this.log_likelihoods = new double[X.length];
			this.responsibilities = new double[X.length][GMM.this.numOfComponents];
			this.means = means;
			this.covars = covars;
			this.weights = weights;
			
			
			try{
				double[][] lpr = log_multivariate_normal_density(this.data, this.means, this.covars);
				lpr = Matrixes.addValue(lpr, Matrixes.makeLog(this.weights));
				this.logprob = Matrixes.logsumexp(lpr);
				// gmm.py line 321
				this.responsibilities = Matrixes.exp(Matrixes.substractValue(lpr, logprob));
			}
			catch(Exception myEx)
	        {
	            //System.out.println("An exception encourred: " + myEx.getMessage());
	            myEx.printStackTrace();
	            System.exit(1);
	        }
			
		}
		
		public double[] getLogprob(){
			return this.logprob;
		}
		
		public double[][] getResponsibilities(){
			return this.responsibilities;
		}
		
		private double[][] log_multivariate_normal_density(double[][] data, double[][] means, double[][] covars){
			//diagonal type
			double[][] lpr = new double[data.length][means.length];
			int n_samples = data.length;
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
}
