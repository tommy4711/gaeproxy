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

import java.io.IOException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class GAEProxyReceiver extends BroadcastReceiver {

	private String proxy;
	private String proxyType;
	private int port;
	private boolean isAutoConnect = false;
	private boolean isInstalled = false;
	private String sitekey;
	private boolean isGlobalProxy;
	private boolean isHTTPSProxy;
	
	private static final String TAG = "GAEProxy";

	@Override
	public void onReceive(Context context, Intent intent) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		isAutoConnect = settings.getBoolean("isAutoConnect", false);
		isInstalled = settings.getBoolean("isInstalled", false);
		
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		String locationProvider = LocationManager.NETWORK_PROVIDER;
		// Or use LocationManager.GPS_PROVIDER

		Location lastKnownLocation = locationManager
				.getLastKnownLocation(locationProvider);
		Geocoder geoCoder = new Geocoder(context);

		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String countryCode = tm.getSimCountryIso();

		try {
			List<Address> addrs = geoCoder.getFromLocation(
					lastKnownLocation.getLatitude(),
					lastKnownLocation.getLongitude(), 1);
			if (addrs != null && addrs.size() > 0) {
				Address addr = addrs.get(0);
				Log.d(TAG, "Location: " + addr.getCountryName());
				if (addr.getCountryCode().toLowerCase().equals("cn")
						&& countryCode.toLowerCase().equals("cn")) {
					String command = "setprop gsm.sim.operator.numeric 31026\n"
							+ "setprop gsm.operator.numeric 31026\n"
							+ "setprop gsm.sim.operator.iso-country us\n"
							+ "setprop gsm.operator.iso-country us\n"
							+ "setprop gsm.operator.alpha T-Mobile\n"
							+ "setprop gsm.sim.operator.alpha T-Mobile\n"
							+ "kill $(ps | grep vending | tr -s  ' ' | cut -d ' ' -f2)\n"
							+ "rm -rf /data/data/com.android.vending/cache/*\n";
					GAEProxy.runRootCommand(command);
				}
			}
		} catch (IOException e) {
			// Nothing
		}

		if (isAutoConnect && isInstalled) {
			proxy = settings.getString("proxy", "");
			proxyType = settings.getString("proxyType", "GAppProxy");
			String portText = settings.getString("port", "");
			if (portText != null && portText.length() > 0) {
				port = Integer.valueOf(portText);
				if (port <= 1024)
					port = 1984;
			} else {
				port = 1984;
			}
			sitekey = settings.getString("sitekey", "");
			isGlobalProxy = settings.getBoolean("isGlobalProxy", false);
			isHTTPSProxy = settings.getBoolean("isHTTPSProxy", false);
			
			Intent it = new Intent(context, GAEProxyService.class);
			Bundle bundle = new Bundle();
			bundle.putString("proxy", proxy);
			bundle.putString("proxyType", proxyType);
			bundle.putInt("port", port);
			bundle.putString("sitekey", sitekey);
			bundle.putBoolean("isGlobalProxy", isGlobalProxy);
			bundle.putBoolean("isHTTPSProxy", isHTTPSProxy);

			it.putExtras(bundle);
			context.startService(it);
		}
	}

}
