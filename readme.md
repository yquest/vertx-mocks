after build copy to container the file build/distributions/app/vertx-mocks-1.0-SNAPSHOT.tar, in container untar the the file with comamnd
    
    tar -C ./ -xvf vertx-mocks-1.0-SNAPSHOT.tar

to renamme original path to app for instance
    
    mv vertx-mocks-1.0-SNAPSHOT app
    
example to publish port 9210 from port 8080

    docker run -it -v ./app:/app -p 9210:8080 openjdk:8 sh /app/bin/vertx-mocks