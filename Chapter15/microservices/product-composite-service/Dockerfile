# Before we can run our microservice as a Docker container, we need to package it in
# a Docker image. To build a Docker image, we need a Dockerfile, so we will start with that. Next, we
# need a Docker-specific configuration for our microservice. Since a microservice that runs in a container is
# isolated from other microservices – it has its own IP address, hostname, and ports – it needs
# a different configuration compared to when it’s running on the same host with other microservices.
# For example, since the other microservices no longer run on the same host, no port conflicts will
# occur. When running in Docker, we can use the default port 8080 for all our microservices without
# any risk of port conflicts. On the other hand, if we need to talk to the other microservices, we can no
# longer use localhost like we could when we ran them on the same host.

# When it comes to handling the suboptimal packaging of fat JAR files in Docker images, Spring Boot
# addressed this issue in v2.3.0, making it possible to extract the content of a fat JAR file into a number
# of folders. By default, Spring Boot creates the following folders after extracting a fat JAR file:
#  • dependencies, containing all dependencies as JAR files
#  • spring-boot-loader, containing Spring Boot classes that know how to start a Spring Boot
#    application
#  • snapshot-dependencies, containing snapshot dependencies, if any
#  • application, containing application class files and resources
# The Spring Boot documentation recommends creating one Docker layer for each folder in the order listed above.

# To handle the extraction of the fat JAR file in the Dockerfile we use a multi-stage build, meaning that
# there is a first step, named builder, that handles the extraction. The second stage builds the actual
# Docker image that will be used at runtime, picking the files as required from the first stage. Using this
# technique, we can handle all packaging logic in the Dockerfile but, at the same time, keep the size of
# the final Docker image to a minimum.

# Workflow Process:
# The builder stage sets the working directory to extracted and adds the fat JAR file from the Maven build library,
# target/, to that folder.

# The builder stage then runs the command java -Djarmode=layertools -jar app.jar
  #extract, which will perform the extraction of the fat JAR file into its working directory, the
  #extracted folder.

# The next and final stage starts with the line: FROM eclipse-temurin:17.0.5_8-jre-focal

# It copies the exploded files from the builder stage, folder by folder, into the application folder.
# This creates one layer per folder, as described above. The parameter --from=builder is used to instruct Docker
# to pick the files from the file system in the builder stage.

# After exposing the proper ports, 8080 in this case, the Dockerfile wraps up by telling Docker what
# Java class to run to start the microservice in the exploded format, that is, org.springframework.boot.loader.JarLauncher.


FROM eclipse-temurin:17.0.5_8-jre-focal as builder
WORKDIR extracted
ADD ./target/product-composite-service-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17.0.5_8-jre-focal
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]



# Jab Docker container start ho, to Java ko directly Spring Boot launcher class run karne ke liye bolo —
# ye class hi tumhare Spring Boot app ko boot karti hai.
# Jab tum layers extract karte ho: java -Djarmode=layertools -jar app.jar extract
# to jar ke andar ke parts (dependencies, loader, classes) alag ho jaate hain — isliye ab -jar app.jar command nahi
# chal sakti (kyunki ab app.jar single file nahi rahi). Isliye hume manually batana padta hai:
# “Java, please use the Boot loader launcher class.”