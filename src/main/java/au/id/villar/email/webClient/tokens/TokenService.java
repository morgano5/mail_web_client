package au.id.villar.email.webClient.tokens;

public interface TokenService {

    String createToken(String ... pemissions);

    String[] retrievePermissions(String token);

}
