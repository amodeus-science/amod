FROM maven

# Copies project directory into container
COPY . amod

# Builds JAR file
RUN mvn -f amod install -DskipTests=true

# Move to output directory for easy access to JAR
WORKDIR /amod/target/

