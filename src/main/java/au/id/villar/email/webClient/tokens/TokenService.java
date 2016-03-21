package au.id.villar.email.webClient.tokens;

interface TokenService {

    TokenInfo createToken(String password, String ... permissions);

    TokenInfo getTokenInfo(String token);

    void removeToken(String token);

}
