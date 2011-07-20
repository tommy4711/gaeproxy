/**
 * This file is part of the hyk-proxy-framework project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: CommonUtil.java 
 *
 * @author yinqiwen [ 2010-8-29 | 10:13:24 PM]
 *
 */
package org.hyk.proxy.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

import com.hyk.util.net.NetUtil;

/**
 *
 */
public class CommonUtil
{

	private static final String TAG = "hyk-proxy";

	public static URLConnection openRemoteDescriptionFile(String urlstr) throws MalformedURLException 
	{
		URL url = new URL(urlstr);
		
		try
        {
	        URLConnection conn = url.openConnection();
	        conn.connect();
	        return conn;
        }
        catch (Exception e)
        {
        	// TODO: here
//			SimpleSocketAddress localServAddr = conf
//			        .getLocalProxyServerAddress();
        	SimpleSocketAddress localServAddr = null;
			Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(
			        localServAddr.host, localServAddr.port));
			URLConnection conn;
            try
            {
	            conn = url.openConnection(proxy);
	            conn.connect();
		        return conn;
            }
            catch (IOException e1)
            {
            	Log.e(TAG, "Failed to retrive desc file:" + url, e1);
            }
        }
        return null;
		
	}
	
	public static File downloadFile(String url, File destPath)
	{
		try
		{
			//download direct first
			return NetUtil.downloadFile(new URL(url), destPath);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to download file:" + url, e);
			// Try use proxy again
			// TODO: here
//			SimpleSocketAddress localServAddr = conf
//			        .getLocalProxyServerAddress();
			SimpleSocketAddress localServAddr = null;
			Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(
			        localServAddr.host, localServAddr.port));

			try
			{
				return NetUtil.downloadFile(proxy, new URL(url), destPath);
			}
			catch (Exception e1)
			{
				Log.e(TAG, "Failed to download file:" + url, e1);
			}
		}
		return null;
	}
}
