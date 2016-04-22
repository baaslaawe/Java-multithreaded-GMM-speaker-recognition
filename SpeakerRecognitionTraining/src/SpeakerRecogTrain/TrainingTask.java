package SpeakerRecogTrain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class TrainingTask implements Runnable{
	
	
	public TrainingTask(BlockingQueue<File> queue, int char_for_sentence, int char_for_speaker, int numOfSpeakers, String databaseOutputFile){
		this.queue = queue;
		this.char_for_sentence = char_for_sentence;
		this.char_for_speaker = char_for_speaker;
		this.numOfSpeakers = numOfSpeakers;
		this.databaseOutputFile = databaseOutputFile;
		
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
						
						train(file, char_for_speaker, char_for_sentence, speaker_models);
						
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
	
	public void train(File file, int char_for_speaker, int char_for_sentence, ArrayList<SpeakerModel> speaker_models) throws IOException, MyException{
		
		extension = file.getName().substring(file.getName().length()-3);
		if(file.isFile() && (extension.equals("wav") | extension.equals("WAV"))){
			fileName = file.getName().substring(0, file.getName().length()-4);
			
			if(fileName.length()!=(this.char_for_sentence+this.char_for_speaker)){
	    		  throw new MyException("File name "+fileName+" does not meet previously set conditions");
	    	  }
			
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
			System.out.println(file.toString());
			
			int fs = wavFile.getFs();
			int[] test_samples = wavFile.getSamples();
			MFCC mfcc = new MFCC(test_samples, fs);
			
			double[][] speaker_mfcc = mfcc.getMFCC();
			
			GMM gmm = new GMM(speaker_mfcc, 32);
			gmm.fit();
			ArrayList<String> sentences = new ArrayList<String>();
			sentences.add(fileName.substring(char_for_speaker, char_for_speaker+char_for_sentence));
			
			synchronized(speaker_models){
				speaker_models.add(new SpeakerModel(gmm.get_means(), gmm.get_covars(), gmm.get_weights(), fileName.substring(0, char_for_speaker), sentences));
			}

			
			if(speaker_models.size()==numOfSpeakers){
				System.out.println("GENERAL STOP TIME = "+System.currentTimeMillis());
				FileOutputStream fout = new FileOutputStream(databaseOutputFile);
				ObjectOutputStream wy = new ObjectOutputStream(fout);	
				 wy.writeObject(speaker_models);
				 wy.close();
			}
		}
	}
	

	
	private int char_for_speaker = 0;
	private int char_for_sentence = 0;
	private String fileName = null;
	private String extension = null;
	private BlockingQueue<File> queue;
	private static ArrayList<SpeakerModel> speaker_models = new ArrayList<SpeakerModel>();
	private int numOfSpeakers;
	private String databaseOutputFile;

}
