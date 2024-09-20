package org.asozial.guerteltier.schule.temperatur;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GeohashingService {
    @GET("hash/{date}/global")
    Call<GeohashingGlobalHash> getGlobalHash(@Path(value = "date", encoded = true) String date);
}
