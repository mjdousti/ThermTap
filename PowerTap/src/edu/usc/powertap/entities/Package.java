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


public class Package
{
	private String shardUserID;
	private String userID;
	private String packageName;
	private String nativeLibraryPath;
	private String codePath;

	public Package() {}

	public Package(String _packagename)
	{
		setAppName(_packagename);
	}

	public String getAppName()
	{
		return this.packageName;
	}

	public void setAppName(String appName)
	{
		this.packageName = appName;
	}

	public String getUserID()
	{
		return this.userID;
	}

	public void setUserID(String _uid)
	{
		this.userID = _uid;
	}

	public String getSharedUserID()
	{
		return this.shardUserID;
	}

	public void setShardUserID(String shardUserID)
	{
		this.shardUserID = shardUserID;
	}

	public String getNativeLibraryPath()
	{
		return this.nativeLibraryPath;
	}

	public void setNativeLibraryPath(String nativeLibraryPath)
	{
		this.nativeLibraryPath = nativeLibraryPath;
	}

	public String getCodePath()
	{
		return this.codePath;
	}

	public void setCodePath(String codePath)
	{
		this.codePath = codePath;
	}

	public void showThisPackage()
	{
		System.out.println("Package Name:" + getAppName() + ":");
		System.out.println(" shardUserId:" + getSharedUserID() + ":");
		System.out.println(" userId:" + getUserID() + ":");
		System.out.println(" nativeLib:" + getNativeLibraryPath() + ":");
		System.out.println(" codePath:" + getCodePath());
	}
}

