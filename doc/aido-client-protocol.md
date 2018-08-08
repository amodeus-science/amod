# AIDO client protocol

The communication is text based.

Each message terminates with a line break.

The parameters adhere to the Mathematica standard for nested lists.

## Port

The server listens for incoming `TCP/IP` connections at port `9382`.


## Initialization

The server requires information about which scenario to run.
The client sends the first line for instance as

    {SanFrancisco, 0.4, 180}

The line encodes the name of the scenario `SanFrancisco`, the size ratio `0.4`, and the number of vehicles `180`.

Possible scenarios names are `SanFrancisco`, `TelAviv`, `Santiago`.
The size ratio should be between `0` and `1`.
The latter corresponds to the full scenario.

The server replies with The city grid is inside the WGS:84 coordinates bounded by the box

    {{-71.38020297181387, -33.869660953686626}, {-70.44406349551404, -33.0303523690584}}

The interpretation is

    {{lngMin, latMin}, {lngMax, latMax}}


## Main loop


## Evaluation

The server replies with

    {1,2,3}

