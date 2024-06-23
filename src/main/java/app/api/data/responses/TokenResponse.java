package app.api.data.responses;

public class TokenResponse{

    public String accessToken;
    public String refreshToken;

    public TokenResponse(String a, String r) {
        accessToken = a;
        refreshToken = r;
    }

}
