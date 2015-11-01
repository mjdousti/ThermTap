/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.utils;


public class UserProperty{
	public static Mode operationMode = Mode.DISCONNECTED;
	public static String previousDeviceID = "";
	public static String currentDeviceID = "";
	public static String currentPackageFile = "";
	public static String DATA_DIR = "";
	public static String LOCAL_ADB_PATH = "";
	public static String PACKAGE_FILE = "";
	private static String logDir = "";

	public static boolean isFirstRun = true;
	private static double seq = -2.0D;

	public static enum componentNames {CPU, GPU, Display, Disk, WIFI, LTE, GPS, DRAM, idle};
	public static final int numOfComponent = componentNames.values().length;



	public static enum Mode{
		DISCONNECTED,  NO_DEVICE,  FOUND_DEVICE,  DEVICE,  CONNECTED,  RUN,  STOP;
	}



	public static boolean setMode(String mode){
		if (mode.equalsIgnoreCase("DISCONNECTED")){
			operationMode = Mode.DISCONNECTED;
			return true;
		}
		if (mode.equalsIgnoreCase("NO_DEVICE"))	{
			operationMode = Mode.NO_DEVICE;
			return true;
		}
		if (mode.equalsIgnoreCase("FOUND_DEVICE")){
			operationMode = Mode.FOUND_DEVICE;
			return true;
		}
		if (mode.equalsIgnoreCase("DEVICE")){
			operationMode = Mode.DEVICE;
			return true;
		}
		if (mode.equalsIgnoreCase("CONNECTED"))	{
			operationMode = Mode.CONNECTED;
			return true;
		}
		if (mode.equalsIgnoreCase("RUN")){
			operationMode = Mode.RUN;
			return true;
		}
		if (mode.equalsIgnoreCase("STOP")){
			operationMode = Mode.STOP;
			return true;
		}
		return false;
	}

	public static String getMode(){
		switch (operationMode)
		{
		case CONNECTED: 
			return "CONNECTED";
		case DEVICE: 
			return "DEVICE";
		case DISCONNECTED: 
			return "DISCONNECTED";
		case FOUND_DEVICE: 
			return "FOUND_DEVICE";
		case NO_DEVICE: 
			return "NO_DEVICE";
		case RUN: 
			return "RUN";
		case STOP: 
			return "STOP";
		}
		return "UNKNOWN";
	}



	public synchronized static double getSeq(){
		return seq;
	}

	public synchronized static double getSeqThenIncrease(){
		return seq++;
	}
	
	public static String getLogDir(){
		return logDir;
	}

	public static void setLogDir(String logDir){
		UserProperty.logDir = logDir;
	}
}
