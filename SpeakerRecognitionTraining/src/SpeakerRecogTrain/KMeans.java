package SpeakerRecogTrain;

import java.util.Arrays;

public class KMeans {
	private int numOfClusters;
	private int numOfRows;
	private int numOfCols;
	private double[][] data;
	private int n_iter = 0;
	private int n_init = 10;
	private int max_iter = 300;
	private double tol = 0.0001;
	
	// output parameters////
	private double[][] best_cluster_centers = null;
	private int[] best_labels = null;
	private double best_inertia = Double.MAX_VALUE;
	private int n_iter_ = 0;
	/////////////////////////
	
	
	KMeans(double[][] x, int numOfClust){
		this.numOfClusters = numOfClust;
		//int numOfClusters = this.numOfCols;
		int n_init = 10;
		int max_iter = 300;
		double tol = 0.0001;
		this.tol = tolerance(x, tol);
		this.numOfRows = x.length;
		this.numOfCols = x[0].length;
		this.data = Matrixes.copy2dArray(x);
		this.best_cluster_centers = new double[numOfClust][x[0].length];
		this.best_labels = new int[x.length];
		
	}
	
	public void fit(){
		double[][] cluster_centers = null;
		int[] labels = null;
		double inertia = 0;
		//int n_iter = 0;
		//double[] result = null;
		//double[][] centers = new double[this.numOfClusters][this.numOfCols];
		
		try{
		
			////////// substracting mean //////////////
			double[] X_mean = Statistics.getMean(data);
			for(int i=0;i<this.numOfRows;i++){
				for(int j=0; j<this.numOfCols; j++){
					data[i][j] -= X_mean[j];
				}
			}
			
			
			
			////////// numpy einsum //////////////
			double[] x_squared_norms = Matrixes.einsum(data);
			
			for(int i=0;i<this.n_init;i++){
				Kmeans_single kmeans_single = new Kmeans_single (this.data, this.numOfClusters, x_squared_norms, this.max_iter, this.tol);
				cluster_centers = kmeans_single.get_best_centers().clone();
				inertia = kmeans_single.get_best_inertia();
				labels = kmeans_single.get_best_labels().clone();
				
				if (inertia<this.best_inertia){
					this.best_labels = labels.clone();
					this.best_inertia = inertia;
					this.best_cluster_centers = cluster_centers.clone();
					
				}
				
			}
			
			this.best_cluster_centers = Matrixes.addValue(this.best_cluster_centers, X_mean);
		}
		catch(Exception myEx)
        {
            //System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);
        }
		
		
		
		//System.out.println("kmeans end");
		//return result;
	
	}
	
	public double[][] get_centers(){
		return this.best_cluster_centers;
	}
	
	
	
	private double[][] centers_dense(double[][] X, int[] labels, int n_clusters, double[] distances){
		double[][] result = new double[n_clusters][X[0].length];
		for(int j=0;j<X[0].length;j++){
			double[] sum = new double[n_clusters];
			for(int k=0;k<n_clusters;k++){
				int samples_num = 0;
				for(int z=0;z<labels.length;z++){
					if(labels[z]==k){
						sum[k] += X[z][j];
						samples_num += 1;
					}
				}
				sum[k] /= samples_num;
				
			}
			for(int i=0;i<n_clusters;i++)
				result[i][j] = sum[i];
		}
		return result;
		
	}
	
	private double[][] init_centroids(double[][] data, int n_clusters, double[] x_sq_norms){
		double centers[][] = new double[n_clusters][this.numOfCols];
		
		try{
			int n_local_trials = 2+(int)(Math.log(n_clusters));
			/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/int center_id = (int) Math.floor(Math.random() * this.numOfRows);
			for(int i=0;i<this.numOfCols;i++){
				centers[0][i] = data[center_id][i];
			}
			double[] closest_dist_sq =  Matrixes.euclidean_distances(centers[0], data, x_sq_norms);
			double current_pot = Matrixes.sum(closest_dist_sq);
			
			for(int c=1; c<n_clusters;c++){
				/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/ double[] rand_vals = Matrixes.genRandMatrix(current_pot, n_local_trials);
				double[] closest_dist_sq_cumsum = Matrixes.cumsum(closest_dist_sq);
				int[] candidate_ids = Matrixes.searchsorted(closest_dist_sq_cumsum, rand_vals);
				double[][] data_candidates = new double[n_local_trials][this.numOfCols];
				
				for(int z=0;z<n_local_trials;z++){
					for(int j=0;j<this.numOfCols;j++){
						data_candidates[z][j] = data[candidate_ids[z]][j];
					}
				}
					
				int best_candidate = -1;
				double best_pot = 99999999;
				double[] best_dist_sq = null;
	
				double[][] distance_to_candidates = Matrixes.euclidean_distances(data_candidates, data, x_sq_norms);
				
				for(int trial=0;trial<n_local_trials;trial++){
					double[] new_dist_sq = Matrixes.minimum(closest_dist_sq, Matrixes.select_row(distance_to_candidates, trial));
					double new_pot = Matrixes.sum(new_dist_sq);
					
					if(best_candidate==-1 | new_pot < best_pot){
						best_candidate = candidate_ids[trial];
						best_pot = new_pot;
						best_dist_sq = Arrays.copyOf(new_dist_sq, new_dist_sq.length);
					}
				}
				double[] center_temp = Arrays.copyOf(data[best_candidate], data[best_candidate].length);
				for(int ii=0;ii<centers[0].length;ii++){
					centers[c][ii] = center_temp[ii];
				}
				current_pot = best_pot;
				closest_dist_sq = Arrays.copyOf(best_dist_sq, best_dist_sq.length);
				//System.out.println("temp");
				
			}
		}
		catch(Exception myEx)
        {
            //System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);
        }
		
		return centers;
		
	}
	
	
	private double tolerance(double[][] x, double tol){
		
		double temp[] = Statistics.getVariance(x);
		
		for(int i=0;i<temp.length;i++){
			temp[i] = temp[i]*tol;
		}
		return Statistics.getMean(temp);
	}
	

	
	/*private void labels_inertia(double[][]data, double x_squared_norms, double[][] centers, double[] distances){
	
	}*/
	
	private class Kmeans_single{
		private int[] best_labels = null;
		private double[][] best_centers = null;
		private double best_inertia = Double.MAX_VALUE;
		private double[] distances = null;
		
		Kmeans_single(double[][] data, int n_clusters, double[] x_sq_norms, int max_iter, double tol){
			
			try{

				double[][] centers = init_centroids(data, n_clusters, x_sq_norms);
				this.distances = new double[data.length];
				
				for(int i=0; i<max_iter;i++){
					double[][] centers_old = centers.clone();
					LabelsInertia labelsInertia = new LabelsInertia(data, x_sq_norms, centers, this.distances);
					int[] labels = labelsInertia.getLabels().clone();
					double inertia = labelsInertia.getInertia();
					this.distances = labelsInertia.getDistances().clone();
					
					centers = centers_dense(data, labels, n_clusters, distances);
					
					if (inertia<best_inertia){
						this.best_labels = labels.clone();
						this.best_centers = centers.clone();
						this.best_inertia = inertia;
					}
					
					if (Matrixes.squared_norm(Matrixes.substractMatrixes(centers_old, centers))<=tol)
						break;
					
				}
			}
			catch(Exception myEx)
	        {
	            //System.out.println("An exception encourred: " + myEx.getMessage());
	            myEx.printStackTrace();
	            System.exit(1);
	        }
		}
		
		public int[] get_best_labels(){
			return this.best_labels;
		}
		
		public double[][] get_best_centers(){
			return this.best_centers;
		}
		
		public double get_best_inertia(){
			return this.best_inertia;
		}
	}
	
	private class LabelsInertia{
		private double[][] data = null;
		private int[] labels = null;
		private double[][] centers = null;
		private double[] distances = null;
		private double[] x_squared_norms = null;
		private double inertia = 0;
		
		private int[] getLabels(){
			return this.labels.clone();
		}
		
		private double getInertia(){
			return this.inertia;
		}
		
		private double[] getDistances(){
			return this.distances.clone();
		}
		
		LabelsInertia(double[][] X, double[] x_squared_norms, double[][] centers, double[] distances){
			this.centers = centers;
			this.x_squared_norms = x_squared_norms;
			this.distances = distances;
			this.data = X;
			
			int n_samples = data.length;
			int[] labels = new int[n_samples];
			labels = Matrixes.addValue(labels, -1);
			
			LabelsPrecomputeDence result = new LabelsPrecomputeDence(this.data, this.x_squared_norms, this.centers, this.distances);
			this.labels = result.labels.clone();
			this.inertia = result.inertia;
			this.distances = result.distances.clone();
			
		}
		
		private class LabelsPrecomputeDence{
			private double[][] data = null;
			private int[] labels = null;
			private double[][] centers = null;
			private double[] distances = null;
			private double[] x_squared_norms = null;
			private double inertia = 0;
			
			LabelsPrecomputeDence(double[][] X, double[] x_squared_norms, double[][] centers, double[] distances){
				this.centers = centers;
				this.x_squared_norms = x_squared_norms;
				this.distances = distances; ////////// huston, problem - k_means.py line 490, niejawne zwracanie
				this.data = X;
				
				int n_samples = data.length;
				int k = centers.length;
				double[][] all_distances = Matrixes.euclidean_distances(centers, X, x_squared_norms);
				this.labels = new int[n_samples];
				this.labels = Matrixes.addValue(this.labels, -1);
				double[] mindist = new double[n_samples];
				mindist = Matrixes.addValue(mindist, Double.POSITIVE_INFINITY);
				
				for(int center_id=0;center_id<k;center_id++){
					double[] dist = all_distances[center_id];
					for(int i=0;i<labels.length;i++){
						if(dist[i]<mindist[i]){
							this.labels[i] = center_id;
						}
						mindist[i]=Math.min(dist[i], mindist[i]);
					}
				}
				if(n_samples == this.distances.length)
					this.distances = mindist;
				this.inertia = Matrixes.sum(mindist);
			}
		}
	}

	

}
