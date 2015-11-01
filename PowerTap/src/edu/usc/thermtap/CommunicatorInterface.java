/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */

package edu.usc.thermtap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

public class CommunicatorInterface extends Thread {
	private Process therminatorProcess=null;
	
	int pid,uid;
	boolean running = false;
	int watchID;
	static boolean success;
	BufferedReader stdError ;

	public static void reportSucess(boolean s){
		success = s;
	}
	
	public CommunicatorInterface(int uid, int pid) {
		this.pid = pid;
		this.uid = uid;
	}
	
	private void waitOnFileDeletion(String toBeDeletedFile){
		WatchService watcher;
		try {
			watcher = FileSystems.getDefault().newWatchService();
			Path dir = Paths.get("./");
			dir.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
			
			while (true) {
			    WatchKey key = null;
			    try {
			        // wait for an event to be available
			        key = watcher.take();
			    } catch (InterruptedException e){
			    	e.printStackTrace();
			    }
			 
			    for (WatchEvent<?> event : key.pollEvents()) {
			        // get event type
			        WatchEvent.Kind<?> kind = event.kind();
			        
			        if (kind == StandardWatchEventKinds.OVERFLOW) {
			            continue;
			        }
			        
			        // The filename is the context of the event.
			        @SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>)event;
			        Path filename = ev.context();
			        
			        if (filename.toString().equals(toBeDeletedFile)){
						watcher.close();

			        	return;
			        }
			    
			    }
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
	@Override
	public void run() {
		while(!interrupted()){
			if(!running){
				try {
					Thread.sleep(ThermTap.communicationInterval);
				} catch (InterruptedException e) {
				}
				continue;
			}
			
			ThermTap.printStats(uid, pid);
			success = false;
			
			waitOnFileDeletion("next");
			ThermTap.loadTherminatorOutput("output_0");
		}
	}
	
	public void startRunning(){
		this.running = true;
	}
	
	public void stopRunning(){
		this.running = false;
	}
	
	public void initTherminator(int uid,int pid){
		this.uid = uid;
		this.pid = pid;
		try {
			ThermTap.printStatsInitialization(uid,pid);
			
			waitOnFileDeletion("init_power");
			
			ThermTap.loadTherminatorOutput("output_0");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public void startTherminator(){
		if (therminatorProcess!=null){
			return;
		}
		try {
			Runtime.getRuntime().exec("killall -9 therminator").waitFor();

			String therminatorCmd = "./therminator -t -d " + ThermTap.getPackageSpecFile() + " -p trace -o output";
			
			ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-l", "-c", therminatorCmd);

			Map<String, String> env = processBuilder.environment();
			if (env.containsKey("PATH")){
				env.put("PATH", env.get("PATH")+":\""+System.getProperty("user.dir")+'"');
			}else{
				env.put("PATH", '"' + System.getProperty("user.dir")+'"');
			}

			therminatorProcess = processBuilder.start();
			System.out.println("Therminator 2 is started.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			FileWriter fw = new FileWriter(new File("init"));
			fw.write("123");
			fw.close();
			
			waitOnFileDeletion("init");

			
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	
	}
}

