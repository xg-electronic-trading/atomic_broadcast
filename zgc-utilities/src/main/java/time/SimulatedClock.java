package time;

import org.agrona.concurrent.EpochClock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class SimulatedClock implements EpochClock {

    private final String DEFAULT_START_TIME = "2022-01-01 08:00:00";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private long currentTime;


   public SimulatedClock() {
       this("Europe/London");
   }

   public SimulatedClock(String zoneid) {
       LocalDateTime dt = LocalDateTime.parse(DEFAULT_START_TIME, formatter);
       ZonedDateTime zdt = ZonedDateTime.of(dt, ZoneId.of(zoneid));
       currentTime = zdt.toInstant().toEpochMilli();
   }

   public void advance() {
       currentTime += 1;
   }

   public void advanceSeconds(long seconds) {
       currentTime += TimeUnit.SECONDS.toMillis(seconds);
   }

    @Override
    public long time() {
        return currentTime;
    }
}
