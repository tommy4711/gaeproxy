package org.gaeproxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import android.util.Log;

public class Utils {

	public final static String TAG = "GAEProxy";
	public static int isRoot = -1;
	public static String ROOT_SHELL = "/system/bin/su";
	public final static String DEFAULT_SHELL = "/system/bin/sh";
	public final static String IPTABLES = "/data/data/org.gaeproxy/iptables";

	public static boolean isRoot() {

		if (isRoot != -1)
			return isRoot == 1 ? true : false;

		// Check is binary exists
		if (!new File(ROOT_SHELL).exists()) {
			ROOT_SHELL = "/system/xbin/su";
			if (!new File(ROOT_SHELL).exists()) {
				isRoot = 0;
				return false;
			}

		}

		Process process = null;
		DataInputStream es = null;
		DataOutputStream os = null;
		String line = null;

		try {
			process = Runtime.getRuntime().exec(ROOT_SHELL);
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

		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec(ROOT_SHELL);
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
}
