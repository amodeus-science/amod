import ast
import re
from utils.RoboTaxiStatus import RoboTaxiStatus


def tensorStringToList(string):
    """turn java tensor string representation into python list"""
    assert isinstance(string, str)
    # make string readable for ast.literal_eval
    s = re.sub(r" ?\[[^)]+\]", "", string).replace('{', '[').replace('}', ']').replace('-Infinity', "'-Infinity'")
    for status in RoboTaxiStatus.values():
        s = s.replace(status, "'%s'" % status)
    # evaluate string
    try:
        array = ast.literal_eval(s)
    except (ValueError, SyntaxError):
        raise ValueError("Unable to translate '%s'!" % string)
    else:
        return replaceStatus(array)  # insert meaningful values


def listToTensorString(array):
    """turn python list into java tensor string representation"""
    assert isinstance(array, list)
    a = [listToTensorString(element) if isinstance(element, list) else element for element in array]
    return '{%s}' % ', '.join(list(map(str, a)))


def replaceStatus(element):
    if isinstance(element, list):
        return [replaceStatus(e) for e in element]
    else:
        try:
            return RoboTaxiStatus[element]
        except KeyError:
            return element
