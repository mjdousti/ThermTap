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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.powertap.entities.UsageNode;
import edu.usc.powertap.entities.UsageNode.Types;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.thermtap.PowerMeter;


public class InputStreamToUsage{	

	static private PowerMeter powerMeter;
	static double time;
	static int cpuNum, freq, uid, pid, gid, idlet, usert, systemt;



	public static void run(BufferedReader br){
		final Pattern ftracePattern = Pattern.compile(".*>-(\\d+)\\s*.*\\].*\\s(\\d+\\.\\d+): ");
		Matcher matcher;

		String line = "";
		UsageNode usageNode = null;
		int index = -1;
		
		String [] args;
		
		try
		{
			powerMeter = new PowerMeter();
			while ((line = br.readLine()) != null) {
				usageNode = null;
				if (!line.isEmpty() && !line.startsWith("#")) {	
					
					if((index = line.indexOf("function_ftrace")) == -1){
						continue;
					}
					
					matcher = ftracePattern.matcher(line.substring(0,index));
					if(matcher.find()){
						pid = Integer.parseInt(matcher.group(1));
						time = Double.parseDouble(matcher.group(2));
						if(time < 1.0)
							System.out.println("wrong time in line : " + line);
					}else{
//						System.err.println(line.substring(0,index));
						continue;
					}
					
					args = line.substring(index + 17, line.length()).split(" ");
					
					if(args[0].contains("CPUCNTX")){
						pid = Integer.parseInt(args[1]);
						gid = Integer.parseInt(args[2]);
						uid = Integer.parseInt(args[3]);
						cpuNum = Integer.parseInt(args[4]);
						idlet = Integer.parseInt(args[5]);
						usert = Integer.parseInt(args[6]);
						systemt = Integer.parseInt(args[7]);
						
						usageNode = new UsageNode(Types.cntx_switch,time,args[8],pid,gid,uid,cpuNum,idlet,usert,systemt);
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("DISK")){
						pid = Integer.parseInt(args[1]);
						gid = Integer.parseInt(args[2]);
						uid = Integer.parseInt(args[3]);
						boolean startEnd = Boolean.parseBoolean(args[4]);
						boolean readWrite = Boolean.parseBoolean(args[5]);
						
						usageNode = new UsageNode(Types.disk,time,"",pid,gid,uid,startEnd,readWrite,Integer.parseInt(args[6]));
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("NETWORK")){
						pid = Integer.parseInt(args[1]);
						gid = Integer.parseInt(args[2]);
						uid = Integer.parseInt(args[3]);
						boolean startEnd = Boolean.parseBoolean(args[4]);
						boolean sendRecv = Boolean.parseBoolean(args[5]);
						boolean lteWifi = args[7].contains("wlan") ? true : false;
						Types type = args[7].contains("wlan") ? Types.wifi : Types.LTE;
						
						if(type == Types.LTE)
							System.out.println("event type is: " + args[6]);
						
						usageNode = new UsageNode(type,time,"",pid,gid,uid,startEnd,sendRecv,Integer.parseInt(args[6]), lteWifi);
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("CPUFREQ")){
						cpuNum = Integer.parseInt(args[1]);
						freq = Integer.parseInt(args[2]);
						
						usageNode = new UsageNode(Types.cpu_freq, time, cpuNum, freq);
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("CPUDISABLED")){
						usageNode = new UsageNode(Types.cpu_disable, time, Integer.parseInt(args[1]));
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("GPULEVEL")){
						usageNode = new UsageNode(Types.gpulevel, time, Integer.parseInt(args[1]));
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("DISPLAY")){
						usageNode = new UsageNode(Types.display, time, Integer.parseInt(args[1]));
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("GPUENABLED")){
						usageNode = new UsageNode(Types.gpuenable, time, true);
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("GPUDISABLED")){
						usageNode = new UsageNode(Types.gpuenable, time, false);
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("GPUUTIL")){
						usageNode = new UsageNode(Types.gpu_util, time, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
						powerMeter.addDataFromInlineData(usageNode);
					}else if(args[0].contains("GPUCNTX")){
						pid = Integer.parseInt(args[1]);
						
						if(time < 1e-8)
							System.out.println("line is: " + line);
						usageNode = new UsageNode(Types.gpu_cntx, time, pid, args[2]);
						powerMeter.addDataFromInlineData(usageNode);
					}
				}
			}
		}catch (IOException e){
			e.toString();
			e.printStackTrace();
		}
		
		if ((powerMeter != null) && (UserProperty.getSeq() > -1)){
			powerMeter.getPowers();
		}
	}

}
