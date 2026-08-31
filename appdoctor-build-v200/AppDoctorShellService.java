package ch.piiwii.appdoctor;

import android.content.Context;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class AppDoctorShellService extends IAppDoctorShell.Stub {
    private static final int MAX_CHARS = 4_000_000;

    public AppDoctorShellService() {}

    public AppDoctorShellService(Context context) {}

    @Override
    public String exec(String command) throws RemoteException {
        if (command == null || command.length() > 4096) throw new RemoteException("Commande invalide");
        StringBuilder out = new StringBuilder();
        Process p = null;
        try {
            p = new ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() + line.length() + 1 > MAX_CHARS) {
                        out.append("\n[sortie tronquée par AppDoctor]\n");
                        break;
                    }
                    out.append(line).append('\n');
                }
            }
            if (!p.waitFor(45, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                out.append("\n[commande interrompue: timeout]\n");
            }
        } catch (Throwable t) {
            throw new RemoteException(t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            if (p != null) try { p.destroy(); } catch (Throwable ignored) {}
        }
        return out.toString();
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
