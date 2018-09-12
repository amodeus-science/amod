function FlowsOut = predictPassengers(currentTime)
	load('forecastFlowsOut.mat'); 
    
    timeStep = currentTime/(60*15);
    flowsout = sprintf('FlowsOut%d',timeStep);
    
	FlowsOut = forecastFlowsOut.(flowsout); 
