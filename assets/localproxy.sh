#!/system/bin/sh

export PYTHONPATH=/data/data/org.gaeproxy/python:/data/data/org.gaeproxy/python/lib/python2.6:/data/data/org.gaeproxy/python/lib/python2.6/lib-dynload:/data/data/org.gaeproxy/python/lib:$1/python-extras
export LD_LIBRARY_PATH=/data/data/org.gaeproxy/python/lib
export PYTHONHOME=$PYTHONHOME:/data/data/org.gaeproxy/python
export TEMP=$1/python-extras

chmod 755 /data/data/org.gaeproxy/python.pid
kill -9 `cat /data/data/org.gaeproxy/python.pid`
rm /data/data/org.gaeproxy/python.pid
killall -9 python

case $2 in

 goagent)
 
 echo " 

[listen]
ip = 127.0.0.1
port = $4
visible = 1

[gae]
enable = 1
appid = $3
password = $7
path = /$6
debuglevel = 0

[php]
enable = 0
ip = 127.0.0.1
port = 8088
fetchserver = http://scan.org/fetch.php

[proxy]
enable = 0
host = 10.64.1.63
port = 8080
username = domain\username
password = 123456

[appspot]
mode = http
autoswitch = 0
hosts = cn
cn = $5|203.208.46.1|203.208.46.2|203.208.46.3|203.208.46.4|203.208.46.5|203.208.46.6|203.208.46.7|203.208.46.8
hk = 74.125.71.103|74.125.71.104|74.125.71.105|74.125.71.106|74.125.71.147|74.125.71.17|74.125.71.18|74.125.71.19|74.125.71.83|74.125.71.99
ipv6 = 2404:6800:8005::6a|2404:6800:8005::62|2404:6800:8005::2c

[google]
sites = .google.com|.googleusercontent.com|.google-analytics.com|.googlecode.com|.google.com.hk|.appspot.com|.android.com
forcehttps = groups.google.com|code.google.com|mail.google.com|docs.google.com|profiles.google.com|developer.android.com
withgae = plus.google.com|reader.googleusercontent.com|music.google.com
hosts = $4|203.208.46.1|203.208.46.2|203.208.46.3|203.208.46.4|203.208.46.5|203.208.46.6|203.208.46.7|203.208.46.8

[fetchmax]
local =
server =

[autorange]
hosts = .youtube.com|.googlevideo.com|video.*.fbcdn.net|av.vimeo.com
endswith = .jpg|.jpeg|.png|.bmp|.gif

[hosts]
www.253874.com = 76.73.90.170


"> /data/data/org.gaeproxy/proxy.ini
 
 
/data/data/org.gaeproxy/python/bin/python /data/data/org.gaeproxy/goagent.py

;;

 gappproxy)
 
/data/data/org.gaeproxy/python/bin/python /data/data/org.gaeproxy/gappproxy.py

;;

 wallproxy)
 
 echo "
server['listen'] = ('127.0.0.1', $4)
server['log_file'] = None 

hosts = '''
$5  .appspot.com
$5 www.youtube.com
'''

plugins['plugins.hosts'] = 'hosts'

gaeproxy = [{
    'url': '$3',
    'key': '$6',
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