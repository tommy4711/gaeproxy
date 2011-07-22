/**
 * This file is part of the hyk-proxy-framework project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: Misc.java 
 *
 * @author yinqiwen [ 2010-8-17 | 11:36:30 AM ]
 *
 */
package org.hyk.proxy.framework.common;

import java.util.concurrent.ExecutorService;

import org.hyk.proxy.framework.management.UDPManagementServer;

/**
 *
 */
public class Misc
{
	private static ExecutorService  globalThreadPool;
	private static UDPManagementServer managementServer;

	public static UDPManagementServer getManagementServer()
	{
		return managementServer;
	}

	public static void setManagementServer(UDPManagementServer managementServer)
	{
		Misc.managementServer = managementServer;
	}

	public static ExecutorService getGlobalThreadPool()
    {
    	return globalThreadPool;
    }

	public static void setGlobalThreadPool(ExecutorService globalThreadPool)
    {
		if(null != Misc.globalThreadPool)
		{
			Misc.globalThreadPool.shutdown();
		}
		Misc.globalThreadPool = globalThreadPool;
    }

}
