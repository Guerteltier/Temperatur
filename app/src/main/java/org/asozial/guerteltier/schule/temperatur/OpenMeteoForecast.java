package org.asozial.guerteltier.schule.temperatur;

public final class OpenMeteoForecast {
    public double latitude;
    public double longitude;
    public double generationtime_ms;
    public int utc_offset_seconds;
    public String timezone;
    public String timezone_abbreviation;
    public short elevation;
    public OpenMeteoForecastCurrentUnits current_units;
    public OpenMeteoForecastCurrent current;
    public OpenMeteoForecastDailyUnits daily_units;
    public OpenMeteoForecastDaily daily;
}
