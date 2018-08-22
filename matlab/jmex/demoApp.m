function demoApp(server)
% routine is terminated anytime by pressing Ctrl+C

while 1

  socket = jmexWaitForConnection(server)

  while socket.isConnected()
    if socket.hasContainer()

% ======================================
% get data structure from client socket
container = socket.pollContainer();
[SMPC] = jmexStruct(container)

x = SMPC.TEST';
test = zeros(1,length(x));

for i = 1:1:length(x)
    test(i) = x(i) + 0.5;
end

disp(test);

%sol=func(MPC,id) % for instance, solve MPC here
%[res,id]=jmexStruct(sol)

% write reply to client socket
sol = ch.ethz.idsc.jmex.Container('solution');
sol.add(jmexArray('solution',test))

socket.writeContainer(sol)

% ======================================

    else
      pause(.002)
    end
  end

  socket.close()

end

