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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.powertap.entities.Package;
import edu.usc.powertap.entities.Phone;
import edu.usc.powertap.entities.UidNode;
import edu.usc.powertap.powermodel.CPUPower;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.thermtap.ThermTap;

public class ADBHelper
{
	private static Logger logger = Logger.getLogger("");
	public static void setPath()
	{
		UserProperty.DATA_DIR = getCurPath() + getFileSeparator() + "data";
		logger.finer("Data file path: " + UserProperty.DATA_DIR);

		/*
		 * This part is retained in case one wants to port ThermTap to Windows or Mac
		 * Note that you need to get adb for Mac and Windows and place them in adb_tools/osx and adb_tools/win, respectively.
		 */
		if ((getOSName().contains("Win")) || (getOSName().contains("win"))) {
			UserProperty.LOCAL_ADB_PATH = getCurPath() + getFileSeparator() + "adb_tools" + getFileSeparator() + "win" + getFileSeparator();
		} else if ((getOSName().contains("Linux")) || (getOSName().contains("linux"))) {
			UserProperty.LOCAL_ADB_PATH = getCurPath() + getFileSeparator() + "adb_tools" + getFileSeparator() + "linux" + getFileSeparator();
			new File(UserProperty.LOCAL_ADB_PATH+"adb").setExecutable(true, false);	//Makes sure that adb is executable
		} else if ((getOSName().contains("Mac")) || (getOSName().contains("mac"))) {
			UserProperty.LOCAL_ADB_PATH = getCurPath() + getFileSeparator() + "adb_tools" + getFileSeparator() + "osx" + getFileSeparator();
			new File(UserProperty.LOCAL_ADB_PATH+"adb").setExecutable(true, false);	//Makes sure that adb is executable
		}
		logger.finer("OS name: " + getOSName());
		logger.finer("ADB tools path: " + UserProperty.LOCAL_ADB_PATH);
	}

	public static void setDataPath(String _device){
		UserProperty.DATA_DIR = getCurPath() + getFileSeparator() + "data" + getFileSeparator() + _device + getFileSeparator();
		logger.finer("Reset Data file path to " + UserProperty.DATA_DIR);
		UserProperty.PACKAGE_FILE = UserProperty.DATA_DIR + "packages.xml";
	}

	private static String getOSName(){
		return System.getProperty("os.name");
	}

	public static String getFileSeparator(){
		return System.getProperty("file.separator");
	}

	public static String getCurPath(){
		return System.getProperty("user.dir");
	}

	public static ArrayList<UidNode> makeUidList(){
		logger.finer("PackageList Parsing result >>");

		ArrayList<UidNode> uidNodeList = new ArrayList<UidNode>();
		try	{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			File xmlFile = new File(UserProperty.currentPackageFile);
			Document doc = docBuilder.parse(xmlFile);

			doc.getDocumentElement().normalize();

			logger.finer("Root element >>" + doc.getDocumentElement().getNodeName());

			NodeList package_node = doc.getElementsByTagName("package");
			for (int i = 0; i < package_node.getLength(); i++){
				NamedNodeMap nodemap = package_node.item(i).getAttributes();
				Node temp_packageName = nodemap.getNamedItem("name");
				Node temp_sharedUserId = nodemap.getNamedItem("sharedUserId");
				Node temp_userId = nodemap.getNamedItem("userId");
				Node temp_nativeLibraryPath = nodemap
						.getNamedItem("nativeLibraryPath");
				Node temp_codePath = nodemap.getNamedItem("codePath");

				String _uid = "";
				if (temp_packageName != null)
				{
					Package p_info = new Package(
							temp_packageName.getNodeValue());
					if (temp_sharedUserId != null)
					{
						p_info.setShardUserID(temp_sharedUserId.getNodeValue());
						_uid = temp_sharedUserId.getNodeValue();
					}
					if (temp_userId != null)
					{
						p_info.setUserID(temp_userId.getNodeValue());
						_uid = temp_userId.getNodeValue();
					}
					if (temp_nativeLibraryPath != null) {
						p_info.setNativeLibraryPath(temp_nativeLibraryPath
								.getNodeValue());
					}
					if (temp_codePath != null) {
						p_info.setCodePath(temp_codePath.getNodeValue());
					}
					boolean isDuplicated = false;
					for (int j = 0; j < uidNodeList.size(); j++)
					{
						UidNode _temp = (UidNode)uidNodeList.get(j);
						if (Integer.parseInt(_uid) == _temp.getUid())
						{
							_temp.addPackage(p_info);
							isDuplicated = true;
							break;
						}
					}
					if (!isDuplicated) {
						uidNodeList.add(new UidNode(p_info));
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return uidNodeList;
	}

	public static boolean makeDatafileDir()
	{
		boolean success = false;
		File data_dir=new File(UserProperty.DATA_DIR);

		if (!data_dir.exists()){
			success = data_dir.mkdir();
		}else{
			logger.fine("Data directory already exists.");
			success = true;
		}

		if (!success){
			logger.fine("Failed to create the data directory.");
		}

		return success;
	}

	public static boolean makeDatafileDir(String s)
	{
		boolean success = false;
		success = new File(UserProperty.DATA_DIR + s + getFileSeparator())
		.mkdir();

		return success;
	}

	public static boolean copySystemTapModule(){
		logger.fine("Try to install the SystemTap module");
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String[]> commands = new ArrayList<>();
		
		commands.add(new String[]{"shell", "su", "-c", "mkdir", "-p", "/data/local/systemtap"});
		
		commands.add(new String[]{"shell", "su", "-c", "mkdir", "-p", "/sdcard/systemtap"});
		
		commands.add(new String[]{"shell", "su", "-c", "chmod", "777", "/sdcard/systemtap"});

		commands.add(new String[]{"shell", "su", "-c", "rm", "/sdcard/systemtap/*"});
		
		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/powertap.ko", "/sdcard/systemtap/"});
		
		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/jcat", "/sdcard/systemtap/"});
		
		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/staprun", "/sdcard/systemtap/"});

		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/stapio", "/sdcard/systemtap/"});
		
		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/start_stap.sh", "/sdcard/systemtap/"});
		
		commands.add(new String[]{"push", 
				 getCurPath() + getFileSeparator() + 
				"android/kill_stap.sh", "/sdcard/systemtap/"});
		
		commands.add(new String[]{"shell", "su", "-c", "mv", "/sdcard/systemtap/*", "/data/local/systemtap"});
		
		commands.add(new String[]{"shell", "su", "-c", "chmod", "777", "/data/local/systemtap/*"});
	
		
		
		int exitVal = 0;
		for (String[] cmd : commands) {
			exitVal = new ADBShell(UserProperty.currentDeviceID, cmd).execute(result);
			if(exitVal != 0){
				System.out.print("failed in: ");
				for (String string : cmd) {
					System.out.print(string + " ");
				}
				System.out.println();
			}		
		}
		
		//Simulating an "mv" command for Android!
		//More info: http://stackoverflow.com/questions/3555755/how-can-i-copy-the-contentsof-data-to-sdcard-without-using-adb
		if (exitVal == 0) {
			return true;
		}else{
			System.err.println("Could not push modules!");
			System.exit(-1);
		}
		
		return false;
	}

	public static boolean makeSystemTapDirectory()
	{
		ArrayList<String> result = new ArrayList<String>();
		int exitVal = new ADBShell(UserProperty.currentDeviceID, 
				new String[]{"shell", "su", "-c", "mkdir", "/data/systemtap/"}).execute(result);
		if (exitVal == 0) {
			return true;
		}
		return false;
	}

	public static boolean checkSystemTapDirectory(){
		ArrayList<String> result = new ArrayList<String>();
		int exitVal = new ADBShell(UserProperty.currentDeviceID, 
				new String[]{"shell", "su", "-c", "ls", "/data/systemtap/"}).execute(result);
		if (exitVal == 0)
		{
			if (result.size() > 0)
			{
				if (((String)result.get(0)).contains("No such file or directory"))
				{
					logger.fine("No such file or directory");
					return false;
				}
				if (copySystemTapModule()) {
					return true;
				}
				return false;
			}
			return true;
		}
		return false;
	}

	public static void getPackageFile(String _device)
	{
		int exitVal = new ADBShell(_device, new String[]{"pull", "/data/system/packages.xml", 
				UserProperty.DATA_DIR + getFileSeparator() + "packages.xml"}).execute();

		// Fixing permission problems on Samsung Galaxy S4
		exitVal |= new ADBShell(_device, new String[]{"shell", "su", "-c", "cp", "/data/system/packages.xml", "/sdcard/"}).execute();

		exitVal |= new ADBShell(_device, new String[]{"pull", "/sdcard/packages.xml", 
				UserProperty.DATA_DIR + getFileSeparator() + "packages.xml"}).execute();

		exitVal |= new ADBShell(_device, new String[]{"shell", "su", "-c", "rm", "/sdcard/packages.xml"}).execute();


		if (exitVal == 0) {
			UserProperty.currentPackageFile = UserProperty.PACKAGE_FILE;
		}else{
			logger.warning("Failed to fetch packages.xml from the phone." );
		}
	}

	public static void sendKeyCode(String _device, String keycode){
		int exitVal = new ADBShell(_device, new String[]{"shell", "su", "-c", "input", "keyevent", keycode}).execute();
		if (exitVal!=0){
			//TODO: Take proper action!
		}
	}

	public static void tryToScreenOff(String _device){
		sendKeyCode(_device, "26");
	}

	public static void doRmMod(String _device){
		int exitVal = new ADBShell(_device, new String[]{"shell", "su", "-c", "sh", "/data/local/systemtap/kill_stap.sh"}).execute();
		if (exitVal!=0){
			//Take proper action!
			System.err.println("Could not kill systemtap.");
		}
	}

	public static void doInsmod(String _device)
	{
//		int exitVal = new ADBShell(_device,	new String[]{"shell", "su", "-c", "sh", "/data/local/systemtap/kill_stap.sh"}).execute();
		int exitVal = new ADBShell(_device,	new String[]{"shell", "su", "-c", "/data/local/systemtap/start_stap.sh"}).execute();
		
		if (exitVal == 0) {
			UserProperty.currentPackageFile = UserProperty.PACKAGE_FILE;
			logger.finer("Running systemtap on device "+_device+".");
		}else{
			System.err.println("Failed to run systemtap.");
		}
	}
	
	private static double readNumericFile(String path, String _device){
		int exitVal;
		double value;
		
		exitVal = new ADBShell(_device,	new String[]{"pull", path, UserProperty.DATA_DIR + getFileSeparator()+"temp"}).execute();
		

		if (exitVal!=0)
			return -1;

		Scanner sc;
		try {
			sc = new Scanner(new File(UserProperty.DATA_DIR + getFileSeparator() + "temp"));
			value = sc.nextDouble();
			sc.close();
			new File(UserProperty.DATA_DIR + getFileSeparator() + "temp").delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
		
		return value;
	}
	
	public static void getInitFreqs(String _device){
		Phone phone = ThermTap.getPhone();
		double[] freq=new double[phone.coreCount];
		double out;
		for (int i = 0; i < phone.coreCount; i++) {
			out =readNumericFile("/sys/devices/system/cpu/cpu"+i+"/online", _device);
			if (out==0){	//cpu #i is offline
				freq[i] = 0;
			}else{
				out =readNumericFile("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_cur_freq", _device);
				freq[i] = out / 1.0e6;		//converting freq to GHz
				System.out.println("core "+i+" has frequency: "+freq[i]);
				if (out == -1) {
					System.err.println("Failed to read the frequency of CPU "+i);
					System.exit(-1);
				}
			}
			logger.finer("Freq. of cpu #"+i+" is set to "+freq[i]+" GHz.");
		}
		CPUPower.setFreq(freq);
	}

	private static boolean ADBStartServer(){
		if (new ADBShell("start-server").execute() == 0) {
			return true;
		}
		return false;
	}

	public static ArrayList<String> getDevices()
	{
		logger.fine("Try to get connected devices...");

		ArrayList<String> list = new ArrayList<String>();
		ArrayList<String> temp = new ArrayList<String>();

		ADBStartServer();

		int exitVal = new ADBShell("devices").execute(temp);
		if (exitVal == 0) {
			for (String s : temp) {
				if ((s.contains("device")) && (!s.contains("List of"))){
					list.add(s.substring(0, s.length() - 6).trim());
					logger.config("Detected: " + (String)list.get(list.size() - 1));
				}
			}
		}
		return list;
	}
	
	public static void initFtrace(String _device){
		int exitVal = new ADBShell(_device,	new String[]{"shell", "su", "-c", "\"echo 1024 > /sys/kernel/debug/tracing/buffer_size_kb\""}).execute();
		exitVal |= new ADBShell(_device, new String[]{"shell", "su", "-c", "\"echo 0 > /sys/kernel/debug/tracing/tracing_on\""}).execute();
		exitVal |= new ADBShell(_device, new String[]{"shell", "su", "-c", "\"echo > /sys/kernel/debug/tracing/trace\""}).execute();
		exitVal |= new ADBShell(_device, new String[]{"shell", "su", "-c", "\"echo 1 > /sys/kernel/debug/tracing/tracing_on\""}).execute();
	}
}
