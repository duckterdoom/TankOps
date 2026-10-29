import java.awt.Toolkit;
import java.util.ServiceLoader;
import javax.accessibility.AccessibilityProvider;

/** Run with the packaged runtime, including when Windows Access Bridge is enabled. */
public final class RuntimeSmokeTest {
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("accessibility")) {
                boolean found = ServiceLoader.load(AccessibilityProvider.class).stream()
                        .map(ServiceLoader.Provider::get)
                        .anyMatch(provider -> provider.getName().equals("com.sun.java.accessibility.AccessBridge"));
                if (!found) throw new AssertionError("Windows Access Bridge provider is missing");
                System.setProperty("javax.accessibility.assistive_technologies", "com.sun.java.accessibility.AccessBridge");
            }
            Toolkit.getDefaultToolkit().getSystemEventQueue();
            System.out.println("PASS: AWT toolkit initialized, assistive technologies="
                    + System.getProperty("javax.accessibility.assistive_technologies", "user default"));
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
