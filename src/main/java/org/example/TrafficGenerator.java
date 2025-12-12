package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class TrafficGenerator implements Runnable {
    private static final Logger log = LogManager.getLogger(TrafficGenerator.class);

    private final String host;
    private final int port;
    private final int count;
    private final int sizeBytes;
    private final int intervalMs;

    private final Consumer<PacketStats> onPacket;

    public TrafficGenerator(String host, int port, int count, int sizeBytes, int intervalMs,
                            Consumer<PacketStats> onPacket) {
        this.host = host;
        this.port = port;
        this.count = count;
        this.sizeBytes = sizeBytes;
        this.intervalMs = intervalMs;
        this.onPacket = onPacket;
    }

    @Override
    public void run() {
        // ВАЖНО: генератор НЕ должен биндиться на порт приёмника!
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress addr = InetAddress.getByName(host);

            for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
                long sendTimeNs = System.nanoTime();

                byte[] payload = new byte[sizeBytes];
                // первые 4 байта — sequence, дальше можно чем угодно
                ByteBuffer.wrap(payload).putInt(i);

                DatagramPacket packet = new DatagramPacket(payload, payload.length, addr, port);
                socket.send(packet);

                PacketStats s = new PacketStats(
                        i,                // sequence
                        sendTimeNs,        // sendTimeNs
                        0L,               // recvTimeNs (на генераторе не знаем)
                        sizeBytes,         // sizeBytes
                        0.0,              // delayMs (на генераторе не считаем)
                        "sent"
                );

                if (onPacket != null) onPacket.accept(s);

                log.info("Sent packet #{} ({} bytes) to {}:{}", i, sizeBytes, host, port);

                if (intervalMs > 0) Thread.sleep(intervalMs);
            }

        } catch (Exception e) {
            log.error("Generator error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
