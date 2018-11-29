package it.feio.android.omninotes.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CemsApi {
    @POST("api-case/cases/")
    Call<CrimeCase> post_case(@Body CrimeCase crimeCase);

    @DELETE("api-case/cases/{number}/")
    Call<String> delete_case(@Path("number") String number);

    //@POST("/api/restaurants/")
    //Call<Restaurant> post_restaruant(@Body Restaurant restaruant);

    /*
    @GET("/api/restaurants/")

    Call<List<Restaurant>> get_restaruant();

    @GET("/api/restaurants/{pk}/")
    Call<Restaurant> get_pk_restaruant(@Path("pk") int pk);
     */
}