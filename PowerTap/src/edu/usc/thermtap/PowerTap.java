/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.thermtap;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import edu.usc.powertap.android.ADBHelper;
import edu.usc.powertap.android.ADBShell;
import edu.usc.powertap.entities.Phone;
import edu.usc.powertap.scheduler.TaskScheduler;
import edu.usc.powertap.utils.Tool;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.thermtap.ThermTap.Actions;

public class PowerTap {
	private Phone phone;
	private TaskScheduler task = new TaskScheduler();

	
	public static double startTime = -1;

	PowerTap(){

		initControls();
		
		setPhone(new Phone());
		
		ADBHelper.setPath();
		ADBHelper.makeDatafileDir();


		setMode("DISCONNECTED");

	}
	
	private void setMode(String mode){
		if (UserProperty.setMode(mode)) {
			System.out.println("Status: " + UserProperty.getMode());
		}
	}


	private void getApplications(String _device){
		ADBHelper.getPackageFile(_device);
		phone.appDataDump(ADBHelper.makeUidList());
	}


	private ArrayList<String> getConnectedDevices()
	{
		ArrayList<String> list = ADBHelper.getDevices();
		if ((list.size() == 0) || (list == null)){
			setMode("NO_DEVICE");
			return null;
		}else{
			setMode("FOUND_DEVICE");
		}
		return list;
	}

	private void selectDevice(ArrayList<String> list){
		UserProperty.previousDeviceID = UserProperty.currentDeviceID;
		UserProperty.currentDeviceID = list.get(0);
		
		if (!UserProperty.previousDeviceID.isEmpty()) {
			try{
				this.task.stop(UserProperty.previousDeviceID);
			}
			catch (Exception localException) {}
		}
		
		ADBHelper.makeSystemTapDirectory();
		ADBHelper.copySystemTapModule();
		
		ADBHelper.doInsmod(UserProperty.currentDeviceID);
		ADBHelper.tryToScreenOff(UserProperty.currentDeviceID);
		ADBHelper.setDataPath(UserProperty.currentDeviceID);

		setMode("CONNECTED");
	}

	
	
	public boolean performAction(Actions action){
		if (action.equals(Actions.CONNECT)){
			ArrayList<String> connectedDevices= getConnectedDevices();
			if (connectedDevices==null){
				JOptionPane.showMessageDialog(null, "Try to connect to the device manually using the 'adb shell'\n"+
						"command through the terminal to test the connection.", "No device is found! ", JOptionPane.ERROR_MESSAGE);
				return false;
			}else{
				selectDevice(connectedDevices);
				UserProperty.previousDeviceID = "";
			}
		}else if (action.equals(Actions.START)){
			ThermTap.communicator = new CommunicatorInterface(-1, -1);
			ThermTap.communicator.startTherminator();
			ThermTap.communicator.start();
			
			String[] cmd = new String[]{"shell", "su", "-c", "echo"
					, "1", ">", "/sys/kernel/debug/tracing/tracing_on"};
			new ADBShell(UserProperty.currentDeviceID, cmd).execute();
			
			
			ADBHelper.getInitFreqs(UserProperty.currentDeviceID);
			getApplications(UserProperty.currentDeviceID);

			cleanUp();
			task.start(UserProperty.currentDeviceID);
			setMode("RUN");			
		}else if (action.equals(Actions.STOP)){
			task.stop(UserProperty.currentDeviceID);
			if(ThermTap.communicator != null)
				ThermTap.communicator.interrupt();
			setMode("STOP");
			startTime = -1;
			
			String[] cmd = new String[]{"shell", "su", "-c", "echo"
					, "0", ">", "/sys/kernel/debug/tracing/tracing_on"};
			new ADBShell(UserProperty.currentDeviceID, cmd).execute();
		}else if (action.equals(Actions.DISCONNECT)){
			ADBHelper.doRmMod(UserProperty.currentDeviceID);
			
			cleanUp();
			setMode("DISCONNECTED");
		}
		
		return true;
	}


	private void initControls(){
		UserProperty.previousDeviceID = "";
	}

	private void cleanUp(){
		phone.reset();
		Tool.delete(new File(ADBHelper.getCurPath() + ADBHelper.getFileSeparator() + "data")); //+ UserProperty.getLogDir())
	}

	public Phone getPhone()
	{
		return phone;
	}

	public void setPhone(Phone ph){
		phone = ph;
	}
}
