# Trivia application

A simple web application for quizzes made in Java.


## Starting

### 1. Preparation of the database
Start the MySQL server and in the file `resources/META-INF/persistence.xml` change the values ​​for `user` and `password`.

### 2. Build applications
```bash
gradle build
```

### 3. Starting the application
```bash
gradle appRun
```

The application will be available at: `http://localhost:8080/trivia`

### 4. Generating test data (optional)
```bash
gradle generateData
```

## Technologies

- Java
- Gradle
- Hibernate (JPA)
- MySQL
- Jakarta Servlet API
- WebSocket
- Gretty (Tomcat 10)

## Structure

- `src/main/java` - Java code
- `src/main/webapp` - Web resources (HTML, CSS, JS)
- `src/main/resources` - Configuration
