/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.entities;

import java.util.logging.Logger;

import edu.usc.powertap.utils.UserProperty;


public class UsageNode{
	private Logger logger = Logger.getLogger("");

	private double timeStamp;
	private String processName;
	private int pid, tgid , uid;


	private Types usageNodeType;
	private double coreUtilization;
	private double coreFrequency = -1;
	private int coreID;
	
	private int gpuLevel = -1;
	private double gpuUtilization = 0;
	
	private int displayBrightness = -1;
	
	
	private String devName;
	private int packetSize;
	private boolean sendRecv;

	//By default, gpu is off, i.e., it is power gated and indeed clock gated
	private boolean powerGated=true, clockGated=true;
	private boolean readWrite;
	private double duration;
	private boolean startEnd=false;

	private int chunkSize=0;

	private double[] activePower = new double[UserProperty.numOfComponent];
	
	public static enum Types{cpu_freq, 
							cntx_switch, 
							cpu_disable,
							wifi,
							LTE,
							disk,
							gpulevel,
							gpuenable,
							gpu_cntx,
							gpu_util,
							display,
							gps,
							dram};

	
	public boolean isEnd(){
		return startEnd;
	}

							
	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public boolean isPowerGated() {
		return powerGated;
	}
	public void setPowerGated(boolean powerGated) {
		this.powerGated = powerGated;
	}
	public boolean isClockGated() {
		return clockGated;
	}
	public void setClockGated(boolean clockGated) {
		this.clockGated = clockGated;
	}
	

	public double getTimeStamp() {
		return timeStamp;
	}
//	public void setTimeStamp(double timeStamp) {
//		this.timeStamp = timeStamp;
//	}
	public boolean isWrite() {
		return readWrite;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public int getGpuLevel() {
		return gpuLevel;
	}
	
	public double getGpuUtil() {
		return gpuUtilization;
	}
	
	public int getDisplayBrightness() {
		return displayBrightness;
	}
	
	
	public Types getUsageNodeType() {
		return usageNodeType;
	}
	public void setUsageNodeType(Types usageNodeType) {
		this.usageNodeType = usageNodeType;
	}
	public double getCoreUtilization() {
		if (usageNodeType!=Types.cntx_switch){
			System.err.println("Core utilization is unavailable.");
			System.exit(-1);
		}
		return coreUtilization;
	}
	public void setCoreUtilization(double coreUtilization) {
		this.coreUtilization = coreUtilization;
	}
	public double getCoreFrequency() {
		return coreFrequency;
	}
	public void setCoreFrequency(double coreFrequency) {
		this.coreFrequency = coreFrequency;
	}
	public int getCoreID() {
		return coreID;
	}
	public void setCoreID(int coreID) {
		this.coreID = coreID;
	}
	public String getDevName() {
		return devName;
	}
	public void setDevName(String devName) {
		this.devName = devName;
	}
	public int getPacketSize() {
		return packetSize;
	}
	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}

	
	
	//---------- for context switch -----------------
	public UsageNode(Types type, double timeStamp, String procName, int pid, int tgid, int uid, int cpuNum, int idleTime, int userTime, int systemTime){
		init(timeStamp, procName, pid, tgid, uid);
		
		coreUtilization = (userTime+systemTime)/(userTime+systemTime+idleTime);
		this.coreID = cpuNum;
//		usageNodeType = Types.cntx_switch;
		usageNodeType = type;
	}
	//---------- for cpu freq change and gpu utilization change-----------------
	public UsageNode(Types type, double timeStamp, int val1, int val2){
		if(type == Types.cpu_freq){
			init(timeStamp, "FREQ-CHANGE", -1, -1, -1);
			this.coreID = val1;
			if(coreID > 3)
				System.out.println("core id is invalid!!!");
			this.coreFrequency = val2 / 1.0e6;	//converting frequency to GHz
//			usageNodeType = Types.cpu_freq;
		}else if(type == Types.gpu_util){
			init(timeStamp, "GPU-FREQ-CHANGE", -1, -1, -1);
			this.gpuUtilization = val2 == 0 ? 0 : val1/val2;
//			usageNodeType = Types.gpu_util;
		}
		usageNodeType = type;
	}	
	//---------- for wifi/LTE -----------------
	public UsageNode(Types type, double timeStamp, String procName, int pid, int tgid, int uid, boolean startEnd, boolean isRecv, int packetSize, boolean lteWifi){
		init(timeStamp, procName, pid, tgid, uid);
		this.packetSize = packetSize;
		this.sendRecv = isRecv;
		
		this.startEnd = startEnd;
//		if(lteWifi)
//			usageNodeType = Types.wifi;
//		else{
//			usageNodeType = Types.LTE;
//			System.out.println("LTE event is recorded!!!");
//		}
		usageNodeType = type;
	}
	
	//---------- for GPU Freq Change & display brightness change & cpu core disable -----------------
	public UsageNode(Types type, double timeStamp, int value){//0=gpu, 1=display
		init(timeStamp, "GPU/DISP", -1, -1, -1);
		usageNodeType = type;
		if(type == Types.gpulevel){
			gpuLevel = value;
//			usageNodeType = Types.gpulevel;
		}else if(type == Types.display){
			displayBrightness = value;
//			usageNodeType = Types.display;
		}else if(type == Types.cpu_disable){
			coreID = value;
//			usageNodeType = Types.cpu_disable;
		}else{
			System.err.println("type cannot be null");
			System.exit(-1);
		}
	}
		
	//---------- for GPU enable/disable -----------------
	public UsageNode(Types type, double timeStamp,boolean enabled){
		init(timeStamp, "UNKNOWN", -1,-1,-1);
		usageNodeType = type;
		this.clockGated = !enabled;
	}
	
	//---------- for GPU context switch -----------------
	public UsageNode(Types type, double timeStamp,int pid, String pname){
		init(timeStamp, pname, pid,-1,-1);
//		System.out.println(timestamp);
		usageNodeType = type;
	}
	
	//---------- for Disk -----------------
	public UsageNode(Types type, double timeStamp, String procName, int pid, int tgid, int uid, boolean startEnd, boolean readWrite, int chunkSize){
		init(timeStamp, procName, pid, tgid, uid);
//		usageNodeType = Types.disk;
		usageNodeType = type;
		this.chunkSize = chunkSize;
		this.readWrite = readWrite;
		
		this.startEnd = startEnd;
	}


	private void init(double timeStamp, String procName, int pid, int tgid, int uid){
		if(timeStamp < 1.0)
			System.out.println("timestamp not correct");
		this.timeStamp=timeStamp;
		setProcessName(procName);
		setPid(pid);
		setTgid(tgid);
		setUid(uid);
	}

	public String getProcessName(){
		return processName;
	}

	private void setProcessName(String processName){
		this.processName = processName;
	}

	public int getPid(){
		return pid;
	}

	public void setPid(int pid){
		this.pid = pid;
	}

	public int getTgid(){
		return tgid;
	}

	private void setTgid(int tgid){
		this.tgid = tgid;
	}

	public int getUid(){
		return uid;
	}

	private void setUid(int uid){
		this.uid = uid;
	}


	public boolean isRecv() {
		return sendRecv;
	}


	public synchronized void setActivePower(double[] p) {
		if (p.length!=activePower.length){
			logger.severe("Size of the power vectors defined and read are different.");
			System.exit(-1);
		}
		System.arraycopy(p, 0, activePower, 0, p.length);
	}

	
	public synchronized double[] getActivePower(){
		return activePower;		
	}

}
