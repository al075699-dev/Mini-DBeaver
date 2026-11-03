package database;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshTunnel {

    private Session session;
    private int localPort;

    public void abrir(String host, String usuario, String password, String hostBd)
            throws JSchException {

        JSch jsch = new JSch();
        session = jsch.getSession(usuario, host, 22);
        session.setPassword(password);
        localPort = session.setPortForwardingL(0, hostBd, 3306);


        session.setConfig("StrictHostKeyChecking", "no");

        session.connect(10_000); // timeout de 10 s


        // Redirige a localhost:3306 visto DESDE el servidor
        localPort = session.setPortForwardingL(0, "localhost", 3306);
    }

    public int getLocalPort() {
        return localPort;
    }

    public boolean estaAbierto() {
        return session != null && session.isConnected();
    }

    public void cerrar() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
