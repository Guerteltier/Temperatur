package org.asozial.guerteltier.schule.temperatur;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public final class MainActivity extends AppCompatActivity {
    private static final Converter.Factory CONVERTER_FACTORY = JacksonConverterFactory.create(JsonMapper.builder().addModule(new JavaTimeModule()).build());
    private static final Retrofit.Builder RETROFIT_BUILDER = new Retrofit.Builder().addConverterFactory(CONVERTER_FACTORY);
    private static final Retrofit RETROFIT_GEOHASHING = RETROFIT_BUILDER.baseUrl("https://data.geohashing.info/").build();
    private static final Retrofit RETROFIT_OPEN_METEO = RETROFIT_BUILDER.baseUrl("https://api.open-meteo.com/v1/").build();
    private static final GeohashingService GEOHASHING_SERVICE = RETROFIT_GEOHASHING.create(GeohashingService.class);
    private static final OpenMeteoService OPEN_METEO_SERVICE = RETROFIT_OPEN_METEO.create(OpenMeteoService.class);
    private Thread temperatureUpdateThread;
    private Boolean keepUpdating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        keepUpdating = true;
        if (temperatureUpdateThread == null || !temperatureUpdateThread.isAlive()) {
            temperatureUpdateThread = createTUT();
            temperatureUpdateThread.start();
            setState(State.LOADING);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        keepUpdating = false;
        super.onStop();
    }

    private Thread createTUT() {
        return new Thread(() -> {
            while (keepUpdating) {
                SystemClock.sleep(updateTemperature());
            }
        });
    }

    private void setState(State state) {
        runOnUiThread(() -> {
            var temperature = findViewById(R.id.temperature);
            var progressBar = findViewById(R.id.progressBar);
            var error = findViewById(R.id.error);
            switch (state) {
                case LOADING:
                    temperature.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    error.setVisibility(View.GONE);
                    break;
                case READY:
                    temperature.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    error.setVisibility(View.GONE);
                    break;
                case ERROR:
                    temperature.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void setTemperature(double temperature) {
        runOnUiThread(() -> ((TextView)findViewById(R.id.temperature)).setText(String.valueOf(temperature)));
    }

    private long updateTemperature() {
        long sleepDuration;
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        var formatter = DateTimeFormatter.ofPattern("y/M/d");
        Response<GeohashingGlobalHash> geohashingResponse;
        try {
            geohashingResponse = GEOHASHING_SERVICE.getGlobalHash(date.format(formatter)).execute();
        } catch (IOException e) {
            setState(State.ERROR);
            return 5000;
        }
        var hash = geohashingResponse.body();
        if (!geohashingResponse.isSuccessful() || hash == null) {
            setState(State.ERROR);
            return 5000;
        }
        Response<OpenMeteoForecast> openMeteoResponse;
        try {
            openMeteoResponse = OPEN_METEO_SERVICE.getForecast(hash.lat, hash.lng).execute();
        } catch (IOException e) {
            setState(State.ERROR);
            return 5000;
        }
        var forecast = openMeteoResponse.body();
        if (!openMeteoResponse.isSuccessful() || forecast == null) {
            setState(State.ERROR);
            return 5000;
        }
        double temp = forecast.current.apparent_temperature;
        var temp_min = forecast.daily.apparent_temperature_min[0];
        var temp_max = forecast.daily.apparent_temperature_max[0];
        temp = (temp - temp_min) / (temp_max - temp_min);
        temp = Math.round(temp * 100) / 100.0;
        setTemperature(temp);
        setState(State.READY);
        var sleepUntil = forecast.current.time.plus(forecast.current.interval);
        sleepDuration = ChronoUnit.MILLIS.between(LocalDateTime.now(Clock.systemUTC()), sleepUntil);
        // system clock may be incorrect
        sleepDuration = Math.max(sleepDuration, 30_000);
        sleepDuration = Math.min(sleepDuration, 900_000);
        return sleepDuration;
    }
}
