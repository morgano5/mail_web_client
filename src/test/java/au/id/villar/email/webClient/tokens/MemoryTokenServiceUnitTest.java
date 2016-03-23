package au.id.villar.email.webClient.tokens;

import au.id.villar.email.webClient.TestAppConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestAppConfig.class)
public class MemoryTokenServiceUnitTest {

    private static final long REFRESH_TIME = 1000;
    private static final long EXPIRE_TIME = 2000;

    @Test
    public void basicFunctionality() throws InterruptedException {
        TokenService service = new MemoryTokenService(REFRESH_TIME, EXPIRE_TIME);

        TokenTestCase testCase = new TokenTestCase(service);
        testCase.run();
        AssertionError error = testCase.getErrorIfAny();
        if(error != null) throw error;
    }

    @Test
    public void loadingTest() throws InterruptedException {
        TokenService service = new MemoryTokenService(REFRESH_TIME, EXPIRE_TIME);

        int size = 1000;
        List<Thread> threadList = new ArrayList<>(size);
        List<TokenTestCase> tokenList = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            TokenTestCase token = new TokenTestCase(service);
            tokenList.add(token);
            Thread thread = new Thread(token);
            thread.setDaemon(true);
            thread.setName(String.format("Testing TokenService #%d", i));
            thread.start();
            threadList.add(thread);
        }
        for(Thread thread: threadList) thread.join();
        for(TokenTestCase token: tokenList) if(token.getErrorIfAny() != null) throw token.getErrorIfAny();
    }


    private class TokenTestCase implements Runnable {

        private TokenService service;
        private AssertionError error;

        TokenTestCase(TokenService service) {
            this.service = service;
        }

        @Override
        public void run() {

            try {
                TokenInfo token = service.createToken("userX", generateRandomPassword());
                Thread.sleep(REFRESH_TIME / 2);
                TokenInfo recoveredToken = service.getTokenInfo("xxx");
                assertNull("Service shouldn't return anything", recoveredToken);
                recoveredToken = service.getTokenInfo(token.getToken());
                assertNotNull("Service should return given token", recoveredToken);
                assertEquals("Retrieved token should be the same", token.getToken(), recoveredToken.getToken());

                Thread.sleep(EXPIRE_TIME / 2);
                recoveredToken = service.getTokenInfo(token.getToken());
                assertNotNull("Service should return the expected token", recoveredToken);
                assertFalse("Token must have changed", token.getToken().equals(recoveredToken.getToken()));
                assertEquals("Internal data shouldn't have changed", token.getPassword(), recoveredToken.getPassword());

                Thread.sleep(EXPIRE_TIME);
                recoveredToken = service.getTokenInfo(token.getToken());
                assertNull("Token must have expired", recoveredToken);
            } catch (InterruptedException ignored) {
            } catch (AssertionError e) {
                error = e;
            }

        }

        AssertionError getErrorIfAny() {
            return error;
        }

        private String generateRandomPassword() {
            java.util.Random random = new java.util.Random();
            int length = Math.abs(random.nextInt()) % 40 + 10;
            char[] chars = new char[length];
            for(int i = 0; i < length; i++) chars[i] = (char)((Math.abs(random.nextInt()) - 'a') % ('z' - 'a') + 'a');
            return new String(chars);
        }
    }

}
