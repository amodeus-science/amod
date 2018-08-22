function socket = jmexWaitForConnection(server)

disp('waiting for client socket...')
while ~server.hasSocket()
  pause(.25)
end
socket = server.pollSocket();
disp('client socket connected')

