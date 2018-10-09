import socket
from utils import Translator


class StringSocket(socket.socket):
    def __init__(self, ip, port=9382):
        try:
            super(StringSocket, self).__init__(socket.AF_INET, socket.SOCK_STREAM)
            self.connect((ip, port))
        except socket.error:
            raise IOError("Unable to create StringSocket!")
        self.buffer = ''

    def writeln(self, message):
        assert isinstance(message, (str, list)), "Input %s <%s> has to be string or list!" % (message, type(message))
        msg = Translator.listToTensorString(message) if isinstance(message, list) else message
        self.sendall(str.encode(msg + '\n'))

    def readLine(self, buffer=4096):
        while '\n' not in self.buffer:
            self.buffer += self.recv(buffer).decode()
        lines = self.buffer.split('\n')
        line = lines.pop(0)
        self.buffer = '\n'.join(lines)
        return Translator.tensorStringToList(line)
