FROM maven

COPY . amod

RUN mvn -f amod install 

WORKDIR /amod/target/

RUN curl -O "https://www.ethz.ch/content/dam/ethz/special-interest/mavt/dynamic-systems-n-control/idsc-dam/Research_Frazzoli/{AMoDeusScenarioSanFrancisco,BerlinAMoDeus,SantiagoAMoDeus,TelavivAMoDeus}.zip" && \
    unzip \*.zip -d . && mv SanFransisco/* .