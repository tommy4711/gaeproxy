/*
 * Zirco Browser for Android
 * 
 * Copyright (C) 2010 J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.zirco.ui.components;

import org.zirco.controllers.Controller;
import org.zirco.ui.activities.ZircoMain;
import org.zirco.utils.ApplicationUtils;
import org.zirco.utils.Constants;
import org.zirco.utils.UrlUtils;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.HitTestResult;

/**
 * Convenient extension of WebViewClient.
 */
public class ZircoWebViewClient extends WebViewClient {

	private ZircoMain mMainActivity;

	public ZircoWebViewClient(ZircoMain mainActivity) {
		super();
		mMainActivity = mainActivity;
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		((ZircoWebView) view).notifyPageFinished();
		mMainActivity.onPageFinished(url);

		super.onPageFinished(view, url);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {

		// Some magic here: when performing WebView.loadDataWithBaseURL, the url
		// is "file:///android_asset/startpage,
		// whereas when the doing a "previous" or "next", the url is
		// "about:start", and we need to perform the
		// loadDataWithBaseURL here, otherwise it won't load.
		if (url.equals(Constants.URL_ABOUT_START)) {
			view.loadDataWithBaseURL("file:///android_asset/startpage/",
					ApplicationUtils.getStartPage(view.getContext()),
					"text/html", "UTF-8", "about:start");
		}

		((ZircoWebView) view).notifyPageStarted();
		mMainActivity.onPageStarted(url);

		super.onPageStarted(view, url, favicon);
	}

	@Override
	public void onReceivedSslError(WebView view, final SslErrorHandler handler,
			SslError error) {

		// ignore all SSL errors
		handler.proceed();

		// StringBuilder sb = new StringBuilder();
		//
		// sb.append(view.getResources().getString(R.string.Commons_SslWarningsHeader));
		// sb.append("\n\n");
		//
		// if (error.hasError(SslError.SSL_UNTRUSTED)) {
		// sb.append(" - ");
		// sb.append(view.getResources().getString(R.string.Commons_SslUntrusted));
		// sb.append("\n");
		// }
		//
		// if (error.hasError(SslError.SSL_IDMISMATCH)) {
		// sb.append(" - ");
		// sb.append(view.getResources().getString(R.string.Commons_SslIDMismatch));
		// sb.append("\n");
		// }
		//
		// if (error.hasError(SslError.SSL_EXPIRED)) {
		// sb.append(" - ");
		// sb.append(view.getResources().getString(R.string.Commons_SslExpired));
		// sb.append("\n");
		// }
		//
		// if (error.hasError(SslError.SSL_NOTYETVALID)) {
		// sb.append(" - ");
		// sb.append(view.getResources().getString(R.string.Commons_SslNotYetValid));
		// sb.append("\n");
		// }
		//
		// ApplicationUtils.showContinueCancelDialog(view.getContext(),
		// android.R.drawable.ic_dialog_info,
		// view.getResources().getString(R.string.Commons_SslWarning),
		// sb.toString(),
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// handler.proceed();
		// }
		//
		// },
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// handler.cancel();
		// }
		// });
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {

		Log.d("URL", url);
		
		// Mobile youTube video link
		if (url.contains("m.youtube.com")) {
			String playVideo = "www.youtube.com/?nomobile=1";
			view.loadUrl(playVideo);
			return true;
			
		// YouTube video link
		} else if (url.startsWith("vnd.youtube:")) {
			int n = url.indexOf("?");
			if (n > 0) {
				String playVideo = String
						.format("<html><body>Youtube video .. <br> <iframe class=\"youtube-player\" type=\"text/html\" width=\"640\" height=\"385\" src=\"http://www.youtube.com/embed/%s\" frameborder=\"0\"></body></html>",
								url.substring("vnd.youtube:".length(), n));
				view.loadData(playVideo, "text/html", "utf-8");
			}
			return true;

		} else if (url.startsWith("vnd.")) {
			mMainActivity.onVndUrl(url);
			return true;

		} else if (url.startsWith(Constants.URL_ACTION_SEARCH)) {
			String searchTerm = url.replace(Constants.URL_ACTION_SEARCH, "");

			String searchUrl = Controller
					.getInstance()
					.getPreferences()
					.getString(Constants.PREFERENCES_GENERAL_SEARCH_URL,
							Constants.URL_SEARCH_GOOGLE);
			String newUrl = String.format(searchUrl, searchTerm);

			view.loadUrl(newUrl);
			return true;

		} else if (view.getHitTestResult().getType() == HitTestResult.EMAIL_TYPE) {
			mMainActivity.onMailTo(url);
			return true;

		} else {

			// If the url is not from GWT mobile view, and is in the mobile view
			// url list, then load it with GWT.
			if ((!url.startsWith(Constants.URL_GOOGLE_MOBILE_VIEW_NO_FORMAT))
					&& (UrlUtils.checkInMobileViewUrlList(view.getContext(),
							url))) {

				String newUrl = String.format(Constants.URL_GOOGLE_MOBILE_VIEW,
						url);
				view.loadUrl(newUrl);
				return true;

			} else {
				((ZircoWebView) view).resetLoadedUrl();
				mMainActivity.onUrlLoading(url);
				return false;
			}
		}
	}
}
