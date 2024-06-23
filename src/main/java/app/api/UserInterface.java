package app.api;

import app.api.data.requests.*;
import app.api.data.responses.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface UserInterface {

    /** auth-----------------  */
    @POST("/auth/sign-in")
    Call<MyResponse<TokenResponse>> login(@Body LogInData data);

    @POST("auth/sign-up")
    Call<ResponseBody> register(@Body LogUpData data);

    @POST("auth/refresh")
    Call<ResponseBody> refresh(@Body RefreshToken token);

    @POST("auth/logout")
    Call<ResponseBody> logout(@Header("Authorization") String token);

    /** organizations-----------------  */

    @POST("api/organizations/invite/{id}")
    Call<ResponseBody> inviteUser(@Path("id") Integer userId, @Header("Authorization") String token, @Body OrganizationIdAndRole body);

    @POST("api/organizations/kick/{id}")
    Call<ResponseBody> deleteUser(@Header("Authorization") String token, @Path("id") Integer userId, @Body OrganizationId body);

    @POST("api/organizations/change-role/{id}")
    Call<ResponseBody> changeUserRole(@Header("Authorization") String token, @Path("id") Integer userId, @Body OrganizationIdAndRole body);

    @PATCH("api/organizations/{id}")
    Call<ResponseBody> renameOrganization(@Header("Authorization") String token, @Path("id") Integer id, @Body Organization body);

    @GET("api/organizations")
    Call<MyResponse<List<UserOrganization>>> getUsersOrganizations(@Header("Authorization") String token);

    @POST("api/organizations")
    Call<ResponseBody> createOrganization(@Header("Authorization") String token, @Body Organization body);

    @DELETE("api/organizations/{id}")
    Call<ResponseBody> deleteOrganization(@Header("Authorization") String token, @Path("id") int id);


    /**  Datasources   */

    @GET("api/datasources/{id}")
    Call<MyResponse<List<Datasource>>> getDatasources(@Header("Authorization") String token, @Path("id") int id);

    @POST("api/datasources/{id}")
    Call<ResponseBody> createDatasource(@Header("Authorization") String token, @Path("id") int id, @Body CreateDatasource name);

    @DELETE("api/datasources/{id}")
    Call<ResponseBody> deleteDatasource(@Header("Authorization") String token, @Path("id") int id);


    /**  Users   */

    @GET("api/users")
    Call<MyResponse<Users>> getAllUsers(@Header("Authorization") String token);

    @GET("api/users/{id}")
    Call<MyResponse<Users>> getUsersInOrganization(@Header("Authorization") String token, @Path("id") int id);

}
