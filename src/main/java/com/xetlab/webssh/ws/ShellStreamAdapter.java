package com.xetlab.webssh.ws;

import net.schmizz.concurrent.Event;
import net.schmizz.concurrent.ExceptionChainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;

public class ShellStreamAdapter {

    private static final Logger log = LoggerFactory.getLogger(ShellStreamAdapter.class);

    private InputStream in;
    private WebSocketSession session;

    private int bufSize = 1;

    public ShellStreamAdapter(InputStream in, WebSocketSession session) {
        this.in = in;
        this.session = session;
    }

    public ShellStreamAdapter bufSize(int bufSize) {
        this.bufSize = bufSize;
        return this;
    }

    public Event<IOException> spawn(String name) {
        return spawn(name, false);
    }

    public Event<IOException> spawnDaemon(String name) {
        return spawn(name, true);
    }

    private Event<IOException> spawn(final String name, final boolean daemon) {
        final Event<IOException> doneEvent =
                new Event<IOException>("copyDone", new ExceptionChainer<IOException>() {
                    @Override
                    public IOException chain(Throwable t) {
                        return (t instanceof IOException) ? (IOException) t : new IOException(t);
                    }
                }, net.schmizz.sshj.common.LoggerFactory.DEFAULT);

        new Thread() {
            {
                setName(name);
                setDaemon(daemon);
            }

            @Override
            public void run() {
                try {
                    log.debug("Will copy from {} to websocket", in);
                    copy();
                    log.debug("Done copying from {}", in);
                    doneEvent.set();
                } catch (IOException ioe) {
                    log.error(String.format("In pipe from %1$s to websocket", in.toString()), ioe);
                    doneEvent.deliverError(ioe);
                }
            }
        }.start();
        return doneEvent;
    }

    public long copy()
            throws IOException {
        final byte[] buf = new byte[bufSize];
        long count = 0;
        int read;

        final long startTime = System.currentTimeMillis();

        while ((read = in.read(buf)) != -1) {
            count += write(buf, read);
        }

        final double timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        final double sizeKiB = count / 1024.0;
        log.debug(String.format("%1$,.1f KiB transferred in %2$,.1f seconds (%3$,.2f KiB/s)", sizeKiB, timeSeconds, (sizeKiB / timeSeconds)));

        if (read == -1)
            throw new IOException("Encountered EOF, could not transfer bytes");

        return count;
    }

    private long write(byte[] buf, int length) throws IOException {
        byte[] bytesToSend = new byte[length];
        System.arraycopy(buf, 0, bytesToSend, 0, length);
        session.sendMessage(new TextMessage(bytesToSend));
        return buf.length;
    }
}
