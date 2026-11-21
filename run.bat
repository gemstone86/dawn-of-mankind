@echo off
rem Starts Dawn of Mankind on http://localhost:8080/dawn/ (needs Java 21 and Maven)
call mvn -q install -DskipTests || exit /b 1
mvn -f dawn-app/pom.xml spring-boot:run
