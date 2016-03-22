package au.id.villar.email.webClient.tokens;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MemoryTokenService implements TokenService {

    private static final Logger LOG = Logger.getLogger(MemoryTokenService.class);

    private static final int TOKEN_SIZE = 64;

    private static final int DEFAULT_EXPIRY_TIME_MILLIS = 1_200_000;
    private static final int DEFAULT_REFRESH_TIME_MILLIS = 300_000;

    private final Map<String, InternalTokenInfo> tokenInfos = new HashMap<>();
    private final Set<String> tokens = tokenInfos.keySet();
    private final Object lock = new Object();
    private final long refreshTimeMillis;
    private final long expiryTimeMillis;

    public MemoryTokenService() {
        this(DEFAULT_REFRESH_TIME_MILLIS, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    public MemoryTokenService(long refreshTimeMillis, long expiryTimeMillis) {
        this.refreshTimeMillis = refreshTimeMillis;
        this.expiryTimeMillis = expiryTimeMillis;
        Thread cleaner = new Thread(new Cleaner());
        cleaner.setDaemon(true);
        cleaner.setName("MemoryTokenService Cleaner");
        cleaner.start();
    }

    @Override
    public TokenInfo createToken(String username, String password, String... permissions) {
        InternalTokenInfo token;
        permissions = copyAndInternalize(permissions);
        synchronized(lock) {
            String strToken = generateToken();
            token = new InternalTokenInfo(strToken, username, password, permissions);
            tokenInfos.put(strToken, token);
        }

        LOG.info("Token created");

        return token;
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        InternalTokenInfo tokenInfo;
        long now = System.currentTimeMillis();
        synchronized(lock) {
            tokenInfo = tokenInfos.get(token);
            if(tokenInfo == null) return null;

            if(tokenInfo.getCreationTime() + expiryTimeMillis < now) {
                tokenInfos.remove(token);
                return null;
            }

            if(tokenInfo.getCreationTime() + refreshTimeMillis < now) {
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

        LOG.info("Token removed");
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
        private String username;
        private String password;
        private long creationTime;

        private InternalTokenInfo(String token, String username, String password, String[] permissions) {
            this.token = token;
            this.username = username;
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
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        private long getCreationTime() {
            return creationTime;
        }

        private InternalTokenInfo clone(String newToken) {
            return new InternalTokenInfo(newToken, username, password, permissions);
        }

    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(expiryTimeMillis * 3 / 2);
                while(!Thread.interrupted()) {
                    Thread.sleep(expiryTimeMillis);
                    LOG.info("Token cleaning up started");
                    long now = System.currentTimeMillis();
                    synchronized (lock) {
                        Iterator<Map.Entry<String, InternalTokenInfo>> iterator = tokenInfos.entrySet().iterator();
                        while(iterator.hasNext()) {
                            Map.Entry<String, InternalTokenInfo> entry = iterator.next();
                            if(entry.getValue().getCreationTime() + expiryTimeMillis < now) iterator.remove();
                        }
                    }
                    LOG.info("Token cleaning up finished");
                }
            } catch(InterruptedException ignore) {}
        }
    }
}
