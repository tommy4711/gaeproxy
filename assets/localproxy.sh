#!/system/bin/sh

export PYTHONPATH=/data/data/org.gaeproxy/python:/data/data/org.gaeproxy/python/lib/python2.6:/data/data/org.gaeproxy/python/lib/python2.6/lib-dynload:/data/data/org.gaeproxy/python/lib:/sdcard/python-extras
export LD_LIBRARY_PATH=/data/data/org.gaeproxy/python/lib
export PYTHONHOME=$PYTHONHOME:/data/data/org.gaeproxy/python
export TEMP=/sdcard/python-extras

echo "" > /data/data/org.gaeproxy/python.pid
chmod 777 /data/data/org.gaeproxy/python.pid

case $1 in

 goagent)
 
 echo " 

[listen]
ip = 127.0.0.1
port = $3
visible = 1

[gae]
appid = $2
password = $6
path = /$5
debuglevel = 0

[proxy]
enable = 0
host = 10.0.0.172
port = 80
username = 
password = 

[google]
prefer = http
autoswitch = 0
sites = .google.com|.googleusercontent.com|.googleapis.com|.google-analytics.com|.googlecode.com|.google.com.hk|.appspot.com|.android.com
forcehttps = http://groups.google.com|http://code.google.com|http://mail.google.com|http://plus.google.com|http://docs.google.com|http://profiles.google.com|http://www.google.com/reader|http://developer.android.com
http = $4|203.208.46.2|203.208.46.3|203.208.46.4|203.208.46.5|203.208.46.6|203.208.46.7|203.208.46.8||203.208.37.22|203.208.37.184|203.208.39.22|203.208.39.184||203.208.46.174|203.208.46.175|203.208.46.176|203.208.46.177|203.208.46.178|203.208.46.179
https = $4|203.208.46.2|203.208.46.3|203.208.46.4|203.208.46.5|203.208.46.6|203.208.46.7|203.208.46.8||74.125.71.17|74.125.71.18|74.125.71.19|74.125.71.32|74.125.71.33|74.125.71.34|74.125.71.35|74.125.71.36|74.125.71.37|74.125.71.38|74.125.71.39|74.125.71.40|74.125.71.41|74.125.71.42|74.125.71.43|74.125.71.44|74.125.71.45|74.125.71.46|74.125.71.47|74.125.71.48|74.125.71.49|74.125.71.50|74.125.71.51|74.125.71.52|74.125.71.53|74.125.71.54|74.125.71.56|74.125.71.57|74.125.71.58|74.125.71.59|74.125.71.60|74.125.71.61|74.125.71.62|74.125.71.63|74.125.71.64|74.125.71.65|74.125.71.66|74.125.71.68|74.125.71.69|74.125.71.72|74.125.71.73|74.125.71.74|74.125.71.75|74.125.71.76|74.125.71.77|74.125.71.78|74.125.71.79|74.125.71.81|74.125.71.82|74.125.71.83|74.125.71.84|74.125.71.85|74.125.71.86|74.125.71.87|74.125.71.91|74.125.71.93|74.125.71.95|74.125.71.96|74.125.71.98|74.125.71.99|74.125.71.100|74.125.71.101|74.125.71.102|74.125.71.103|74.125.71.104|74.125.71.105|74.125.71.106|74.125.71.112|74.125.71.113|74.125.71.115|74.125.71.116|74.125.71.117|74.125.71.118|74.125.71.120|74.125.71.123|74.125.71.125|74.125.71.136|74.125.71.137|74.125.71.138|74.125.71.139|74.125.71.141|74.125.71.142|74.125.71.143|74.125.71.144|74.125.71.145|74.125.71.146|74.125.71.147|74.125.71.148|74.125.71.149|74.125.71.152|74.125.71.154|74.125.71.155|74.125.71.156|74.125.71.157|74.125.71.160|74.125.71.161|74.125.71.162|74.125.71.163|74.125.71.164|74.125.71.165|74.125.71.166|74.125.71.167|74.125.71.176|74.125.71.178|74.125.71.184|74.125.71.189|74.125.71.190|74.125.71.191|74.125.71.193|74.125.71.210|74.125.71.211

[fetchmax]
local =
server =

[autorange]
hosts = .youtube.com|video.*.fbcdn.net|av.vimeo.com
endswith = .jpg|.jpeg|.png|.bmp|.gif

[hosts]
__merge__ = 0
www.253874.com = 76.73.90.170


"> /data/data/org.gaeproxy/proxy.ini
 
 
/data/data/org.gaeproxy/python/bin/python /data/data/org.gaeproxy/goagent.py

;;

 gappproxy)
 
/data/data/org.gaeproxy/python/bin/python /data/data/org.gaeproxy/gappproxy.py

;;

 wallproxy)
 
 echo "
server['listen'] = ('127.0.0.1', $3)
server['log_file'] = None 

hosts = '''
$4  .appspot.com
$4 www.youtube.com
'''

plugins['plugins.hosts'] = 'hosts'

gaeproxy = [{
    'url': '$2',
    'key': '$5',
    'crypto':'XOR--0',
    'max_threads':5
}]

plugins['plugins.gaeproxy'] = 'gaeproxy'

def find_http_handler(method, url, headers):
    if method not in ('GET', 'HEAD', 'PUT', 'POST', 'DELETE'):
        return rawproxy[0]
    if 80<=url.port<=90 or 440<=url.port<=450 or url.port>=1024:
        return gaeproxy
    return None

fakehttps = None
plugins['plugins.fakehttps'] = 'fakehttps'

def find_sock_handler(reqtype, ip, port, cmd):
    if reqtype == 'https': return fakehttps
    return None

def check_client(ip, reqtype, args):
    return True
 " > /data/data/org.gaeproxy/proxy.conf
 /data/data/org.gaeproxy/python/bin/python /data/data/org.gaeproxy/wallproxy.py
 
 ;;
 
 esac