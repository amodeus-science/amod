# ch.ethz.idsc.amod <a href="https://travis-ci.org/idsc-frazzoli/amod"><img src="https://travis-ci.org/idsc-frazzoli/amod.svg?branch=master" alt="Build Status"></a>

This repository allows to run an autonomous mobility-on-demand scenario using the amodeus library (https://github.com/idsc-frazzoli/amodeus).

Try it, orchestrate your own fleet of amod-taxis! Watch a [visualization](https://www.youtube.com/watch?v=QkFtIQQSHto) of a traffic simulation in San Francisco generated using this repository.

<table><tr>
<td>

![p1t1](https://user-images.githubusercontent.com/4012178/38852194-23c0b602-4219-11e8-90af-ce5c589ddf47.png)

<td>

![p1t4](https://user-images.githubusercontent.com/4012178/38852209-30616834-4219-11e8-81db-41fe71f7599e.png)

<td>

![p1t3](https://user-images.githubusercontent.com/4012178/38852252-4f4d178e-4219-11e8-9634-434200922ed0.png)

<td>

![p1t2](https://user-images.githubusercontent.com/4012178/38852212-3200c8d8-4219-11e8-9dad-eb0aa33e1357.png)

</tr></table>

## Getting Started

- You may work on a Linux, Mac or Windows OS with a set of different possible IDEs. The combination Ubuntu, Java 8, Eclipse has worked well. 
- Install Java SE Development Kit (version 8, or above)
- Install Apache Maven
- Install IDE (ideally Eclipse Oxygen or Photon)
- Install GLPK and GLPK for Java (Ensure you install compatible versions, e.g. [here](http://glpk-java.sourceforge.net/gettingStarted.html))
	- Prerequisites are: GCC, Libtool, Swig and Subversion
- Install Git and connect to GitHub with [SSH](https://help.github.com/articles/connecting-to-github-with-ssh/)

The code format of the `amod` repository is specified in the `amodeus` profile that you can import from [amodeus-code-style.xml](https://raw.githubusercontent.com/idsc-frazzoli/amodeus/master/amodeus-code-style.xml).

## Installation guidelines for amod repository

1. Clone amod
2. Import to eclipse as existing maven project (Package Explorer->Import) using the pom.xml in the top folder of this repository.
3. Set up Run Configurations for: (ScenarioPreparer; ScenarioServer; ScenarioViewer), chose the Working Directory to be the top Simulation Folder directory. You can get a sample simulation scenario at http://www.idsc.ethz.ch/research-frazzoli/amodeus.html
4. Adjust the simulation settings in the 3 config files: av.xml for av fleet values (e.g. number vehicles), AmodeusOptions.properties for AMoDeus settings (e.g. max number of people) and config.xml for Matsim settings (e.g. output directory). 
5. Add JAVA VM arguments if necessary, e.g., `-Xmx10000m` to run with 10 GB of RAM and `-Dmatsim.preferLocalDtds=true` to prefer the local Dtds. 
6. Run the `ScenarioPreparer` as a Java application: wait until termination
7. Run the `ScenarioServer` as a Java application: the simulation should run
8. Run the `ScenarioViewer` as a Java application: the visualization of the scenario should open in a separate window

## Gallery

<table><tr>
<td>

![usecase_amodeus](https://user-images.githubusercontent.com/4012178/35968174-668b6e54-0cc3-11e8-9c1b-a3e011fa0600.png)

Zurich

<td>

![p1t5](https://user-images.githubusercontent.com/4012178/38852351-ce176dc6-4219-11e8-93a5-7ad58247e82b.png)

San Francisco

<td>

![San Francisco](https://user-images.githubusercontent.com/4012178/37365948-4ab45794-26ff-11e8-8e2d-ceb1b526e962.png)

San Francisco

</tr></table>

## Docker

Run `docker-compose up` to run the San Fransisco simulation. This will run two services, `aido-host` and `aido-guest`, which will communicate over port `9382`.

The protocol is specified [here](https://github.com/idsc-frazzoli/amod/blob/master/doc/aido-client-protocol.md).
