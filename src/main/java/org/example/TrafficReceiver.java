package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class TrafficReceiver implements Runnable {
    private static final Logger log = LogManager.getLogger(TrafficReceiver.class);

    private final int port;
    private final int expected;
    private final Consumer<PacketStats> onPacket;
    private final Consumer<ReceiverSummary> onSummary;

    public TrafficReceiver(int port, int expected,
                           Consumer<PacketStats> onPacket,
                           Consumer<ReceiverSummary> onSummary) {
        this.port = port;
        this.expected = expected;
        this.onPacket = onPacket;
        this.onSummary = onSummary;
    }

    @Override
    public void run() {
        int received = 0;
        long totalBytes = 0;
        double totalDelayMs = 0.0;

        // ВАЖНО: приёмник ДОЛЖЕН слушать порт
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(1000); // чтобы корректно выходить по interrupt

            while (received < expected && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] buf = new byte[65507];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    long recvTimeNs = System.nanoTime();
                    int len = p.getLength();

                    int seq = ByteBuffer.wrap(p.getData(), 0, Math.min(4, len)).getInt();
                    // sendTimeNs мы НЕ знаем (генератор не кладёт его в пакет), поэтому delayMs = 0
                    // Если хочешь реальную задержку — надо класть sendTimeNs в payload.

                    PacketStats s = new PacketStats(
                            seq,
                            0L,
                            recvTimeNs,
                            len,
                            0.0,
                            "recv"
                    );

                    received++;
                    totalBytes += len;
                    if (onPacket != null) onPacket.accept(s);

                    log.info("Received packet #{} ({} bytes) from {}:{}",
                            seq, len, p.getAddress().getHostAddress(), p.getPort());

                } catch (java.net.SocketTimeoutException ignore) {
                    // просто даём шанс выйти по interrupt
                }
            }

        } catch (Exception e) {
            log.error("Receiver error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            ReceiverSummary summary = new ReceiverSummary(
                    expected,
                    received,
                    expected - received,
                    totalBytes,
                    received == 0 ? 0.0 : (totalDelayMs / received)
            );
            if (onSummary != null) onSummary.accept(summary);
            log.info("Receiver finished: expected={}, received={}, lost={}",
                    expected, received, (expected - received));
        }
    }
}
