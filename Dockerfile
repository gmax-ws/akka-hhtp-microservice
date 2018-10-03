FROM frolvlad/alpine-scala

EXPOSE 8088 9142

COPY target/scala-2.12/akka-http-microservice-assembly-0.1.jar target/scala-2.12/akka-http-microservice-assembly-0.1.jar
COPY native/* native/

CMD ["java", "-Djava.library.path=native", "-jar", "target/scala-2.12/akka-http-microservice-assembly-0.1.jar"]
