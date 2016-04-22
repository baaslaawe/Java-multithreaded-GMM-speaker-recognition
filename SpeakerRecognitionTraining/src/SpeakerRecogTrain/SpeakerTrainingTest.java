package SpeakerRecogTrain;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.*;

public class SpeakerTrainingTest {

	public static void main(String[] args) {
		
		String trainDirectory = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\train\\"; //directory with files of speakers for training (!!!!! only 1 file per one speaker !!!!)
		String databaseOutputFile = "C:\\Users\\PUEPS Admin\\eclipse_workspace\\ThreadedSpeakerRecognition\\speaker_database.dat"; //the output database file to be generated 
		
		/* This solution assumes that there is more than one speech file of each speaker, and the files are named as:
		 * [SPEAKER_NAME][SENTENCE_NUMBER].wav
		 * E.G FADG00.wav, FADG01.wav, FADG02.wav etc. - number of caracters for speaker is 5 (FADG0) and for sentence number is 1 (0,1 or 2).
		 */
		
		int char_for_speaker = 5;
		int char_for_sentence = 1;
		
		File dir = new File(trainDirectory);
		int numOfSpeakers = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".wav")||name.toLowerCase().endsWith(".WAV");
			}
		}).length;
		
		final int FILE_QUEUE_SIZE = 20;
		final int SEARCH_THREADS = 8;
				
		BlockingQueue<File> queue = new ArrayBlockingQueue<File>(FILE_QUEUE_SIZE);
		
		Thread[] threads = new Thread[SEARCH_THREADS];
		FileEnumerationTask enumerator = new FileEnumerationTask(queue, new File(trainDirectory));
		new Thread(enumerator).start();
		
		for(int i=0; i<SEARCH_THREADS; i++ ){
			threads[i] = new Thread(new TrainingTask(queue, char_for_sentence, char_for_speaker, numOfSpeakers, databaseOutputFile));
			threads[i].start();
		}
		
	}
}
