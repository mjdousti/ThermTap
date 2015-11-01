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

public class DisplayPower {
	static double [] betaDisplay = {0.0033, 0};
	static int brightness;
	
	public static void setBrightness(int br) {
		brightness = br;
	}
	
	public static double getPower() {
		double power = brightness*betaDisplay[0] + betaDisplay[1];
		if(power < 0)
			System.out.println("disk power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("disk power got so big!! "+ power);
		return power;
	}
}
