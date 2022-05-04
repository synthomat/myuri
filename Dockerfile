FROM eclipse-temurin:17-jre

RUN mkdir /opt/app
COPY target/myuri-*.jar /opt/app/myuri.jar

EXPOSE 3000
CMD ["java", "-jar", "/opt/app/myuri.jar"]