apply plugin: 'java'
apply plugin: 'maven'

group = 'ch.ethz.idsc'
version = '1.2.8'

description = """"""

sourceCompatibility = 1.8
targetCompatibility = 1.8



repositories {
        
     maven { url "https://raw.github.com/idsc-frazzoli/tensor/mvn-repo/" }
     maven { url "https://raw.github.com/idsc-frazzoli/amodeus/mvn-repo/" }
     maven { url "https://repo.maven.apache.org/maven2" }
}

dependencies {
    compile group: 'ch.ethz.idsc', name: 'amodeus', version:'1.2.8'
    compile group: 'ch.ethz.idsc', name: 'tensor', version:'0.5.7'
    compile group: 'com.google.inject', name: 'guice', version:'4.1.0'
    testCompile group: 'junit', name: 'junit', version:'3.8.1'
}
