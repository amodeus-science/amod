function requestsMATSimConnector(server)
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
numberRequests = inputCarpooling.NumberRequests;

Request.numberRequests = numberRequests;
save('Requets200000','Request');

sol = ch.ethz.idsc.jmex.Container('solution');

socket.writeContainer(sol)

% ======================================

    else
      pause(.002)
    end
  end

  socket.close()
end

