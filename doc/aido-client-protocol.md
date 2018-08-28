# AIDO client protocol

The communication is text based.

Each message is a string that terminates with a line break `\n`. Apart from the last character, the string does **not** contain another line break.

*Remark:* The notation adheres to the *Mathematica* standard for nested lists.

## Port of server

The server listens for incoming `TCP/IP` connections at port `9382`.

## Initialization

### Client -> Server

The server requires information about which scenario to run.
The client sends the first line for instance as

    {SanFrancisco, 0.4, 180}

The line encodes the name of the scenario `SanFrancisco`, the population size ratio `0.4`, and the number of vehicles `180`.

Valid scenarios names include `SanFrancisco`, `TelAviv`, `Santiago`, `Berlin`.
The size ratio should be between `0` and `1`.
The value `1` corresponds to the full scenario.

### Server -> Client

The server replies with the bounding box of the scenario coordinates.
The city grid is inside the WGS:84 coordinates bounded by the box

    {{-71.38020297181387, -33.869660953686626}, {-70.44406349551404, -33.0303523690584}}

The interpretation is

    {{longitude min, latitude min}, {longitude max, latitude max}}

## Main loop

### Server -> Client

The server gives the signal that the simulation has completed, or notifies the client about the state of the simulation.

When simulation is finished, the server sends an empty list

	{}

in which case the client should exit the main loop and proceed to receive the evaluation.

When the simulation is ongoing, the server sends a list of the following structure:

	{simulation time, VEHICLES, REQUESTS, REWARDS}

#### VEHICLES

The variable `VEHICLES` encodes the status of all vehicles. `VEHICLES` is a list of the form

	{VEHICLE_STATUS[1], ..., VEHICLE_STATUS[n]}

The variable `VEHICLE_STATUS` is a list of the form

	{vehicle index, {longitude position, latitude position}, STATUS, IS_DIVERTABLE}

The vehicle index is a non-negative integer.

The field `STATUS` is one of the following strings `DRIVEWITHCUSTOMER`, `DRIVETOCUSTOMER`, `REBALANCEDRIVE`, or `STAY` (without quotation marks).

The field `IS_DIVERTABLE` is either the value `0` or `1`. The value `0` encodes that the vehicle is not divertable. The value `1` encodes that the vehicle is divertable.

#### REQUESTS

The variable `REQUESTS` encodes the status of all open, i.e. unserved requests. `REQUESTS` is a list of the form

	{REQUEST_STATUS[1], ..., REQUEST_STATUS[m]}

The variable `REQUEST_STATUS` is a list of the form

	{request index, submission time, {longitude origin, latitude origin}, {longitude destination, latitude destination}}

Each request has a unique, non-negative integer as index.

The `submission time` is a numeric value less than `simulation time`.

#### REWARDS

The variable `REWARDS` is a list that contains the gains accumulated in different categories since the previous simulation step. The sum of all rewards equal the final score.

	TODO

### Client -> Server

The client has to instruct the simulation server on how to divert the vehicles.

## Evaluation

### Server -> Client

The server sends with

    {1,2,3}

