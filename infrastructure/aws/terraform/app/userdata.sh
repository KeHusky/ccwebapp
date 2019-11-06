#! /bin/bash
sudo echo DBCreationType=update >> /etc/profile.d/envvariable.sh
sudo echo export DBCreationType >> /etc/profile.d/envvariable.sh
sudo echo username=dbuser >> /etc/profile.d/envvariable.sh
sudo echo export username >> /etc/profile.d/envvariable.sh
sudo echo password=abc123456 >> /etc/profile.d/envvariable.sh
sudo echo export password >> /etc/profile.d/envvariable.sh
sudo echo awsRDS=jdbc:mysql://${Endpoint}/csye6225 >> /etc/profile.d/envvariable.sh
sudo echo export awsRDS >> /etc/profile.d/envvariable.sh
sudo echo BucketName=${bucketName}>> /etc/profile.d/envvariable.sh
sudo echo export BucketName >> /etc/profile.d/envvariable.sh
source /etc/profile.d/envvariable.sh
