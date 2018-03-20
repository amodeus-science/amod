# ch.ethz.idsc.amod <a href="https://travis-ci.org/idsc-frazzoli/amod"><img src="https://travis-ci.org/idsc-frazzoli/amod.svg?branch=master" alt="Build Status"></a>

The code format of the `amod` repository is specified in the `amodeus` profile that you can import from `amodeus-code-style.xml` in the the `amodeus` repository.





Sample usage of amodeus, the autonomous mobility-on-demand simulation library, version `1.0.0`
(https://github.com/idsc-frazzoli/amodeus)

## Purpose

This library offers a basic code implementation that allows to run an autonomous mobility-on-demand scenario with the amodeus library.

Try it, orchestrate your own fleet of amod-taxis!

## Getting Started


- You may work on a Linux, Mac or Windows OS with a set of different possible IDEs. The combination Ubuntu, Eclipse has worked well. 
- Install Java SE Development Kit X (preferably most recent) 
- Install Apache Maven
- Install IDE (Ideally Eclipse)
- Install GLPK and GLPK for Java (Ensure your install compatible versions)


## Installation guidelines for amod repository

1. Clone amod
2. Import to eclipse as existing maven project using the pom.xml in the top folder.
3. Set up Run Configurations for: (ScenarioPreparer; ScenarioServer; ScenarioViewer), chose the Working Directory to be the top Simulation Folder directory.
4. Add JAVA VM arguments if necessary, e.g., -Xmx10000m to run with 10 GB of RAM. 
5. Run the ScenarioPreparer main Function
6. Run the ScenarioServer main Function, the simulation should run.
7. Run the ScenarioViewer main Function, a viewer should open that allows visualization of the scenario.




## Gallery

<table>
<tr>
<td>

![usecase_amodeus](https://user-images.githubusercontent.com/4012178/35968174-668b6e54-0cc3-11e8-9c1b-a3e011fa0600.png)

Zurich

<td>

![San Francisco](https://user-images.githubusercontent.com/4012178/37365948-4ab45794-26ff-11e8-8e2d-ceb1b526e962.png)

San Francisco

</table>

## Integration

Specify `repository` and `dependency` of the amodeus library in the `pom.xml` file of your maven project:

    <repositories>
      <repository>
        <id>amodeus-mvn-repo</id>
        <url>https://raw.github.com/idsc-frazzoli/amodeus/mvn-repo/</url>
        <snapshots>
          <enabled>true</enabled>
          <updatePolicy>always</updatePolicy>
        </snapshots>
      </repository>
    </repositories>
    
    <dependencies>
      <dependency>
        <groupId>ch.ethz.idsc</groupId>
        <artifactId>amodeus</artifactId>
        <version>1.0.0</version>
      </dependency>
    </dependencies>

The source code is attached to every release.










