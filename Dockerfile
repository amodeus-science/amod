FROM maven

COPY . amod

RUN mvn -f amod package
