/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.scheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import edu.usc.powertap.android.ADBHelper;
import edu.usc.powertap.android.ADBShell;
import edu.usc.powertap.android.InputStreamToUsage;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.thermtap.ThermTap;




public class TaskScheduler{
	private ReadingTask readTask = null;
	private String dirName = "";

	Process backProc;
	InputStream ibackIs;

	//	Runtime runtime;
	InputStream is;
	BufferedReader backBr;

	public static BufferedReader br = null;

	public void start(String deviceId){
		if (readTask == null) {
			readTask = new ReadingTask(deviceId);
		}
		try{

			ADBHelper.initFtrace(UserProperty.currentDeviceID);
		}catch (Exception e){
			e.printStackTrace();
		}

		readTask.start();
	}

	public void stop(String deviceId){
		if(readTask != null)
			readTask.interrupt();
		readTask = null;
	}

	public String getDirName(){
		return dirName;
	}

	private class ReadingTask extends Thread{
		private InputStreamToUsage istu = null;
		private PrintStream ps;
		private String deviceId = "";


		public ReadingTask(String deviceId) {
			this.deviceId = deviceId;
		}


		@Override
		public void run()
		{
			while(!interrupted()){
				UserProperty.getSeqThenIncrease();


				try {
					Thread.sleep(ThermTap.samplingInterval);
				} catch (InterruptedException e1) {
					return;
				}


				if (UserProperty.getSeq() >= 0){
					try {
						String[] cmd = new String[]{"shell", "su", "-c", "/data/local/systemtap/jcat"
								, "/sys/kernel/debug/tracing/trace_pipe", ">"
								, "/data/local/tmp/trace"};


						new ADBShell(deviceId, cmd).execute();

						cmd = new String[]{"pull", "/data/local/tmp/trace", "data/temp/" + UserProperty.getSeq()};
						new ADBShell(deviceId, cmd).execute();

						is = new FileInputStream(new File("data/temp/"+UserProperty.getSeq()));
						br = new BufferedReader(new InputStreamReader(is));

						InputStreamToUsage.run(br);
						is.close();
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	private class periodicReadData extends Thread{
		int activeMem = -1;

		@Override
		public void run() {
			while(true){
				try {
					Process p = Runtime.getRuntime().exec("adb pull /proc/meminfo ./data/DRAM.txt");
					if(activeMem == -1)
						p.waitFor();
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
