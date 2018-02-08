# amodeus
AMoD test bed

TODO list (not comprehensive):


2) Streamline the viewer and remove some parts.

4) Include an empty dispatcher on avian itself (not aviantools)
5) Include the SF Scenario and all needed parts. 
6) Do the homepage including the ranking.
7) in aviantools remove all notions of AV and replace with RoboTaxi
8) Document the code
9) Add authorship and licensing information everywhere. 
10) Add maven testing
11) Thorough refactoring of avian, aviantools, av --> old TODOS completed.
12) finish the paper. 

- Solve the problem, that GLPK highly limits the easiness of use and isntallation. New solver? Try- catch and message to user? ... 
- Problem: Current implemntation of Hungarian matching too slow to allow many iterations. Go from o(n3) to o(n) implementation. 
- all input settings into one file (av_config.xml, av.xml, IDSCoptions)
- make one superfile "amodeus Start" with input options and output, input: pop-size, dispatcher, ... all in one config object,  output visualizer, report and data 



-- hand-out and publication --

- skim down existing matsim repo and feed with aviantools code wehere meaningful
- rename matsim repo as aviantools internal

