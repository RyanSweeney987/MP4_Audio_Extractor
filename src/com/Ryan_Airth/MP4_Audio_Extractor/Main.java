package com.Ryan_Airth.MP4_Audio_Extractor;

import java.io.Console;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Main {
	
	private String inURL = "";
	private String outURL = "";
	private Console console;
	private int sampleRate; 	// In Hz
	private int channels; 		// Audio channels usually refers to mono (1), 
									//stereo (2), 5.1 surround (5) etc
	private int bitDepth; 		// Usually 8, 16, or 32
	private AudioExtractor audioExtractor;
	
	public static void main(String[] args) {
		Main app = new Main();
		app.runLoop();
	}
	
	public Main() {
		this.console = System.console();	
	}
	
	public void runLoop() {
		System.out.println("Welcome to the command line MP4 audio extractor.");
		
		getFileLocations();
		validateFileLocations();
		getAudioData();
		
		audioExtractor = new AudioExtractor(inURL, outURL, sampleRate, 
				channels, bitDepth, console);
		
		try {
			ExecutorService thread = Executors.newSingleThreadExecutor();
			
			Future<String> future = thread.submit(audioExtractor);
						
			while(audioExtractor.getPercentageComplete() < 100) {
				if(audioExtractor.isExtracting()) {
					System.out.println(audioExtractor.getPercentageComplete() + "%");
					
					Thread.sleep(1000);
				}
			}
			
			System.out.println(future.get());
		
			thread.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
			
			System.out.println("Error extracting audio file, thread was interrupted." 
					+ e.toString());
		} catch (ExecutionException e) {
			e.printStackTrace();
			
			System.out.println("Error executing." + e.toString());
		}
	}
	
	private void getFileLocations() {	
		inURL = console.readLine("%s", "First, please enter the MP4 video location: ");
		outURL = console.readLine("%s", "Now please enter the WAV file location. Or just the file"
				+ " name: ");
	}
	
	private void validateFileLocations() {
		String regex = "\\{2}|/{1}?";
		
		inURL = inURL.replaceAll(regex, "\\\\");
		if(inURL.contains("\"")) {
			inURL = inURL.split("\"")[1];
		}
		
		if(outURL.contains("\\/")) {
			outURL = outURL.replaceAll(regex, "\\\\");
		} else {
			Path fileInURL = Paths.get(inURL);
						
			outURL = fileInURL.resolveSibling(outURL).toAbsolutePath().toString();
		}
	}		
	
	private void getAudioData() {
		sampleRate = Integer.parseInt(console.readLine("%s", "Please enter the desired sample "
				+ "rate in Hz. (e.g: 48000): "));
		
		channels = Integer.parseInt(console.readLine("%s", "Please enter the desired number "
				+ "of audio channels. (e.g: 1 - Mono, 2 - Stereo): "));
		
		bitDepth = Integer.parseInt(console.readLine("%s", "Please enter the desired bit depth "
				+ "for the audio. (e.g: 16, 24): "));
	}
}
