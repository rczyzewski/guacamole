#### Local setup for sonarQube
Download sonarQube image with
```shell
docker pull sonarqube
```
create a project configuration, and generate tokens:

```shell
JAVA_HOME=/Users/rafal.czyzewski/Library/Java/JavaVirtualMachines/graalvm-ce-11/Contents/Home
java -version
mvn clean verify sonar:sonar \
-Dsonar.projectKey=guacamole \
-Dsonar.projectName='guacamole' \
-Dsonar.host.url=http://127.0.0.1:9000 \
-Dsonar.token=sqp_7292e566def56727ba277c0d92eddd0a48dfc762
```
