/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.powermodel;

public class DiskPower {
	private static double activePower;
	private static double writeSpeed = 69.8 * 1e6; // write speed in B/sec
	private static double readSpeed = 71.2 * 1e6; // read speed in B/sec
	

	/*
	 * 	sync
		echo 3 > /proc/sys/vm/drop_caches
		which drops all sorts of caches.
		For details see /usr/src/linux/Documentation/sysctl/vm.txt on drop_caches.
		http://unix.stackexchange.com/questions/82820/force-dd-not-to-cache-or-not-to-read-from-cache
	 */
	public static double getPower(boolean readWrite, int chunkSize, double duration){
		//It seems that read and write have the same power consumption
		//read speed : 71.2MB/s
		//write speed : 69.8MB/s
		if (chunkSize>0){
			if(readWrite)	//write power
				activePower = duration > (chunkSize/writeSpeed) ? 0.4587340 : 0.693841;//0.048562;	
			else			//read power
				activePower = duration > (chunkSize/writeSpeed) ? 0.3258461 : 0.512308;//0.15896;
		}else{
			activePower = 0;
		}
		
		if(activePower < 0)
			System.out.println("disk power got negative!! "+ activePower);
		if(Math.abs(activePower) > 100)
			System.out.println("disk power got so big!! "+ activePower);
		
		return activePower;
	}
	
	public static double getPower(){
		return activePower;
	}			
	
	
	public static void makeDisable(){
		activePower = 0;
	}
	
	public static double getDuration(boolean readWrite, int chunkSize){
		if(readWrite)
			return chunkSize/writeSpeed;
		return chunkSize/readSpeed;
	}
}
