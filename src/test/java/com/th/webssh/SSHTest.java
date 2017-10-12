package com.th.webssh;

import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class SSHTest {
    public static void main(String args[]) throws Exception {
        DefaultConfig defaultConfig = new DefaultConfig();
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
        final SSHClient ssh = new SSHClient(defaultConfig);
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.getConnection().getKeepAlive().setKeepAliveInterval(5);
        ssh.loadKnownHosts();

        ssh.connect("192.168.0.107");
        try {
//            ssh.authPublickey(System.getProperty("user.name"));
            ssh.authPassword("xetlab", "gs825078");
            final Session session = ssh.startSession();
            try {
                session.allocateDefaultPTY();

                final Session.Shell shell = session.startShell();

                new StreamCopier(shell.getInputStream(), System.out, LoggerFactory.DEFAULT)
                        .bufSize(shell.getLocalMaxPacketSize())
                        .spawn("stdout");

                new StreamCopier(shell.getErrorStream(), System.err, LoggerFactory.DEFAULT)
                        .bufSize(shell.getLocalMaxPacketSize())
                        .spawn("stderr");

                // Now make System.in act as stdin. To exit, hit Ctrl+D (since that results in an EOF on System.in)
                // This is kinda messy because java only allows console input after you hit return
                // But this is just an example... a GUI app could implement a proper PTY
                new StreamCopier(System.in, shell.getOutputStream(), LoggerFactory.DEFAULT)
                        .bufSize(shell.getRemoteMaxPacketSize())
                        .copy();
            } finally {
                session.close();
            }
        } finally {
            ssh.disconnect();
        }
    }
}
