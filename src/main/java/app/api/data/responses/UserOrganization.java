package app.api.data.responses;

public class UserOrganization {

    public int id;
    public String name;
    public String role;

    public UserOrganization(int id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

}
