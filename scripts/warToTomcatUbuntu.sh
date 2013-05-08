 #!/bin/bash
# Stop Tomcat if already running
sudo /usr/local/apache-tomcat-7/bin/shutdown.sh
cd /usr/local/apache-tomcat-7/webapps
# Working directory: /usr/local/apache-tomcat-7.0.37/
sudo rm jbilling.war
sudo rm -rf jbilling 
cd ~/workspace/jbilling3
sudo cp -r resources /usr/local/apache-tomcat-7/
grails war
cd target
sudo cp jbilling.war /usr/local/apache-tomcat-7/webapps/jbilling.war

cd /usr/local/apache-tomcat-7/
sudo /usr/local/apache-tomcat-7/bin/startup.sh