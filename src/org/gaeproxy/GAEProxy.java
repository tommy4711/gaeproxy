package org.gaeproxy;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class GAEProxy extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static final String TAG = "GAEProxy";
	public static final String PREFS_NAME = "GAEProxy";
	private static final String SERVICE_NAME = "org.gaeproxy.GAEProxyService";
	private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

	private String proxy;
	private int port;
	public static boolean isAutoStart = false;
	public static boolean isAutoSetProxy = false;
	public static boolean isRoot = false;

	private CheckBoxPreference isAutoConnectCheck;
	private CheckBoxPreference isInstalledCheck;
	private CheckBoxPreference isAutoSetProxyCheck;
	private EditTextPreference proxyText;
	private EditTextPreference portText;
	private CheckBoxPreference isRunningCheck;

	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
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
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

	/**
	 * 解压缩一个文件
	 * 
	 * @param zipFile
	 *            压缩文件
	 * @param folderPath
	 *            解压缩的目标目录
	 * @throws IOException
	 *             当解压缩过程出错时抛出
	 */
	public static void upZipFile(File zipFile, String folderPath)
			throws ZipException, IOException {
		File desDir = new File(folderPath);
		if (!desDir.exists()) {
			desDir.mkdirs();
		}
		ZipFile zf = new ZipFile(zipFile);
		for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
			ZipEntry entry = ((ZipEntry) entries.nextElement());
			InputStream in = zf.getInputStream(entry);
			String str = folderPath + File.separator + entry.getName();
			str = new String(str.getBytes("8859_1"), "GB2312");
			File desFile = new File(str);
			if (!desFile.exists()) {
				File fileParentDir = desFile.getParentFile();
				if (!fileParentDir.exists()) {
					fileParentDir.mkdirs();
				}
				desFile.createNewFile();
			}
			OutputStream out = new FileOutputStream(desFile);
			byte buffer[] = new byte[BUFF_SIZE];
			int realLength;
			while ((realLength = in.read(buffer)) > 0) {
				out.write(buffer, 0, realLength);
			}
			in.close();
			out.close();
		}
	}

	private void CopyAssets(String path) {

		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list(path);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		for (int i = 0; i < files.length; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {
				// if (!(new File("/data/data/org.gaeproxy/" +
				// files[i])).exists()) {
				in = assetManager.open(files[i]);
				out = new FileOutputStream("/data/data/org.gaeproxy/"
						+ files[i]);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
				// }
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	private boolean isTextEmpty(String s, String msg) {
		if (s == null || s.length() <= 0) {
			showAToast(msg);
			return true;
		}
		return false;
	}

	public boolean isWorked(String service) {
		ActivityManager myManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30);
		for (int i = 0; i < runningService.size(); i++) {
			if (runningService.get(i).service.getClassName().toString()
					.equals(service)) {
				return true;
			}
		}
		return false;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//
		// File python = new File("/data/data/org.gaeproxy/python_r11.zip");
		// File python_extras = new
		// File("/data/data/org.gaeproxy/python_extras_r11.zip");
		//
		// try {
		// upZipFile(python, "/data/data/org.gaeproxy/python/");
		// upZipFile(python_extras, "/data/data/org.gaeproxy/");
		// } catch (ZipException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		addPreferencesFromResource(R.xml.gae_proxy_preference);

		proxyText = (EditTextPreference) findPreference("proxy");
		portText = (EditTextPreference) findPreference("port");

		isRunningCheck = (CheckBoxPreference) findPreference("isRunning");
		isAutoSetProxyCheck = (CheckBoxPreference) findPreference("isAutoSetProxy");
		isAutoConnectCheck = (CheckBoxPreference) findPreference("isAutoConnect");
		isInstalledCheck = (CheckBoxPreference) findPreference("isInstalled");

		final CheckBoxPreference isRunningCheck = (CheckBoxPreference) findPreference("isRunning");
		if (this.isWorked(SERVICE_NAME)) {
			isRunningCheck.setChecked(true);
		} else {
			isRunningCheck.setChecked(false);
		}

		if (!runRootCommand("ls")) {
			isRoot = false;
		} else {
			isRoot = true;
		}

		if (!isRoot) {
			final CheckBoxPreference isAutoSetProxyCheck = (CheckBoxPreference) findPreference("isAutoSetProxy");
			isAutoSetProxyCheck.setChecked(false);
			isAutoSetProxyCheck.setEnabled(false);
		}

		if (!isWorked(SERVICE_NAME)) {
			CopyAssets("");

			runRootCommand("chmod 777 /data/data/org.gaeproxy/iptables_g1");
			runRootCommand("chmod 777 /data/data/org.gaeproxy/iptables_n1");
			runRootCommand("chmod 777 /data/data/org.gaeproxy/redsocks");
			runRootCommand("chmod 777 /data/data/org.gaeproxy/proxy.sh");
		}
	}

	/** Called when the activity is closed. */
	@Override
	public void onDestroy() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("isConnected", isWorked(SERVICE_NAME));

		editor.commit();
		super.onDestroy();
	}

	/**
	 * Called when connect button is clicked.
	 * 
	 * @throws Exception
	 */
	public void serviceStart() {

		if (isWorked(SERVICE_NAME)) {
			try {
				stopService(new Intent(this, GAEProxyService.class));
			} catch (Exception e) {
				// Nothing
			}
			return;
		}

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		proxy = settings.getString("proxy", "");
		if (isTextEmpty(proxy, getString(R.string.proxy_empty)))
			return;

		String portText = settings.getString("port", "");
		if (isTextEmpty(portText, getString(R.string.port_empty)))
			return;
		port = Integer.valueOf(portText);
		if (port <= 1024)
			this.showAToast(getString(R.string.port_alert));

		isAutoStart = settings.getBoolean("isAutoStart", false);
		isAutoSetProxy = settings.getBoolean("isAutoSetProxy", false);

		try {

			Intent it = new Intent(this, GAEProxyService.class);
			Bundle bundle = new Bundle();
			bundle.putString("proxy", proxy);
			bundle.putInt("port", port);
			bundle.putBoolean("isAutoSetProxy", isAutoSetProxy);

			it.putExtras(bundle);
			startService(it);
		} catch (Exception e) {
			// Nothing
		}

		return;
	}

	private void showAToast(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg)
				.setCancelable(false)
				.setNegativeButton(getString(R.string.ok_iknow),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		if (preference.getKey() != null
				&& preference.getKey().equals("isRunning")) {
			serviceStart();
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		Editor edit = settings.edit();

		if (this.isWorked(SERVICE_NAME)) {
			edit.putBoolean("isRunning", true);
		} else {
			edit.putBoolean("isRunning", false);
		}

		edit.commit();

		if (settings.getBoolean("isRunning", false)) {
			isRunningCheck.setChecked(true);
			disableAll();
		} else {
			isRunningCheck.setChecked(false);
			enableAll();
		}

		// Setup the initial values

		if (!settings.getString("port", "").equals(""))
			portText.setSummary(settings.getString("port",
					getString(R.string.port_summary)));
		if (!settings.getString("proxy", "").equals(""))
			proxyText.setSummary(settings.getString("proxy",
					getString(R.string.proxy_summary)));

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Let's do something a preference value changes
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		if (key.equals("isInstalled")) {
			if (settings.getBoolean("isInstalled", false)) {
				install();
				isInstalledCheck.setChecked(true);
			} else {
				uninstall();
				isInstalledCheck.setChecked(false);
			}
		}
		
		if (key.equals("isRunning")) {
			if (settings.getBoolean("isRunning", false)) {
				disableAll();
				isRunningCheck.setChecked(true);
			} else {
				enableAll();
				isRunningCheck.setChecked(false);
			}
		}

		if (key.equals("port"))
			if (settings.getString("port", "").equals(""))
				portText.setSummary(getString(R.string.port_summary));
			else
				portText.setSummary(settings.getString("port", ""));
		else if (key.equals("proxy"))
			if (settings.getString("proxy", "").equals(""))
				proxyText.setSummary(getString(R.string.proxy_summary));
			else
				proxyText.setSummary(settings.getString("host", ""));
	}

	private void disableAll() {
		proxyText.setEnabled(false);
		portText.setEnabled(false);

		isAutoSetProxyCheck.setEnabled(false);
		isAutoConnectCheck.setEnabled(false);
		isInstalledCheck.setEnabled(false);
	}

	private void enableAll() {
		proxyText.setEnabled(true);
		portText.setEnabled(true);

		isAutoSetProxyCheck.setEnabled(true);
		isAutoConnectCheck.setEnabled(true);
		isInstalledCheck.setEnabled(true);
	}
	
	private void install() {
		
		 File python = new File("/data/data/org.gaeproxy/python_r11.zip");
		 File python_extras = new
		 File("/data/data/org.gaeproxy/python_extras_r11.zip");
		
		 try {
		 upZipFile(python, "/data/data/org.gaeproxy/python/");
		 upZipFile(python_extras, "/data/data/org.gaeproxy/");
		 } catch (ZipException e1) {
		 // TODO Auto-generated catch block
		 e1.printStackTrace();
		 } catch (IOException e1) {
		 // TODO Auto-generated catch block
		 e1.printStackTrace();
		 }
	}
	
	private void uninstall() {
		
	}

}