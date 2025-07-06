FROM gradle:jdk21 AS build

WORKDIR /tmp
RUN wget -O lego.tar.gz "https://github.com/go-acme/lego/releases/download/v4.22.2/lego_v4.22.2_linux_amd64.tar.gz" && tar -xzf lego.tar.gz && rm -f lego.tar.gz

WORKDIR /home/gradle/project

COPY --chown=gradle:gradle build.gradle settings.gradle .
RUN gradle dependencies --no-daemon

COPY --chown=gradle:gradle src ./src
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

COPY --from=build /tmp/lego /usr/local/bin/lego
RUN chmod +x /usr/local/bin/lego

WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
