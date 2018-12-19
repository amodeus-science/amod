function [rebalanceQueue, output] = amod_p_mpc_remote_v1(RoadNetwork, RebWeight, Passengers, Flags)
%{
README:
    INPUTS:
    - RoadNetwork is a struct with 4 fields.
        * T (int) is the planning horizon in multiples of 5 minutes. For example,
        if T is 12, then the planning horizon is 1 hour. 
        * Roadgraph (N length cell) is an adjacency list representation of 
        the graph. For example, Roadgraph{i} is the edge list of station i.
        * TravelTimes (NxN array) specifies the time required to travel
        between any pair of stations in multiples of 5 minutes. 
        * Starters is a struct with 3 fields
            ** r_state (NxT array) : r_state(i,t) specifies the number of vehicles that will
            become vacant in station i at time t. In particular, for t=1,
            this is just the number of vehicles currently available at
            station i. 
            ** x_state (NxN array) : x(m,i,t) specifies the number of vehicles
            at station i carrying one passenger whose final destination is
            station m that are available at time t. In particular, for t=1,
            this is just the total number of such vehicles available now.

            (we don't need to keep track of p, because p only lives on edges)

    - RebWeight (float) is the cost per unit distance of moving a vehicle.
    - Passengers is a struct with only 1 field.
        * FlowsOut (NxNxT) is a 3D array of integers specifying forecasted
        demand. The 3 indices represent (origin, destination, time) and the
        value is the forecasted number of people requested that trip type. 
    - Flags is a struct with two fields.
        * milpflag (bool) specifies whether we want to enforce integer constraints
        or relax them.
        * ignorerealpax (bool) specifies whether there are outstanding customers
        or not. 
        * pooling_flag (bool) specifies whether to use carpooling. 
%}

%% Unpack things

T=RoadNetwork.T;
RoadGraph=RoadNetwork.RoadGraph;
TravelTimes=RoadNetwork.TravelTimes;
Starters=RoadNetwork.Starters; % Starters(i) gives the number of available vehs at t=1

FlowsOut = Passengers.FlowsOut;

milpflag=Flags.milpflag;
ignorerealpax = Flags.ignorerealpax;
N=length(RoadGraph);

%% Assuming roadgraph to be fully connected. 

%% parameters

dialogflag = 1; % prints diagnostics

SourceRelaxCost=1e6; % The cost of dropping a source or sink altogether
CustomerTransitCost=1e5/T; % the cost of having a customer wait while in the car (i.e. transit time)
WaitTimeCost = SourceRelaxCost / T; %The cost per unit of time letting customers wait.



%% Indexing functions

% true state variables 

% zo = zero occupancy (origin, destination, time) indexing
r_flow = @(i,j,t) (t-1)*N*N + (i-1)*N + j; 
% so = single occupancy (goal, origin, destination, time) indexing
x_flow = @(i,j,t) N*N*T + (t-1)*N*N + (i-1)*N + j;
% dropped customers (origin, destination, time) indexing
find_drop = @(i,j,t) 2*N*N*T + (t-1)*N*N + (i-1)*N + j;
% find the number of i->j trips are serviced at time t. 
find_served = @(i,j,t) 3*N*N*T + (t-1)*N*N + (i-1)*N + j;

% state size determined here

statesize = 4*N*N*T;

if (dialogflag == 1)
    tic % measure time for preprocessing and building the problem 
    fprintf('StateSize: %d \n', statesize)
end

%% build cost vector
f = zeros(statesize,1);

for t=1:T
    for i=1:N
        for j=1:N
            if (i == j)
                f(r_flow(i,j,t)) = 0.7*RebWeight*TravelTimes(i,j);
            else
                f(r_flow(i,j,t)) = RebWeight*TravelTimes(i,j);
            end
            % f(find_wait(i,j,t)) = WaitTimeCost*t;
            f(find_drop(i,j,t)) = SourceRelaxCost;
            
            % only pay for the first leg of the trip because the flow
            % type will change when the car drops off first cust.
            f(x_flow(i,j,t)) = CustomerTransitCost*TravelTimes(i,j);
        end
    end
end

if (dialogflag == 1)
    fprintf('Cost function built. \n')
end
%% build equality constraints

                 %vehicle conservation      delayed customer        delay cost
num_eq_constr =  N*T                      + N*N*T                +  N*N*T; % 
num_eq_entries = N*T*(4*N)                + N*N*T*1              + (N*N*T+ ceil(N*N*T*(T+1)*0.5));% 

Aeqsparse = zeros(num_eq_entries, 3);
Beq = zeros(num_eq_constr,1);
Aeqrow = 0;
Aeqentry = 0;

for t=1:T
    for i=1:N
    % zero occupancy outflow equation
        Aeqrow = Aeqrow + 1; % allocate a row for this constraint.
        for j=1:N
            if (t > TravelTimes(j,i)) % total number of vacant vehicles we have in station i at time t. 
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, r_flow(j,i,t-TravelTimes(j,i)), -1]; % check how many vehicles are coming in as vacant.
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_flow(j,i,t-TravelTimes(j,i)), -1]; % singly occupied vehicles dropping off their customer and becoming vacant.
            end
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, r_flow(i,j,t), 1]; % cars that will leave empty
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, x_zo_flow(i,j,t), 1]; % cars that will pick up one customer
        end
        Beq(Aeqrow) = Starters.r_state(i,t); % specify the influx of vehicles. t=1 is the initial condition.
    end
end

% dropped customer equation
for t=1:T
    for i=1:N
        for j=1:N
            Aeqrow = Aeqrow + 1; % start a new line for this constraint. 
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, find_served(i,j,t), -1]; % total customer served 
            
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, x_zo_flow(j,i,t), 1]; % cars carrying one customer, of the 1st kind. 
            
        end
    end
end

% clear everything... this should make things work.
Aeqsparse_part1 = Aeqsparse(1:Aeqentry,:);
Aeqsparse = zeros(num_eq_entries, 3);
Aeqentry = 0;


% calculate the total amount of time (in units of "5 minutes") that
% customers are forced to wait. 
for t=1:T
    for i=1:N
        for j=1:N
            Aeqrow = Aeqrow + 1; % start a new line for this constraint. 
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry, :) = [Aeqrow, find_drop(i,j,t), 1]; % the number of i->j customers delayed at time t
            for tau=1:t
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry, :) = [Aeqrow, find_served(i,j,tau), 1]; % total number of i-> j customers serviced up to time t.
            end
            Beq(Aeqrow) = sum(FlowsOut(i,j,1:t)); % total number of i->j requests up until time t. 
        end
    end
end


if (dialogflag == 1)
    fprintf('Equality constraints specified. %d constraints, %d expected. \n', Aeqrow, num_eq_constr)
end

%% build inequality constraints

% no extra inequality constraints this time.

%% build matrices from equality and inequality constraints. 

Aeqsparse = Aeqsparse(1:Aeqentry,:); % truncate to only include non-zero entries.
disp('Stacking...')
Aeqsparse = [Aeqsparse_part1;Aeqsparse]; % stack things together...
disp('Done!')
Aeq = sparse(Aeqsparse(:,1), Aeqsparse(:,2), Aeqsparse(:,3), Aeqrow, statesize); % construct the sparse matrix. 

if (dialogflag == 1)
    fprintf('Aeq built. \n')
end

%% Upper and lower bounds
if (dialogflag)
    disp('Building upper and lower bounds')
end

lb=zeros(statesize,1); %everything is non-negative
ub=Inf*ones(statesize,1); %no constraints

%% Setting variable types

if (dialogflag == 1)
    disp('Setting variable type.')
end    
ConstrType=char(zeros(1,statesize));
if (milpflag == 1)
    % only demand that the first timestep is integer.
    ConstrType(1:end)='C';
    for i=1:N
        for j=1:N
            ConstrType(r_flow(i,j,1)) = 'I';
            ConstrType(x_flow(i,j,1)) = 'I';
        end
    end                
else
    ConstrType(1:end)='C';
end

%% Running Optimization

if (milpflag == 1)
    if (dialogflag)
        toc % measure time for preprocessing and building the problem 
        disp('Solving ILP...')
    end
    tic
    MyOptions=cplexoptimset('cplex');
    [cplex_out,fval,exitflag,output]=cplexmilp(f,[],[],Aeq,Beq,[],[],[],lb,ub,ConstrType,[],MyOptions);
    toc
else
    if (dialogflag)
        toc % measure time for preprocessing and building the problem 
        disp('Solving LP...')
    end
    tic
    [cplex_out,fval,exitflag,output]=cplexlp(f,[],[],Aeq,Beq,lb,ub);
    toc
end


if (dialogflag)
    fprintf('Solved! fval: %f\n', fval)
    disp(output)
end

%% Now time to compute some results

% compute dropped passengers
wait_cycle = 0;
num_pax = sum(sum(sum(FlowsOut)));
for i=1:N
    for j=1:N
        for t=1:T
            wait_cycle = wait_cycle + cplex_out(find_drop(i,j,t));
        end
    end
end
num_served = sum(cplex_out( find_served(1,1,1):find_served(N,N,T) ));

fprintf('Number of customers: %d \n', num_pax)
fprintf('Number of serviced customers: %d \n', num_served)
fprintf('Number of dropped customers: %d \n', num_pax - num_served)
fprintf('Average wait time: %d \n', wait_cycle/num_pax)

output.cplex_out = cplex_out;
iflag = max(cplex_out - round(cplex_out));
if (iflag > 0.00001)
    fprintf('Warning: Solution is fractional. Max fractional component is %d \n', iflag)
else
    disp('Obtained integer solution')
end

disp('Exporting control action...')

% output the station interaction instructions (zo, so, do, etc) 
r = cell(N,1);
for i=1:N
    for j=1:N
        if (i ~= j)
            for k=1:floor(cplex_out(r_flow(i,j,1)))
                r{i} = [r{i} j];
            end
        end
    end
end
rebalanceQueue.r = r;

x = cell(N,1); % format: first index is final destination, second index is current position. 
for i=1:N
    for j=1:N
        for k=1:floor(cplex_out(x_flow(i,j,1)))
            x{s,i} = [x{i} j];
        end
    end
end
rebalanceQueue.x = x;

output.r_flow = r_flow;
output.x_flow = x_flow;

% check the number of people who will be delivered
delivered = 0;
en_route = 0;
for i=1:N
    for j=1:N
        for t=1:T
            if (t + TravelTimes(j,i) <= T)
                delivered = delivered + cplex_out(x_flow(i,j,i,t));
            else
                en_route = en_route + cplex_out(x_flow(i,j,i,t));
            end
        end
    end
end

fprintf('%d customers delivered. \n', delivered)
fprintf('%d customers en route. \n', en_route)
