package au.id.villar.email.webClient.tokens;

public interface TokenInfo {

    String getCurrentToken();

    boolean containsPermission(String permission);

}
