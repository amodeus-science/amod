function stochasticCarpoolingMATSimConnector(server)
% routine is terminated anytime by pressing Ctrl+C

while 1

  socket = jmexWaitForConnection(server)

  while socket.isConnected()
    if socket.hasContainer()

% ======================================
% get data structure from client socket
container = socket.pollContainer();
[inputCarpooling] = jmexStruct(container);

reactiveFlag = 1;

% get number of nodes
numberNodes = length(inputCarpooling.roadGraph0);

% get planning horizon
T = inputCarpooling.PlanningHorizon;
currentTime = inputCarpooling.currentTime;
RoadNetwork.T = T;
fprintf('Planning horizon: %d \n', T);

% initialize inputs
RoadGraph = cell(1,numberNodes);
travelTimes = zeros(numberNodes,numberNodes);
rState = zeros(T,numberNodes);
xState = zeros(numberNodes, numberNodes, T);
unassignedRequests = zeros(numberNodes, numberNodes);

% get struct elements names
InputNames = fieldnames(inputCarpooling);

% get network and travel times
for i = 1:1:numberNodes
    RoadGraph{i} = inputCarpooling.(InputNames{i})';
    travelTimes(i,:) = inputCarpooling.(InputNames{i+numberNodes})';
end

Delta_Threshold = 0;

RoadNetwork.RoadGraph = RoadGraph;
RoadNetwork.TravelTimes = travelTimes;
RoadNetwork.Delta_Threshold = Delta_Threshold;


% get number of availaible cars at t
for i = 1:1:T
    rState(i,:) = inputCarpooling.(InputNames{i+2*numberNodes})';
end

for m = 1:1:numberNodes
    for t = 1:1:T
        xState(m,:,t) = inputCarpooling.(InputNames{t + 2*numberNodes + T + (m-1)*T})';
    end
end

Starters.r_state = rState';
Starters.x_state = xState;

RoadNetwork.Starters = Starters;

for i = 1:1:numberNodes
    unassignedRequests(i,:) = inputCarpooling.(InputNames{i + 2*numberNodes + T*numberNodes + T})';
end

FlowsOut = predictPassengers(currentTime);

if(reactiveFlag == 1)
    for t=2:1:T
        FlowsOut(:,:,t) = FlowsOut(:,:,t)*0;
    end
end


FlowsOut(:,:,1) = FlowsOut(:,:,1) + unassignedRequests;

Passengers.FlowsOut = FlowsOut;

use_outpax = 1;
Flags.milpflag = 1;
Flags.ignorerealpax = 1 - use_outpax;
Flags.pooling_flag = 1;

RebWeight = 5.0;

save('Input','RoadNetwork','RebWeight','Passengers','Flags');

% Optimization!!!!!!!
[rebalanceQueue, output] = amod_p_mpc_v11(RoadNetwork, RebWeight, Passengers, Flags);

save('Output','rebalanceQueue','output');

r = rebalanceQueue.r;
x_zo = rebalanceQueue.x_zo;
x_so = rebalanceQueue.x_so;
p_zo = rebalanceQueue.p_zo;
p_so = rebalanceQueue.p_so;

for i = 1:1:numberNodes
    if(isempty(r{i}) == true)
        r{i} = zeros(1,1);
    end
    for j = 1:1:numberNodes
        if(isempty(x_zo{i,j}) == true)
            x_zo{i,j} = zeros(1,1);
        end
        
        if(isempty(x_so{i,j}) == true)
            x_so{i,j} = zeros(1,1);
        end
        
        if(isempty(p_zo{i,j}) == true)
            p_zo{i,j} = zeros(1,1);
        end
        
        if(isempty(p_so{i,j}) == true)
            p_so{i,j} = zeros(1,1);
        end
        
        
    end
    
end

% write reply to client socket
sol = ch.ethz.idsc.jmex.Container('solution');

for i = 1:1:numberNodes
    rNodeName = sprintf('rState%d',i);
    sol.add(jmexArray(rNodeName,r{i}));
    for j = 1:1:numberNodes
        xzoNodeName = sprintf('xzoState%d%d%d',i,0,j);
        sol.add(jmexArray(xzoNodeName,x_zo{i,j}));
        
        xdNodeName = sprintf('xsoState%d%d%d',i,0,j);
        sol.add(jmexArray(xdNodeName,x_so{i,j}));
        
        pzoNodeName = sprintf('pzoState%d%d%d',i,0,j);
        sol.add(jmexArray(pzoNodeName,p_zo{i,j}));
        
        psoNodeName = sprintf('psoState%d%d%d',i,0,j);
        sol.add(jmexArray(psoNodeName,p_so{i,j}));
    end
    
end


socket.writeContainer(sol)

% ======================================

    else
      pause(.002)
    end
  end

  socket.close()
end

