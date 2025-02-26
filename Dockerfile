FROM openjdk:8-oraclelinux8

RUN microdnf update && \
    microdnf install vi iputils telnet && \
    microdnf clean all
    
RUN mkdir -p /opt/snmp-poller/conf && \
    mkdir -p /opt/snmp-poller/final-profiles && \
    mkdir -p /opt/snmp-poller/logs
    
COPY src/main/resources/collector.properties /opt/snmp-poller/conf
COPY src/main/resources/log4j.properties /opt/snmp-poller/conf
COPY src/main/resources/discoverytree.json /opt/snmp-poller/conf      
COPY final-profiles/* /opt/snmp-poller/final-profiles/
COPY target/snmp-poller-full.jar /opt/snmp-poller/

#ENTRYPOINT ["sleep", "1000000000"]
WORKDIR "/opt/snmp-poller"
ENTRYPOINT ["/bin/bash", "-c", "java -cp /opt/snmp-poller/snmp-poller-full.jar -Dcollector.log4j=/opt/snmp-poller/conf/log4j.properties -DcollectorSettings=/opt/snmp-poller/conf/collector.properties ai.netoai.collector.startup.CollectorMain"]
