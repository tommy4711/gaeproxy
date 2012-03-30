package org.gaeproxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.gaeproxy.db.DNSResponse;
import org.gaeproxy.db.DatabaseHelper;

import android.content.Context;
import android.util.Log;

import com.github.droidfu.http.BetterHttp;
import com.github.droidfu.http.BetterHttpRequest;
import com.github.droidfu.http.BetterHttpResponse;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

/**
 * 此类实现了DNS代理
 * 
 * @author biaji
 * 
 */
public class DNSServer implements WrapServer {

	public static byte[] int2byte(int res) {
		byte[] targets = new byte[4];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
		return targets;
	}

	private final String TAG = "GAEProxyDNSProxy";

	private DatagramSocket srvSocket;

	private volatile int threadNum = 0;
	private final static int MAX_THREAD_NUM = 5;
	public HashSet<String> domains;

	private int srvPort = 8153;
	protected String dnsHost;
	protected int dnsPort;
	final protected int DNS_PKG_HEADER_LEN = 12;
	final private int[] DNS_HEADERS = { 0, 0, 0x81, 0x80, 0, 0, 0, 0, 0, 0, 0, 0 };
	final private int[] DNS_PAYLOAD = { 0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x3c,
			0x00, 0x04 };

	final private int IP_SECTION_LEN = 4;
	final private int DNS_ERROR_LIMIT = 20;

	private boolean inService = false;
	private boolean httpMode = false;
	private volatile int dnsError = 0;

	/**
	 * 内建自定义缓存
	 * 
	 */
	private Hashtable<String, String> orgCache = new Hashtable<String, String>();

	private String target = "8.8.8.8:53";

	private String appHost = "203.208.46.1";

	private static final String CANT_RESOLVE = "Error";

	private DatabaseHelper helper;

	public DNSServer(Context ctx, String dnsHost, int dnsPort, String appHost, boolean httpMode) {

		this.dnsHost = dnsHost;
		this.dnsPort = dnsPort;
		this.httpMode = httpMode;
		this.appHost = appHost;

		BetterHttp.setupHttpClient();
		BetterHttp.setSocketTimeout(10 * 1000);

		domains = new HashSet<String>();

		initOrgCache();

		OpenHelperManager.setOpenHelperClass(DatabaseHelper.class);

		if (helper == null) {
			helper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
		}

		if (dnsHost != null && !dnsHost.equals(""))
			target = dnsHost + ":" + dnsPort;

		try {
			srvSocket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
			srvPort = srvSocket.getLocalPort();
			Log.d(TAG, "start at port " + srvPort);

			inService = true;

		} catch (SocketException e) {
			Log.e(TAG, "error to initilized at port " + srvPort, e);
		} catch (UnknownHostException e) {
			Log.e(TAG, "error to initilized at port " + srvPort, e);
		}
	}

	/**
	 * 在缓存中添加一个域名解析
	 * 
	 * @param questDomainName
	 *            域名
	 * @param answer
	 *            解析结果
	 */
	private synchronized void addToCache(String questDomainName, byte[] answer) {
		DNSResponse response = new DNSResponse(questDomainName);
		response.setAddress(DNSResponse.getIPString(answer));
		try {
			Dao<DNSResponse, String> dnsCacheDao = helper.getDNSCacheDao();
			dnsCacheDao.createOrUpdate(response);
		} catch (Exception e) {
			Log.e(TAG, "Cannot open DAO", e);
		}
	}

	@Override
	public void close() throws IOException {
		inService = false;
		srvSocket.close();
		if (helper != null) {
			OpenHelperManager.releaseHelper();
			helper = null;
		}
		Log.i(TAG, "DNS Proxy closed");
	}

	/*
	 * Create a DNS response packet, which will send back to application.
	 * 
	 * @author yanghong
	 * 
	 * Reference to:
	 * 
	 * Mini Fake DNS server (Python)
	 * http://code.activestate.com/recipes/491264-mini-fake-dns-server/
	 * 
	 * DOMAIN NAMES - IMPLEMENTATION AND SPECIFICATION
	 * http://www.ietf.org/rfc/rfc1035.txt
	 */
	protected byte[] createDNSResponse(byte[] quest, byte[] ips) {
		byte[] response = null;
		int start = 0;

		response = new byte[128];

		for (int val : DNS_HEADERS) {
			response[start] = (byte) val;
			start++;
		}

		System.arraycopy(quest, 0, response, 0, 2); /* 0:2 | NAME */
		System.arraycopy(quest, 4, response, 4, 2); /* 4:6 -> 4:6 | TYPE */
		System.arraycopy(quest, 4, response, 6, 2); /* 4:6 -> 7:9 | CLASS */
		/* 10:14 | TTL */
		System.arraycopy(quest, DNS_PKG_HEADER_LEN, response, start, quest.length
				- DNS_PKG_HEADER_LEN); /* 12:~ -> 15:~ */
		start += quest.length - DNS_PKG_HEADER_LEN;

		for (int val : DNS_PAYLOAD) {
			response[start] = (byte) val;
			start++;
		}

		/* IP address in response */
		for (byte ip : ips) {
			response[start] = ip;
			start++;
		}

		byte[] result = new byte[start];
		System.arraycopy(response, 0, result, 0, start);
		Log.d(TAG, "DNS Response package size: " + start);

		return result;
	}

	/**
	 * 由上级DNS通过TCP取得解析
	 * 
	 * @param quest
	 *            原始DNS请求
	 * @return
	 */
	protected byte[] fetchAnswer(byte[] quest) {

		Socket innerSocket = new InnerSocketBuilder(dnsHost, dnsPort, target).getSocket();
		DataInputStream in;
		DataOutputStream out;
		byte[] result = null;
		try {
			if (innerSocket != null && innerSocket.isConnected()) {
				// 构造TCP DNS包
				int dnsqLength = quest.length;
				byte[] tcpdnsq = new byte[dnsqLength + 2];
				System.arraycopy(int2byte(dnsqLength), 0, tcpdnsq, 1, 1);
				System.arraycopy(quest, 0, tcpdnsq, 2, dnsqLength);

				// 转发DNS
				in = new DataInputStream(innerSocket.getInputStream());
				out = new DataOutputStream(innerSocket.getOutputStream());
				out.write(tcpdnsq);
				out.flush();

				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				int b = -1;
				while ((b = in.read()) != -1) {
					bout.write(b);
				}

				byte[] tcpdnsr = bout.toByteArray();
				if (tcpdnsr != null && tcpdnsr.length > 2) {
					result = new byte[tcpdnsr.length - 2];
					System.arraycopy(tcpdnsr, 2, result, 0, tcpdnsr.length - 2);
				}
				innerSocket.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return result;
	}

	public byte[] fetchAnswerHTTP(byte[] quest) {
		byte[] result = null;
		String domain = getRequestDomain(quest);
		String ip = null;

		DomainValidator dv = DomainValidator.getInstance();
		/* Not support reverse domain name query */
		if (domain.endsWith("ip6.arpa") || domain.endsWith("in-addr.arpa") || !dv.isValid(domain)) {
			return createDNSResponse(quest, parseIPString("127.0.0.1"));
		}

		ip = resolveDomainName(domain);

		if (ip == null) {
			Log.e(TAG, "Failed to resolve domain name: " + domain);
			return null;
		}

		if (ip.equals(CANT_RESOLVE)) {
			return null;
		}

		byte[] ips = parseIPString(ip);
		if (ips != null) {
			result = createDNSResponse(quest, ips);
		}

		return result;
	}

	/**
	 * 获取UDP DNS请求的域名
	 * 
	 * @param request
	 *            dns udp包
	 * @return 请求的域名
	 */
	protected String getRequestDomain(byte[] request) {
		String requestDomain = "";
		int reqLength = request.length;
		if (reqLength > 13) { // 包含包体
			byte[] question = new byte[reqLength - 12];
			System.arraycopy(request, 12, question, 0, reqLength - 12);
			requestDomain = parseDomain(question);
			if (requestDomain.length() > 1)
				requestDomain = requestDomain.substring(0, requestDomain.length() - 1);
		}
		return requestDomain;
	}

	@Override
	public int getServPort() {
		return this.srvPort;
	}

	private void initOrgCache() {
		InputStream is = null;
		try {
			File f = new File("/data/data/org.gaeproxy/hosts");
			if (!f.exists()) {
				URL aURL = new URL("http://myhosts.sinaapp.com/hosts");
				HttpURLConnection conn = (HttpURLConnection) aURL.openConnection();
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(10000);
				conn.connect();
				is = conn.getInputStream();
			} else {
				is = new FileInputStream(f);
			}
			loadOrgCache(is);
			is.close();
		} catch (Exception e) {
			Log.e(TAG, "cannot get remote host files", e);
		}

	}

	@Override
	public boolean isClosed() {
		return srvSocket.isClosed();
	}

	public boolean isInService() {
		return inService;
	}

	/**
	 * 由缓存载入域名解析缓存
	 */
	private void loadCache() {
		try {
			Dao<DNSResponse, String> dnsCacheDao = helper.getDNSCacheDao();
			List<DNSResponse> list = dnsCacheDao.queryForAll();
			for (DNSResponse resp : list) {
				// expire after 10 days
				if ((System.currentTimeMillis() - resp.getTimestamp()) > 864000000L) {
					Log.d(TAG, "deleted: " + resp.getRequest());
					dnsCacheDao.delete(resp);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Cannot open DAO", e);
		}
	}

	private void loadOrgCache(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = reader.readLine();
		if (line == null)
			return;
		if (!line.startsWith("#SSHTunnel"))
			return;
		while (true) {
			line = reader.readLine();
			if (line == null)
				break;
			if (line.startsWith("#"))
				continue;
			line = line.trim().toLowerCase();
			if (line.equals(""))
				continue;
			String[] hosts = line.split(" ");
			if (hosts.length == 2) {
				orgCache.put(hosts[1], hosts[0]);
			}
		}
		Log.d(TAG, "Load hosts: " + orgCache.size());
	}

	/**
	 * 解析域名
	 * 
	 * @param request
	 * @return
	 */
	private String parseDomain(byte[] request) {

		String result = "";
		int length = request.length;
		int partLength = request[0];
		if (partLength == 0)
			return result;
		try {
			byte[] left = new byte[length - partLength - 1];
			System.arraycopy(request, partLength + 1, left, 0, length - partLength - 1);
			result = new String(request, 1, partLength) + ".";
			result += parseDomain(left);
		} catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		return result;
	}

	/*
	 * Parse IP string into byte, do validation.
	 * 
	 * @param ip IP string
	 * 
	 * @return IP in byte array
	 */
	protected byte[] parseIPString(String ip) {
		byte[] result = null;
		int value;
		int i = 0;
		String[] ips = null;

		ips = ip.split("\\.");

		Log.d(TAG, "Start parse ip string: " + ip + ", Sectons: " + ips.length);

		if (ips.length != IP_SECTION_LEN) {
			Log.e(TAG, "Malformed IP string number of sections is: " + ips.length);
			return null;
		}

		result = new byte[IP_SECTION_LEN];

		for (String section : ips) {
			try {
				value = Integer.parseInt(section);

				/* 0.*.*.* and *.*.*.0 is invalid */
				if ((i == 0 || i == 3) && value == 0) {
					return null;
				}

				result[i] = (byte) value;
				i++;
			} catch (NumberFormatException e) {
				Log.e(TAG, "Malformed IP string section: " + section);
				return null;
			}
		}

		return result;
	}

	private synchronized DNSResponse queryFromCache(String questDomainName) {
		try {
			Dao<DNSResponse, String> dnsCacheDao = helper.getDNSCacheDao();
			return dnsCacheDao.queryForId(questDomainName);
		} catch (Exception e) {
			Log.e(TAG, "Cannot open DAO", e);
		}
		return null;
	}

	/*
	 * Resolve host name by access a DNSRelay running on GAE:
	 * 
	 * Example:
	 * 
	 * http://www.hosts.dotcloud.com/lookup.php?(domain name encoded)
	 * http://gaednsproxy.appspot.com/?d=(domain name encoded)
	 */
	private String resolveDomainName(String domain) {
		String ip = null;

		InputStream is;

		String encode_host = URLEncoder.encode(Base64.encodeBytes(Base64.encodeBytesToBytes(domain
				.getBytes())));

		String url = "http://gaednsproxy1.appspot.com/?d=" + encode_host;
		String host = "gaednsproxy1.appspot.com";
		url = url.replace(host, appHost);

		Random random = new Random(System.currentTimeMillis());
		int n = random.nextInt(2);
		if (n == 0) {
			url = "http://gaednsproxy2.appspot.com/?d=" + encode_host;
			host = "gaednsproxy2.appspot.com";
			url = url.replace(host, appHost);
		} else if (n == 1) {
			url = "http://gaednsproxy3.appspot.com/?d=" + encode_host;
			host = "gaednsproxy3.appspot.com";
			url = url.replace(host, appHost);
		}

		Log.d(TAG, "DNS Relay URL: " + url);

		// RFC 2616: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

		BetterHttpRequest conn = BetterHttp.get(url, host);

		try {
			BetterHttpResponse resp = conn.send();
			is = resp.getResponseBody();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			ip = br.readLine();
		} catch (ConnectException e) {
			Log.e(TAG, "Failed to request URI: " + url, e);
		} catch (IOException e) {
			Log.e(TAG, "Failed to request URI: " + url, e);
		}

		return ip;
	}

	@Override
	public void run() {

		loadCache();

		byte[] qbuffer = new byte[576];
		long starTime = System.currentTimeMillis();

		while (true) {
			try {
				final DatagramPacket dnsq = new DatagramPacket(qbuffer, qbuffer.length);

				srvSocket.receive(dnsq);

				// try to build dnsreq here
				byte[] data = dnsq.getData();
				int dnsqLength = dnsq.getLength();
				final byte[] udpreq = new byte[dnsqLength];
				System.arraycopy(data, 0, udpreq, 0, dnsqLength);

				// begin to query from dns cache
				final String questDomain = getRequestDomain(udpreq);
				Log.d(TAG, "Resolving: " + questDomain);
				DNSResponse resp = queryFromCache(questDomain);
				if (resp != null) {
					sendDns(createDNSResponse(udpreq, parseIPString(resp.getAddress())), dnsq,
							srvSocket);
					Log.d(TAG, "DNS cache hit");
				} else if (orgCache.containsKey(questDomain)) { // 如果为自定义域名解析
					byte[] ips = parseIPString(orgCache.get(questDomain));
					byte[] answer = createDNSResponse(udpreq, ips);
					addToCache(questDomain, answer);
					sendDns(answer, dnsq, srvSocket);
					Log.d(TAG, "Custom DNS resolver");
				} else if (questDomain.toLowerCase().contains("appspot.com")) { // 如果为apphost域名解析
					byte[] ips = parseIPString(appHost);
					byte[] answer = createDNSResponse(udpreq, ips);
					addToCache(questDomain, answer);
					sendDns(answer, dnsq, srvSocket);
					Log.d(TAG, "Custom DNS resolver: " + questDomain);
				} else {

					synchronized (this) {
						if (domains.contains(questDomain))
							continue;
						else
							domains.add(questDomain);
					}

					if (threadNum >= MAX_THREAD_NUM) {
						continue;
					}

					if (dnsError > DNS_ERROR_LIMIT)
						httpMode = false;
					else
						httpMode = true;

					threadNum++;

					new Thread() {
						@Override
						public void run() {
							long startTime = System.currentTimeMillis();
							try {
								byte[] answer;
								if (httpMode)
									answer = fetchAnswerHTTP(udpreq);
								else
									answer = fetchAnswer(udpreq);

								if (answer != null && answer.length != 0) {
									addToCache(questDomain, answer);
									sendDns(answer, dnsq, srvSocket);
									Log.d(TAG,
											"Success to get DNS response，length:" + answer.length
													+ "  cost："
													+ (System.currentTimeMillis() - startTime)
													/ 1000 + "s");
								} else {
									Log.e(TAG, "The size of DNS packet returned is 0");
									if (httpMode)
										dnsError++;
								}
							} catch (Exception e) {
								// Nothing
								if (httpMode)
									dnsError++;
							}
							synchronized (DNSServer.this) {
								domains.remove(questDomain);
							}
							threadNum--;
						}
					}.start();

				}

			} catch (SocketException e) {
				Log.e(TAG, e.getLocalizedMessage());
				break;
			} catch (NullPointerException e) {
				Log.e(TAG, "Srvsocket wrong", e);
				break;
			} catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage());
			}
		}

	}

	/**
	 * send response to the source
	 * 
	 * @param response
	 *            response
	 * @param dnsq
	 *            request
	 * @param srvSocket
	 *            local socket
	 */
	private void sendDns(byte[] response, DatagramPacket dnsq, DatagramSocket srvSocket) {

		// 同步identifier
		System.arraycopy(dnsq.getData(), 0, response, 0, 2);

		DatagramPacket resp = new DatagramPacket(response, 0, response.length);
		resp.setPort(dnsq.getPort());
		resp.setAddress(dnsq.getAddress());
		try {
			srvSocket.send(resp);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
	}

	@Override
	public void setProxyHost(String host) {
		// TODO Auto-generated method stub

	}

	public void setTarget(String target) {
		this.target = target;
	}

}
