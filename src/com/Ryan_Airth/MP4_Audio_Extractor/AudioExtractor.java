package com.Ryan_Airth.MP4_Audio_Extractor;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import net.sourceforge.jaad.util.wav.WaveFileWriter;

public class AudioExtractor implements Callable<String> {
	private String inURL;
	private String outURL;
	private int sampleRate; 	// In Hz
	private int channels; 		// Audio channels usually refers to mono (1), 
									//stereo (2), 5.1 surround (5) etc
	private int bitDepth; 		// Usually 8, 16, or 32
		
	private int currentFrame;
	private int totalFrames;
	
	private Console console;
	
	private boolean isExtracting;
	
	public AudioExtractor(String inURL, String outURL, int sampleRate, int channels, int bitDepth,
			Console console) {
		super();
		this.inURL = inURL;
		this.outURL = outURL;
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.bitDepth = bitDepth;
		this.console = console;
	}
	
	public synchronized boolean isExtracting() {
		return isExtracting;
	}
	
	public synchronized int getPercentageComplete() {
		if(totalFrames == 0) {
			return -1;
		} else {
			return (int)Math.round((float)currentFrame/(float)totalFrames * 100f);
		}
	}
		
	private void setMaxFrameCount(List<Track> tracks) {
		// Use list in case there are multiple track, in which case, the total frames will
		// be much greater overall
		System.out.println("Finding max frame count.");
		
		for(Track track : tracks) {
			while(track.hasMoreFrames()) {
				totalFrames++;
				
				try {
					track.readNextFrame();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Reset track to the start
			track.seek(0);
		}
		
		System.out.println("Total frame count: " + totalFrames);
	}

	@Override
	public String call() throws Exception {
		boolean operationComplete = false;
	
		while(!operationComplete) {
			try(RandomAccessFile raf = new RandomAccessFile(inURL, "rw");){
				System.out.println("Starting to extract audio!");
						
				MP4Container container = new MP4Container(raf);
			
				Movie movie = container.getMovie();
				List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
											
				if(tracks.isEmpty()) throw new Exception("Video doesn't contain any AAC tracks.");
				
				List<Integer> tracksToExtract = getTracksToExtract(tracks.size());
				
				if(tracksToExtract.size() == 1) {
					setMaxFrameCount(tracks);
					System.out.println("Extracting audio track " + tracksToExtract.get(0));
					
					extractTrack(tracks.get(tracksToExtract.get(0)), 1);						
				} else {
					List<Track> subList = tracks.subList(tracksToExtract.get(0), 
							tracksToExtract.get(1));
					setMaxFrameCount(subList);
					
					for(int i = tracksToExtract.get(0); i < tracksToExtract.get(1); i++) {
						System.out.println("Extracting audio track " + i + " of " 
								+ tracksToExtract.get(1) + ".");
					
						extractTrack(tracks.get(i), i);
				}
			}
			
				operationComplete = true;
			} catch (IOException e) {
				String tryAgain = console.readLine("%s", "Do you wish to try again? (y/n) ");
			
				if(!tryAgain.toLowerCase().equals("n")) {
					operationComplete = true;
				}
			} 
		}	
		
		return "Audio extraction complete, file is saved at: " + outURL;
	}
	
	private List<Integer> getTracksToExtract(int numberOfTracks) {
		boolean validEntry = false;
		
		List<Integer> returnValues = new ArrayList<>();
		
		while(!validEntry) {
			String trackQuery = console.readLine("%s", numberOfTracks + " number of"
					+ " tracks have been found, please enter the range of tracks\nyou wish"
					+ " to extract or enter the same value twice to extract a specific"
					+ " track.\n(trackFromIndex/trackToIndex) ");
			
			String[] values = trackQuery.split("/");
			
			if(validateGivenString(values, numberOfTracks)) {
				// Take 1 away from given values as the user isn't using a 0 based system 
				// like we do in programming
				int[] intValues = new int[] {Integer.parseInt(values[0]) - 1, 
						Integer.parseInt(values[1]) - 1};
								
				if(intValues[0] == intValues[1]) {
					returnValues.add(intValues[0]);
				} else {
					returnValues.add(intValues[0]);
					returnValues.add(intValues[1]);
				}
				
				validEntry = true;
			}
		}
				
		return returnValues;
	}
	
	private boolean validateGivenString(String[] values, int numberOfTracks) {	
		if(!(values.length == 2)) {
			System.out.println("Please enter a valid string and provide the correct amount of " 
					+"values. (trackFromIndex/trackToIndex): ");
						
			return false;
		} else {
			int[] intValues = new int[] {Integer.parseInt(values[0]), 
					Integer.parseInt(values[1])};
			// Translation: if the first value is less than one and greater than the number
			// of tracks and if the second value is less than the first value and greater
			// than the number of tracks, return false
			if((intValues[0] < 1 && intValues[0] > numberOfTracks) ||
					(intValues[1] < intValues[0] && intValues[1] > numberOfTracks)) {
				System.out.println("Please ensure that the values give are valid and are not any"
						+ " greater than the number of tracks found, or less than 0.");
				return false;
			}
		}
		
		return true;
	}
	
	private void extractTrack(Track track, int trackNumber) {
		isExtracting = true;
		
		try {
			byte[] decoderSpecificInfo = track.getDecoderSpecificInfo();
			
			Decoder decoder = new Decoder(decoderSpecificInfo);
			SampleBuffer buf = new SampleBuffer();
			
			File outFile = new File(outURL + "_track_" + trackNumber + ".wav");
			
			WaveFileWriter writer = new WaveFileWriter(outFile, sampleRate, channels, bitDepth);
						
			while(track.hasMoreFrames()) {
				// First we read the frame, store the data in a byte a array
				Frame aacFrame = track.readNextFrame();
				byte[] aacFrameData = aacFrame.getData();
				// Then we decode the data
				decoder.decodeFrame(aacFrameData, buf);
				// Afterwards we get the decoded data to write it to a new file
				byte[] audio = buf.getData();
				
				writer.write(audio);
				
				currentFrame++;
			}
			
			writer.close();
		} catch(IOException e) {
			System.out.println("Error extracting audio, please ensure the following:");
			System.out.println("/t/t- The given file paths are valid."
					+ "/t/t- If over-writing a file, the file location is accissble.");
			e.printStackTrace();
		}
	}
}
