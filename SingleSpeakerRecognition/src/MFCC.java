

import org.jtransforms.fft.DoubleFFT_1D;


public class MFCC {
	
	private int frame_len;
	private int frame_shift;
	private int fft_size;// = 256;
	private static int melfilter_bands = 40;
	private static int mfcc_num = 13;
	private static double power_spectrum_floor = 0.0001;
	private static double pre_emph = 0.95;
	private double[] window = null; 
	private double[][] M = null;
	//private double[] CF = null;
	private double[][] melfb_coeffs = null;
	private double[][] mfcc_coeffs = null;
	private int[] samples = null; 
	private int fs;
	private double[][] D1 = null;
	
	MFCC(int[] x, int y){
		this.fs = y;
		this.samples = x;
		this.frame_len = 256;//setFrameLen(fs); !!!!!!!!!!!!! ZMIANA !!!!!!!!!!!!!!!!!!!!!!
		this.fft_size = this.frame_len;
		this.frame_shift = setFrameShift(fs);
		window = hamming(frame_len);
		
		//this.melfb_coeffs = melfb(melfilter_bands, 256, fs); //!!!!!!!!!!!!!!!! USUN¥Æ !!!!!!!!!!!!!!!!!
		this.melfb_coeffs = melfb(melfilter_bands, fft_size, fs);
		
		this.D1 = dctmatrix(melfilter_bands);
		
		if(this.melfb_coeffs==null) System.out.println("Cannot initialize melfilter bank");
	}
	
/////////// setters for MFCC parameters ///////////////////////
	
	private int setFrameLen(int sample_rate){
		return (int) (0.025*(double)(sample_rate));
	}
	
	private int setFrameShift(int sample_rate){
		return (int) (0.0125*(double)(sample_rate));
	}
	
	private double[] hamming(int frame_len){
		double[] window_temp = new double[frame_len];
		for(int i=0;i<window_temp.length;i++){
			window_temp[i] = 0.54-0.46*Math.cos(2*Math.PI/(double)frame_len*((double)i+0.5));
		}
		return window_temp;
	}

////////////////////////////////////////////////////////////////

//////// getters for MFCC results/////////////////////////////
	double[][] getMFCC(){
		extract_MFCC();
		return this.mfcc_coeffs;
	}

///////////////// computation of mel filterbank ////////////////

	private double[][] melfb(int p, int n, int fs){
		// p - number of filterbanks
		// n - length of fft
		// fs - sample rate 
		
		double f0 = 700/(double)fs;
		int fn2 = (int)Math.floor((double)n/2);
		double lr = Math.log((double)1+0.5/f0)/(p+1);
		double[] CF = arange(1,p+1);
		
		for(int i=0;i<CF.length;i++){
			CF[i] = fs*f0*(Math.exp(CF[i]*lr)-1);
			//CF[i] = (Math.exp(CF[i]*lr));
		}
		
		double[] bl = {0, 1, p, p+1};
		
		for(int i=0;i<bl.length;i++){
			bl[i] = n*f0*(Math.exp(bl[i]*lr)-1);
		}
		
		int b1 = (int)Math.floor(bl[0])+1;
		int b2 = (int)Math.ceil(bl[1]);
		int b3 = (int)Math.floor(bl[2]);
		int b4 = Math.min(fn2, (int)Math.ceil(bl[3]))-1;
		double[] pf = arange(b1, b4+1);
		
		for(int i=0;i<pf.length;i++){
			pf[i] = Math.log(1+pf[i]/f0/(double)n)/lr;
		}
		
		double[] fp = new double[pf.length];
		double[] pm = new double[pf.length];
		
		for(int i=0;i<fp.length;i++){
			fp[i] = Math.floor(pf[i]);
			pm[i] = pf[i] - fp[i];
		}
		
		this.M = new double[p][1+fn2];
		int r=0;
		
		for(int i=b2-1;i<b4;i++){
			r = (int)fp[i]-1;
			this.M[r][i+1] += 2* (1-pm[i]);
		}
		
		for(int i=0;i<b3; i++){
			r = (int)fp[i];
			this.M[r][i+1] += 2* pm[i];
		}
		
		/////////// normalization part //////////
		
		//int xx = M.length;
		double[] temp_row = null;
		double row_energy = 0;
		//System.out.println(Integer.toString(M.length));
		for (int i=0;i<this.M.length;i++){
			temp_row = this.M[i];
			row_energy = energy(temp_row);
			if(row_energy < 0.0001)
				temp_row[i] = i;
			else{
				while(row_energy>1.01){
					temp_row = Matrixes.row_mul(temp_row, 0.99);
					row_energy = energy(temp_row);
				}
				while(row_energy<0.99){
					temp_row = Matrixes.row_mul(temp_row, 1.01);
					row_energy = energy(temp_row);
				}
			}
			this.M[i] = temp_row;
			
		}
		
	
		
		return this.M;		
	}

//////////////////////////////////////////////////////////////////////////////////////////

	private void extract_MFCC(){
		// https://gist.github.com/jongukim/4037243
		//http://dp.nonoo.hu/projects/ham-dsp-tutorial/05-sine-fft/
		
		if(this.samples!=null){
			DoubleFFT_1D fftDo = new DoubleFFT_1D(this.frame_len);
			double[] fft1 = new double[this.frame_len * 2];
			double[] fft_final = new double[this.frame_len/2+1];
			//int[] x = this.samples;
			int frames_num = (int)((double)(this.samples.length - this.frame_len)/(double)(this.frame_shift))+1;
			this.mfcc_coeffs = new double[frames_num][MFCC.mfcc_num];
			double[] frame = new double[this.frame_len];
							
			for(int i=0;i<frames_num;i++){
				
				for(int j=0;j<this.frame_len;j++){
					frame[j] = (double)this.samples[i*this.frame_shift+j];
				}
				
				try{
					frame = Matrixes.row_mul(frame, window);
				
					frame = preemphasis(frame);
					System.arraycopy(frame, 0, fft1, 0, this.frame_len);
					fftDo.realForwardFull(fft1);
					/*for(double d: fft1) {
			          System.out.println(d);
					}*/
					
					for(int k=0;k<(this.frame_len/2+1);k++){
						fft_final[k] = Math.pow(Math.sqrt(Math.pow(fft1[k*2],2)+Math.pow(fft1[k*2+1],2)), 2);
						
						if(fft_final[k]<power_spectrum_floor) fft_final[k]=power_spectrum_floor;
					}
					
					double[] dot_prod = Matrixes.multiplyByMatrix(this.melfb_coeffs, fft_final);
					for(int j=0;j<dot_prod.length;j++){
						dot_prod[j] = Math.log(dot_prod[j]);
					}
					//double[][]D1 = dctmatrix(melfilter_bands);
					dot_prod = Matrixes.multiplyByMatrix(this.D1, dot_prod);
					this.mfcc_coeffs[i] = dot_prod;
				}
				catch(Exception myEx)
		        {
					System.out.println("An exception encourred: " + myEx.getMessage());
		            myEx.printStackTrace();
		            System.exit(1);		            
		        }
				
			}
			//this.mfcc_coeffs = 
		}
		else{
			System.out.println("Vector of input samples is null");
		}
		
	}
	
	///////////// math functions ///////////////////////////////////////////////////////////////
		
	private static double[] arange(int x1, int x2){
		double[] temp = null;
		try{
		temp = new double[x2-x1];
			for(int i=0;i<temp.length;i++){
				temp[i] = x1+i;
			}
		
		}
		catch(IndexOutOfBoundsException e){
			System.err.println("IndexOutOfBoundsException: " + e.getMessage());
		}
		return temp;
	}
	
	private static double energy(double[] x){
		double en = 0;
		for(int i=0; i<x.length;i++)
			en = en + Math.pow(x[i], 2);
		return en;
		}
		
	private double[] preemphasis(double[] x){
		double[] y = new double[x.length];
		y[0] = x[0];
		for(int i=1;i<x.length;i++){
			y[i] = x[i]-MFCC.pre_emph*x[i-1];
		}
		return y;
	}

	private double[][] dctmatrix(int n){
		double[][] d1 = new double[n][n];
		double[][] x = Matrixes.meshgrid_ox(n);
		double[][] y = Matrixes.meshgrid_oy(n);
		for(int i=0;i<n;i++){
			for(int j=0;j<n;j++){
				x[i][j] = (x[i][j]*2+1)*Math.PI/(2*n);
			}
		}
		
		try{
			d1 = Matrixes.multiplyMatrixesElByEl(x, y);
		}
		catch(Exception myEx)
        {
            //System.out.println("An exception encourred: " + myEx.getMessage());
            myEx.printStackTrace();
            System.exit(1);
        }
		
		for(int i=0;i<n;i++){
			for(int j=0;j<n;j++){
				d1[i][j] = Math.sqrt(2/(double)n)*Math.cos(d1[i][j]);
			}
		}
		for(int i=0;i<n;i++){
			d1[0][i] /= Math.sqrt(2);
		}
		
		double[][] d = new double[MFCC.mfcc_num][n];
		for(int i=1;i<MFCC.mfcc_num+1;i++){
			for(int j=0;j<n;j++){
				d[i-1][j] = d1[i][j];
			}
			
		}
		
		return d;
	}
}
