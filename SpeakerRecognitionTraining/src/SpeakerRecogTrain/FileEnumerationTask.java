package SpeakerRecogTrain;

import java.io.File;
import java.util.concurrent.BlockingQueue;

public class FileEnumerationTask implements Runnable{
	
	public FileEnumerationTask(BlockingQueue<File> queue, File startingDirectory){
		this.queue = queue;
		this.startingDirectory = startingDirectory;
	}
	
	public void run(){
		try{
			enumerate(startingDirectory);
			queue.put(DUMMY);
		}
		catch(InterruptedException e){
			
		}
	}
	
	public void enumerate(File directory) throws InterruptedException{
		File[] files = directory.listFiles();
		for(File file : files){
			if(file.isDirectory())
				enumerate(file);
			else
				queue.put(file);
		}
	}
	
	public static File DUMMY = new File("");
	
	private BlockingQueue<File> queue;
	private File startingDirectory;
}
