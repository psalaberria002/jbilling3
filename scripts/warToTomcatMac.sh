 #!/bin/bash
# Stop Tomcat if already running
sudo /usr/local/apache-tomcat-7.0.37/bin/shutdown.sh
cd /usr/local/apache-tomcat-7.0.37/webapps
# Working directory: /usr/local/apache-tomcat-7.0.37/
sudo rm jbilling.war
sudo rm -rf jbilling 
cd ~/grails/jbilling3
cp -r resources /usr/local/apache-tomcat-7.0.37/
grails test war
cd target
cp jbilling.war /usr/local/apache-tomcat-7.0.37/webapps/jbilling.war

cd /usr/local/apache-tomcat-7.0.37/
sudo /usr/local/apache-tomcat-7.0.37/bin/startup.sh