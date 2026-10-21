package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailConfigurationTest {

    @Test
    void defaultConfigurationHasTestModeOff() {
        EmailConfiguration config = new EmailConfiguration(false, true);
        assertFalse(config.isTestMode());
        assertTrue(config.shouldArchiveCopy());
    }

    @Test
    void testModeCanBeEnabled() {
        EmailConfiguration config = new EmailConfiguration(true, false);
        assertTrue(config.isTestMode());
        assertFalse(config.shouldArchiveCopy());
    }

    @Test
    void archiveCopyCanBeDisabled() {
        EmailConfiguration config = new EmailConfiguration(false, false);
        assertFalse(config.isTestMode());
        assertFalse(config.shouldArchiveCopy());
    }
}
