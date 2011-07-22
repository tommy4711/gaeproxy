/**
 * This file is part of the hyk-proxy-framework project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: AppData.java 
 *
 * @author yinqiwen [ 2010-8-28 | 11:28:59 PM]
 *
 */
package org.hyk.proxy.framework.appdata;

import java.io.File;

import org.hyk.proxy.framework.common.Constants;

import android.util.Log;



/**
 *
 */
public class AppData
{
	private static final String TAG = "hyk-proxy";

	private static File USR_APP_HOME = null;
	private static File USR_APP_ETC = null;
	private static File USR_APP_PLUGINS = null;
	private static File APP_PLUGINS = null;
	private static File APP_UPDATES = null;
	private static File USR_FRAMEWORK_CONF = null;
	private static File APP_FRAMEWORK_CONF = null;
	private static File APP_HOME = null;
	private static File USR_PLUGINS_STATE = null;
	private static File USR_PREFERENCE = null;
	private static File LOG_HOME = null;

	private static File FAKE_CERT_HOME = null;
	static
	{
		init();
	}

	private static void init()
	{
		try
		{
			String filesp = System.getProperty("file.separator");
			String usrHome = System.getProperty("user.home");
			String appHome = System.getProperty(Constants.SOFT_HOME_VAR);
			APP_HOME = new File(appHome);
			FAKE_CERT_HOME = new File(appHome + filesp + "etc" + filesp
			        + "cert");
			boolean appHomeWraitable = true;
			APP_PLUGINS = new File(APP_HOME, "plugins");
			if (!APP_PLUGINS.exists())
			{
				APP_PLUGINS.mkdir();
			}
			if (!FAKE_CERT_HOME.exists())
			{
				try
				{
					FAKE_CERT_HOME.mkdir();
				}
				catch (Exception e)
				{
					FAKE_CERT_HOME = null;
					appHomeWraitable = false;
				}

			}

			APP_FRAMEWORK_CONF = new File(APP_HOME, "etc" + Constants.FILE_SP
			        + Constants.CONF_FILE);

			USR_APP_HOME = new File(usrHome + filesp + "."
			        + Constants.PROJECT_NAME);

			if (!USR_APP_HOME.exists())
			{
				USR_APP_HOME.mkdir();
			}

			LOG_HOME = new File(USR_APP_HOME, ".log");
			if (!LOG_HOME.exists())
			{
				LOG_HOME.mkdir();
			}

			USR_PREFERENCE = new File(USR_APP_HOME, ".prefernce");
			if (!USR_PREFERENCE.exists())
			{
				USR_PREFERENCE.createNewFile();
			}

			if (USR_APP_HOME.isDirectory())
			{
				USR_APP_ETC = new File(USR_APP_HOME, ".etc");
				if (!USR_APP_ETC.exists())
				{
					USR_APP_ETC.mkdir();
				}

				if (null == FAKE_CERT_HOME)
				{
					FAKE_CERT_HOME = new File(USR_APP_ETC, "cert");
					if (!FAKE_CERT_HOME.exists())
					{
						FAKE_CERT_HOME.mkdir();
					}
				}

				USR_FRAMEWORK_CONF = new File(USR_APP_ETC, Constants.CONF_FILE);
				USR_APP_PLUGINS = new File(USR_APP_HOME, ".plugins");
				if (!USR_APP_PLUGINS.exists())
				{
					USR_APP_PLUGINS.mkdir();
				}
				APP_UPDATES = new File(USR_APP_HOME, ".update");
				if (!APP_UPDATES.exists())
				{
					APP_UPDATES.mkdir();
				}

				USR_PLUGINS_STATE = new File(USR_APP_HOME, ".pluginstate");
				if (!USR_PLUGINS_STATE.exists())
				{
					USR_PLUGINS_STATE.createNewFile();
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to init appdata.", e);
		}

	}

	public static File getUserFrameworkConf()
	{
		return USR_FRAMEWORK_CONF;
	}

	public static File getAppFrameworkConf()
	{
		return APP_FRAMEWORK_CONF;
	}

	public static File getUserPluginsHome()
	{
		return USR_APP_PLUGINS;
	}

	public static File getUserPrefernceFile()
	{
		return USR_PREFERENCE;
	}

	public static File GetFakeSSLCertHome()
	{
		return FAKE_CERT_HOME;
	}

	public static File getPluginsHome()
	{
		return APP_PLUGINS;
	}

	public static File getUserPluginState()
	{
		return USR_PLUGINS_STATE;
	}

	public static File getUserUpdateHome()
	{
		return APP_UPDATES;
	}

	public static File getLogHome()
	{
		return LOG_HOME;
	}
}
