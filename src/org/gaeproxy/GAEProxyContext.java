package org.gaeproxy;

import android.app.Application;
import android.content.Context;

public class GAEProxyContext extends Application{
    private static Context context;

    public void onCreate(){
    	GAEProxyContext.context=getApplicationContext();
    }

    public static Context getContext() {
    	return context;
    }
}
