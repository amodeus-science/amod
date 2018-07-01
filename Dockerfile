FROM maven

COPY . amod

RUN mvn -f amod install 

ADD https://www.ethz.ch/content/dam/ethz/special-interest/mavt/dynamic-systems-n-control/idsc-dam/Research_Frazzoli/AMoDeusScenarioSanFrancisco.zip /amod/

WORKDIR /amod/

RUN unzip AMoDeusScenarioSanFrancisco.zip -d .
