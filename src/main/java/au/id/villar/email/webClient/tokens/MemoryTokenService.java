package au.id.villar.email.webClient.tokens;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MemoryTokenService implements TokenService {

    private final Map<String, TokenInfo> tokenPermissions = new HashMap<>();
    private final Set<String> tokens = tokenPermissions.keySet();
    private final Object lock = new Object();

    @Override
    public TokenInfo createToken(String... permissions) {
        String[] copy = new String[permissions.length];
        System.arraycopy(permissions, 0, copy, 0, permissions.length);
        for(int i = 0; i < copy.length; i++) copy[i] = copy[i].intern();

        InternalTokenInfo token;
        synchronized(lock) {
            String strToken;
            do strToken = generateToken(); while(tokens.contains(strToken));
            token = new InternalTokenInfo(strToken, copy);
            tokenPermissions.put(strToken, token);
        }
        return token;
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        String[] permissions = null;
        synchronized(lock) {
            tokenPermissions.get(token)
            // TODO implement
        }
        return permissions;
    }

    @Override
    public void removeToken(String token) {
        synchronized(lock) {
            // TODO implement
        }
    }

    @Override
    public String refresh(String token) {
        // TODO implement
        return null;
    }

    private class InternalTokenInfo implements TokenInfo {

        private String token;
        private String[] permissions;

        public InternalTokenInfo(String token, String[] permissions) {
            this.token = token;
            this.permissions = permissions;
        }

        @Override
        public String getCurrentToken() {
            return token;
        }

        @Override
        public boolean containsPermission(String permission) {
            for(String regPermission: permissions) if(regPermission.equals(permission)) return true;
            return false;
        }

        private void setToken(String token) {
            this.token = token;
        }

    }

    private static final int TOKEN_RANDOM_PART_SIZE = 64;
    private static final int LIFE_TIME_MILLIS = 1_200_000;
    private static final int TIME_REFRES_MILLIS = 300_000;


    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_PART_SIZE];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + LIFE_TIME_MILLIS;
        return Base64.getEncoder().encodeToString(randomBytes) + '_' + startTime + '_' + endTime;
    }
}
