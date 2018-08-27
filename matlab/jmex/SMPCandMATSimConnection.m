function SMPCandMATSimConnection(server)
% routine is terminated anytime by pressing Ctrl+C

while 1

  socket = jmexWaitForConnection(server)

  while socket.isConnected()
    if socket.hasContainer()

% ======================================
% get data structure from client socket
container = socket.pollContainer();
[InputSMPC] = jmexStruct(container);

% get number of nodes
numberNodes = length(InputSMPC.roadGraph0);

% get planning horizon
T = InputSMPC.PlanningHorizon;
RoadNetwork.T = T;
fprintf('Planning horizon: %d \n', T);

% get current time
currentTime = (InputSMPC.currentTime/60)/5;
if(currentTime > 288)
    currentTime = currentTime - 288;
end
fprintf('current Time: %d \n', currentTime);

% initialize inputs
RoadGraph = cell(1,numberNodes);
travelTimes = zeros(numberNodes,numberNodes);
Starters = zeros(T,numberNodes);

% get struct elements names
InputNames = fieldnames(InputSMPC);

% get network and travel times
for i = 1:1:numberNodes
    RoadGraph{i} = InputSMPC.(InputNames{i})';
    travelTimes(i,:) = InputSMPC.(InputNames{i+numberNodes})';
end

RoadNetwork.RoadGraph = RoadGraph;
RoadNetwork.TravelTimes = travelTimes;

% get number of availaible cars at t
for i = 1:1:T
    Starters(i,:) = InputSMPC.(InputNames{i+2*numberNodes})';
end

RoadNetwork.Starters = Starters;

Passengers.FlowsOut = predictDemandK(currentTime, 0, 0, 0);

Flags.milpflag = 1;

RebWeight = 5.0;

% Optimization!!!!!!!
[rebalanceQueue, output] = SMPC_B3(RoadNetwork, RebWeight, Passengers, Flags);

testque = rebalanceQueue;

for i = 1:1:numberNodes
    if(isempty(testque{i}) == true)
        testque{i} = zeros(1,1);
    end
end

% write reply to client socket
sol = ch.ethz.idsc.jmex.Container('solution');

for i = 1:1:numberNodes
    NodeName = sprintf('solution%d',i);
    sol.add(jmexArray(NodeName,testque{i}));
end

socket.writeContainer(sol)

% ======================================

    else
      pause(.002)
    end
  end

  socket.close()

end
