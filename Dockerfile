FROM maven
RUN apt-get update
RUN apt-get install  -y make build-essential libssl-dev zlib1g-dev libbz2-dev \
libreadline-dev libsqlite3-dev wget curl llvm libncurses5-dev libncursesw5-dev \
xz-utils tk-dev libffi-dev liblzma-dev

RUN wget https://www.python.org/ftp/python/3.7.2/Python-3.7.2.tgz
RUN tar xvf Python-3.7.2.tgz
RUN cd Python-3.7.2 && ./configure --enable-optimizations --with-ensurepip=install
#RUN cd Python-3.7.2 && make -j8
RUN cd Python-3.7.2 && make -j8 altinstall




# Copies project directory into container
COPY . amod

# Builds JAR file
RUN mvn -f amod install -DskipTests=true

# Move to output directory for easy access to JAR
WORKDIR /amod/target/

