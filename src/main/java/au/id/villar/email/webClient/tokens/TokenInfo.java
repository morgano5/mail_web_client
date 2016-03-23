package au.id.villar.email.webClient.tokens;

public interface TokenInfo {

    String getToken();

    boolean containsPermission(String ... permission);

    String getUsername();

    String getPassword();
}
