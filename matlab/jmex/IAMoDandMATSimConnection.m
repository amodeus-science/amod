function SMPCandMATSimConnection(server)
% routine is terminated anytime by pressing Ctrl+C

while 1

  socket = jmexWaitForConnection(server)

  while socket.isConnected()
    if socket.hasContainer()

% ======================================
% get data structure from client socket
container = socket.pollContainer();
[InputIAMoD] = jmexStruct(container);

% get struct elements names
InputNames = fieldnames(InputIAMoD);

if(strcmp(InputNames{1},'Disutility'))
    fromNode = InputIAMoD.FromNode + 1;
    toNode = InputIAMoD.ToNode + 1;
    load('iMatrix');
    link = iMatrix(fromNode,toNode);
    load('solution.mat');
    toll = sol.PigRoad(link);
    solJava = ch.ethz.idsc.jmex.Container('solution');
    solJava.add(jmexArray('solution',toll));
    socket.writeContainer(sol)
    
end

if(strcmp(InputNames{1},'numberNodes'))
    numberNodes = InputIAMoD.numberNodes;

    % initialize inputs
    RoadGraph = cell(1,numberNodes);
    Distance = cell(1,numberNodes);
    Velocity = cell(1,numberNodes);
    Capacity = cell(1,numberNodes);

    % get network and travel times
    for i = 1:1:numberNodes
        RoadGraph{i} = InputIAMoD.(InputNames{i+1})';
        Distance{i} = InputIAMoD.(InputNames{i+1+numberNodes})';
        Velocity{i} = InputIAMoD.(InputNames{i+1+2*numberNodes})';
        Capacity{i} = InputIAMoD.(InputNames{i+1+3*numberNodes})';
    end

    RoadNetwork.RoadGraph = RoadGraph;
    RoadNetwork.Distance = Distance;
    RoadNetwork.Velocity = Velocity;
    RoadNetwork.Capacity = Capacity;

    Location = zeros(numberNodes,2);

    for i = 1:1:numberNodes
       Location(i,:) = InputIAMoD.(InputNames{i+1+4*numberNodes})';
    end

    RoadNetwork.Location = Location;

    numberRequests = InputIAMoD.numberRequests;
    Requests = zeros(numberRequests,2);

    for i = 1:1:numberRequests
       Requests(i,:) = InputIAMoD.(InputNames{i+1+5*numberNodes})';
    end

    RoadNetwork.Requests = Requests;

    save('Road','RoadNetwork');
    % write reply to client socket
    sol = ch.ethz.idsc.jmex.Container('solution');
    testque = [1, 2];

    sol.add(jmexArray('solution',testque));

    % for i = 1:1:numberNodes
    %     NodeName = sprintf('solution%d',i);
    %     sol.add(jmexArray(NodeName,testque));
    % end

    socket.writeContainer(sol)
end


% ======================================

    else
      pause(.002)
    end
  end

  socket.close()

end
