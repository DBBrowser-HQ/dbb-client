package app.api;

import app.api.data.requests.*;
import app.api.data.responses.*;
import io.qt.core.QObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Callback;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.List;

public class ApiCalls {

    private static final String backendUrl = "http://db-cloud.ru:8081";//System.getProperty("backendUrl"); ////"http://192.168.0.108:8080";
    private static UserInterface userService = null;

    private static UserInterface getUserService() {
        if (userService == null) {
            userService = new Retrofit.Builder()
                .baseUrl(backendUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UserInterface.class);
        }
        return userService;
    }

    public static void signIn(QObject.Signal1<String> signal1, String login, String password) {

        var service = getUserService();

        LogInData data = new LogInData(login, password);

        service.login(data).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<MyResponse<TokenResponse>> call, Response<MyResponse<TokenResponse>> response) {
                if(response.code() == 200) {
                    UserDataRepository.userData = data;
                    UserDataRepository.accessToken = response.body().payload.accessToken;
                    UserDataRepository.refreshToken = response.body().payload.refreshToken;
                    signal1.emit("OK");
                }
                else {
                    try(ResponseBody errBody = response.errorBody()) {
                        if (response.code() == 401) {
                            signal1.emit("Wrong login/password");
                        }
                        else {
                            signal1.emit(errBody.string());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse<TokenResponse>> call, Throwable t) {
                System.out.println(t.toString());
                signal1.emit("Cannot connect to server");
            }
        });
    }

    public static void signUp(QObject.Signal1<String> signal1, String login, String password) {

        var service = getUserService();

        LogUpData data = new LogUpData(login, password);

        service.register(data).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code() == 200) {
                    UserDataRepository.userData = new LogInData(login, password);
                    signal1.emit("OK");
                }
                else {
                    try {
                        signal1.emit(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                signal1.emit("Er2");
            }
        });
    }

    public static void getUserOrganizations(QObject.Signal1<List<UserOrganization>> signalOk, QObject.Signal1<String> signalErr) {

        var service = getUserService();

        service.getUsersOrganizations("Bearer " + UserDataRepository.accessToken).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<MyResponse<List<UserOrganization>>> call, Response<MyResponse<List<UserOrganization>>> response) {
                if(response.code() == 200) {
                    signalOk.emit(response.body().payload);
                }
                else {
                    try(ResponseBody responseBody = response.errorBody()) {
                        signalErr.emit(responseBody.string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse<List<UserOrganization>>> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void createOrganization(QObject.Signal1<String> signal, String orgName) {

        var service = getUserService();

        service.createOrganization("Bearer " + UserDataRepository.accessToken, new Organization(orgName)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onResponsePost(response, signal, signal);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                signal.emit("Er2");
            }
        });
    }

    public static void renameOrganization(QObject.Signal1<String> signal, int orgId, String orgName) {

        var service = getUserService();

        service.renameOrganization("Bearer " + UserDataRepository.accessToken, orgId, new Organization(orgName)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onResponsePost(response, signal, signal);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                signal.emit("Er2");
            }
        });
    }

    public static void getDatasources(QObject.Signal1<List<Datasource>> signalOk, QObject.Signal1<String> signalErr) {

        var service = getUserService();

        service.getDatasources("Bearer " + UserDataRepository.accessToken, UserDataRepository.currentCompany.first).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<MyResponse<List<Datasource>>> call, Response<MyResponse<List<Datasource>>> response) {
                if(response.code() == 200) {
                    signalOk.emit(response.body().payload);
                }
                else {
                    try(ResponseBody responseBody = response.errorBody()) {
                        signalErr.emit(responseBody.string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse<List<Datasource>>> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void createDataSource(QObject.Signal1<String> callback, int id, String name) {

        var service = getUserService();

        service.createDatasource("Bearer " + UserDataRepository.accessToken, id, new CreateDatasource(name)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onResponsePost(response, callback, callback);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                callback.emit("Er2");
            }
        });
    }

    public static void deleteDataSource(int id, Runnable callback) {

        var service = getUserService();

        service.deleteDatasource("Bearer " + UserDataRepository.accessToken, id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code() == 200) {
                    System.out.println("OK");
                    callback.run();
                }
                else {
                    System.out.println(response.code());
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
            }
        });
    }

    public static void deleteOrganization(int id, Runnable callback) {

        var service = getUserService();

        service.deleteOrganization("Bearer " + UserDataRepository.accessToken, id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code() == 200) {
                    System.out.println("OK");
                    callback.run();
                }
                else {
                    System.out.println(response.code());
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
            }
        });
    }

    public static void addUser(int userId, int orgId, String role, QObject.Signal1<String> callback) {

        var service = getUserService();

        service.inviteUser(userId, "Bearer " + UserDataRepository.accessToken, new OrganizationIdAndRole(orgId, role)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onResponsePost(response, callback, callback);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
            }
        });
    }

    public static void getAllUsers(QObject.Signal1<Users> signalOk, QObject.Signal1<String> signalErr) {

        var service = getUserService();

        service.getAllUsers("Bearer " + UserDataRepository.accessToken).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<MyResponse<Users>> call, Response<MyResponse<Users>> response) {
                if(response.code() == 200) {
                    signalOk.emit(response.body().payload);
                }
                else {
                    try(ResponseBody responseBody = response.errorBody()) {
                        signalErr.emit(responseBody.string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse<Users>> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void getUsersInOrganization(QObject.Signal1<List<User>> signalOk, QObject.Signal1<String> signalErr, int id) {

        var service = getUserService();

        service.getUsersInOrganization("Bearer " + UserDataRepository.accessToken, id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<MyResponse<Users>> call, Response<MyResponse<Users>> response) {
                if(response.code() == 200) {
                    signalOk.emit(response.body().payload.rows);
                }
                else {
                    try(ResponseBody responseBody = response.errorBody()) {
                        signalErr.emit(responseBody.string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse<Users>> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void deleteUser(Runnable signalOk, QObject.Signal1<String> signalErr, int userId, int orgId) {

        var service = getUserService();

        service.deleteUser("Bearer " + UserDataRepository.accessToken, userId, new OrganizationId(orgId)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    signalOk.run();
                }
                else {
                    try {
                        signalErr.emit(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void changeUserRole(QObject.Signal1<String> signalOk, QObject.Signal1<String> signalErr, int userId, int orgId, String role) {

        var service = getUserService();

        service.changeUserRole("Bearer " + UserDataRepository.accessToken, userId, new OrganizationIdAndRole(orgId, role)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onResponsePost(response, signalOk, signalErr);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                signalErr.emit("Er2");
            }
        });
    }

    public static void changePassword(QObject.Signal1<String> signal1, String oldPassword, String newPassword) {

        var service = getUserService();

//        service.register(data).enqueue(new Callback<>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                System.out.println(response.body().toString());
//                if(response.code() == 200) {
//                    UserDataRepository.userData = new LogInData(login, password);
//                    signal1.emit("OK");
//                }
//                else {
//                    signal1.emit("Er1");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                System.out.println(t.toString());
//                signal1.emit("Er2");
//            }
//        });
    }

    private static void onResponsePost(Response<ResponseBody> response, QObject.Signal1<String> signalOk, QObject.Signal1<String> signalErr) {
        if(response.code() == 200) {
            signalOk.emit("OK");
        }
        else {
            try(ResponseBody responseBody = response.errorBody()) {
                signalErr.emit(responseBody.string());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
