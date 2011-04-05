package org.gaeproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class GAEProxyReceiver extends BroadcastReceiver {

	private String proxy;
	private String proxyType;
	private int port;
	private boolean isAutoStart = false;
	private boolean isInstalled = false;
	private String sitekey;
	private boolean isGlobalProxy;
	private boolean isHTTPSProxy;

	@Override
	public void onReceive(Context context, Intent intent) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		isAutoStart = settings.getBoolean("isAutoStart", false);
		isInstalled = settings.getBoolean("isInstalled", false);

		if (isAutoStart && isInstalled) {
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
