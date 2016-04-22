

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class RecognitionTaskModelQueue implements Runnable{

	public RecognitionTaskModelQueue(BlockingQueue<SpeakerModel> queue, double[][] mfcc, String name, int num_of_speakers){
		this.queue = queue;
		this.name = name;
		this.mfcc = mfcc;
		this.num_of_speakers = num_of_speakers;

	}
	
	public void run() {
		try{
			boolean done = false;
			while(!done)
			{
				SpeakerModel model = queue.take();
				if(model == ModelEnumerationTask.DUMMY){
					queue.put(model);
					done = true;
				} else
					try {
						
						recognition(model, mfcc, name);
						
					} 
				catch (MyException e) {
						e.printStackTrace();
					}
			}

		}
		catch(IOException e){
			e.printStackTrace();
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void recognition(SpeakerModel model, double[][] mfcc, String name) throws IOException, MyException{
		score_temp = model.getScore(mfcc);
		winner = checkNewWinner(score_temp, model.getName(), winner);
		incrementNumOfTests();
		if(num_of_all_tests == num_of_speakers){
			System.out.println("Speaker "+name+" recognized as "+winner);
		}
		
	}
	
	public synchronized String checkNewWinner(double score_temp, String modelName, String winner) {
		
		if(score_temp > score_final){
			score_final = score_temp;
			winner = modelName;
		}
		return winner;
	}
	
	
	public static synchronized boolean getIsFinished(){
		return is_finished;
	}
	
	public static synchronized void incrementNumOfTests(){
		num_of_all_tests+=1;
	}
	
	public static synchronized void reinitializeClassVariables(){
		num_of_all_tests = 0;
		score_final = Double.NEGATIVE_INFINITY;
		winner = null;
		is_finished = false;
	}
	
	private static boolean is_finished = false;
	private String name;
	private static int num_of_all_tests = 0;
	private double score_temp;
	public static double score_final = Double.NEGATIVE_INFINITY;
	private static String winner = null;
	private BlockingQueue<SpeakerModel> queue;
	private double[][] mfcc = null;
	private int num_of_speakers;

}
