package com.xetlab.webssh.ws;

import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TerminalWebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TerminalWebSocketHandler.class);
    private SSHClient ssh;
    private static Map<String, Session> sshSessions = new ConcurrentHashMap<>();
    private static Map<String, Session.Shell> shells = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info(">>>received text message:{}", message.getPayload());
        Session.Shell shell = shells.get(session.getId());
        shell.getOutputStream().write(message.asBytes());
        shell.getOutputStream().flush();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        logger.info(">>>received binary message, length:{}", message.getPayloadLength());
        Session.Shell shell = shells.get(session.getId());
        shell.getOutputStream().write(message.getPayload().array());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (ssh == null || !ssh.isConnected()) {
            initSSH();
        }
        final String sessionId = session.getId();
        Session sshSession = sshSessions.get(sessionId);
        if (sshSession == null || !sshSession.isOpen()) {
            sshSessions.remove(sessionId);
            sshSession = ssh.startSession();
            sshSession.allocateDefaultPTY();
            sshSessions.put(sessionId, sshSession);
        }

        Session.Shell shell = shells.get(sessionId);
        if (shell == null || !shell.isOpen()) {
            shells.remove(sessionId);
            shell = sshSession.startShell();
            shells.put(sessionId, shell);
        }

        new ShellStreamAdapter(shell.getInputStream(), session)
                .bufSize(shell.getLocalMaxPacketSize())
                .spawn("stdout");

        new ShellStreamAdapter(shell.getErrorStream(), session)
                .bufSize(shell.getLocalMaxPacketSize())
                .spawn("stderr");

    }

    private void initSSH() throws IOException {
        DefaultConfig defaultConfig = new DefaultConfig();
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
        ssh = new SSHClient(defaultConfig);
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.getConnection().getKeepAlive().setKeepAliveInterval(60);
        ssh.loadKnownHosts();

        ssh.connect("192.168.1.29");
        ssh.authPassword("root", "linux123456");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        final String sessionId = session.getId();
        Session sshSession = sshSessions.get(sessionId);
        if (sshSession != null) {
            sshSession.close();
            sshSessions.remove(sessionId);
        }
    }
}
