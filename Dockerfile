FROM maven

# Copies project directory into container
COPY . amod

# Builds JAR file
RUN mvn -f amod install

# Move to output directory for easy access to JAR
WORKDIR /amod/target/

# Fetches scenario files
# TODO: This should really be moved to into pom.xml
RUN curl -O "https://www.ethz.ch/content/dam/ethz/special-interest/mavt/dynamic-systems-n-control/idsc-dam/Research_Frazzoli/{AMoDeusScenarioSanFrancisco,BerlinAMoDeus,SantiagoAMoDeus,TelavivAMoDeus}.zip" && \
    unzip \*.zip -d . && \
    rm *.zip