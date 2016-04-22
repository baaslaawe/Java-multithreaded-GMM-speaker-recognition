import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SpeakerRecogTest {
	
	public static void main(String[] args) {
		
		String testDirectory = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\test\\"; //directory with files to be tasted
		String speaker_database = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\ThreadedSpeakerRecognition\\database.dat"; //file containing speaker database
		ArrayList<SpeakerModel> speaker_models = null;
		
		
		
		ObjectInputStream we;
		try {
			we = new ObjectInputStream(new FileInputStream(speaker_database));
			speaker_models = (ArrayList<SpeakerModel>)we.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/* This solution assumes that there is more than one speech file of each speaker, and the files are named as:
		 * [SPEAKER_NAME][SENTENCE_NUMBER].wav
		 * E.G FADG00.wav, FADG01.wav, FADG02.wav etc. - number of caracters for speaker is 5 (FADG0) and for sentence number is 1 (0,1 or 2).
		 */
		
		int char_for_speaker = 5;
		int char_for_sentence = 1;
		
		
		File dir = new File(testDirectory);
		int numOfSpeakers = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".wav")||name.toLowerCase().endsWith(".WAV");
			}
		}).length;
		
		final int FILE_QUEUE_SIZE = 20;
		final int SEARCH_THREADS = 8;
		
		BlockingQueue<File> queue = new ArrayBlockingQueue<File>(FILE_QUEUE_SIZE);
		
		
		Thread[] threads = new Thread[SEARCH_THREADS];
		FileEnumerationTask enumerator = new FileEnumerationTask(queue, new File(testDirectory));
		new Thread(enumerator).start();
		
		for(int i=0; i<SEARCH_THREADS; i++ ){
			threads[i] = new Thread(new RecognitionTask(queue, char_for_sentence, char_for_speaker, speaker_models, numOfSpeakers));
			threads[i].start();
			//new Thread(new SearchTask(queue, char_for_sentence, char_for_speaker, speaker_models)).start();
		}
		
	}

}
