function flowsOutMATSimConnector(server)
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
numberNodes = length(inputCarpooling.flowsOut000);

% get planning horizon
T = inputCarpooling.PlanningHorizon;
fprintf('Planning horizon: %d \n', T);

% initialize inputs
FlowsOut = zeros(numberNodes, numberNodes, T);

% get struct elements names
InputNames = fieldnames(inputCarpooling);


for t = 1:1:T
    for i = 1:1:numberNodes
        FlowsOut(i,:,t) = inputCarpooling.(InputNames{i + (t-1)*numberNodes})';
    end
end

global flows;

if isempty(flows)
    flows = 0;
end

flows = flows + 1;

flowsout = sprintf('FlowsOut%d',flows);

if flows > 1
    load Passengers0609;
end

Passengers0609.(flowsout) = FlowsOut;
save('Passengers0609','Passengers0609');

sol = ch.ethz.idsc.jmex.Container('solution');

socket.writeContainer(sol)

% ======================================

    else
      pause(.002)
    end
  end

  socket.close()
end

