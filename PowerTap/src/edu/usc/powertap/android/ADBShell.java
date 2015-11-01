/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */

package edu.usc.powertap.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import edu.usc.powertap.utils.UserProperty;


public class ADBShell{
	private Logger logger = Logger.getLogger("");

	private Process process;
	private final String adbPath = UserProperty.LOCAL_ADB_PATH + "adb";		//avoiding spaces in the path 
	private ArrayList<String> cmd = new ArrayList<String>();

	public ADBShell(String arg){
		cmd.add(adbPath);
		cmd.add(arg);
	}

	public ADBShell(String dev, String[] args){
		cmd.add(adbPath);
		cmd.add("-s");
		cmd.add(dev);
		cmd.addAll(Arrays.asList(args));
	}

	public int execute(){
		ProcessBuilder processBuilder = new ProcessBuilder(cmd.toArray(new String[cmd.size()]));
		File log = new File("log.txt");
		processBuilder.redirectErrorStream(true);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(log,true));
			for (String string : cmd) {
				bw.write(string + " ");
			}
			bw.write("\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		processBuilder.redirectOutput(Redirect.appendTo(log));
		try{
			process = processBuilder.start();
			process.waitFor();
		}
		catch (Exception ioe)
		{
			logger.warning(ioe.toString());
//			ioe.printStackTrace();
			return -1;
		}
		
		return process.exitValue();
	}

	public int execute(ArrayList<String> result){
		try
		{
			this.process = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));

			StreamGobbler gb1 = new StreamGobbler(this.process.getInputStream(), result);

			gb1.start();
			while (gb1.isAlive()) {}
			this.process.waitFor();
		}
		catch (Exception ioe)
		{
			logger.warning(ioe.toString());
			ioe.printStackTrace();
		}
		return this.process.exitValue();
	}

	private class StreamGobbler extends Thread{
		InputStream is;
		ArrayList<String> stringList;

		public StreamGobbler(InputStream is, ArrayList<String> al) throws IOException{
			this.is = is;
			this.stringList = al;
		}

		public void run(){
			try	{
				InputStreamReader isr = new InputStreamReader(this.is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null)
				{
					if (!line.trim().isEmpty()) {
						this.stringList.add(line);
					}
				}
			}
			catch (IOException ioe){
				logger.finer(ioe.getMessage());
			}
		}
	}
}
