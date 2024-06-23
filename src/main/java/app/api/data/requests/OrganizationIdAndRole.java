package app.api.data.requests;

public class OrganizationIdAndRole {
    public Integer organizationId;
    public String role;

    public OrganizationIdAndRole(int id, String r) {
        organizationId = id;
        role = r;
    }

}
