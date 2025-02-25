#!/usr/bin/python

"""
Packages the profiles into .tar files, to be deployed in the 
runtime NMS environment.
"""

import os, sys, shutil, json, tarfile

if len(sys.argv) != 3:
    print("Usage: {0} <Path of profile dir> <Target dir of tars>".format(sys.argv[0]))
    exit(1)

class createTars():

    sourcebasePath = sys.argv[1]
    targetbasePath = ""
    def get_profiles(self):
        if len(sys.argv) > 1:
            #print sys.argv[1]
            if os.path.exists(sys.argv[2]):
                self.targetbasePath = sys.argv[2]
                profilesMap = {}
                for filename in os.listdir(self.sourcebasePath):
                    fpath = os.path.join(self.sourcebasePath, filename)
                    if os.path.isfile(fpath):
                        profilesMap[filename] = fpath
                        # profilesList.append(filename)

                for key, value in profilesMap.items():
                    print("Key: {0}, Value: {1}".format(key, value))
                    if key.endswith(".conf"):
                        key = key.replace(".conf", "")
                    self.create_tar_for_profile(key, value)
            else:
                sys.exit("Path Incorrect")
        else:
            sys.exit("Path not provided")



    def create_tar_for_profile(self,dirName,dirPath):
        #print "DIRECTORY NAME - "+dirName
        #print "DIRECTORY PATH - "+dirPath
        #print "Target Path "+self.targetbasePath
        targtpath = self.targetbasePath +"/"+dirName
        if os.path.exists(targtpath):
            shutil.rmtree(targtpath)

        if not os.path.exists(targtpath):
            # Creating Main_Directory
           dir = os.mkdir(targtpath,0o755)
            # copying config file into the Directory
           shutil.copy(dirPath,targtpath)

            #loading the config file to see the child configs
           config_file =  targtpath+"/"+dirName+".conf"
           with open(config_file) as json_data:
               d = json.load(json_data)
               child_configs = d["childConfigs"]

           for child in child_configs:
               try:
                   if child.find("/device/") != -1:
                       device_path = targtpath+"/device"
                       if not os.path.exists(device_path):
                           dir = os.mkdir(device_path, 0o755)
                       shutil.copy(self.sourcebasePath+"/"+child,device_path)
                       #print "IN DEVICE "+child
                   elif child.find("/inventory/") != -1:
                       inventory_path = targtpath + "/inventory"
                       if not os.path.exists(inventory_path):
                           dir = os.mkdir(inventory_path, 0o755)
                       shutil.copy(self.sourcebasePath + "/" + child, inventory_path)
                       #print "IN INVENTORY "+child
                   elif child.find("/perf/") != -1:
                       perf_path = targtpath + "/perf"
                       if not os.path.exists(perf_path):
                           dir = os.mkdir(perf_path, 0o755)
                       shutil.copy(self.sourcebasePath + "/" + child, perf_path)
                       #print "IN PERF "+child
                   elif child.find("/traps/") != -1:
                       traps_path = targtpath + "/traps"
                       if not os.path.exists(traps_path):
                           dir = os.mkdir(traps_path, 0o755)
                       shutil.copy(self.sourcebasePath + "/" + child, traps_path)
                       #print "IN TRAPS "+child

                   elif child.find("/topology/") != -1:
                       topology_path = targtpath + "/topology"
                       if not os.path.exists(topology_path):
                           dir = os.mkdir(topology_path, 0o755)
                       shutil.copy(self.sourcebasePath + "/" + child, topology_path)
                       # print "IN TOPOLOGY "+child
               except:
                   continue

        self.make_tarfile(dirName+".tar",self.targetbasePath+"/"+dirName)


    def make_tarfile(self,output_filename, source_dir):
        cwd = os.getcwd()
        print(f"Current dir: {cwd}")
        os.chdir(self.targetbasePath)
        #print source_dir
        with tarfile.open(output_filename, "w:gz") as tar:
            tar.add(source_dir, arcname=os.path.basename(source_dir))
        print("Created "+output_filename+" in "+self.targetbasePath)
        dir_name = str(output_filename).replace(".tar", "")
        shutil.rmtree(dir_name)
        os.chdir(cwd)

ct = createTars()
ct.get_profiles()
