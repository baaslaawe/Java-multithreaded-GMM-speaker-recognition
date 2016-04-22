

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SpeakerRecogModelQueueTest {

	public static void main(String[] args) throws MyException {
		
		String fileToTest = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\test\\FADG01.wav"; // file with speaker voice to be recognized
		String speaker_database = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\ThreadedSpeakerRecognition\\database.dat"; //the database of speakers
		ArrayList<SpeakerModel> speaker_models = null;
		
		
		ObjectInputStream we;
		try {
			we = new ObjectInputStream(new FileInputStream(speaker_database));
			speaker_models = (ArrayList<SpeakerModel>)we.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		
		/* This solution assumes that there is more than one speech file of each speaker, and the files are named as:
		 * [SPEAKER_NAME][SENTENCE_NUMBER].wav
		 * E.G FADG00.wav, FADG01.wav, FADG02.wav etc. - number of caracters for speaker is 5 (FADG0) and for sentence number is 1 (0,1 or 2).
		 */
		
		
		int char_for_speaker = 5; 
		int char_for_sentence = 1;
		int numOfModels = speaker_models.size();
		
		final int FILE_QUEUE_SIZE = 20;
		final int SEARCH_THREADS = 8;

		String extension = null;
		String fileName = null;
		String tested_speaker_name = null;

		
		File file = new File(fileToTest);
			extension = file.getName().substring(file.getName().length()-3);
			if(file.isFile() && (extension.equals("wav") | extension.equals("WAV"))){
				fileName = file.getName().substring(0, file.getName().length()-4);
				
				if(fileName.length()!=(char_for_sentence+char_for_speaker)){
		    		  throw new MyException("File name "+fileName+" does not meet previously set conditions");
		    	  }
				tested_speaker_name = fileName.substring(0, char_for_speaker);
				
				WavFile wavFile = new WavFile(file.toString());
				try{
					wavFile.open();
				}
				catch(Exception myEx)
		        {
		            myEx.printStackTrace();
		            System.exit(1);
		        }
				
				int fs = wavFile.getFs();
				int[] test_samples = wavFile.getSamples();
				MFCC mfcc = new MFCC(test_samples, fs);
				
				double[][] speaker_mfcc = mfcc.getMFCC();
				
				BlockingQueue<SpeakerModel> queue = new ArrayBlockingQueue<SpeakerModel>(FILE_QUEUE_SIZE);
				
				Thread[] threads = new Thread[SEARCH_THREADS];
				ModelEnumerationTask enumerator = new ModelEnumerationTask(queue, speaker_models);
				new Thread(enumerator).start();

				for(int i=0; i<SEARCH_THREADS; i++ ){
					threads[i] = new Thread(new RecognitionTaskModelQueue(queue, speaker_mfcc, tested_speaker_name, numOfModels));
					threads[i].start();
				}
				
		}
		
		
		

	}

}
