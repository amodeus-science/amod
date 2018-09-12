function [rebalanceQueue, output] = amod_p_mpc_v11(RoadNetwork, RebWeight, Passengers, Flags)
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
Delta_Threshold = RoadNetwork.Delta_Threshold;

FlowsOut = Passengers.FlowsOut;

milpflag=Flags.milpflag;
ignorerealpax = Flags.ignorerealpax;
pooling_flag = Flags.pooling_flag;
N=length(RoadGraph);

%% Assuming roadgraph to be fully connected. 

%% parameters

dialogflag = 1; % prints diagnostics

SourceRelaxCost=1e6; % The cost of dropping a source or sink altogether
CustomerTransitCost=1e5/T; % the cost of having a customer wait while in the car (i.e. transit time)
WaitTimeCost = SourceRelaxCost / T; %The cost per unit of time letting customers wait.



%% Indexing functions

% distance efficiency metric
Delta = @(i,j,k) (TravelTimes(i,j) + TravelTimes(j,k))/TravelTimes(i,k);

% true state variables 

% zo = zero occupancy (origin, destination, time) indexing
r_flow = @(i,j,t) (t-1)*N*N + (i-1)*N + j; 
% so = single occupancy (goal, origin, destination, time) indexing
x_flow = @(s,i,j,t) N*N*T + (s-1)*N*N*T + (t-1)*N*N + (i-1)*N + j;
% do = double occupancy (origin, destination1, destination2, time) indexing
p_flow = @(i,j,k,t) (N+1)*N*N*T + (t-1)*N*N*N + (i-1)*N*N + (j-1)*N + k;
% dropped customers (origin, destination, time) indexing
find_drop = @(i,j,t) (2*N+1)*N*N*T + (t-1)*N*N + (i-1)*N + j;
% find the number of i->j trips are serviced at time t. 
find_served = @(i,j,t) (2*N+2)*N*N*T + (t-1)*N*N + (i-1)*N + j;

% book-keeping variables

% book-keeping for x
x_zo_flow = @(s,i,j,t) (2*N+3)*N*N*T + (s-1)*N*N*T + (t-1)*N*N + (i-1)*N + j;
x_so_flow = @(s,i,j,t) (3*N+3)*N*N*T + (s-1)*N*N*T + (t-1)*N*N + (i-1)*N + j;

% book-keeping for p
p_zo_flow = @(i,j,k,t) (4*N+3)*N*N*T + (t-1)*N*N*N + (i-1)*N*N + (j-1)*N + k;
p_so_flow = @(i,j,k,t) (5*N+3)*N*N*T + (t-1)*N*N*N + (i-1)*N*N + (j-1)*N + k;
%p_do_flow = @(i,j,k,t) (6*N+3)*N*N*T + (t-1)*N*N*N + (i-1)*N*N + (j-1)*N + k;

% state size determined here

statesize = (6*N + 3)*N*N*T;

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
                f(r_flow(i,j,t)) = 0.9*RebWeight*TravelTimes(i,j);
            else
                f(r_flow(i,j,t)) = RebWeight*TravelTimes(i,j);
            end
            % f(find_wait(i,j,t)) = WaitTimeCost*t;
            f(find_drop(i,j,t)) = SourceRelaxCost;
            for s = 1:N
                % only pay for the first leg of the trip because the flow
                % type will change when the car drops off first cust. 
                f(p_flow(i,j,s,t)) = CustomerTransitCost*TravelTimes(i,j); 
                f(x_flow(s,i,j,t)) = CustomerTransitCost*TravelTimes(i,j); 
            end
        end
    end
end

if (dialogflag == 1)
    fprintf('Cost function built. \n')
end
%% build equality constraints

                 %zo outflow         so outflow         delayed customer              so component    do component  delay cost
num_eq_constr =  N*T                + N*(N-1)*T         + N*N*T                     + N*N*N*T       + N*N*N*T       + N*N*T; % 
num_eq_entries = N*T*(4*N + 2*N*N)  + N*(N-1)*T*(4*N)   + N*N*T*(2 + 2*N + 2*(N-1)) + N*N*N*T*3     + N*N*N*T*3     + (N*N*T+ ceil(N*N*T*(T+1)*0.5));% 

%num_eq_constr = num_eq_constr + N*T;
%num_eq_entries = num_eq_entries + N*T*(N + 2*N*N);

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
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_flow(i,j,i,t-TravelTimes(j,i)), -1]; % singly occupied vehicles dropping off their customer and becoming vacant.
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_flow(j,i,i,t-TravelTimes(j,i)), -1]; % doubly occupied vehicles dropping off both their customers and becoming vacant. 
            end
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, r_flow(i,j,t), 1]; % cars that will leave empty
            for s=1:N
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_zo_flow(s,i,j,t), 1]; % cars that will pick up one customer
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_zo_flow(i,j,s,t), 1]; % cars that will pick up 2 customers. 
            end
        end
        Beq(Aeqrow) = Starters.r_state(i,t); % specify the influx of vehicles. t=1 is the initial condition.
    end
end

% single occupancy outflow equation
for t=1:T
    for i=1:N
        for s=1:N % specify the destination of a single occupancy vehicle
            if (s ~= i)
                Aeqrow = Aeqrow + 1; % start a new row for this constraint. 
                for j=1:N
                    if (t > TravelTimes(j,i))
                        Aeqentry = Aeqentry + 1;
                        Aeqsparse(Aeqentry,:) = [Aeqrow, x_flow(s,j,i,t-TravelTimes(j,i)), -1]; % count all single occupancy vehicles going to s passing through here. 
                        Aeqentry = Aeqentry + 1;
                        Aeqsparse(Aeqentry,:) = [Aeqrow, p_flow(j,i,s,t-TravelTimes(j,i)), -1]; % count double occupancy cars that will drop off a customer to be single occ.
                    end
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, x_so_flow(s,i,j,t), 1]; % cars leaving without picking up a customer.
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, p_so_flow(i,s,j,t), 1]; % how many of these cars will pick up another customer.                    
                end
                Beq(Aeqrow) = Starters.x_state(s,i,t); % influx of cars. t=1 is the initial condition.  
            end
        end
    end
end
%{
disp('Checkpoint 2')
% double occupancy outflow equation
for t=1:T
    for i=1:N
        for k=1:N
            if (i ~= k)
                Aeqrow = Aeqrow + 1; % start a new line for this constraint.
                for j=1:N
                    if (t > TravelTimes(j,i))
                        Aeqentry = Aeqentry + 1;
                        Aeqsparse(Aeqentry,:) = [Aeqrow, p_flow(j,i,k,t - TravelTimes(j,i)), -1]; % count all double occupancy cars coming into node i.
                    end
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, x_so_flow(k,i,j,t), 1]; % car will drop off a customer and move on.
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, p_do_flow(i,k,j,t), 1]; % car will drop off a customer and pick up one more.
                end
                % p's will become single occupancy when they arrive at the
                % station, so we include the x_state here. 
                Beq(Aeqrow) = Starters.x_state(s,i,t); 
            end
        end
    end
end
%}
% dropped customer equation
for t=1:T
    for i=1:N
        for j=1:N
            Aeqrow = Aeqrow + 1; % start a new line for this constraint. 
            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, find_served(i,j,t), -1]; % total customer served 

            Aeqentry = Aeqentry + 1;
            Aeqsparse(Aeqentry,:) = [Aeqrow, p_zo_flow(i,j,j,t), 2]; % customers picked up in pairs.
            
            for u=1:N
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_zo_flow(j,i,u,t), 1]; % cars carrying one customer, of the 1st kind. 
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_so_flow(i,u,j,t), 1]; % customers picked up in pairs.
                if (u ~= j)
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, p_zo_flow(i,u,j,t), 1]; % customers picked up in pairs, but only the second customer is i->j. The first is something else.
                    Aeqentry = Aeqentry + 1;
                    Aeqsparse(Aeqentry,:) = [Aeqrow, p_zo_flow(i,j,u,t), 1]; % customers picked up in pairs, but only the first customer is i->j. The second is something else.
                end
            end
        end
    end
end

% clear everything... this should make things work.
Aeqsparse_part1 = Aeqsparse(1:Aeqentry,:);
Aeqsparse = zeros(num_eq_entries, 3);
Aeqentry = 0;

% single occupancy components
for t=1:T
    for i=1:N
        for s=1:N
            for j=1:N
                Aeqrow = Aeqrow + 1;
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_flow(s,i,j,t), 1]; % total outbound singly occupied vehicles. 
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_zo_flow(s,i,j,t), -1]; % total outbound singly occupied vehicles, 1st kind.
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, x_so_flow(s,i,j,t), -1]; % total outbound singly occupied vehicles, 2nd kind.
            end
        end
    end
end

% double occupancy components
for t=1:T
    for i=1:N
        for j=1:N
            for k=1:N
                Aeqrow = Aeqrow + 1;
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_flow(i,j,k,t), 1]; % total outbound doubly occupied vehicles. 
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_zo_flow(i,j,k,t), -1]; % first kind
                Aeqentry = Aeqentry + 1;
                Aeqsparse(Aeqentry,:) = [Aeqrow, p_so_flow(i,j,k,t), -1]; % second kind
            end
        end
    end
end

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

% uncomment to disable double occupancy. 

if (pooling_flag == 0)
    for i=1:N
        for j=1:N
            for k=1:N
                for t=1:T
                    ub(p_flow(i,j,k,t)) = 0;
                    ub(p_zo_flow(i,j,k,t)) = 0;
                    ub(p_so_flow(i,j,k,t)) = 0;

                    ub(x_so_flow(k,i,j,t)) = 0;
                    if (k~= j)
                        ub(x_zo_flow(k,i,j,t)) = 0;
                    end

                end
            end
        end
    end
end


for i=1:N
    for j=1:N
        for k=1:N
            if (i==j)
                for t=1:T
                    ub(p_so_flow(i,j,k,t)) = 0;
                    ub(x_so_flow(j,i,k,t)) = 0;
                end
            end
        end
    end
end

prune_counter = 0;
for i=1:N
    for j=1:N
        for k=1:N
            if ((i ~= j) && (j ~= k) &&(i ~= k))
                if (Delta(i,j,k) > Delta_Threshold) % if the path is too inefficient
                    for t=1:T
                        ub(x_flow(k,i,j,t)) = 0; % prune away these flows
                        ub(p_flow(i,j,k,t)) = 0; % prune these flows too. 
                    end
                    prune_counter = prune_counter + 2*T; % update the number of flows that are pruned away. 
                end
            end
        end
    end
end

if (dialogflag == 1)
    effective_statesize = statesize - prune_counter;
    fprintf('Pruned %d flows. Effective state size is %d. \n', prune_counter, effective_statesize)
end



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
            for k=1:N
                ConstrType(x_flow(k,i,j,1)) = 'I';
                ConstrType(x_zo_flow(k,i,j,1)) = 'I';
                ConstrType(x_so_flow(k,i,j,1)) = 'I';
                ConstrType(p_flow(i,j,k,1)) = 'I';
                ConstrType(p_zo_flow(i,j,k,1)) = 'I';
                ConstrType(p_so_flow(i,j,k,1)) = 'I';
            end
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

x_zo = cell(N,N); % format: first index is final destination, second index is current position. 
x_so = cell(N,N);
for i=1:N
    for j=1:N
        for s=1:N
            if (i ~= j) % controller asks the car to move to a different station
                for k=1:floor(cplex_out(x_zo_flow(s,i,j,1)))
                    x_zo{s,i} = [x_zo{s,i} j];
                end
                for k=1:floor(cplex_out(x_so_flow(s,i,j,1)))
                    x_so{s,i} = [x_so{s,i} j];
                end
            else
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                 % if car does not move, override the command and send the car to the customer's destination.
                 % this is because cars that cannot reach their
                 % destinations within the time horizon will give up and do
                 % nothing. We do not want to allow that to happen.
                for k=1:floor(cplex_out(x_zo_flow(s,i,j,1)))
                    x_zo{s,i} = [x_zo{s,i} s];
                end
                for k=1:floor(cplex_out(x_so_flow(s,i,j,1)))
                    x_so{s,i} = [x_so{s,i} s];
                end
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            end
        end
    end
end
rebalanceQueue.x_zo = x_zo;
rebalanceQueue.x_so = x_so;

p_zo = cell(N,N); % format: first index is current location, second index is first destination. 
p_so = cell(N,N);

for i=1:N
    for j=1:N
        for k=1:N
            for kk=1:floor(cplex_out(p_zo_flow(i,j,k,1)))
                p_zo{i,j} = [p_zo{i,j} k];
            end
            for kk=1:floor(cplex_out(p_so_flow(i,j,k,1)))
                p_so{i,j} = [p_so{i,j} k];
            end     
        end
    end
end
rebalanceQueue.p_zo = p_zo;
rebalanceQueue.p_so = p_so;

output.r_flow = r_flow;
output.x_flow = x_flow;
output.x_zo_flow = x_zo_flow;
output.x_so_flow = x_so_flow;
output.p_flow = p_flow;
output.p_zo_flow = p_zo_flow;
output.p_so_flow = p_so_flow;

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
            if (t + TravelTimes(i,j) <= T)
                for k=1:N
                    if (k==j)
                        delivered = delivered + 2*cplex_out(p_flow(i,j,k,t));
                    else
                        delivered = delivered + cplex_out(p_flow(i,j,k,t));
                    end
                end
            else
                for k=1:N
                    if (k==j)
                        en_route = en_route + 2*cplex_out(p_flow(i,j,k,t));
                    else
                        en_route = en_route + cplex_out(p_flow(i,j,k,t));
                    end
                end                
            end
        end
    end
end

fprintf('%d customers delivered. \n', delivered)
fprintf('%d customers en route. \n', en_route)
