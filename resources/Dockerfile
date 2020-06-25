FROM centos:7 as centos-jdk

MAINTAINER xico <francisco.ab.monteiro@gmail.com>

ENV jacoco_version 0.8.3

RUN yum update -y && \
yum install -y wget && \
yum install -y java-1.8.0-openjdk Java-1.8.0-openjdk-devel && \
yum clean all &&\
mkdir temp

FROM centos-jdk as app

COPY app.tar /

RUN mkdir /app && \
tar -xvf /app.tar -C /app && \
rm /app.tar

# Define working directory.
WORKDIR /app
