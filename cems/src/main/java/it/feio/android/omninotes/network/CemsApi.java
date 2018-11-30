package it.feio.android.omninotes.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CemsApi {
    @POST("api-case/cases/")
    Call<CrimeCase> post_case(@Body CrimeCase crimeCase);

    @DELETE("api-case/cases/{number}/")
    Call<String> delete_case(@Path("number") String number);

    @Multipart
    @POST("api/evidences/")
    Call<ResponseBody> post_evidence(@Part("evi_case") RequestBody evi_case,
                                     @Part("evi_type") RequestBody evi_type,
                                     @Part("summary") RequestBody summary,
                                     @Part("evi_time") RequestBody evi_time,
                                     @Part MultipartBody.Part picture,
                                     @Part MultipartBody.Part signiture,
                                     @Part MultipartBody.Part record);
}