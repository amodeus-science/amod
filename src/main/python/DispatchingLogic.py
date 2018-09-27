import random
from utils.RoboTaxiStatus import RoboTaxiStatus


class DispatchingLogic:
    """
    dispatching logic in the AidoGuest demo to compute dispatching instructions that are forwarded to the AidoHost
    """
    def __init__(self, bottomLeft, topRight):
        """
        :param bottomLeft: {lngMin, latMin}
        :param topRight: {lngMax, latMax}
        """
        self.lngMin = bottomLeft[0]
        self.lngMax = topRight[0]
        self.latMin = bottomLeft[1]
        self.latMax = topRight[1]

        print("minimum longitude in network: ", self.lngMin)
        print("maximum longitude in network: ", self.lngMax)
        print("minimum latitude  in network: ", self.latMin)
        print("maximum latitude  in network: ", self.latMax)

        # Example:
        # minimum longitude in network: -71.38020297181387
        # maximum longitude in network: -70.44406349551404
        # minimum latitude in network: -33.869660953686626
        # maximum latitude in network: -33.0303523690584

        self.matchedReq = set()
        self.matchedTax = set()

    def of(self, status):
        assert isinstance(status, list)
        pickup = []
        rebalance = []

        time = status[0]
        if time % 60 == 0:  # every minute
            index = 0

            # sort requests according to submission time
            requests = sorted(status[2].copy(), key=lambda request: request[1])

            # for each unassigned request, add a taxi in STAY mode
            for request in requests:
                if request[0] not in self.matchedReq:
                    while index < len(status[1]):
                        roboTaxi = status[1][index]
                        if roboTaxi[2] is RoboTaxiStatus.STAY:
                            pickup.append([roboTaxi[0], request[0]])
                            self.matchedReq.add(request[0])
                            self.matchedTax.add(roboTaxi[0])
                            index += 1
                            break
                        index += 1

            # rebalance 1 of the remaining and unmatched STAY taxis
            for roboTaxi in status[1]:
                if roboTaxi[2] is RoboTaxiStatus.STAY and roboTaxi[0] not in self.matchedTax:
                    rebalanceLocation = self.getRandomRebalanceLocation()
                    rebalance.append([roboTaxi[0], rebalanceLocation])
                    break

        return [pickup, rebalance]

    def getRandomRebalanceLocation(self):
        """
        ATTENTION: AMoDeus internally uses the convention (longitude, latitude) for a WGS:84 pair, not the other way
        around as in some other cases.
        """
        return [random.uniform(self.lngMin, self.lngMax),
                random.uniform(self.latMin, self.latMax)]
