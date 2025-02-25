package ai.netoai.collector.startup;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import ai.netoai.collector.settings.KafkaTopicSettings;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrapsForwardManager {
	private static final Logger log = LoggerFactory.getLogger(TrapsForwardManager.class);
	private static final String tracePrefix = "[" + TrapsForwardManager.class.getSimpleName() + "]: ";

	// Gets all the KeyedMessages from LogTraps and delete those Particular
	// Files
	public static List<String> readStoredMessages(String topicName) {
		List<String> batch = new ArrayList<>();
		
		String location = null;
		String fileNamePart = null;
		if ( topicName.equals(KafkaTopicSettings.FAULT_TOPIC) ) {
			location = "../store";
			fileNamePart = "traps.log";
		} else if ( topicName.equals(KafkaTopicSettings.PERF_TOPIC) ) {
			location = "../store_perf";
			fileNamePart = "perf.log";
		} else if ( topicName.equals(KafkaTopicSettings.INVENTORY_TOPIC) ) {
			location = "../store_inv";
			fileNamePart = "inv.log";
		} else {
			return null;
		}

		File file = new File(location);
		File[] trapLogs = filterFiles(file, fileNamePart);

		if (trapLogs.length > 0) {
			File archiveDir = new File("../archive");
			boolean archiveExists = false;
			File currentArchive = null;
			if ( archiveDir.exists() && archiveDir.isDirectory() ) {
				log.info(tracePrefix + "Archive dir exists: " + archiveDir);
				archiveExists = true;
				String dirName = topicName + "_Archive_" + System.currentTimeMillis();
				currentArchive = new File("../archive/" + dirName);
				if ( currentArchive.mkdir() ) {
					log.info(tracePrefix + "Created archive directory for current traps: " + currentArchive);
				}
			} else {
				log.error(tracePrefix + "*** Archive directory does not exists ***");
			}
			long startTime = System.currentTimeMillis();
			for (File trapFile : trapLogs) {
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(trapFile.getAbsolutePath()));
					String line = reader.readLine();
					while (line != null) {
						if (line.contains("heartbeat")) {
							continue;
						}
						batch.add(line);
						line = reader.readLine();
					}
					reader.close();
					
					if ( archiveExists ) {
						File dst = new File(currentArchive.getAbsolutePath() + "/" + trapFile.getName());
						Files.copy(trapFile.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					
					if ( trapFile.getName().endsWith(".log") ) {
						log.info(tracePrefix + "Emptying the file: " + trapFile.getName());
						PrintWriter writer = new PrintWriter(trapFile);
						writer.print("");
						writer.close();
					} else {
						log.info(tracePrefix + "Deleting the file: " + trapFile.getName());
						boolean success = trapFile.delete();
						if ( !success ) {
							log.warn(tracePrefix + "Failed deleting the file, moving to /tmp");
							FileUtils.moveToDirectory(trapFile, new File("/tmp"), false);
						}
					}
				} catch (Exception e) {
					log.error(tracePrefix + e);
				}
			}
			log.info(tracePrefix + " Time took to read " + trapLogs.length + " is: " + (System.currentTimeMillis() - startTime) + " ms");
			if ( archiveExists ) {
				log.info(tracePrefix + "Building archive from location: " + currentArchive);
				TarArchiveOutputStream out = null;
				try {
					out = new TarArchiveOutputStream(
							new GZIPOutputStream(
							new BufferedOutputStream(new FileOutputStream("../archive/" + currentArchive.getName() + ".tar.gz"))));
					File[] files = currentArchive.listFiles();
					for(File archiveFile : files) {
						ArchiveEntry entry = out.createArchiveEntry(archiveFile, archiveFile.getName());
						out.putArchiveEntry(entry);
						IOUtils.copy(new FileInputStream(archiveFile), out);
						out.closeArchiveEntry();
					}
					log.info(tracePrefix + "Archive built successfully, removing archive directory");
					File[] fileList = currentArchive.listFiles();
					for(File f : fileList) {
						boolean success = f.delete();
						if ( !success ) {
							FileUtils.moveToDirectory(f, new File("/tmp"), false);
						}
					}
					boolean deleted = currentArchive.delete();
					log.info(tracePrefix + "Deleted current archive directory, after compression: " + deleted);
				} catch (Exception ex) {
					log.error(tracePrefix + "Failed building an archive: " + currentArchive, ex);
				} finally {
					if ( out != null ) {
						try {
							out.close();
						} catch (Exception ex) {
							log.error(tracePrefix + "Failed closing the TarOutputStream", ex);
						}
					}
				}
			}
		}
		log.info(tracePrefix + "Number of traps read from trapFiles: " + batch.size());
		return batch;
	}

	private static File[] filterFiles(File file, String fileNamePart) {
		File[] trapLogs = file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathName) {
				if (pathName != null && pathName.isFile() && pathName.getName().contains(fileNamePart)) {
					return true;
				}
				return false;
			}
		});
		return trapLogs;

	}
	
	public static void main(String[] args) throws Exception {
		File src = new File("/home/lokesh/docsDevNotifMIB.c");
		File target = new File("/home/lokesh/new/" + src.getName());
		Path targetPath = Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.println("Copied to path: " + targetPath);
		TarArchiveOutputStream out = new TarArchiveOutputStream(
				new GZIPOutputStream(
				new BufferedOutputStream(new FileOutputStream(target.getParentFile() + ".tar.gz"))));
		ArchiveEntry entry = out.createArchiveEntry(target, target.getName());
		out.putArchiveEntry(entry);
		IOUtils.copy(new FileInputStream(target), out);
		out.closeArchiveEntry();
		out.close();
		System.out.println("Done archive");
	}

}
