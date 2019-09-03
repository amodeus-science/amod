package amod.scenario.chicago;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/* package */ enum ScenarioConstants {
    ;

    public static final double milesToM = 1609.34;
    public static final DateFormat inFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final DateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final DateTimeFormatter onlineFormat //
            = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

}
