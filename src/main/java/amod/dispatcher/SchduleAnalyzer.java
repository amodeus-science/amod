package amod.dispatcher;

import java.util.List;

import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.matsim.av.schedule.AVDriveTask;
import ch.ethz.matsim.av.schedule.AVDropoffTask;
import ch.ethz.matsim.av.schedule.AVPickupTask;
import ch.ethz.matsim.av.schedule.AVStayTask;

/** @author clruch */
public enum SchduleAnalyzer {
    ;

    /* package */ static ScheduleObject getSchedules(List<RoboTaxi> roboTaxis) {

        for (RoboTaxi roboTaxi : roboTaxis) {
            // TODO this is all the scheudle information that I can currently access.
            // There might be the chance to access more but I have to see with the MATsim API.
            // I think it should be enough to get started.
            // Use this to build your ScheduleObject filled with the information that  you need...

            Schedule schedule = roboTaxi.getSchedule(); // ensure you can get schedule from RoboTaxi
                                                        // here by changing
            // the access function

            List<? extends Task> taskList = schedule.getTasks();
            for (Task task : taskList) {

                /** this is all the info that every MATSim task has */
                double beginTime = task.getBeginTime();
                double endTime = task.getEndTime();
                TaskStatus status = task.getStatus();

                boolean roboTaxiDriving = false;
                boolean roboTaxiInPickup = false;
                boolean roboTaxiInDropoff = false;
                boolean roboTaxiInStay = false;

                if (task instanceof AVDriveTask) {
                    roboTaxiDriving = true;
                }

                if (task instanceof AVDropoffTask) {
                    roboTaxiInDropoff = true;
                }

                if (task instanceof AVPickupTask) {
                    roboTaxiInPickup = true;
                }

                if (task instanceof AVStayTask) {
                    roboTaxiInStay = true;
                }

            }

        }
        
        // TODO fill my custom built SchdeuleObject here
        ScheduleObject scheduleObject = null;

    }

}
