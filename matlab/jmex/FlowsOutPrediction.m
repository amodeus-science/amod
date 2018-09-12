clear all; close all; clc;

load('Passengers0520.mat'); load('Passengers0521.mat'); load('Passengers0522.mat');
load('Passengers0523.mat'); load('Passengers0526.mat'); load('Passengers0527.mat');
load('Passengers0528.mat'); load('Passengers0529.mat'); load('Passengers0530.mat');
load('Passengers0602.mat'); load('Passengers0603.mat'); load('Passengers0604.mat');
load('Passengers0605.mat'); load('Passengers0606.mat'); load('Passengers0609.mat');

for i=1:1:96
    flowsout = sprintf('FlowsOut%d',i);
    FlowsOut = Passengers0520.(flowsout) + Passengers0521.(flowsout) + Passengers0522.(flowsout) + ...
        Passengers0523.(flowsout) + Passengers0526.(flowsout) + Passengers0527.(flowsout) + ...
        Passengers0528.(flowsout) + Passengers0529.(flowsout) + Passengers0530.(flowsout) + ...
        Passengers0602.(flowsout) + Passengers0603.(flowsout) + Passengers0604.(flowsout) + ...
        Passengers0605.(flowsout) + Passengers0606.(flowsout) + Passengers0609.(flowsout);
    
    FlowsOut = FlowsOut * (1/15);
    FlowsOut = round(FlowsOut);
    forecastFlowsOut.(flowsout) = FlowsOut;
end

save('forecastFlowsOut','forecastFlowsOut');
