package model.comms.util;

import model.env.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.PasswordAuthentication;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the authenticator used for Reddit API requests.
 */
public class RedditAuthenticatorTest {
    private RedditAuthenticator fixture;

    // Mocks
    private Environment mockEnv;

    // Setup
    @BeforeEach
    public void setup() {
        // Mock setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn("fish123");
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn("pingu's dark secrets");

        // Fixture setup
        fixture = new RedditAuthenticator();
        fixture.injectNewEnv(mockEnv);
    }

    // Tests
    /**
     * Tests that getting password authentication gives correct credentials.
     */
    @Test
    public void getPasswordAuthTest() {
        PasswordAuthentication auth = fixture.getPasswordAuthentication();
        assertThat(auth.getUserName(), equalTo("fish123"));
        assertThat(auth.getPassword(), equalTo("pingu's dark secrets".toCharArray()));
    }
}
