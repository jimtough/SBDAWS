FROM openjdk:8-jdk-alpine

# (comment below is from a tutorial example)
# We added a VOLUME pointing to "/tmp" because that is where a Spring Boot application creates
# working directories for Tomcat by default. The effect is to create a temporary file on your
# host under "/var/lib/docker" and link it to the container under "/tmp".
VOLUME /tmp

VOLUME /datafiles

# Copy the Spring Boot "uberjar" that is built by Maven into the Docker image
ADD target/this-is-the-app.jar app.jar
# Add any JVM parameters here
ENV JAVA_OPTS=""

#HEALTHCHECK --interval=5m --timeout=3s CMD curl -f http://localhost/ || exit 1

# To reduce Tomcat startup time we added a system property pointing to "/dev/urandom" as a source of entropy.
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
