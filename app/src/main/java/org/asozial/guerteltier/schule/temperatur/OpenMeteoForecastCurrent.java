package org.asozial.guerteltier.schule.temperatur;

import java.time.Duration;
import java.time.LocalDateTime;

public final class OpenMeteoForecastCurrent {
    public LocalDateTime time;
    public Duration interval;
    public float apparent_temperature;
}
