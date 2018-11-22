clear all; close all; clc;

% cost adjustments flags
fleetFlag       = 1;
aoutomatedFlag  = 1;
electricFlag    = 0;
soloFlag        = 1;
midsizeFlag     = 0;

fleetSize = 300;

% controller
RAMoDMPCFlag = 1;

if(RAMoDMPCFlag == 1)
    pkm = giveCustomerDistance(fleetSize);
end

% profit margin
r = 0.03;

% payment transaction fee
p = 0.0044;

% Value-added tax
VAT = 0.08;

% average passenger kilometers per vehicle and day
pkm = pkm/fleetSize;;

% average total kilometers per vehicle and day
vkm = 453450/fleetSize;

% service provider overhead costs
OHC = 14;   % CHF/day

% variable service costs
%VSC = 10;   % CHF/day

% fixed costs
asquisitionSolo = 13000;  % CHF
insuranceSolo   = 500;    % CHF/year
taxSolo         = 120;    % CHF/year
parkingSolo     = 1500;   % CHF/year
tollSolo        = 40;     % CHF/year

asquisitionMidsize = 35000;  % CHF
insuranceMidsize   = 1000;   % CHF/year
taxMidsize         = 250;    % CHF/year
parkingMidsize     = 1500;   % CHF/year
tollMidsize        = 40;     % CHF/year

% variable costs
maintenanceSolo = 0.02;   % CHF/km
cleaningSolo    = 0.02;   % CHF/km
tiresSolo       = 0.02;   % CHF/km
fuelSolo        = 0.06;   % CHF/km

maintenanceMidsize = 0.06;   % CHF/km
cleaningMidsize    = 0.03;   % CHF/km
tiresMidsize       = 0.02;   % CHF/km
fuelMidsize        = 0.08;   % CHF/km

% cost adjustments automated
asquisitionAutomated = 0.2;
insuranceAutomated   = -0.5;
tiresAutomated       = -0.1;
fuelAutomated        = -0.1;

if(aoutomatedFlag == 0)
    asquisitionAutomated = 0;
    insuranceAutomated   = 0;
    tiresAutomated       = 0;
    fuelAutomated        = 0;
end

% cost adjustments fleet
asquisitionFleet = -0.3;
insuranceFleet   = -0.2;
parkingFleet     = 13.3;
maintenancFleet  = -0.25;
tiresFleet       = -0.25;
fuelFleet        = -0.05;

if(fleetFlag == 0)
    asquisitionFleet = 0;
    insuranceFleet   = 0;
    parkingFleet     = 0;
    maintenancFleet  = 0;
    tiresFleet       = 0;
    fuelFleet        = 0;
end

% cost adjustments electric
insuranceElectric   = -0.35;
taxElectric         = -1;
maintenancElectic   = 0.28;
fuelElectric        = -0.5;

if(electricFlag == 0)
    insuranceElectric   = 0;
    taxElectric         = 0;
    maintenancElectic   = 0;
    fuelElectric        = 0;
end

if(soloFlag == 1)
    asquisition = asquisitionSolo;  % CHF
    insurance   = insuranceSolo;    % CHF/year
    tax         = taxSolo;          % CHF/year
    parking     = parkingSolo;      % CHF/year
    toll        = tollSolo;         % CHF/year
    
    maintenance = maintenanceSolo;   % CHF/km
    cleaning    = cleaningSolo;   % CHF/km
    tires       = tiresSolo;   % CHF/km
    fuel        = fuelSolo;   % CHF/km
elseif(midsizeFlag ==1)
    asquisition = asquisitionMidsize;  % CHF
    insurance   = insuranceMidsize;    % CHF/year
    tax         = taxMidsize;          % CHF/year
    parking     = parkingMidsize;      % CHF/year
    toll        = tollMidsize;         % CHF/year
    
    maintenance = maintenanceMidsize;   % CHF/km
    cleaning    = cleaningMidsize;   % CHF/km
    tires       = tiresMidsize;   % CHF/km
    fuel        = fuelMidsize;   % CHF/km
end

% total fixed cost
totalFixedCost = asquisition*(1 + asquisitionAutomated + asquisitionFleet) + insurance*(1 + insuranceAutomated + ...
        insuranceFleet + insuranceElectric) + tax*(1 + taxElectric) + parking*(1 + parkingFleet) + toll;
    
% total variable cost
totalVariableCost = maintenance*(1 + maintenancFleet + maintenancElectic) + cleaning + ...
    tires*(1 + tiresAutomated + tiresFleet) + fuel*(1 + fuelAutomated + fuelFleet + fuelElectric);

% average total cost er vehicle and day
Cvd = totalFixedCost/365 + OHC + 10 + totalVariableCost*vkm;

% Cost per Passenger-kilometer
Cpkm = Cvd/pkm;
Cvkm = Cvd/vkm;

% price per passenger-kilometer
Ppkm = Cpkm/((1-r)*(1-p))*(1 + VAT);


    

