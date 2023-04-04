FROM clojure:temurin-17-focal as build
RUN mkdir -p /opt/build
COPY . /opt/build
WORKDIR /opt/build
RUN clj -T:build uber

FROM eclipse-temurin:17-jre
RUN mkdir -p /opt/app
COPY --from=build /opt/build/target/myuri-*.jar /opt/app/myuri.jar

EXPOSE 3000
CMD ["java", "-jar", "/opt/app/myuri.jar"]