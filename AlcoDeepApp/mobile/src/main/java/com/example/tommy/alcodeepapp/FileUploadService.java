package com.example.tommy.alcodeepapp;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by Tommy on 2/1/2018.
 */

public interface FileUploadService {
    // new code for multiple files
    @Multipart
    @POST("upload")
    Call<ResponseBody> uploadMultipleFiles(
            @Part MultipartBody.Part phone,
            @Part MultipartBody.Part watch);
}
