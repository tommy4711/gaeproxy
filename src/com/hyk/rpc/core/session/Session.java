/**
 * 
 */
package com.hyk.rpc.core.session;

import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.hyk.proxy.framework.config.Config;




import android.util.Log;

import com.hyk.rpc.core.RequestListener;
import com.hyk.rpc.core.ResponseListener;
import com.hyk.rpc.core.Rpctimeout;
import com.hyk.rpc.core.message.Message;
import com.hyk.rpc.core.message.MessageFactory;
import com.hyk.rpc.core.message.Request;
import com.hyk.rpc.core.message.Response;
import com.hyk.rpc.core.remote.RemoteObjectFactory;
import com.hyk.rpc.core.transport.RpcChannel;
import com.hyk.timer.TimerTask;
import com.hyk.util.reflect.ClassUtil;

/**
 * @author Administrator
 * 
 */
public class Session
{
	private static final String TAG = "hyk-proxy";
	public static final int		CLIENT					= 0;
	public static final int		SERVER					= 1;

	private int					retransmitTimeStep		= 1000;
	private int					waitRetransmitTimeout	= retransmitTimeStep * 10;

	Message						request;
	Message						response;
	private RpcChannel			channel;
	private RemoteObjectFactory	remoteObjectFactory;
	private SessionManager		sessionManager;
	private TimerTask			retransmitTask;
	private TimerTask			sessionTimeoutTask;

	private ResponseListener	responseListener;
	private RequestListener		requestListener;

	private long				bornTime				= System.currentTimeMillis();

	public Session(SessionManager manager, Message request, RpcChannel channel, RemoteObjectFactory remoteObjectFactory)
	{
		this.sessionManager = manager;
		this.request = request;
		this.channel = channel;
		this.remoteObjectFactory = remoteObjectFactory;
		// request.setSessionID(id);
	}

	public long getBornTime()
	{
		return bornTime;
	}

	public void setResponseListener(ResponseListener responseListener)
	{
		this.responseListener = responseListener;
	}

	public void setRequestListener(RequestListener requestListener)
	{
		this.requestListener = requestListener;
	}

	public void sendRequest() throws NotSerializableException, IOException
	{
		// request.setSessionID(id);
		channel.sendMessage(request);
		if(!channel.isReliable() && null == retransmitTask)
		{
			Runnable task = new RetransmitTimerTask();
			retransmitTask = sessionManager.timer.schedule(task, retransmitTimeStep);
		}
		if(sessionManager.getSessionTimeout() > 0 && null == sessionTimeoutTask)
		{
			Runnable task = new SessionTimeoutTask();
			sessionTimeoutTask = sessionManager.timer.schedule(task, sessionManager.getSessionTimeout());
		}
	}

	public void sendResponse(Message response) throws NotSerializableException, IOException
	{
		if(Config.isDebug())
		{
			Log.d(TAG, "Send invocation result back!");
		}
		channel.sendMessage(response);
	}

	public void processResponse(Message res)
	{
		this.response = res;
		if(null != responseListener)
		{
			try
			{
				responseListener.processResponse(this, (Response)res.getValue());
			}
			catch(Exception e)
			{
				Log.e(TAG, "Failed to process response", e);
			}
		}
		close();

	}

	public void close()
	{
		if(null != retransmitTask)
		{
			retransmitTask.cancel();
			retransmitTask = null;
		}
		if(null != sessionTimeoutTask)
		{
			sessionTimeoutTask.cancel();
			sessionTimeoutTask = null;
		}
		channel.clearSessionData(request.getId());
		responseListener = null;
		requestListener = null;
		request = null;
		response = null;
	}

	public void processRequest()
	{
		if(response != null)
		{
			try
			{
				sendResponse(response);
			}
			catch(Exception e)
			{
				Log.e(TAG, "Failed to resend response", e);
			}
			return;
		}

		Request req = (Request)request.getValue();
		long objid = req.getObjID();
		Object[] paras = req.getArgs();
		try
		{
			Object target = remoteObjectFactory.getRawObject(objid);
			if(null == target)
			{
				Log.e(TAG, "Failed to get raw object with ID:" + objid + " messageID:" + request.getId());
			}
			Method method = ClassUtil.getMethod(target.getClass(), req.getOperation(), paras);
			if(Config.isDebug())
			{
				Log.d(TAG, "execute invocation:" + method.getName() + ", paras:" + Arrays.toString(paras));
			}
			Object result = null;
			try
			{
				result = method.invoke(target, paras);
			}
			catch(InvocationTargetException e)
			{
				e.getCause().getStackTrace();
				result = e.getCause();
			}
			catch(Throwable e)
			{
				e.getCause().getStackTrace();
				result = e;
			}

			if(Config.isDebug())
			{
				Log.d(TAG, "Invoked finish with normal result.");
			}
			response = MessageFactory.instance.createResponse(request, result);
			sendResponse(response);
			if(channel.isReliable())
			{
				sessionManager.removeServerSession(this);
			}
			else
			{
				sessionManager.timer.schedule(new CleanTimerTask(), waitRetransmitTimeout);
			}

		}
		catch(Exception e)
		{
			Log.e(TAG, "Failed to execute invocation", e);
		}
	}

	class CleanTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			sessionManager.removeServerSession(Session.this);
		}
	}

	class RetransmitTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				sendRequest();
			}
			catch(Throwable e)
			{
				Log.e(TAG, "Failed to retransmit request", e);
			}
		}
	}

	class SessionTimeoutTask implements Runnable
	{

		@Override
		public void run()
		{
			if(null == request || null == sessionManager)
			{
				return;
			}
			sessionManager.removeClientSession(request.getSessionID());
			Message resMsg = MessageFactory.instance.createResponse(request, new Rpctimeout(""));
			processResponse(resMsg);
		}

	}
}
