import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class RecognitionTask implements Runnable {
	
	public RecognitionTask(BlockingQueue<File> queue, int char_for_sentence, int char_for_speaker, ArrayList<SpeakerModel> speaker_models, int numOfFiles){
		this.queue = queue;
		this.char_for_sentence = char_for_sentence;
		this.char_for_speaker = char_for_speaker;
		this.speaker_models = speaker_models;
		this.numOfFiles = numOfFiles;
	}
	
	
	public void run(){
		try{
			boolean done = false;
			while(!done)
			{
				File file = queue.take();
				if(file == FileEnumerationTask.DUMMY){
					queue.put(file);
					done = true;
				} else
					try {
						
						recognition(file, char_for_speaker, char_for_sentence, speaker_models);
						
					} catch (MyException e) {
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
	
	public void recognition(File file, int char_for_speaker, int char_for_sentence, ArrayList<SpeakerModel> speaker_models) throws IOException, MyException{
		
		extension = file.getName().substring(file.getName().length()-3);
		if(file.isFile() && (extension.equals("wav") | extension.equals("WAV"))){
			fileName = file.getName().substring(0, file.getName().length()-4);
			
			if(fileName.length()!=(this.char_for_sentence+this.char_for_speaker)){
	    		  throw new MyException("File name "+fileName+" does not meet previously set conditions");
	    	  }
			tested_speaker_name = fileName.substring(0, this.char_for_speaker);
						
			WavFile wavFile = new WavFile(file.toString());
			try{
				wavFile.open();
			}
			catch(Exception myEx)
	        {
	            //System.out.println("An exception encourred: " + myEx.getMessage());
	            myEx.printStackTrace();
	            System.exit(1);
	        }
			//System.out.println(file.toString());
			
			int fs = wavFile.getFs();
			int[] test_samples = wavFile.getSamples();
			MFCC mfcc = new MFCC(test_samples, fs);
			
			double[][] speaker_mfcc = mfcc.getMFCC();
			
			score_temp = Double.NEGATIVE_INFINITY;
			score_final = Double.NEGATIVE_INFINITY;
			winner = null;
			
			for(SpeakerModel model : speaker_models){
				score_temp = model.getScore(speaker_mfcc);
				if(score_temp > score_final){
					score_final = score_temp;
					winner = model.getName();
				}
			}
			
			System.out.println("Speaker "+tested_speaker_name+" recognized as "+winner);
			incrementPositiveNegativeTests();
			
			if(num_of_all_tests == numOfFiles){
				System.out.println("\nOverall efficiency = "+Double.toString(((double)num_of_positive_tests/(double)num_of_all_tests)*100)+" %");
			}
			
			
		}
		
	}
	

	
	public synchronized void incrementPositiveNegativeTests() {
		if(winner.equals(tested_speaker_name)){
			
			num_of_positive_tests+=1;
		}
		num_of_all_tests+=1;
	}
	
	
	private static int num_of_all_tests = 0;
	private static int num_of_positive_tests = 0;
	private String tested_speaker_name = null;
	private double score_temp;
	private double score_final;
	private String winner = null;
	private int char_for_speaker = 0;
	private int char_for_sentence = 0;
	private String fileName = null;
	private String extension = null;
	private BlockingQueue<File> queue;
	private ArrayList<SpeakerModel> speaker_models = new ArrayList<SpeakerModel>();
	private int numOfFiles;


}
