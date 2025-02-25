#!/usr/bin/python

"""
Packages the profiles into .tar files, to be deployed in the
runtime NMS environment.
"""
import sys, os, ipaddress, subprocess

class getPingResponse():
    response = '';
    ip = sys.argv[1]
    #ip = '11.0.0.1'
    #r = subprocess.check_output(["ping",ip,"-c","5"],stderr=subprocess.STDOUT,universal_newlines=True)
    #process = subprocess.Popen(['ping', '-c 2', ip], stdout=subprocess.PIPE, stdin=subprocess.PIPE)
    nagios_ping = '/usr/lib/nagios/plugins/check_ping'

    if not os.path.exists(nagios_ping):
        nagios_ping = '/usr/lib64/nagios/plugins/check_ping'

    if not os.path.exists(nagios_ping):
        print "check_ping plugin not found"
        exit(1)

    process = subprocess.Popen([nagios_ping,'-H',ip,'-w 400,50%','-c 500,70%'], stdout=subprocess.PIPE, stdin=subprocess.PIPE)

    for line in iter(process.stdout.readline,''):
        response += line

    print response



