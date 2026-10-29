package tankops.client;

import tankops.client.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--self-test")) {
            System.exit(tankops.client.diagnostics.StartupCheck.run());
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
