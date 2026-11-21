# Starts Dawn of Mankind on http://localhost:8080/dawn/ (needs Java 21 and Maven)
mvn -q install -DskipTests
if ($LASTEXITCODE -eq 0) { mvn -f dawn-app/pom.xml spring-boot:run }
