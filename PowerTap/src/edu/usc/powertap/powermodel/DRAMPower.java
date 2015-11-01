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

public class DRAMPower {
	static double power = 0;
	static double refreshPower = 0;
	static double peakPower;
	
	public synchronized static double getPower(){
		return power + refreshPower;
	}
	
	public synchronized static void updatePower(int diff, int period){// delay for read/write is 4.8 ms per MB, so for 500ms period 100MB keeps 
		power = 0;//peakPower * diff/1000/((period/500)*100);				//the dram at max power for whole period
	}
}
