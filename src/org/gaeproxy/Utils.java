package org.gaeproxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class Utils {

	public final static String TAG = "GAEProxy";
	private final static String DEFAULT_SHELL = "/system/bin/sh";
	public static String root_shell = "/system/bin/su";

	private static ArrayList<String> parse(String cmd) {
		final int PLAIN = 0;
		final int WHITESPACE = 1;
		final int INQUOTE = 2;
		int state = WHITESPACE;
		ArrayList<String> result = new ArrayList<String>();
		int cmdLen = cmd.length();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < cmdLen; i++) {
			char c = cmd.charAt(i);
			if (state == PLAIN) {
				if (Character.isWhitespace(c)) {
					result.add(builder.toString());
					builder.delete(0, builder.length());
					state = WHITESPACE;
				} else if (c == '"') {
					state = INQUOTE;
				} else {
					builder.append(c);
				}
			} else if (state == WHITESPACE) {
				if (Character.isWhitespace(c)) {
					// do nothing
				} else if (c == '"') {
					state = INQUOTE;
				} else {
					state = PLAIN;
					builder.append(c);
				}
			} else if (state == INQUOTE) {
				if (c == '\\') {
					if (i + 1 < cmdLen) {
						i += 1;
						builder.append(cmd.charAt(i));
					}
				} else if (c == '"') {
					state = PLAIN;
				} else {
					builder.append(c);
				}
			}
		}
		if (builder.length() > 0) {
			result.add(builder.toString());
		}
		return result;
	}

	public static synchronized FileDescriptor createSubprocess(String shell, int[] processId) {

		if (shell == null || shell.equals("")) {
			shell = DEFAULT_SHELL;
		}
		ArrayList<String> args = parse(shell);
		String arg0 = args.get(0);
		String arg1 = null;
		String arg2 = null;

		if (args.size() >= 2) {
			arg1 = args.get(1);
		}
		if (args.size() >= 3) {
			arg2 = args.get(2);
		}

		return Exec.createSubprocess(arg0, arg1, arg2, processId);
	}

	private static int isRoot = -1;

	private final static class DetectorRunnable extends Thread {

		private DataInputStream es = null;
		private DataOutputStream os = null;
		private int processId = -1;
		private FileDescriptor process = null;

		/**
		 * Destroy this script runner
		 */
		public synchronized void destroy() {
			if (es != null) {
				try {
					es.close();
				} catch (IOException e) {
					Log.e(TAG, "Eroor in close termIn", e);
				}
				

				es = null;
			}

			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					Log.e(TAG, "Eroor in close termOut", e);
				}
				
				os = null;
			}


			Exec.hangupProcessGroup(processId);

			if (process != null) {
				Exec.close(process);
				process = null;
			}
		}

		public void run() {

			String line = null;
			
			try {
				
				int[] processIds = new int[1];
				if (!new File(root_shell).exists()) {
					root_shell = "/system/xbin/su";
					if (! new File(root_shell).exists()) {
						isRoot = 0;
						return;
					}
				} 

				process = createSubprocess(
						root_shell, processIds);
				processId = processIds[0];
				Log.d(TAG, "Process ID: " + processId);
				
				es = new DataInputStream(new FileInputStream(process));
				os = new DataOutputStream(new FileOutputStream(process));
				os.writeBytes("ls /\n");
				os.writeBytes("exit\n");
				os.flush();

				Exec.waitFor(processId);

				while (null != (line = es.readLine())) {
					Log.d(TAG, line);
					if (line.contains("system")) {
						isRoot = 1;
						break;
					}
				}

			} catch (Exception e) {
				Log.e(TAG, "Unexpected Error", e);
			} finally {
				try {
					
					if (es != null) {
						es.close();
						es = null;
					}

					if (os != null) {
						os.close();
						os = null;
					}

					Exec.hangupProcessGroup(processId);

					if (process != null) {
						Exec.close(process);
						process = null;
					}
				} catch (Exception e) {
					// nothing
				}
			}
		}
	}

	public static boolean isRoot() {

		if (isRoot != -1)
			return isRoot == 1 ? true : false;

		final DetectorRunnable detector = new DetectorRunnable();

		detector.start();

		try {
			detector.join(12 * 1000);
			if (detector.isAlive()) {
				// Timed-out
				detector.destroy();
				detector.join(150);
			}
		} catch (InterruptedException ex) {
			// Nothing
		}

		if (isRoot == -1) {
			isRoot = 0;
		}

		return isRoot == 1 ? true : false;
	}

	public static boolean runRootCommand(String command) {

		if (isRoot())
			return runCommand(root_shell, command);
		else
			return false;

	}

	public static boolean runCommand(String command) {

		return runCommand(DEFAULT_SHELL, command);

	}

	public static boolean runCommand(String shell, String command) {

		int processId = -1;

		FileDescriptor process = null;
		DataOutputStream os = null;

		Log.d(TAG, command);

		try {

			int[] processIds = new int[1];
			process = createSubprocess(shell, processIds);
			processId = processIds[0];
			
			os = new DataOutputStream(new FileOutputStream(process));

			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();

			Exec.waitFor(processId);

		} catch (Exception e) {
			Log.e(TAG, "Unexcepted Error", e);
			return false;
		} finally {
			try {

				if (os != null) {
					os.close();
					os = null;
				}
				
				Exec.hangupProcessGroup(processId);

				if (process != null) {
					Exec.close(process);
					process = null;
				}
				
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}
}
