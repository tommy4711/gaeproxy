/**
 * This file is part of the hyk-proxy-framework project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: Framework.java 
 *
 * @author yinqiwen [ 2010-8-12 | 09:28:05 PM]
 *
 */
package org.hyk.proxy.framework;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hyk.proxy.framework.common.Misc;
import org.hyk.proxy.framework.config.Config;
import org.hyk.proxy.framework.event.HttpProxyEventServiceFactory;
import org.hyk.proxy.framework.httpserver.HttpLocalProxyServer;
import org.hyk.proxy.framework.management.UDPManagementServer;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import android.util.Log;

import com.hyk.proxy.client.application.gae.event.GoogleAppEngineHttpProxyEventServiceFactory;
import com.hyk.proxy.common.ExtensionsLauncher;



/**
 *
 */
public class Framework 
{
	private static final String TAG = "hyk-proxy";
	
	private HttpLocalProxyServer server;
	private UDPManagementServer commandServer;
	private HttpProxyEventServiceFactory esf = null;
//	private PluginManager pm ;
//	private Updater updater;

	private boolean isStarted = false;
	private boolean isStarting = false;
	
	static
	{
		ExtensionsLauncher.init();
	}
	
	private static Framework instance = null;
	
	private Framework()
	{
		//Preferences.init();
//		pm = PluginManager.getInstance();
		ThreadPoolExecutor workerExecutor = new OrderedMemoryAwareThreadPoolExecutor(
				100, 0, 0);

		//ThreadPoolExecutor workerExecutor = new ScheduledThreadPoolExecutor(config.getThreadPoolSize());
		Misc.setGlobalThreadPool(workerExecutor);
		init();
	}
	
	public static Framework getInstance()
	{
		if(null == instance)
		{
			 instance = new Framework();
		}
		return instance;
	}
	
	private void init()
	{
		HttpProxyEventServiceFactory.Registry.register(new GoogleAppEngineHttpProxyEventServiceFactory());
	}

	public void stop()
	{
		try
		{
			if (null != commandServer)
			{
				commandServer.stop();
				commandServer = null;
			}
			if (null != server)
			{
				server.close();
				server = null;
			}
			if (null != esf)
			{
				esf.destroy();
			}
			isStarted = false;
			isStarting = false;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to stop framework.", e);
		}

	}
	
	public boolean isStarted()
	{
		return isStarted;
	}
	
	public boolean isStarting()
	{
		return isStarting;
	}

	public boolean start()
	{
		return restart();
	}

	public boolean restart()
	{
		try
		{
			stop();
			Config config = Config.getInstance();
			esf = HttpProxyEventServiceFactory.Registry
			        .getHttpProxyEventServiceFactory(config
			                .getProxyEventServiceFactory());
			if (esf == null)
			{
				Log.e(TAG, "No event service factory found with name:"
				        + config.getProxyEventServiceFactory());
				return false;
			}
			isStarting = true;
			esf.init();
			server = new HttpLocalProxyServer(
			        config.getLocalProxyServerAddress(),
			        Misc.getGlobalThreadPool(), esf);
			//Misc.getGlobalThreadPool().execute(commandServer);
			isStarted = true;
			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to launch local proxy server.", e);
			isStarted = false;
			isStarting = false;
		}
		return false;
	}

}
