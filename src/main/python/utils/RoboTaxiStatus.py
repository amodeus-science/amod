from enum import Enum


class RoboTaxiStatus(Enum):
    DRIVEWITHCUSTOMER = 'DRIVEWITHCUSTOMER'
    DRIVETOCUSTOMER = 'DRIVETOCUSTOMER'
    REBALANCEDRIVE = 'REBALANCEDRIVE'
    STAY = 'STAY'
    OFFSERVICE = 'OFFSERVICE'

    @staticmethod
    def values():
        return [e.value for e in RoboTaxiStatus]
