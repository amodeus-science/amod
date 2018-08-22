function [S,id] = jmexStruct(container)

S = struct;

for index = 0 : container.size() - 1
  doubleArray = container.get(index);
  d = doubleArray.size;
  if numel(d)==1
    d = [d;1];
  end
  S = setfield(S, ...
    char(doubleArray.name), ...
    reshape(doubleArray.value,d'));
end

id = char(container.id);

