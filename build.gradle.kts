plugins {
  application
}

application {
  mainClassName = "amod.demo.ScenarioStandalone"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  compile("ch.ethz.idsc:amodeus:1.4.8")
  compile("ch.ethz.idsc:tensor:0.6.1")
  compile("com.google.inject:guice:4.1.0")
  testCompile("junit:junit:3.8.1")
}

repositories {
  maven("http://download.osgeo.org/webdav/geotools")
  jcenter()
  mavenCentral()
  maven("https://raw.github.com/idsc-frazzoli/tensor/mvn-repo/")
  maven("https://raw.github.com/idsc-frazzoli/amodeus/mvn-repo/")
  maven("http://dl.bintray.com/matsim-eth/matsim")
  maven("https://www.xypron.de/repository")
}

group = "ch.ethz.idsc"
version = "1.2.8"