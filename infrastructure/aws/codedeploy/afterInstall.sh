#!/bin/bash
cd /home/centos/webapp
sudo rm -rf logs/*
sudo chown -R centos:centos /home/centos/webapp
sudo chmod +x demo-0.0.1-SNAPSHOT.jar
#source /etc/profile.d/envvariable.sh
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/centos/webapp/cloudwatch-config.json -s
kill -9 $(ps -ef|grep java | grep -v grep)
nohup java -jar demo-0.0.1-SNAPSHOT.jar