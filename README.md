# akka-http-cassandra

A simple Akka, Akka-Http, Cassandra microservice

Swagger [URL](http://localhost:8088/person-services/swagger)

SBT build, run and test

```sh
$ sbt -Djava.library.path=native clean assembly
$ sbt -Djava.library.path=native run
$ sbt -Djava.library.path=native clean test
```

For Windows OS use:
```cmd
> set JAVA_OPTS="-Dcom.datastax.driver.USE_NATIVE_CLOCK=false"
> sbt clean assembly
> sbt run
> sbt clean test
```
Docker commands

```sh
#remove all containers
$ docker rm $(docker ps -a -q)

# remove all images
$ docker rmi $(docker images -q)

# build image
$ docker build -t person-image .

# run interactive, useful for debug
$ docker run --name person -it --entrypoint /bin/bash person-image

# run container
$ docker run --name person -p8088:8088 -p9142:9142 -it person-image
```
