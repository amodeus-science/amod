# ch.ethz.idsc.amod

The code format of the `amod` repository is specified in the `amodeus` profile that you can import from `amodeus-code-style.xml` in the the `amodeus` repository.


TODO list (not comprehensive):


## CODE
0) ensure maven tests are working properly
1) Eliminate all TODOs, partly completed.
2) Include the SF Scenario and all needed parts, included, but still problem with distances to be solved by Claudio, initial scneario based on 100 taxi traces completed. 
3) Remove all notions of AV and replace with robotaxi, partly completed.
4) Add authorship and licensing information everywhere. CLAUDIO
5) Add maven testing, partly completed, not running on all machines, e.g., on Claudio's machine. Should run on all machines. 
5) Solve GLPK problem: GLPK highly limits the easiness of use and installation. New solver? Try- catch and message to user? ...
6) Ensure that 3rd party code is labelled, used corectly. 
7) Move all input settings to one place: av_config.xml, av.xml, IDSCoptions
8) Make one superfile "amod Start" with input options and output, input: pop-size, dispatcher, ... all in one config object,  output visualizer, report and data 
9) More commenting and Javadoc, code documentation.
10) Remove extra code from amodeustools, partly completed.

## API
1) Do the homepage including the ranking.
2) Provide basic installation guidelines
3) Make public for test customers (or add them to github)


## PUBLICATION
1) Finish the paper. 


## INTEGRATION
1) Claudio: integrate local files into amodidsc, started
2) For as many pieces as meaningful: test in amodeus, then make sure tests are written in amodeus, delete from matsim and add references to amodeus, started


-

## COMPLETED
1) Make matsim private and conserve history