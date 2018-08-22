function [outputArg1,outputArg2] = carpoolingMATSimConnector(server)
% routine is terminated anytime by pressing Ctrl+C

while 1

  socket = jmexWaitForConnection(server)

  while socket.isConnected()
    if socket.hasContainer()

% ======================================
% get data structure from client socket
container = socket.pollContainer();
[inputCarpooling] = jmexStruct(container);

% get number of nodes
numberNodes = length(inputCarpooling.roadGraph0);

% get planning horizon
T = inputCarpooling.PlanningHorizon;
RoadNetwork.T = T;
fprintf('Planning horizon: %d \n', T);

% initialize inputs
RoadGraph = cell(1,numberNodes);
travelTimes = zeros(numberNodes,numberNodes);
rState = zeros(T,numberNodes);
xState = zeros(numberNodes, numberNodes, T);
FlowsOut = zeros(numberNodes, numberNodes, T);

% get struct elements names
InputNames = fieldnames(inputCarpooling);

% get network and travel times
for i = 1:1:numberNodes
    RoadGraph{i} = inputCarpooling.(InputNames{i})';
    travelTimes(i,:) = inputCarpooling.(InputNames{i+numberNodes})';
end

RoadNetwork.RoadGraph = RoadGraph;
RoadNetwork.TravelTimes = travelTimes;

% get number of availaible cars at t
for i = 1:1:T
    rState(i,:) = inputCarpooling.(InputNames{i+2*numberNodes})';
end

for m = 1:1:numberNodes
    for t = 1:1:T
        xState(m,:,t) = inputCarpooling.(InputNames{t + 2*numberNodes + T + (m-1)*T});
    end
end

Starters.rState = rState';
Starters.xState = xState;

RoadNetwork.Starters = Starters;

for t = 1:1:T
    for i = 1:1:T
        FlowsOut(i,:,t) = inputCarpooling.(InputNames{i + 2*numberNodes + T*numberNodes + T + (t-1)*numberNodes})';
    end
end

Passengers.FlowsOut = FlowsOut;

Flags.milpflag = 0;

RebWeight = 5.0;

% Optimization!!!!!!!

r = cell(numberNodes,1);
x_zo = cell(numberNodes,numberNodes);
x_do = cell(numberNodes,numberNodes);
p_zo = cell(numberNodes,numberNodes);
p_so = cell(numberNodes,numberNodes);



for i = 1:1:numberNodes
    r{i} = randperm(numberNodes);
    for j = 1:1:numberNodes
       x_zo{i,j} = randperm(numberNodes); 
       x_do{i,j} = randperm(numberNodes); 
       p_zo{i,j} = randperm(numberNodes); 
       p_so{i,j} = randperm(numberNodes); 
    end
end

% write reply to client socket
sol = ch.ethz.idsc.jmex.Container('solution');

for i = 1:1:numberNodes
    NodeName = sprintf('rState%d',i);
    sol.add(jmexArray(NodeName,r{i}));
end

for i = 1:1:numberNodes
    for j = 1:1:numberNodes
        NodeName = sprintf('xzoState%d%d',i,j);
        sol.add(jmexArray(NodeName,x_zo{i,j}));
    end
    
end

for i = 1:1:numberNodes
    for j = 1:1:numberNodes
        NodeName = sprintf('xdoState%d%d',i,j);
        sol.add(jmexArray(NodeName,x_do{i,j}));
    end
    
end

for i = 1:1:numberNodes
    for j = 1:1:numberNodes
        NodeName = sprintf('pzoState%d%d',i,j);
        sol.add(jmexArray(NodeName,p_zo{i,j}));
    end
    
end

for i = 1:1:numberNodes
    for j = 1:1:numberNodes
        NodeName = sprintf('psoState%d%d',i,j);
        sol.add(jmexArray(NodeName,p_so{i,j}));
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

