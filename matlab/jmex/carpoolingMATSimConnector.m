function carpoolingMATSimConnector(server)
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

for t = 1:1:T
    for i = 1:1:numberNodes
        FlowsOut(i,:,t) = inputCarpooling.(InputNames{i + 2*numberNodes + T*numberNodes + T + (t-1)*numberNodes})';
    end
end

Passengers.FlowsOut = FlowsOut;

use_outpax = 1;
Flags.milpflag = 1;
Flags.ignorerealpax = 1 - use_outpax;

RebWeight = 5.0;

% flagTest = 0;
% global deki1;
% 
% if(isempty(deki1))
%     deki1 = 0;
% end
% 
% cont = zeros(1,2);
% contsec = zeros(1,2);
% if(sum(sum(FlowsOut(:,:,1)))~=0 && deki1==0)
%     [row,column] = find(FlowsOut(:,:,1));
%     for(i=1:1:length(row))
%         if(row(i) ~= column(i))
%             cont = [column(i),row(i)];
%             break
%         end
%             
%     end
%     
%     for(i=1:1:length(row))
%         if(row(i) ~= cont(2) && column(i)~=cont(1))
%             contsec(1) = column(i);
%             contsec(2) = row(i);
%             break
%         end
%             
%     end
%     flagTest = 1;
%     deki1 = 1;
% end

% global int;
% if(isempty(int))
%    int = 0; 
% end
% 
% save(sprintf('Input%d',int),'RoadNetwork','RebWeight','Passengers','Flags');
% int = int +1;

save('Input','RoadNetwork','RebWeight','Passengers','Flags');

% Optimization!!!!!!!
[rebalanceQueue, output] = amod_p_mpc_v8(RoadNetwork, RebWeight, Passengers, Flags);

% global out;
% if(isempty(out))
%    out = 0; 
% end
% 
% save(sprintf('Output%d',out),'rebalanceQueue','output');
% 
% out = out + 1;

save('Output','rebalanceQueue','output');

r = rebalanceQueue.r;
x_zo = rebalanceQueue.x_zo;
x_so = rebalanceQueue.x_so;
p_zo = rebalanceQueue.p_zo;
p_so = rebalanceQueue.p_so;

% r = cell(numberNodes,1);
% x_zo = cell(numberNodes,numberNodes);
% x_so = cell(numberNodes,numberNodes);
% p_zo = cell(numberNodes,numberNodes);
% p_so = cell(numberNodes,numberNodes);

% if(flagTest == 1)
%     x_zo{cont(1),cont(2)} = [0, 0, 0, contsec(2)];
%     
% end
% 
% global state;
% global dest1;
% global dest2;
% 
% if(contsec(2)~=0 && cont(1)~=0 && contsec(1)~=0)
%     state = contsec(2);
%     dest1 = cont(1);
%     dest2 = contsec(1);
% end
% 
% if(~isempty(state))
%     if(state ~= 0 && dest1 ~= 0 && dest2~=0)
%      x_so{2,3} = [0, 0, 0, 7];
%     end
% end

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

% save('InputOutput.mat','RoadNetwork','RebWeight','Passengers','Flags','rebalanceQueue','output');

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

