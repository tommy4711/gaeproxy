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
debug = INFO	

[hosts]
# NOTE: Only effect on https

[gae]
host = $2
password = $6
path = /$5
prefer = http
http_timeout = 3
http_step = 1
https_timeout = 3
https_step = 8
http = 203.208.39.104|203.208.39.99|203.208.39.22|203.208.39.132|203.208.37.104|203.208.37.99|203.208.37.22|203.208.37.132|203.208.46.99|203.208.46.22|203.208.46.132
https = 203.208.46.18|203.208.46.171|203.208.46.17|203.208.46.27|203.208.46.28|203.208.46.65|203.208.46.66|203.208.46.103|203.208.46.100|203.208.46.162|203.208.46.171|203.208.37.97|203.208.39.97|$4

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

gaeproxy = [{
    'url': '$2',
    'key': '$4',
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