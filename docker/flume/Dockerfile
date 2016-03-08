FROM java:8
MAINTAINER amitnira@gmail.com

# TODO: a way to install latest version automatically?
RUN mkdir /opt/flume
RUN wget -qO- \
  https://archive.apache.org/dist/flume/stable/apache-flume-1.6.0-bin.tar.gz \
  | tar zxvf - -C /opt/flume --strip 1

ADD build/iz-flume-es-1.0-SNAPSHOT.jar /opt/flume/lib/iz-flume-es-1.0-SNAPSHOT.jar
ADD build/elasticsearch-sink2-assembly-1.0.jar /opt/flume/lib/elasticsearch-sink2-assembly-1.0.jar

ADD flume.conf /var/tmp/flume.conf

EXPOSE 44444

# Add Flume to path
ENV PATH /opt/flume/bin:$PATH

CMD [ "flume-ng", "agent", "-c", "/opt/flume/conf", "-f", "/var/tmp/flume.conf", "-n", "iz", "-Dflume.root.logger=INFO,console"]

#ADD start-flume.sh /opt/flume/bin/start-flume
#CMD [ "start-flume" ]


