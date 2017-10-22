FROM java:8
MAINTAINER roshambot3000@gmail.com
VOLUME /tmp
ADD build/libs/roshambot-twitter-1.0-SNAPSHOT.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]


