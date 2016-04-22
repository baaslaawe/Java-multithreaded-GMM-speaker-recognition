

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class ModelEnumerationTask implements Runnable {
	
	public ModelEnumerationTask(BlockingQueue<SpeakerModel> queue, ArrayList<SpeakerModel> speakerModels){
		this.queue = queue;
		this.speakerModels = speakerModels;
	}
	
	public void run(){
		try{
			enumerate(speakerModels);
			queue.put(DUMMY);
		}
		catch(InterruptedException e){
			
		}
	}
	
	public void enumerate(ArrayList<SpeakerModel> speakerModels) throws InterruptedException{
		//SpeakerModel modelFile[] files = directory.listFiles();
		for(SpeakerModel model : speakerModels){

				queue.put(model);
		}
	}
	
	private BlockingQueue<SpeakerModel> queue;
	private ArrayList<SpeakerModel> speakerModels;
	//public static File DUMMY = new File("");
	public static SpeakerModel DUMMY = new SpeakerModel(null, null, null, "DUMMY", null);

}
