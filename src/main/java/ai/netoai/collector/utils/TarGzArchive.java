package ai.netoai.collector.utils;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class TarGzArchive {

    private static final Logger log = LoggerFactory.getLogger(TarGzArchive.class);
    private static final String tracePrefix = "[" + TarGzArchive.class.getSimpleName() + "]: ";
    //private String basePath = "../../SnmpCollector/config/tar-extracts/";
    
    private String parentDirectoryName;

    public TarGzArchive(File configFile) throws IOException, FileNotFoundException {
        
        String configName = configFile.getName().split(Pattern.quote("."))[0];
        String destDir = configFile.getParent() + File.separator;
        this.parentDirectoryName = configName;
        log.info(tracePrefix + "Extracting the tar [" + configFile + "] to Destination: " + destDir);

        Map<String, byte[]> archive = createMapTar(configFile);
        Set<String> entries = archive.keySet();
        for (String entry : entries) {
            File newFile = new File(destDir + "/" + entry);

            if (entry.endsWith("/")) {
                newFile.mkdirs();
                continue;
            }

            File parent = newFile.getParentFile();
            if ((parent != null) && (!parent.exists())) {
                parent.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(newFile);
            byte[] bytes = archive.get(entry);
            fos.write(bytes);
            fos.close();
        }
        log.info("Extracted Tar File into Directory at " + destDir);
        /*org.apache.commons.io.FileUtils.forceDelete(file);
	        log.info("Deleted Tar File in location - "+tarFilePath);*/
    }

    public String getParentDirectoryName() {
        return parentDirectoryName;
    }

    public void setParentDirectoryName(String parentDirectoryName) {
        this.parentDirectoryName = parentDirectoryName;
    }

    private Map<String, byte[]> createMapTar(File file) {
        Map<String, byte[]> content = new HashMap<String, byte[]>();
        try {
            FileInputStream fis = new FileInputStream(file);
            GZIPInputStream gis = new GZIPInputStream(fis);
            TarInputStream tis = new TarInputStream(gis);

            TarEntry entry = null;
            ByteArrayOutputStream baos = null;
            while ((entry = tis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName != null) {
                    baos = new ByteArrayOutputStream();
                    tis.copyEntryContents(baos);
                    byte[] bytes = baos.toByteArray();
                    content.put(entryName, bytes);
                }
            }
        } catch (FileNotFoundException e) {
            log.error("Failed", e);
        } catch (IOException e) {
            log.error("Failed", e);
        }
        return content;
    }

}
