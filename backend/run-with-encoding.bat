@echo off
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US
java %JAVA_OPTS% -jar target/ecommerce-platform-1.0-SNAPSHOT.jar --spring.profiles.active=local 