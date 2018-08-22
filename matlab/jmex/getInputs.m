function input = getInputs(server)

disp('waiting for client socket...')
while ~server.hasSocket()
  pause(.25)
end
input = server.pollSocket();
disp('client socket connected')

