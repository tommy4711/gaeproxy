package org.gaeproxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class Utils {

	public final static String TAG = "GAEProxy";
	public final static String DEFAULT_SHELL = "/system/bin/sh";

	public final static String DEFAULT_ROOT = "/system/bin/su";
	public final static String ALTERNATIVE_ROOT = "/system/xbin/su";

	public final static String DEFAULT_IPTABLES = "/system/bin/iptables";
	public final static String ALTERNATIVE_IPTABLES = "/data/data/org.gaeproxy/iptables";
	
	private static boolean initialized = false;
	private static int isRoot = -1;
	private static String root_shell = null;
	private static String iptables = null;
	
	public static boolean isInitialized() {
		if (initialized)
			return true;
		else {
			initialized = true;
			return false;
		}
			
	}

	// always return a string
	public static String getRoot() {
		// check root
		if (isRoot())
			return root_shell;
		else
			return DEFAULT_SHELL;
	}

	public static String getIptables() {
		if (iptables == null)
			checkIptables();
		return iptables;
	}

	public static boolean isRoot() {

		if (isRoot != -1)
			return isRoot == 1 ? true : false;

		// switch between binaries
		if (new File(DEFAULT_ROOT).exists()) {
			root_shell = DEFAULT_ROOT;
		} else if (new File(ALTERNATIVE_ROOT).exists()) {
			root_shell = ALTERNATIVE_ROOT;
		} else {
			isRoot = 0;
			return false;
		}

		Process process = null;
		DataInputStream es = null;
		DataOutputStream os = null;
		String line = null;

		try {
			process = Runtime.getRuntime().exec(root_shell);
			es = new DataInputStream(process.getInputStream());
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("ls /\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();

			while (null != (line = es.readLine())) {
				if (line.contains("system")) {
					isRoot = 1;
					break;
				}
			}

		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (es != null) {
					es.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
				// nothing
			}
		}

		return isRoot == 1 ? true : false;
	}

	private static void checkIptables() {

		if (!isRoot())
			return;

		// Check iptables binary
		if (new File(DEFAULT_IPTABLES).exists()) {
			iptables = DEFAULT_IPTABLES;
		} else {
			iptables = ALTERNATIVE_IPTABLES;
			return;
		}

		Process process = null;
		DataInputStream es = null;
		DataOutputStream os = null;
		String line = null;

		boolean compatible = false;
		boolean version = false;

		try {
			process = Runtime.getRuntime().exec(getRoot());
			es = new DataInputStream(process.getInputStream());
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(iptables + " --version");
			os.writeBytes(iptables + " -L -t nat\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();

			while (null != (line = es.readLine())) {
				if (line.contains("OUTPUT")) {
					compatible = true;
				}
				if (line.contains("v1.4.")) {
					version = true;
				}
			}

		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (es != null) {
					es.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
				// nothing
			}
		}

		if (!compatible || !version) {
			iptables = ALTERNATIVE_IPTABLES;
		}

	}

	public static boolean runCommand(String command) {

		Process process = null;
		DataOutputStream os = null;
		Log.d(TAG, command);
		try {
			process = Runtime.getRuntime().exec(DEFAULT_SHELL);
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null) {
					process.destroy();
				}
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

	public static boolean runRootCommand(String command) {

		if (!isRoot())
			return false;

		Log.d(TAG, command);

		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec(getRoot());
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (process != null)
					process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

	public static Drawable getAppIcon(Context c, int uid) {
		PackageManager pm = c.getPackageManager();
		Drawable appIcon = c.getResources().getDrawable(
				R.drawable.sym_def_app_icon);
		String[] packages = pm.getPackagesForUid(uid);

		if (packages != null) {
			if (packages.length == 1) {
				try {
					ApplicationInfo appInfo = pm.getApplicationInfo(
							packages[0], 0);
					appIcon = pm.getApplicationIcon(appInfo);
				} catch (NameNotFoundException e) {
					Log.e(c.getPackageName(),
							"No package found matching with the uid " + uid);
				}
			}
		} else {
			Log.e(c.getPackageName(), "Package not found for uid " + uid);
		}

		return appIcon;
	}
}
