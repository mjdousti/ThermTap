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

import java.util.logging.Logger;

import edu.usc.thermtap.ThermTap;

public class CPUPower{
	private static Logger logger = Logger.getLogger("");
	
	private static double[] freq = new double[ThermTap.getPhone().coreCount];
	private static double[] utilization = new double[ThermTap.getPhone().coreCount];
	private static int[] cores = new int[4];
	
	// {freq in GHz, beta_freq, beta_idle}
	private static double[][] beta_cpu = {{300000e-6,	0.0294250244,	0.0294250244,	0.023617967,	0.0212463438},
											{300000e-6,	0.0953149406,	0.0985301965,	0.0966682247,	0.0898906178},
											{422400e-6,	0.0204938535,	0.0204938535,	0.019563968,	0.020170119},
											{422400e-6,	0.1510150931,	0.1332927665,	0.1242223279,	0.1168452794},
											{652800e-6,	0.0184034357,	0.0184034357,	0.0193556321,	0.0192173289},
											{652800e-6,	0.259278871,	0.1995662661,	0.1846396312,	0.1800455398},
											{729600e-6,	0.0184558876,	0.0184558876,	0.0204276394,	0.0215541173},
											{729600e-6,	0.3057900427,	0.2193365023,	0.2065766891,	0.2278369527},
											{883200e-6,	0.0179107061,	0.0179107061,	0.0200829179,	0.0244260648},
											{883200e-6,	0.3683133097,	0.2658939792,	0.256884943	,0.2760246434},
											{960000e-6,	0.0236357138,	0.0236357138,	0.0248768183,	0.0263245686},
											{960000e-6,	0.404264761,	0.2908482587,	0.2843114497,	0.3037962591},
											{1036800e-6,	0.0203935474,	0.0203935474,	0.0246842798,	0.0256362017},
											{1036800e-6,	0.4512002475,	0.3213697891,	0.3423622295,	0.335150365},
											{1190400e-6,	0.0243885742,	0.0243885742,	0.0280858753,	0.0301674163},
											{1190400e-6,	0.5185463634,	0.3743961427,	0.4002037996,	0.39006198},
											{1267200e-6,	0.0341490164,	0.0341490164,	0.0342854596,	0.0343631104},
											{1267200e-6,	0.5674772948,	0.3990444261,	0.430182407,	0.4205301194},
											{1497600e-6,	0.0286934907,	0.0286934907,	0.0359051743,	0.0367313196},
											{1497600e-6,	0.6923348023,	0.5129789023,	0.5333139145,	0.5298298223},
											{1574400e-6,	0.0342846037,	0.0342846037,	0.0387371479,	0.0492364412},
											{1574400e-6,	0.7875837355,	0.5955362977,	0.5729978897,	0.5926141884},
											{1728000e-6,	0.038405644,	0.038405644,	0.0425899494	,0.0477081804},
											{1728000e-6,	0.9273791664,	0.6871607491,	0.6721288992,	0.6991873904},
											{1958400e-6,	0.0433168199,	0.0433168199,	0.0539798408,	0.0569659978},
											{1958400e-6,	1.1318216966,	0.8210507493,	0.8493032629,	0.8833132123},
											{2265600e-6,	0.0534164405,	0.0534164405,	0.0678718643,	0.0733721018},
											{2265600e-6,	1.4794816951,	1.0666310156,	1.1307632948,	1.1836468318}};

	
	public static void setFreq(double[] f){
		for (int i = 0; i < f.length; i++) {
			if(f[i] > 0)
				cores[i] = 1;
			freq[i]=f[i];
		}
	}
	
	public synchronized static void setFreq(int i, double f){
		cores[i] = 1;
		freq[i]=f;
	}
	
	public synchronized static double getFreq(int i){
		return freq[i];
	}
	
	public static void setUtilization(int i, double u){
		utilization[i]=u;
	}
	
	public static double getTotalPower(){
		return getIdlePower() + getActivePower();
	}
	
	public synchronized static void turnOffCore(int i){
		cores[i] = 0;
	}
	
	public synchronized static int getNumOnlineCores(){
		return cores[0] + cores[1] + cores[2] + cores[3];
	}

	public synchronized static double getIdlePower(int coreID){
		
		
		double power=0;
		
		for (int i = 0; i < beta_cpu.length; i+= 2) {
			if(Double.compare(freq[coreID], beta_cpu[i][0]) == 0){// even rows are for idle powers
				power+= beta_cpu[i][getNumOnlineCores()];
			}
		}
		
//		System.out.println("cpu idle power requested, on cores: "+getNumOnlineCores()+" freqs: "+freq[0]+" "+freq[1]+" "+freq[2]+" "+freq[3]+" "+power);
		return power;
	}
	
	
	public synchronized static double getIdlePower(){
		double power=0;
		
		for (int coreID = 0; coreID < ThermTap.getPhone().coreCount; coreID++) {
			for (int i = 0; i < beta_cpu.length; i+= 2) {// even rows are for idle power
				if(Double.compare(freq[coreID], beta_cpu[i][0]) == 0){
					power+= beta_cpu[i][getNumOnlineCores()];
				}
			}
		}
//		System.out.println("cpu idle power requested, on cores: "+getNumOnlineCores()+" freqs: "+freq[0]+" "+freq[1]+" "+freq[2]+" "+freq[3]+" "+power);
		if(power < 0)
			System.out.println("cpu idle power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("cpu idle power got so big!! "+ power);
		return power;
	}
	
	public synchronized static double getActivePower(int coreID){
		double power = 0;
		boolean freqFound;
		freqFound = false;
		if (Double.compare(freq[coreID],0)==0){	//CPU i is off, hence it consumes no power
			return 0;
		}else{
			for (int j = 1; j < beta_cpu.length; j+=2) {// odd rows have active betas
				if (Double.compare(beta_cpu[j][0], freq[coreID]) == 0){
					power += beta_cpu[j][getNumOnlineCores()] * utilization[coreID];
					freqFound = true;
					continue;
				}
			}
		}
		if (!freqFound){
			logger.severe("Frequency "+freq[coreID] + " is not matched.");
			System.exit(-1);
		}
		
		if(power < 0)
			System.out.println("cpu core active power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("cpu core active power got so big!! "+ power);
		
//		System.out.println("cpu active power requested, on cores: "+getNumOnlineCores()+" freqs: "+freq[0]+" "+freq[1]+" "+freq[2]+" "+freq[3]+" "+power);
		return power;
	}
	
	public synchronized static double getActivePower(){
		double power = 0;
		boolean freqFound;
		for (int i = 0; i < ThermTap.getPhone().coreCount; i++) {// odd rows have active betas
			freqFound = false;
			if (Double.compare(freq[i],0)==0){	//CPU i is off, hence it consumes no power
				continue;
			}else{
				for (int j = 1; j < beta_cpu.length; j+=2) {
					if (Double.compare(beta_cpu[j][0], freq[i]) == 0){
						power += beta_cpu[j][getNumOnlineCores()] * utilization[i];
						freqFound = true;
						continue;
					}
				}
			}
			if (!freqFound){
				logger.severe("Frequency "+freq[i] + " is not matched.");
				System.exit(-1);
			}
		}
		
		if(power < 0)
			System.out.println("cpu active power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("cpu active power got so big!! "+ power);
		
//		System.out.println("cpu active power requested, on cores: "+getNumOnlineCores()+" freqs: "+freq[0]+" "+freq[1]+" "+freq[2]+" "+freq[3]+" "+power);
		return power;
	}
}
