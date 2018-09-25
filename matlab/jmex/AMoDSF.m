% MATSim network builder
% Dejan Milojevic

clear all; close all; clc;

%%
% setenv('GUROBI_HOME','/cluster/apps/gurobi/7.0.2/x86_64');
% setenv('GRB_LICENSE_FILE','/cluster/apps/gurobi/7.0.2/x86_64/gurobi.lic');
% setenv('PYPHONSTARTUP', [getenv('PYPHONSTARTUP') ':/cluster/apps/gurobi/7.0.2/x86_64/lib/gurobi.py']);
% setenv('PATH', [getenv('PATH') ':/cluster/apps/gurobi/7.0.2/x86_64/bin']);
% setenv('LD_LIBRARY_PATH', [getenv('LD_LIBRARY_PATH') ':/cluster/apps/gurobi/7.0.2/x86_64/lib']);
% addpath('/cluster/apps/gurobi/7.0.2/x86_64/matlab');

%% Load data

load('Road.mat');
disp('loaded road');
%load('ManhattanMap_OSM.mat');

%% Define parameters

% number of nodes
n = length(RoadNetwork.RoadGraph);
fprintf('Nodes %d \n',n);
% Creating connectivity matrix
ActiveConnectivitySF = cell(1,n);

for i=1:n
    ActiveConnectivitySF{i} = RoadNetwork.RoadGraph{i};
end

% Getting geographical position of all nodes
Location = RoadNetwork.Location;

% Creating speed matrix between all nodes
Speed = sparse(n,n);

for i=1:n
    nodes = ActiveConnectivitySF{i};
    velocities = RoadNetwork.Velocity{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        Speed(i,node) = velocities(j);
    end
end

% Creating distance matrix between all nodes
LinkLength = sparse(n,n);

for i=1:n
    nodes = ActiveConnectivitySF{i};
    dists = RoadNetwork.Distance{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        LinkLength(i,node) = dists(j);
    end
end

% Creating distance matrix between all nodes
Capacity = sparse(n,n);

for i=1:n
    nodes = ActiveConnectivitySF{i};
    caps = RoadNetwork.Capacity{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        Capacity(i,node) = caps(j);
    end
end

%% Creating travel time matrix between all nodes
TravelTimes = sparse(n,n);

for i=1:n
    nodes = ActiveConnectivitySF{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        TravelTimes(i,node) = LinkLength(i,node)/Speed(i,node);
    end
end

%% Creating energy consumption matrix between all nodes
Energy = sparse(n,n);
m       = 750;  % kg
Afcd    = 0.4;  % m^2
cr      = 0.008;

for i=1:n
    nodes = ActiveConnectivitySF{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        Energy(i,node) = (0.5*1.25*Afcd*Speed(i,node)^2 + cr*m*9.81)*LinkLength(i,node);
    end
end

%% Creating adjacency matrix
A = sparse(n,n);

for i=1:n
    nodes = ActiveConnectivitySF{i};
    for j=1:length(nodes)
        node = nodes(j) + 1;
        A(i,node) = 1;
    end
end

NOut = A*ones(n,1);
NIn = ones(1,n)*A;
N     = NIn*ones(n,1);
fprintf('Links %d \n',N);
%%
iMatrix = A;
iMatrix(1,:) = cumsum(A(1,:)).*A(1,:);
for i = 2:n
    iMatrix(i,:) = (max(iMatrix(i-1,:))+cumsum(A(i,:))).*A(i,:);
end

% save('iMatrix','iMatrix');
% load('iMatrix');
disp('iMatrix loaded');
%%
AT = A';

C = Capacity';
cR = C(:)*2;
cR = cR(AT(:) > 0);
assert(length(cR) == N,...
    'The road capacity matrix does not coincide with the adjacency matrix')

Time = TravelTimes';
t = Time(:);
dt = t(t > 0);
assert(length(dt) == N,...
    'The time matrix does not coincide with the adjacency matrix')

Dist = LinkLength';
ds = Dist(:);
ds = ds(t > 0);
assert(length(ds) == N,...
    'The distance matrix does not coincide with the adjacency matrix')

% Get congestion time
dtauCR = dt./(cR);

% Get road energy matrix
EnergyR = Energy';
eR = EnergyR(:);
eR = eR(eR > 0);
assert(length(eR) == N,...
    'The road energy matrix does not coincide with the adjacency matrix')

% Get in and out nodes in vector
iNodeIn = cell(n,1);
iNodeOut = cell(n,1);
for i = 1:n
    % Check where out-arcs go
    iNodeOut{i}    = unique(iMatrix(i,:));
    % Delete the zero
    if iNodeOut{i}(1) == 0
        iNodeOut{i} = iNodeOut{i}(2:end);
    end
    % Check where in-arcs come from
    iNodeIn{i}     = unique(iMatrix(:,i)');
    
    % Delete the zero
    if iNodeIn{i}(1) == 0
        iNodeIn{i} = iNodeIn{i}(2:end);
    end
end

% Identify Road Arcs
IRoad = ones(N,1);
counter = 1;
for i = 1:n
    for j = 1:NOut(i)
        IRoad(counter) = iNodeOut{i}(j);
        counter = counter + 1;
    end
end

% Create OP Matrices
% Customers Balance
AIn     = sparse(n,N);
AOut    = sparse(n,N);
for i = 1:n
    AIn(i,iNodeIn{i}) = 1;
    AOut(i,iNodeOut{i}) = 1;
end

A0In     = sparse(n,N);
A0Out    = sparse(n,N);
for i = 1:n
    A0In(i,iNodeIn{i}) = 1;
    A0Out(i,iNodeOut{i}) = 1;
end

% Flows on Road
AcR = sparse(N,N);
for k = 1:N
    AcR(k,IRoad(k)) = 1;
end
% f0 + AcR*fm*ones(M,1) <= cR

%% Requests
UniqueRequests = unique(RoadNetwork.Requests,'rows');
Stations = unique(UniqueRequests);
numberStations = length(Stations);
numberRequests = length(UniqueRequests);

Location = RoadNetwork.Location;

LocationStations = zeros(numberStations,2);

for i=1:numberStations
   LocationStations(i,:) = Location(Stations(i)+1,:);
end

[idx,Centroids] = kmeans(LocationStations,100);

numberClusters = length(Centroids);

Flow = sparse(numberClusters,numberClusters);

for i=1:length(RoadNetwork.Requests)
   k = idx(find(Stations == RoadNetwork.Requests(i,1)));
   j = idx(find(Stations == RoadNetwork.Requests(i,2)));
   Flow(k,j) = Flow(k,j) +1;
end
F=sum(sum(Flow));
Flow = Flow/(2*3600);

for i = 1:numberClusters
    Flow(i,i) = 0;
end
Sources = [];
Sinks = [];
FlowsIn = [];
c = 1;
for i = 1:numberClusters
    StationCluster = find(idx==i);
    stat = Stations(StationCluster(1));
    for j = 1:numberClusters
        if j ~= i && Flow(i,j) > 0
            Sources(c,1) = stat;
            ClusterDestination = find(idx==j);
            statDest = Stations(ClusterDestination(1));
            Sinks(c,1) = statDest;
            FlowsIn(c,1) = Flow(i,j);
            c = c+1;
        end
    end
end


oR = Sources;
dR = Sinks;
aR = FlowsIn;

dBundle = unique(dR);
M       = length(dBundle);
% Define source and sink flows with location
aIn     = zeros(n,M);
aOut    = zeros(n,M);
for b = 1:M
    % Demands on Walking Nodes
    % Get flows with locations corresponding to the b-th bundle
    aIn(oR(dBundle(b) == dR),b) = aR(dBundle(b) == dR);
    % Get sink flow with location corresponding to the b-th bundle
    aOut(dBundle(b),b)          = sum(aR(dBundle(b) == dR));
end

% M = length(FlowsIn);
% aIn     = zeros(n,M);
% aOut    = zeros(n,M);
% 
% for m = 1:M
%     % Demands on Walking Nodes
%     % Get flows with locations corresponding to the m-th request
%     aIn(oR(m)+1,m)    = aR(m);
%     % Get sink flow with location corresponding to the m-th request
%     aOut(dR(m)+1,m)   = aR(m);
% end
disp('Request prepared');



%% Costs
% Tank-to-miles efficiency
etaEV   = 0.72;

% Energy-CO2 Data - NYC - www.eia.gov
eCH4    = 3255;
eGreen  = 6780;
rCH4    = eCH4/(eCH4+eGreen);
etaCH4  = 0.91*0.55;
HCH4    = 50e6;   % LHV in MJ/kg
nuCH4   = 2.75; % CO2 emitted per CH4 burned
fCO2_E  = rCH4*nuCH4/(etaCH4*HCH4)*1e3; % kgCO2/kJ
fCO2_E2 = 1105*0.453/3600/1000; % kg CO2/kJ from api.watttime.org

% Value of Time
fUSD_T  = 24.40/3600;       % USD/s
% Operational Cost per Distance
fUSD_DR = 0.486/1609.34;    % USD/m
% Energy Cost
fUSD_E  = 0.24736/3600;     % USD/kJ
% CO2 Cost
fUSD_CO2 = 0.1; % USD/kg

CostCustomers   = fUSD_T*dt' + (fUSD_DR*ds' + 1/etaEV*fUSD_E*eR')*AcR;
CostRebalancing = fUSD_DR*ds' + 1/etaEV*fUSD_E*eR';

disp('Costs defined');
%% solve

f0 = sdpvar(N,1);
fm = sdpvar(N,M);
disp('Optimization variables defined');
%%
%clearvars -except M f0 fm AIn aIn AOut aOut AcR cR A0In A0Out fUSD_T dt fUSD_DR ds fUSD_E eR etaEV
%%
ConstraintsCustomers_cell   = cell(M,1);
for m = 1:M
    ConstraintsCustomers_cell{m} = ...
        TagConstraintIfNonEmpty(AIn*fm(:,m) + aIn(:,m) ==...
        AOut*fm(:,m) + aOut(:,m),...
        sprintf('Customers Flow %d',m));
end
disp('Customer constraints defined');
%%
ConstraintsCustomers_array = vertcat(ConstraintsCustomers_cell{:});
%%
rC = 0;
ConstraintsRCapacity    = TagConstraintIfNonEmpty(f0 + AcR*fm*ones(M,1)    <= cR*(1-rC),'Road Capacity');
disp('Capacity constraints defined');
%%
ConstraintsRebalancing = TagConstraintIfNonEmpty(A0In*f0 + AIn*fm*ones(M,1) ==...
    A0Out*f0 + AOut*fm*ones(M,1),...
    'Cars Flow');
disp('Rebalancing constraints defined');
%%
ConstraintsPositive     = TagConstraintIfNonEmpty([f0; fm(:)]>=0,'Positive');
disp('Positive constraints defined');
%%
Constraints = [
    ConstraintsCustomers_array;
    ConstraintsRebalancing;
    ConstraintsRCapacity;
    ConstraintsPositive
    ];
disp('Bundle Constraints');
%%
%ConstraintsIAMoD.Constraints = Constraints;
% ConstraintsIAMoD.ConstraintsCustomers_cell = ConstraintsCustomers_cell;
% ConstraintsIAMoD.ConstraintsCustomers_array = ConstraintsCustomers_array;
% ConstraintsIAMoD.ConstraintsRCapacity = ConstraintsRCapacity;
% ConstraintsIAMoD.ConstraintsRebalancing = ConstraintsRebalancing;
% ConstraintsIAMoD.ConstraintsPositive = ConstraintsPositive;
%save('ConstraintsIAMoD','ConstraintsIAMoD');
%%
%clearvars ConstraintsCustomers_array ConstraintsCustomers_cell ConstraintsPositive ConstraintsRCapacity ConstraintsRebalancing ConstraintsIAMoD A0In A0Out aIn AIn aOut AOut cR m rC
%%
Objective = (fUSD_T*dt'*fm*ones(M,1) + fUSD_DR*ds'*(f0+AcR*fm*ones(M,1)) + ...
    + fUSD_E*(eR'*(f0+AcR*fm*ones(M,1))/etaEV))...  % Cost of Electricity for AMoD
    %+ 0*(eR'*(f0+AcR*fm*ones(M,1))/etaEV)*fCO2_E*fUSD_CO2... % Cost of CO2 set to 0!
    + 1e-4*(sum(fm*ones(M,1))+sum(f0)) + 1e-4*(fm(:)'*fm(:) + f0'*f0);
disp('Objective function defined');
%%
solver      = 'gurobi';
options = sdpsettings('verbose',1,'solver',solver);
options.gurobi.QCPDual = 1;
options.gurobi.Crossover = 0;
options.gurobi.CrossoverBasis = 0;
options.mosek.MSK_IPAR_INTPNT_BASIS = 'MSK_BI_NEVER';
options.gurobi.Method = 2; % 2 = barrier
disp('Optimization options');
disp('Starting optimization');
sol = optimize(Constraints,Objective,options)
disp('Finished optimization');
%%
check(Constraints)
% Save
sol.objective = value(Objective);
sol.fmOpt   = value(fm);
sol.f0Opt   = value(f0);

disp('finished');
%%
%%
origin = zeros(1,N);
destination = zeros(1,N);
count = 0;

for i=1:n
    dest = find(A(i,:));
    for j=1:length(dest)       
        origin(i+count) = i;
        destination(i+count) = dest(j);
        count = count + 1;
    end
    count = count - 1;
end

G = graph(origin,destination,dual(Constraints(12)));

%%
sol.TOpt    = dt'*sol.fmOpt*ones(M,1)/sum(sum(aOut));
sol.TRoad   = dt'*AcR'*(AcR*sol.fmOpt*ones(M,1))/sum(sum(aOut));
     
sol.DOpt    = ds'*sol.fmOpt*ones(M,1)/sum(sum(aOut));
sol.DROpt   = ds'*(AcR*sol.fmOpt*ones(M,1)+sol.f0Opt)/sum(sum(aOut));
sol.DRoad   = ds'*(AcR*sol.fmOpt*ones(M,1))/sum(sum(aOut)); 
    
sol.EOpt    = (eR'*(sol.f0Opt+sol.fmOpt(IRoad,:)*ones(M,1))/etaEV)/sum(sum(aOut));
sol.NCars   = dt'*(sol.f0Opt + AcR*sol.fmOpt*ones(M,1));
sol.RCong   = (cR*rC + sol.f0Opt + AcR*sol.fmOpt*ones(M,1))./cR;
sol.RCongAvg    = mean(sol.RCong);
sol.RCongAbs    = sum((cR*rC + sol.f0Opt + AcR*sol.fmOpt*ones(M,1))./cR>= 0.999)/length(cR); % Congested roads
sol.PigRoad = dual(Constraints('Road Capacity'));
sol.USD     = sol.TOpt*fUSD_T + sol.DROpt*fUSD_DR;
sol.MCO2    = sol.EOpt*fCO2_E;

%%
save('solution','sol');
disp('finished');

fmOpt = sol.fmOpt;
f0Opt = sol.f0Opt;
TOpt  = sol.TOpt;
EOpt  = sol.EOpt;

TOptTot  = sol.TOpt;
EOptTot  = sol.EOpt;
NCarsTot = sol.NCars;
RCongTot = sol.RCongAvg;
TRoad    = sol.TRoad;
DRoad    = sol.DRoad;
ObjTot   = sol.objective;
RegTot   = 1e-4*(sum(sol.fmOpt*ones(M,1))+sum(sol.f0Opt));
%%
ColourMap = lines;
roadLevel = 0.2;
ATOL = 1e-4;

%%
% figure('Name','Flow Map'); hold on; box on
% % Plot Road
%     for r = 1:size(A,1)
%         plot3(RoadNetwork.Location(r,2),RoadNetwork.Location(r,1),roadLevel,'.k','MarkerSize',10);
%         for j = find(A(r,:)==1)
%             quiver3(RoadNetwork.Location(r, 2),RoadNetwork.Location(r, 1),roadLevel,...
%                 diff(RoadNetwork.Location([r; j], 2)),diff(RoadNetwork.Location([r; j], 1)),0,0,':k')
%         end
%     end