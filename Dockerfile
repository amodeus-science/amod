FROM maven

# Copies project directory into container
COPY . amod

# Builds JAR file
RUN mvn -DskipTests -f amod install

# Move to output directory for easy access to JAR
WORKDIR /amod/target/

