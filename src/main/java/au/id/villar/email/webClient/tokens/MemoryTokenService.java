package au.id.villar.email.webClient.tokens;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MemoryTokenService implements TokenService {

    private static final int TOKEN_SIZE = 64;
    private static final int EXPIRY_TIME_MILLIS = 1_200_000;
    private static final int REFRESH_TIME_MILLIS = 300_000;

    private final Map<String, InternalTokenInfo> tokenInfos = new HashMap<>();
    private final Set<String> tokens = tokenInfos.keySet();
    private final Object lock = new Object();

    @Override
    public TokenInfo createToken(String password, String... permissions) {
        InternalTokenInfo token;
        permissions = copyAndInternalize(permissions);
        synchronized(lock) {
            String strToken = generateToken();
            token = new InternalTokenInfo(strToken, password, permissions);
            tokenInfos.put(strToken, token);
        }
        return token;
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        InternalTokenInfo tokenInfo;
        long now = System.currentTimeMillis();
        synchronized(lock) {
            tokenInfo = tokenInfos.get(token);

            if(tokenInfo.getCreationTime() + EXPIRY_TIME_MILLIS > now) {
                tokenInfos.remove(token);
                return null;
            }

            if(tokenInfo.getCreationTime() + REFRESH_TIME_MILLIS > now) {
                tokenInfos.remove(token);
                String strToken = generateToken();
                tokenInfo = tokenInfo.clone(strToken);
                tokenInfos.put(strToken, tokenInfo);
            }
        }
        return tokenInfo;
    }

    @Override
    public void removeToken(String token) {
        synchronized(lock) {
            tokenInfos.remove(token);
        }
    }

    private String generateToken() {
        synchronized(lock) {
            String strToken;
            byte[] randomBytes = new byte[TOKEN_SIZE];
            do {
                ThreadLocalRandom.current().nextBytes(randomBytes);
                strToken = Base64.getEncoder().encodeToString(randomBytes);
            } while (tokens.contains(strToken));
            return strToken;
        }
    }

    private String[] copyAndInternalize(String[] permissions) {
        String[] copy = new String[permissions.length];
        System.arraycopy(permissions, 0, copy, 0, permissions.length);
        for(int i = 0; i < copy.length; i++) copy[i] = copy[i].intern();
        return copy;
    }

    private class InternalTokenInfo implements TokenInfo {

        private String token;
        private String[] permissions;
        private String password;
        private long creationTime;

        private InternalTokenInfo(String token, String password, String[] permissions) {
            this.token = token;
            this.password = password;
            this.creationTime = System.currentTimeMillis();
            this.permissions = permissions;
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public boolean containsPermission(String permission) {
            for(String regPermission: permissions) if(regPermission.equals(permission)) return true;
            return false;
        }

        @Override
        public String getPassword() {
            return password;
        }

        private long getCreationTime() {
            return creationTime;
        }

        private InternalTokenInfo clone(String newToken) {
            return new InternalTokenInfo(newToken, password, permissions);
        }

    }
}
