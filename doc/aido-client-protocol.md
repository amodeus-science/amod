# AIDO client protocol

This file outlines the communication protocol for the Artificial Intelligence Driving Olympics (=AIDO) autonomous mobility-on-demand competition, for assistance please contact [mailto] (clruch@idsc.mavt.ethz.ch) . Documentation on the AIDO mobility-on-demand competition can be found [here](https://www.duckietown.org/research/ai-driving-olympics/ai-do-rules), [here](http://docs.duckietown.org/AIDO/out/amod.html) and [here](http://docs.duckietown.org/AIDO/out/performance.html).

The communication is text based.

Each message is a string that terminates with a line break `\n`. Apart from the last character, the string does **not** contain another line break.

*Remark:* The notation adheres to the *Mathematica* standard for nested lists, [see here](https://reference.wolfram.com/language/tutorial/NestedLists.html).

The communication takes place between the main processes in [AidoHost](https://github.com/idsc-frazzoli/amod/blob/master/src/main/java/amod/aido/AidoHost.java) and [AidoGuest](https://github.com/idsc-frazzoli/amod/blob/master/src/main/java/amod/aido/demo/AidoGuest.java). AidoHost contains the simulation environment, while a modified version of AidoGuest can be used by the participant of AIDO to place their own fleet operational policy. **AidoHost** is implemented as a **Server** and **AidoGuest** as a **Client**.

## Port of server

The server listens for incoming `TCP/IP` connections at port `9382`.

## Initialization

### Client -> Server

The server requires information about which scenario to run. The range of options is visible in [this file](https://github.com/idsc-frazzoli/amodeus/blob/master/src/main/resources/aido/scenarios.properties). The client sends the first line for instance as

    {SanFrancisco.20080518}

The line encodes the name of the scenario `SanFrancisco.20080518`, valid scenarios names include also `TelAviv`, `Santiago`, `Berlin`.


### Server -> Client

The server replies with the total number of requests in the scenario, bounding box of the scenario coordinates and the nominal fleet size.

    {190788,{{-71.38020297181387, -33.869660953686626}, {-70.44406349551404, -33.0303523690584}},700}

The interpretation is

    {number of requests,{{longitude min, latitude min}, {longitude max, latitude max}}, nominal fleet size}

The coordinates of the bouding box are denoted in the WGS:84 coordinate system.

### Client -> Server

The client then responds by chosing the desired number of requests and the desired fleet size, for instance

    {10000,277}


The interpretation is

    {desired number of requests, desired fleet size}

The desired number of requests must be a positive integer. If it is larger than the total requests in the chosen scenario, the total requests in the chosen scenario are used in the simulation. The desired fleet size can be any positive integer.

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

The variable `REWARDS` is a list that contains the gains accumulated in different categories since the previous simulation step.

	{service reward, efficiency reward, fleet size reward}

The value of `THIRD_REWARD` is initially `0` but switches to `-Infinity` if the maximum wait-time among the unserviced requests exceeds 10 minutes.

The final score vector is the (undiscounted) sum of all reward vectors.

### Client -> Server

The client has to instruct the simulation server on how to divert the vehicles.
Each vehicle can either be sent on a pickup drive to fetch a waiting customer, or a rebalancing drive.
The message is a list of the form:

	{PICKUPS, REBALANCING}

The variable `PICKUPS` is a list of the form

	{PICKUP_PAIR[1], ..., PICKUP_PAIR[p]}

The variable `PICKUP_PAIR` is a list with two entries that associates a divertable vehicle with an open request. The list is of the form

	{vehicle index, request index}

The variable `REBALANCING` is a list of the form

	{REBALANCE[1], ..., REBALANCE[r]}

The variable `REBALANCE` is of the form

	{vehicle index, {longitude destination, latitude destination}}

Important:
Each vehicle index may only appear in either the pickup or rebalancing instructions. Additionally, within the list, each vehicle index may only appear once. Each request index may only appear once in the list of pickup instructions. However, in the next time step, another vehicle may be assigned to the same request. In that case, the former vehicle will stop driving.

Examples:
* The client may choose to send `{{}, {}}` which corresponds to an empty list of pickup instructions, and an empty list of rebalancing instructions.
* The client is not required to use rebalancing instructions. A message of the form `{PICKUPS, {}}` is valid.
* The client may choose to only send rebalancing instructions. In that case, the message is the form `{{}, REBALANCING}` is valid.


## Evaluation

### Server -> Client

The server sends the (undiscounted) sum of all reward vectors. In the last component, a reward of `-n` incurs, which represents the cost for the number of vehicles used.

    {service score, efficiency score, fleet size score}

In particular, the `fleet size score` takes the value `-n` or `-Infinity`. More information the rewards can be found [here](http://docs.duckietown.org/AIDO/out/performance.html).
