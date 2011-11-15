/* gaeproxy - GAppProxy / WallProxy client App for Android
 * Copyright (C) 2011 <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */

package org.gaeproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ConnectivityBroadcastReceiver";

	private static Object switching = new Object();

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			Log.e(TAG, "onReceived() called uncorrectly");
			return;
		}

		synchronized (switching) {

			Log.e(TAG, "Connection Test");

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(context);

			// only switching profiles when needed
			ConnectivityManager manager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = manager.getActiveNetworkInfo();

			if (networkInfo != null) {

				String lastSSID = settings.getString("lastSSID", "-1");
				String currentSSID = "-1";

				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					WifiManager wm = (WifiManager) context
							.getSystemService(Context.WIFI_SERVICE);
					WifiInfo wInfo = wm.getConnectionInfo();
					if (wInfo != null) {
						currentSSID = wInfo.getSSID();
					}
				} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					currentSSID = Integer
							.toString(ConnectivityManager.TYPE_MOBILE);
				}

				if (currentSSID.equals("-1"))
					return;

				if (!currentSSID.equals(lastSSID)) {
					if (GAEProxyService.isServiceStarted()) {
						context.stopService(new Intent(context,
								GAEProxyService.class));
						
						Toast.makeText(context,
								context.getString(R.string.toast_start),
								Toast.LENGTH_LONG).show();
						
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ignore) {
							// Nothing
						}

						Editor ed = settings.edit();
						ed.putString("lastSSID", currentSSID);
						ed.commit();

						startService(context, settings);

					}

				}
			}

		}
	}

	private void startService(Context context, SharedPreferences settings) {

		String proxy;
		String proxyType;
		int port;
		boolean isInstalled = false;
		String sitekey;
		boolean isGlobalProxy;
		boolean isHTTPSProxy;
		boolean isGFWList = false;

		isInstalled = settings.getBoolean("isInstalled", false);

		if (isInstalled) {
			proxy = settings.getString("proxy", "");
			proxyType = settings.getString("proxyType", "GAppProxy");
			String portText = settings.getString("port", "");
			if (portText != null && portText.length() > 0) {
				try {
					port = Integer.valueOf(portText);
				} catch (NumberFormatException e) {
					port = 1984;
				}
				if (port <= 1024)
					port = 1984;
			} else {
				port = 1984;
			}
			sitekey = settings.getString("sitekey", "");
			isGlobalProxy = settings.getBoolean("isGlobalProxy", false);
			isHTTPSProxy = settings.getBoolean("isHTTPSProxy", false);
			isGFWList = settings.getBoolean("isGFWList", false);

			Intent it = new Intent(context, GAEProxyService.class);
			Bundle bundle = new Bundle();
			bundle.putString("proxy", proxy);
			bundle.putString("proxyType", proxyType);
			bundle.putInt("port", port);
			bundle.putString("sitekey", sitekey);
			bundle.putBoolean("isGlobalProxy", isGlobalProxy);
			bundle.putBoolean("isHTTPSProxy", isHTTPSProxy);
			bundle.putBoolean("isGFWList", isGFWList);

			it.putExtras(bundle);
			context.startService(it);
		}
	}
}