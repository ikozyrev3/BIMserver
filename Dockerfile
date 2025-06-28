# Use official Tomcat image with Java 8
FROM tomcat:9.0-jdk8

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Set environment variables
ENV CATALINA_OPTS="-Xmx2g -Xms1g"
ENV JAVA_OPTS="-Djava.awt.headless=true"

# Create BIMserver home directory
RUN mkdir -p /opt/bimserver/home
RUN chown -R tomcat:tomcat /opt/bimserver/

# Copy the WAR file (this assumes the WAR file has been built)
# In CI/CD, this would be replaced with the actual built WAR file
COPY BimServerWar/target/bimserverwar-*.war /usr/local/tomcat/webapps/ROOT.war

# Set ownership
RUN chown tomcat:tomcat /usr/local/tomcat/webapps/ROOT.war

# Expose port
EXPOSE 8080

# Run as non-root user
USER tomcat

# Start Tomcat
CMD ["catalina.sh", "run"]