package ai.netoai.collector.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class IpHostsDiscoveryUtil {

    private static final Logger log = LoggerFactory.getLogger(IpHostsDiscoveryUtil.class);
    private static final String tracePrefix = "[" + IpHostsDiscoveryUtil.class.getSimpleName() + "]: ";

    private IpHostsDiscoveryUtil() {

    }

    public static Set<String> getIpsFromRange(String iprange) {
        Set<String> IPsList = new LinkedHashSet<String>();
        String[] concatStringIps = iprange.trim().split("\\,");
        if (concatStringIps.length > 1) {
            for (String ipString : concatStringIps) {
                getIpHostsList(ipString, IPsList);
            }
        } else {
            getIpHostsList(iprange, IPsList);
        }
        log.info("Total Ips are " + IPsList.size());
        return IPsList;
    }

    private static void getIpHostsList(String ipRanges, Set<String> ipsList) {
        String uniqueIp = "";
        // which contains ranges like "10.27.10.20-10.27.12.5"
        String[] splits = ipRanges.trim().split("\\-");
        if (splits != null && splits.length > 1) {
            String fromIp = splits[0].trim();
            String toIp = splits[1].trim();
            if (toIp.split("\\.").length == 1) {
                String[] frSplits = fromIp.split("\\.");
                String temp = "";
                int count = 0;
                for (String fr : frSplits) {
                    if (count != (frSplits.length - 1)) {
                        temp += fr + ".";
                    }
                    count++;
                }
                toIp = temp + toIp;
            }
            String rangeString = getUniqueandNonUniqueIp(fromIp, toIp);
            String[] rangeStringSplits = rangeString.split("\\::");
            uniqueIp = rangeStringSplits[0];
            createIpHosts(rangeStringSplits[1], uniqueIp, ipsList);
        } // Range which contains *
        else if (ipRanges.split("\\*", 2) != null && ipRanges.split("\\*", 2).length > 1) {
            String[] splits1 = ipRanges.split("\\*", 2);
            uniqueIp = splits1[0];
            String[] splits2 = uniqueIp.split("\\.");
            int length = 4 - splits2.length;
            //System.out.println(uniqueIp + " -- " + length);
            getcompleteIPs(null, null, uniqueIp, length, ipsList);
        } // single ip
        else {
            ipsList.add(ipRanges);
        }
    }

    /*
	 * Based on given range it divides the ip into two parts,part of ip which is
	 * unique and the other which is not same
     */
    private static String getUniqueandNonUniqueIp(String fromIp, String toIp) {
        log.info(fromIp + " -- " + toIp);
        String uniqueIp = "";
        String ipRanges = null;
        String[] fromIpNumbers = fromIp.trim().split("\\.");
        String[] toIpNumbers = toIp.split("\\.");

        String fromRange = "";
        String toRange = "";
        boolean breakOuter = false;

        for (int i = 0; i < fromIpNumbers.length; i++) {
            for (int j = 0; j < toIpNumbers.length; j++) {
                if (i == j) {
                    if (fromIpNumbers[i].equals(toIpNumbers[j])) {
                        uniqueIp += fromIpNumbers[i] + ".";
                    } else {
                        fromRange = fromIp.replaceFirst(uniqueIp, "");
                        toRange = toIp.replace(uniqueIp, "");
                        breakOuter = true;
                    }
                }
            }
            if (breakOuter) {
                break;
            }
        }

        ipRanges = fromRange.trim() + "-" + toRange.trim();

        log.info("Unique IP is " + uniqueIp);
        log.info("Non-Unique Ip is " + ipRanges);
        return uniqueIp + "::" + ipRanges;
    }

    private static void createIpHosts(String range, String uniqueNo, Set<String> ipsList) {
        if (range != null) {
            String[] splits = range.split("\\-");
            if (splits.length != 0) {
                String from = splits[0];
                String to = splits[1];

                String[] fromParts = from.split("\\.");
                String[] toParts = to.split("\\.");

                getcompleteIPs(fromParts, toParts, uniqueNo, fromParts.length, ipsList);

            }
        }
    }

    /*
	 * Gives the Ips from given Range
     */
    private static void getcompleteIPs(String[] fromParts, String[] toParts, String uniqueIP, int length,
            Set<String> IPsList) {
        if (length == 1) {
            if (fromParts != null && fromParts.length > 0) {
                String startFrom = fromParts[0];
                String endAr = toParts[0];
                int fromInt = Integer.parseInt(fromParts[0]);
                int toInt = Integer.parseInt(toParts[0]);
                for (int i = fromInt; i <= toInt; i++) {
                    //System.out.println(uniqueIP + i);
                    IPsList.add(uniqueIP + i);
                }
            } else {
                for (int i = 0; i <= 255; i++) {
                    //System.out.println(uniqueIP + i);
                    IPsList.add(uniqueIP + i);
                }
            }
        } else if (length == 2) {
            if (fromParts != null && fromParts.length > 1) {
                int fromValue = Integer.parseInt(fromParts[0]);
                int toValue = Integer.parseInt(toParts[0]);
                int diff = toValue - fromValue;

                for (int i = 0; i < diff; i++) {
                    String endAt = "255";
                    String startFrom = "0";
                    if (i == 0) {
                        startFrom = fromParts[1];
                    }

                    int fromInt = Integer.parseInt(startFrom);
                    int toInt = Integer.parseInt(endAt);

                    for (int j = fromInt; j <= toInt; j++) {
                        // System.out.println(uniqueIP + fromValue + "." + j);
                        IPsList.add(uniqueIP + fromValue + "." + j);
                    }
                    fromValue++;
                }

                int finalEndAt = Integer.parseInt(toParts[1]);
                for (int k = 0; k <= finalEndAt; k++) {
                    // System.out.println(uniqueIP + fromValue + "." + k);
                    IPsList.add(uniqueIP + fromValue + "." + k);
                }
            } else {
                for (int i = 0; i <= 255; i++) {
                    for (int j = 0; j <= 255; j++) {
                        // System.out.println(uniqueIP + i + "." + j);
                        IPsList.add(uniqueIP + i + "." + j);
                    }
                }
            }
        } else if (length == 3) {
            if (fromParts != null && fromParts.length > 1) {
                int fromValue0 = Integer.parseInt(fromParts[0]);
                int fromValue1 = Integer.parseInt(fromParts[1]);
                int fromValue2 = Integer.parseInt(fromParts[2]);
                int toValue = Integer.parseInt(toParts[0]);
                int diff = toValue - fromValue0;
                for (int i = 0; i < diff; i++) {
                    int initializeValue1 = 0;
                    if (i == 0) {
                        initializeValue1 = fromValue1;
                    }
                    for (int j = initializeValue1; j <= 255; j++) {
                        int initializeValue2 = 0;
                        if (i == 0 && j == initializeValue1) {
                            initializeValue2 = fromValue2;
                        }
                        for (int k = initializeValue2; k <= 255; k++) {
                            // System.out.println(uniqueIP+fromValue0+"."+j+"."+k);
                            IPsList.add(uniqueIP + fromValue0 + "." + j + "." + k);
                        }
                    }
                    fromValue0++;
                }

                int toValue1 = Integer.parseInt(toParts[1]);
                int toValue2 = Integer.parseInt(toParts[2]);
                for (int x = 0; x <= toValue1; x++) {
                    int conditionValue = 255;
                    if (x == toValue1) {
                        conditionValue = toValue2;
                    }
                    for (int y = 0; y <= conditionValue; y++) {
                        //System.out.println(uniqueIP + fromValue0 + "." + x +
                        //"." + y);
                        IPsList.add(uniqueIP + fromValue0 + "." + x + "." + y);
                    }
                }
            } else {
                for (int i = 0; i <= 255; i++) {
                    for (int j = 0; j <= 255; j++) {
                        for (int k = 0; k <= 255; k++) {
                            // System.out.println(uniqueIP + i + "." + j + "." +
                            // k);
                            IPsList.add(uniqueIP + i + "." + j + "." + k);
                        }
                    }
                }
            }
        }
    }

}
