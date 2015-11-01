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

public class NetworkPower
{	
	private static double wifiPower, LTEPower;
//	private static int wifiQualityNoise = 0;
//	private static int wifiQualityLink = 0;
//	private static int wifiQualityLevel = 0;

	// {base_low, base_high},  {transmit_low, transmit_high}
	private static double[][] beta_wifi_send = {{0.0508e-6, 0.255}, {0.00363e-6, 0.262}, {0.0167e-6, 0.4892}},
							  beta_wifi_recv = {{0.0036e-6, 0.2835}},
							  
							  beta_lte_recv = {{1e-4, 0.387}, {1e-4, 0.4237}, {6e-5, 0.5751}},
							  beta_lte_send = {{1e-4, 0.387}, {1e-4, 0.4237}, {6e-5, 0.5751}};
	
	//threashold for send is 20 Mb/s
	//threashold for recv is 20 Mb/s
	private static int 	wifi_send_threshold1 = 2*1000*1000, 
						wifi_send_threshold2 = 8*1000*1000, 
//						wifi_send_threshold = 1*1000*1000,
						lte_threshold1 = 400*1000, 
						lte_threshold2 = 2000*1000;
	
//	private static double totalDataSize = 0;

	public static double getPower(boolean lteWifi, boolean sendRecv, double packeteSize, double duration)
	{
		
		double packetRates = packeteSize / duration;
			
		if (Math.abs(packetRates) < 1e-20) {
			return 0;
		}
		
//		packetRates*=8;	//converting packet rate to bit/sec
		
		
		
		if (!lteWifi){ //LTE
			if (!sendRecv){ //sending
				if (packetRates <= lte_threshold1) {
					LTEPower = packetRates * beta_lte_send[0][0] + beta_lte_send[0][1];
				} else if (packetRates <= lte_threshold2){
					LTEPower = packetRates * beta_lte_send[1][0] + beta_lte_send[1][1];
				}else {
					LTEPower = packetRates * beta_lte_send[2][0] + beta_lte_send[2][1];
				}
			}else{ //receiving
				if (packetRates <= lte_threshold1) {
					LTEPower = packetRates * beta_lte_recv[0][0] + beta_lte_recv[0][1];
				}else if (packetRates <= lte_threshold2) {
					LTEPower = packetRates * beta_lte_recv[0][0] + beta_lte_recv[0][1];
				} else {
					LTEPower = packetRates * beta_lte_recv[1][0] + beta_lte_recv[1][1];
				}
			}
			return LTEPower;
		}else{ //WiFi
			
			if (!sendRecv){ //sending
				if (packetRates <= wifi_send_threshold1) {
					wifiPower = packetRates * beta_wifi_send[0][0] + beta_wifi_send[0][1];
				}else if (packetRates <= wifi_send_threshold2) {
					wifiPower = packetRates * beta_wifi_send[1][0] + beta_wifi_send[1][1];
				} else {
					wifiPower = packetRates * beta_wifi_send[2][0] + beta_wifi_send[2][1];
				}
			}else{ //receiving
				wifiPower = packetRates * beta_wifi_recv[0][0] + beta_wifi_recv[0][1];
			}
//			totalDataSize += packeteSize*1.0e-6;
//			System.out.println("wifi power requested, rate(Mb/s): "+packetRates/1e-6+"packet size(MB): "+
//							(packeteSize*1.0e-6)+"total data from beginning(MB): "+totalDataSize+" power: "+wifiPower);
			if(wifiPower < 0)
				System.out.println("wifi power got negative!! "+ wifiPower);
			if(Math.abs(wifiPower) > 100)
				System.out.println("wifi power got so big!! "+ wifiPower);
			return wifiPower;
		}
	}
	public static double getPower(boolean lteWifi){
		if(lteWifi){
//			if(wifiPower > 0.0)
//				System.out.println("wifi power requested, power: "+wifiPower);
			return wifiPower;
		}else 
			return LTEPower;
	}			
	
	
	public static void makeWifiDisable(){
//		System.out.println("wifi power reset ");
		wifiPower = 0;
	}
	public static void makeLTEDisable(){
		LTEPower = 0;
	}

}