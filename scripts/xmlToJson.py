#!/usr/bin/python

import sys
import json
import collections
import xml.etree.ElementTree as ET

def convertDiscoveryTree():
	print "Converting discovery tree from: {0}".format(path)
	tree = ET.parse(path)
	root = tree.getroot()

	discTree = {}
	discTree = collections.OrderedDict(discTree)

	for ruleList in root:
		deviceId = ruleList.attrib['deviceid']
		deviceModel = ruleList.attrib['comment']
		discTree[deviceId] = collections.OrderedDict({})
		discTree[deviceId]['deviceId'] = deviceId
		discTree[deviceId]['deviceModel'] = deviceModel
		discTree[deviceId]['ruleList'] = []
		for rule in ruleList:
			opType = rule.attrib['object']
			oid = ""
			regexp = ""
			startingOid = ""
			for prop in rule:
				if prop.attrib['name'] == "oid":
					oid = prop.attrib['value']
				elif prop.attrib['name'] == "startingOid":
					startingOid = prop.attrib['value']
				elif prop.attrib['name'] == "regexp":
					regexp = prop.attrib['value']

			ruleObj = collections.OrderedDict({})
			if "SnmpRuleOIDRegExp" in opType:
				ruleObj['opType'] = 'SNMP_GET'
				ruleObj['oid'] = oid
				ruleObj['regexp'] = regexp
			elif "SnmpRuleRespondsToOID" in opType:
				ruleObj['opType'] = 'SNMP_PING'
				ruleObj['startingOid'] = startingOid
			
			discTree[deviceId]['ruleList'].append(ruleObj)

	print json.dumps(discTree, indent=4)


def main(args):
	global operation, path
	operation = args[0]
	path = args[1]

	if operation == "discovery-tree":
		convertDiscoveryTree()

if __name__ == "__main__":
	if len(sys.argv) == 1:
		print "Usage: {0} <Operation> <Path>".format(sys.argv[0])
		exit(1)
	main(sys.argv[1:])