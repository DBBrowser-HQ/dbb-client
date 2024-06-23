package app.api;

import app.api.data.AccessToken;
import app.api.data.requests.LogInData;
import com.google.gson.Gson;
import io.qt.core.QPair;

import java.util.Base64;

public class UserDataRepository {

    public static LogInData userData = new LogInData("", "");
    public static String accessToken = "";
    public static String refreshToken = "";
    public static QPair<Integer, String> currentCompany;

    public static int getUserId() {
        String[] chunks = accessToken.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        Gson gson = new Gson();
        return gson.fromJson(payload, AccessToken.class).userId;
    }

    public static void logOut() {
        userData = new LogInData("", "");
        accessToken = "";
        refreshToken = "";
    }

}
