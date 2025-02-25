#! /usr/bin/python
import sys
import os
import shutil
import imp
import subprocess
from collections import OrderedDict
from pathlib2 import Path
import xmltodict, json

#returns new using attr key for profile from old xml attr key
def getAttrKey(oldKey):
    attrKeys = {'discovery':'discoverPhysicalLinks',
                'fault':'collectFault','trap-oid':'trapOid',
                'enterprise':'enterpriseOid','specific-trap':'genericTrap',
                'perceivedSeverity':'severity','action':'eventAction'}
    if oldKey in attrKeys.keys():
        return attrKeys[oldKey]
    else:
        return oldKey

def getDirName(oldDirName):
    dirNames = {'pm':'snmp/perf'}
    if oldDirName in dirNames.keys():
        return dirNames[oldDirName]
    return oldDirName

# pass xml path as arg which parses to json
def parseToJson(path):
    contents = Path(path).read_text()
    return xmltodict.parse(contents)

# split the path provided into path dir and file name
def splitPathAndFile(path):
    drive, path = os.path.splitdrive(path)
    return os.path.split(path)

def getAsList(obj):
    isList = isinstance(obj, list)
    objL = obj
    if isList == False:
        objL = [obj]
    return objL

def getAsObj(obj):
    print(obj)


def saveConfig(content, name):
    # print("Tempdir: {0}, name: {1}".format(tempDir, name))
    file = open(tempDir+'/'+name+'.conf','w')
    file.write(content)
    file.close()

# def getValue(json, val):
#     if val.startswith('#') and val.endswith('#'):
#         val = val.replace('#','')
#         print('vvvvvvvvvvvvvvvvvvvvvv',val.split(','))
#         if len(val.split(',')) > 1:
#             resJson = json;
#             for v in val.split(','):
#                 # print('resJson : ', resJson)
#                 resJson = resJson.get(v)
#             return resJson;
#         else:
#             print('dddddddddddddd',json, val)
#             return json.get(val[0]);
#     else:
#         return val;


# def doit(mapper, json, obj):
#     print('doing')
#
#     for key in mapper.keys():
#         val = mapper[key]
#         isList = isinstance(val, basestring) == False
#         # print(isList , ' val : ' , val)
#         if isList == False and key != 'AttrInfo':
#             obj[key] = getValue(json, val);
#         else:
#             # print('asdfffff')
#             # print(mapper)
#             attrInfo = val.get('AttrInfo');
#             print(attrInfo)
#             if attrInfo != None:
#                 print('get as list')
#                 list = [];
#                 for j in getAsList(json, attrInfo.get('node')):
#                     print(j)
#                     # print(val)
#                     # print('jjjj',j)
#                     # cObj = {};
#                     # doit(val, j, cObj)
#                     # list.append(cObj);
#                 obj[key] = doit(val, json, list)
#             # for j in getAsList(json.get(attrInfo['node'])):
#             #     print(j)
#             # list = doit(val, json, [])
#             # obj[key] = list;

def createPerfConfig(path):
    (profilePath, file) = splitPathAndFile(configPath)
    if path.startswith('if-mib') == True:
        executeShellCmd(['cp','../config/snmp/perf/if-mib.conf',tempDir+'/snmp/perf/'],True,True);
        return True
    elif path.find('cisco-envmon-mib') != -1:
        executeShellCmd(['cp','../config/snmp/perf/cisco-envmon-mib.conf',tempDir+'/snmp/perf/'],True,True);
        return True
    elif path.find('cisco-process-mib') != -1:
        executeShellCmd(['cp','../config/snmp/perf/cisco-process-mib.conf',tempDir+'/snmp/perf/'],True,True);
        executeShellCmd(['cp','../config/snmp/perf/cisco-memory-pool-mib.conf',tempDir+'/snmp/perf/'],True,True);
        return True

    childPath = profilePath + '/' + path;
    obj = parseToJson(childPath)
    perfConfig = OrderedDict({})
    pmProfile = obj.get('pmProfile');

    if pmProfile == None:
        terminate('Missing pmProfile node in xml : ' + path)
        return False

    perfConfig['id'] = pmProfile.get('@name')
    perfConfig['name'] = perfConfig['id']

    convFuncMap = {'PER_PERIOD_PER_SECOND':'per_poll_per_second',
                   'PER_PERIOD':'per_poll'}
    metricUnitMap = {'%':'percentage', 'degrees Celsius':'degrees',
                    'bps':'bps','bytes':'bytes'}

    if pmProfile.get('metricGroup') != None:
        metricFmlyL = []
        for metricGrp in getAsList(pmProfile.get('metricGroup')):
            metricFmly = OrderedDict({})
            metricFmly['id'] = metricGrp.get('@id')
            metricFmly['id'] = metricFmly['id'].replace('.','-')
            metricFmly['id'] = metricFmly['id'].replace(' ','-')
            metricFmly['name'] = metricGrp.get('@name')
            metricFmly['protocol'] = metricGrp.get('@protocol')

            if metricGrp.get('metric') != None:
                metricObjL = []
                for metric in getAsList(metricGrp.get('metric')):
                    metricObj = OrderedDict({})
                    metricObj['id'] = metric.get('@id')
                    metricObj['id'] = metricObj['id'].replace('.','-')
                    metricObj['id'] = metricObj['id'].replace(' ','-')
                    metricObj['name'] = metric.get('@name')
                    metricObj['descr'] = metric.get('@desc')
                    metricObj['protocol'] = metric.get('@protocol')
                    metricObj['units'] = metric.get('@units')
                    metricObj['type'] = "gauge" # For the sake of Prometheus
                    if metricObj['units'].lower() in metricUnitMap.keys():
                        metricObj['units'] = metricUnitMap.get(metricObj['units'])
                    else:
                        metricObj['units'] = 'none'
                    if metricObj['units'] == None:
                        metricObj['units'] = 'none'
                    # if metricObj['units'] == '%':
                    #     metricObj['units'] = 'percentage'
                    metricObj['conversionFunction'] = metric.get('@conversion-function')
                    metricObj['consolidation'] = metric.get('@consolidation-function')
                    metricObj['plotType'] = 'LINE'
                    metricObj['color'] = '#45C0FF'


                    if metricObj['conversionFunction'] in convFuncMap.keys():
                        metricObj['conversionFunction'] = convFuncMap.get(metricObj['conversionFunction'])

                    if metric.get('value') != None:
                        metricObj['value'] = metric.get('value').get('@rpn')
                        if (metricObj['value'] == None):
                            metricObj['value'] = metric.get('value').get('@parameter')
                        if metricObj['value'] == None:
                            metricObj['value'] = ''
                    if metric.get('parameter') != None:
                        paramObjL = []
                        for param in getAsList(metric.get('parameter')):
                            paramObj = OrderedDict({})
                            for k in OrderedDict(param).iterkeys():
                                key = k.replace('@','')
                                if key != 'onResynchOid':
                                    paramObj[key] = param.get(k)
                            if paramObj['name'] != None:
                                paramObj['id'] = paramObj['name']
                            if param.get('@onResynchOid') != None:
                                paramObj['oid'] = param.get('@onResynchOid')
                            paramObj['collector'] = ''
                            paramObjL.append(paramObj)
                        metricObj['paramList'] = paramObjL
                    metricObjL.append(metricObj)
                metricFmly['metrics'] = metricObjL

            metricFmlyL.append(metricFmly)
    perfConfig['metricFamilies'] = metricFmlyL
    content = json.dumps(OrderedDict(perfConfig), indent=2)
    (p,f) = splitPathAndFile(path)
    p = getDirName(p)
    savePath = p + '/' + f.replace('.xml', '')
    saveConfig(content, savePath)


def createInventoryConfig(path):
    (profilePath, file) = splitPathAndFile(configPath)
    if path.startswith('if-mib') == True:
        print('if-mib', path)
        executeShellCmd(['cp','../config/snmp/inventory/if-mib.conf',tempDir+'/snmp/inventory/'],True,True);
        return True
    elif path.find('cisco-envmon-mib') != -1:
        executeShellCmd(['cp','../config/snmp/inventory/cisco-envmon-mib.conf',tempDir+'/snmp/inventory/'],True,True);
        return True
    elif path.find('cisco-process-mib') != -1:
        executeShellCmd(['cp','../config/snmp/inventory/cisco-process-mib.conf',tempDir+'/snmp/inventory/'],True,True);
        executeShellCmd(['cp','../config/snmp/inventory/cisco-memory-pool-mib.conf',tempDir+'/snmp/inventory/'],True,True);
        return True

    childPath = profilePath + '/' + path;
    obj = parseToJson(childPath)
    invConfig = {}
    invProfile = obj.get('invProfile');

    if invProfile == None:
        terminate('Missing invProfile node in xml : ' + path)
        return False
    invConfig['name'] = invProfile.get('@name')

    appendMap = {'ifxTable':'ifTable'}
    tableObjL = []
    for table in getAsList(invProfile.get('table')):
        tableObj = OrderedDict({});
        tableObj['id'] = table.get('@name')
        tableObj['name'] = tableObj['id']
        tableObj['oid'] = table.get('@oid')
        if (tableObj['oid'].startswith('.')):
            tableObj['oid'] = tableObj['oid'][1:]
        tableObj['filter'] = {}

        colObjL = []
        for col in getAsList(table.get('column')):
            colObj = OrderedDict({});
            if col != None:
                colObj['id'] = col.get('@name');
                colObj['name'] = col.get('@name');
                colObj['suffix'] = col.get('@suffix');
                colObj['required'] = col.get('@required');
                if colObj['required'] == None or colObj['required'] != 'false' or colObj['required'] != 'true':
                    colObj['required'] = 'true'
                colObjL.append(colObj)
        propObjL = []
        epColMap = {'sourceIndex':'endPointIndex','administrativeStateName':'adminStatus',
                    'operationalStateName':'operStatus', 'macAddress':'physicalAddress',
                    'sourceId':'sourceId','sourceName':'sourceName','sourceType':'type',
                    'speed':'speed','description':'description'}
        epTypes = ['OID','INDEX','COLUMN','EXPLICIT']

        if table.get('end-point') != None:
            for prop in getAsList(table.get('end-point').get('property')):
                propObj = {};

                if prop.get('@name') in epColMap.keys():
                    propObj['name'] = epColMap.get(prop.get('@name'))
                    propObj['column'] = prop.get('@column')
                    propObj['dataType'] = 'String'
                    propObj['fetchType'] = prop.get('@value')
                    if propObj['name'] == 'sourceId':
                        propObj['fetchType'] = 'OID';
                    elif propObj['name'] == 'operStatus':
                        propObj['dataType'] = 'OperStatus'
                    elif propObj['name'] == 'adminStatus':
                        propObj['dataType'] = 'AdminStatus'
                    elif propObj['name'] == 'type':
                        propObj['dataType'] = 'Type'
                        if propObj['column'] == None:
                            propObj['fetchType'] = 'EXPLICIT'
                            propObj['column'] = '1'
                    elif propObj['name'] == 'endPointIndex':
                        propObj['fetchType'] = 'INDEX'

                    if propObj['fetchType'] not in epTypes:
                        propObj['fetchType'] = 'COLUMN'
                    if propObj['name'] != 'endPointIndex' and propObj['fetchType'] == 'INDEX':
                        propObj['fetchType'] = 'COLUMN'
                    if propObj['column'] == None:
                        propObj['column'] = 'UNKNOWN'

                    propObj['value'] = ''
                    propObjL.append(propObj)
        metricL = [];
        if table.get('metrics') != None:
            for metric in getAsList(table.get('metrics').get('group')):
                metricL.append(metric.get('@id'))

        tableObj['columns'] = colObjL
        tableObj['beanType'] = 'ai.netoai.collector.model.EndPoint'
        tableObj['properties'] = propObjL
        if table.get('@appends') != None:
            if table.get('@appends') == 'ifTable' and tableObj['id'] == 'ifxTable':
                tableObj['appends'] = table.get('@appends')
            elif table.get('@appends') != 'ifTable':
                tableObj['appends'] = table.get('@appends')
        tableObj['metricFamilyIds'] = metricL
        tableObj['syncTrapIds'] = ['generic-traps']
        if len(propObjL) != 0:
            tableObjL.append(tableObj)
    if len(tableObjL) == 0:
        return False
    invConfig['tables'] = tableObjL
    content = json.dumps(OrderedDict(invConfig), indent=2)
    savePath = path.replace('.xml', '')
    saveConfig(content, savePath)


def createTrapConfig(path):
    # print('create trap config')
    (profilePath, file) = splitPathAndFile(configPath)
    childPath = profilePath + '/' + path
    obj = parseToJson(childPath)
    allowedParams = ['specificProblem', 'perceivedSeverity', 'probableCause','action']
    trapConfig = {}
    trapGroup = obj.get('trap-group')
    trapConfig['id'] = trapGroup.get('@id')

    trapObjL = []
    for trap in  getAsList(trapGroup.get('trap')):
        trapObj = {'id' : trap.get('@id')}
        trapObj['beanType'] = 'ai.netoai.inventory.NetworkEvent'
        propObj = {}
        if trap.get('alarm') != None:
            trapAlarm = trap.get('alarm')
            isList = isinstance(trapAlarm, list)
            if isList == True:
                trapAlarm = trapAlarm[0]
            if trapAlarm.get('explicit') != None:
                for prop in getAsList(trapAlarm.get('explicit').get('property')):
                    if prop.get('@name') in allowedParams:
                        propObj[getAttrKey(prop.get('@name'))] = prop.get('@value')
                trapObj['properties'] = propObj
        condition = {};
        trapCnd = trap.get('trap-condition')
        if trapCnd != None:
            condition['or'] = []
            if trapCnd.get('or') != None:
                for rule in getAsList(trapCnd.get('or').get('set')):
                    ruleObj = {}
                    for k, v in OrderedDict(rule).iteritems():
                        ruleObj[getAttrKey(k)] = v.get('@value');
                    condition['or'].append(ruleObj)
                trapObj['condition'] = condition

        trapObjL.append(trapObj)

    trapConfig['traps'] = trapObjL
    content = json.dumps(OrderedDict(trapConfig), indent=2)
    savePath = path.replace('.xml', '')
    saveConfig(content, savePath)


def createDeviceConfig(path):
    (profilePath, file) = splitPathAndFile(configPath)
    childPath = profilePath + '/' + path;
    print "Child path: " + childPath
    obj = parseToJson(childPath)

    deviceConfig = {};
    snmpProfile = obj.get('snmp-profile');
    if snmpProfile == None:
        terminate('Missing snmp-profile node in device config xml : ' + path)
    profileId = snmpProfile.get('@id')
    if profileId == None:
        terminate('Missing profile id attr in device config xml : ' + path)
    deviceConfig['id'] = profileId
    deviceConfig['beanType'] = 'ai.netoai.collector.model.NetworkElement'
    deviceProps = []

    device = snmpProfile.get('device')

    for propName in ['explicit','query-agent']:
        propObj = device.get(propName)
        if propObj != None:
            property = propObj['property']
            allProps = [];
            for prop in property:
                deviceProp = {'name':prop.get('@name')};
                allProps.append(deviceProp['name'])
                deviceProp['dataType'] = 'String'
                if str(propName) == 'explicit':
                    # print('explicit')
                    deviceProp['value'] = prop.get('@value')
                    deviceProp['fetchType'] = 'EXPLICIT'
                elif str(propName) == 'query-agent':
                    # print('query agnet')
                    deviceProp['value'] = prop.get('@oid')
                    deviceProp['fetchType'] = 'SNMP_GET'
                deviceProps.append(deviceProp)
            if 'sysUpTime' not in allProps:
                deviceProp = {'name':'sysUpTime', 'dataType':'String',
                'value':'1.3.6.1.2.1.1.3.0', 'fetchType':'SNMP_GET'}
                deviceProps.append(deviceProp)

    deviceConfig['properties'] = deviceProps
    content = json.dumps(OrderedDict(deviceConfig), indent=2)
    savePath = path.replace('.xml','')
    saveConfig(content, savePath)


    # print(json.dumps(obj,indent=True))
    # print(childPath)


def createChildConfig(childPath):
    childPath = str(childPath)
    #parsing only if config if present in the below paths
    reqConfigs = ["trap", "pm","device","inventory"]
    (created, ignore, newPath) = (False, True, None)
    for name in reqConfigs:
        if childPath.find(name + "/") != -1:
            (path, file) = splitPathAndFile(childPath)
            path = getDirName(path)
            print "ParseType: {0}, File = {1}, Path = {2}".format(name, file, path)
            if not (os.path.exists(tempDir+'/'+path)):
                os.makedirs(tempDir+'/'+path)
            if name == 'device':
                createDeviceConfig(childPath)
                created = True
            elif name == 'inventory':
                created = createInventoryConfig(childPath)
                if created == None:
                    created = True
            elif name == 'trap':
                createTrapConfig(childPath)
                created = True
            elif name == 'pm':
                createPerfConfig(childPath)
                created = True
            else:
                created = False
            (ignore, newPath) = (False, path+'/'+file)
    return (created, ignore, newPath)

def createMainConfig():
    obj = parseToJson(configPath)
    module = obj.get('module')
    if module == None:
        terminate('Missing modules for xml : ' + configPath)

    profileId = module.get('@id')
    if profileId == None:
        terminate('Missing config profile id for xml : ' + configPath)

    meta = module.get('meta')
    if meta == None:
        terminate('Missing meta property values for '+profileId+' profile')

    dependencies = module.get('dependencies')
    if dependencies == None:
        terminate('Missing dependencies file paths for '+profileId+' profile')

    (profilePath, file) = splitPathAndFile(configPath)

    config = {'id':profileId}
    for key in meta.keys():
        if(key != 'capabilities'):
            config[getAttrKey(key)] = meta[key]['@name']
        else:
            for k in meta[key].keys():
                val = meta[key][k]
                if val == 'true':
                    val = True
                elif val == 'false':
                    val = False
                elif val == None:
                    val = True
                config[getAttrKey(k)] = val

    childConfigs = [];
    for key in dependencies['file']:
        path = key['@path']
        if path.find('.xml') != -1:
            childPath = profilePath+'/'+path;
            if (os.path.exists(childPath)):
                (created, ignore, newPath) =  createChildConfig(path)
                if created == True and ignore == False:
                    configNewPath = newPath.replace('.xml','.conf')
                    childConfigs.append(configNewPath)

    childConfigs.append('snmp/perf/ping-monitor.conf')
    executeShellCmd(['cp','ping-monitor.conf','.config/snmp/perf'], True, True)
    if profileId.find('cisco') != -1:
        childConfigs.append('snmp/topology/cisco-cdp-mib.conf')
        executeShellCmd(['cp','-r','topology','.config/snmp/'], True, True)
    config['childConfigs'] = childConfigs
    content = json.dumps(config, indent=2)
    saveConfig(content, profileId)

def terminate(msg):
    print('')
    print('Config creation failed ...')
    print('Reason : '+ msg)
    print('')
    # shutil.rmtree(destPath+'/.config')
    # exit(3)
def executeShellCmd(cmd, shell=False, wait=False):
    if shell:
        process = subprocess.Popen(" ".join(cmd), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if wait:
            (out, err) = process.communicate()
            # print("Shell command - Shell mode: " + " ".join(cmd) + ", Output: " + str(out) + ", Error: " + str(err))
    else:
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if wait:
            (out, err) = process.communicate()
            # print("Shell command: " + " ".join(cmd) + ", Output: " + str(out) + ", Error: " + str(err))

def main(args):
    if len(args) != 3:
        print('')
        print('   Usage: parseProfile <XML file path> <destination path>')
        print('')
        exit(3)
    global configPath, destPath
    global tempDir

    configPath = args[1]
    destPath = args[2]

    if not os.path.exists(configPath):
        terminate('Xml File Path does not exists (' + configPath + ')')
        exit(3)

    if not os.path.exists(destPath):
        terminate('Destination path does not exits ('+destPath+')')
        exit(3)
    else:
        if destPath == '.':
            destPath = os.getcwd();
        if os.path.exists(destPath+'/.config'):
            shutil.rmtree(destPath+'/.config')
        os.makedirs(destPath+'/.config')
        tempDir = destPath+'/.config'

    createMainConfig();
    (path, file) = splitPathAndFile(configPath)
    dirName = file.replace('.xml','')
    if os.path.exists(destPath+'/'+dirName):
        shutil.rmtree(destPath+'/'+dirName)
    os.makedirs(destPath+'/'+dirName)

    executeShellCmd(['cp','-r',destPath + '/.config/*',destPath+'/'+dirName], True, True)
    shutil.rmtree(destPath + '/.config')

    print("Parsing Done")

def checkMod(name):
    try:
        imp.find_module(name)
        return True
    except ImportError:
        print('Module not found : ' + name)
        return False

if __name__ == '__main__':
    # for mod in ['xmltodict','pathlib2','Path']:
    #     if checkMod(mod) == False:
    #         print('Cannot parse with missing modules, install modules to continue')
    #         exit(3)
    #     else:
    #         import xmltodict
    #         from pathlib2 import Path
    main(sys.argv)
