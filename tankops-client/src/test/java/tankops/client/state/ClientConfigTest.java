package tankops.client.state;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientConfigTest {
    @Test
    void tailscaleIpv4AndMagicDnsUseDefaultPort() {
        assertEquals("100.80.10.20:5555", ClientConfig.normalizeServerAddress(" 100.80.10.20 "));
        assertEquals("ubuntu.example.ts.net:5555", ClientConfig.normalizeServerAddress("ubuntu.example.ts.net"));
        assertEquals("localhost:5555", ClientConfig.normalizeServerAddress(""));
    }

    @Test
    void explicitPortIsPreserved() {
        assertEquals("100.80.10.20:6000", ClientConfig.normalizeServerAddress("100.80.10.20:6000"));
    }

    @Test
    void malformedAddressAndPortAreRejectedBeforeConnecting() {
        for (String value : new String[] {"http://100.80.10.20", "100.80.10.20:", "host:no", "host:0", "host:65536", "two hosts", ":5555"}) {
            assertThrows(IllegalArgumentException.class, () -> ClientConfig.normalizeServerAddress(value), value);
        }
    }
}
