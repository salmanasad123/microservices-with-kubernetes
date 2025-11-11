#  docker run --rm -p8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" review-service
# The -p8080:8080 option maps port 8080 in the container to port 8080 in the Docker host, which
# makes it possible to call it from the outside. In the case of Docker Desktop for Mac, which runs
# Docker in a local Linux virtual machine, the port will also be port-forwarded to macOS, which
# is made available on localhost

FROM eclipse-temurin:17.0.5_8-jre-focal as builder
WORKDIR extracted
ADD ./target/review-service-0.0.1-SNAPSHOT.jar app.jar
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