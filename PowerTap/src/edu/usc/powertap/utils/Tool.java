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

import java.io.File;
import java.util.ArrayList;

public class Tool
{
	public static double getSumOfArray(double[] array)
	{
		double ret = 0;
		for (int i = 0; i < array.length; i++)
		{
			ret += array[i];
		}
		return ret;
	}
	
	public static double getSumOfArray(ArrayList<Double> array)
	{
		double ret = 0;
		for (int i = 0; i < array.size(); i++)
		{
			ret += array.get(i);
		}
		return ret;
	}


	public static double[] addArrayToArray(double[] dest, double[] src){
		for (int i = 0; i < dest.length; i++) {
			dest[i] += src[i];
		}
		return dest;
	}

	public static void resetArray(double[] array){
		for (int i = 0; i < array.length; i++) {
			array[i] = 0;
		}
	}

	public static void resetArray(int[] array)
	{
		for (int i = 0; i < array.length; i++) {
			array[i] = 0;
		}
	}


	public static void delete(File f){
		if (f.isDirectory()) {
			while(!f.delete()){
				for (File c : f.listFiles()) {
					delete(c);
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			
		}else{
			f.delete();
		}
	}
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s);
		    return true;
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	}
	public static void printArray(double [][] data, String title){
		System.out.println(title + ":");
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				System.out.print(data[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println("-----------------");
	}
}