jbilling3
=========
## Installing and running JBilling3.
This is a modified version of the JBilling3 billing system. Download the project, unzip it, and rename the folder name to jbilling.

### Requirements
```
- Grails 1.3.4
- Java JDK > 1.6
- Apache Tomcat
```

### Run JBilling using the default built-in server
```
$ cd jbilling
$ grails {dev|test|prod} run-app
> Running in http://localhost:8080/jbilling
```

### Run JBilling in Apache Tomcat

Create a .war
```
$ cd jbilling
$ grails {dev|test|prod} war
```
Copy the resources folder to the server (ssh)
```
$ scp -r resources user/passwd@host:tomcat-home/
```
Copy the .war to the server (ssh)
```
$ cd target
$ scp jbilling.war user/passwd@host:tomcat-home/webapps/jbilling.war
```
Stop Tomcat
```
$ cd tomcat-home
$ sudo tomcat-home/bin/shutdown.sh
$ cd temp 
$ sudo rm -rf *
```
Start Tomcat
```
$ cd tomcat-home
$ sudo tomcat-home/bin/startup.sh
> Running in http://host:8080/jbilling
```

More information at [JBilling.com](http://www.jbilling.com/documentation/developers/building-from-source)
