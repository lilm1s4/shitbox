package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetLoadApp extends Application {
    private static final Logger log = LogManager.getLogger(NetLoadApp.class);

    // ===== Generator UI =====
    private TextField genHostField, genPortField, genCountField, genSizeField, genIntervalField;
    private Label genStatusLabel;

    // ===== Receiver UI =====
    private TextField recvPortField, recvExpectedField;
    private Label recvStatusLabel, recvLossLabel, recvReceivedLabel;
    private ProgressBar recvProgressBar;

    // ===== Common UI =====
    private TextArea logArea;

    // ===== Threads/State =====
    private Thread genThread;
    private Thread recvThread;

    private volatile boolean genRunning = false;
    private volatile boolean recvRunning = false;

    // receiver counters (for progress)
    private final AtomicInteger recvCount = new AtomicInteger(0);
    private final AtomicLong recvBytes = new AtomicLong(0);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("NetLoadApp");

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Генератор", createGeneratorPane()));
        tabs.getTabs().add(new Tab("Приёмник", createReceiverPane()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);

        VBox root = new VBox(10, tabs, new Label("Лог:"), logArea);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // ===================== UI BUILDERS =====================

    private Pane createGeneratorPane() {
        genHostField = new TextField("127.0.0.1");
        genPortField = new TextField("5000");
        genCountField = new TextField("100");
        genSizeField = new TextField("128");
        genIntervalField = new TextField("10");

        genStatusLabel = new Label("Ожидание...");

        Button startBtn = new Button("Старт генератора");
        Button stopBtn = new Button("Стоп генератора");

        startBtn.setOnAction(e -> startGenerator());
        stopBtn.setOnAction(e -> stopGenerator());

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(10));

        int r = 0;
        g.addRow(r++, new Label("Host/IP:"), genHostField);
        g.addRow(r++, new Label("Port:"), genPortField);
        g.addRow(r++, new Label("Кол-во пакетов:"), genCountField);
        g.addRow(r++, new Label("Размер пакета (байт):"), genSizeField);
        g.addRow(r++, new Label("Интервал (мс):"), genIntervalField);

        HBox buttons = new HBox(10, startBtn, stopBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, g, buttons, new Label("Статус:"), genStatusLabel);
        box.setPadding(new Insets(10));
        return box;
    }

    private Pane createReceiverPane() {
        recvPortField = new TextField("5000");
        recvExpectedField = new TextField("100");

        recvStatusLabel = new Label("Ожидание...");
        recvLossLabel = new Label("Потери: 0");
        recvReceivedLabel = new Label("Принято: 0");
        recvProgressBar = new ProgressBar(0);

        Button startBtn = new Button("Старт приёмника");
        Button stopBtn = new Button("Стоп приёмника");

        startBtn.setOnAction(e -> startReceiver());
        stopBtn.setOnAction(e -> stopReceiver());

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(10));

        int r = 0;
        g.addRow(r++, new Label("Port:"), recvPortField);
        g.addRow(r++, new Label("Ожидаемое кол-во:"), recvExpectedField);

        HBox buttons = new HBox(10, startBtn, stopBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox stats = new VBox(6, recvStatusLabel, recvReceivedLabel, recvLossLabel, recvProgressBar);
        stats.setPadding(new Insets(0, 0, 0, 10));

        VBox box = new VBox(10, g, buttons, new Separator(), stats);
        box.setPadding(new Insets(10));
        return box;
    }

    // ===================== GENERATOR =====================

    private void startGenerator() {
        if (genRunning) {
            showErr("Уже запущен генератор. Дождитесь завершения.");
            return;
        }

        String host = genHostField.getText().trim();
        int port = parseInt(genPortField.getText(), "Port");
        int count = parseInt(genCountField.getText(), "Кол-во пакетов");
        int size = parseInt(genSizeField.getText(), "Размер пакета");
        int interval = parseInt(genIntervalField.getText(), "Интервал");

        if (port < 1 || port > 65535) { showErr("Port должен быть 1..65535"); return; }
        if (count < 1) { showErr("Кол-во пакетов должно быть > 0"); return; }
        if (size < 4) { showErr("Размер пакета должен быть >= 4 (там лежит sequence)"); return; }
        if (interval < 0) { showErr("Интервал не может быть отрицательным"); return; }

        genRunning = true;
        genStatusLabel.setText("Запущен");
        appendLog("Генератор старт: host=" + host + " port=" + port + " count=" + count +
                " size=" + size + " interval=" + interval);

        TrafficGenerator generator = new TrafficGenerator(
                host, port, count, size, interval,
                s -> Platform.runLater(() -> appendLog("Отправлен пакет #" + s.getSequence() + " (" + s.getSizeBytes() + " bytes)"))
        );

        genThread = new Thread(generator, "generator-thread");
        genThread.setDaemon(true);
        genThread.setUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> {
            genRunning = false;
            genStatusLabel.setText("Ошибка");
            appendLog("Ошибка генератора: " + e.getMessage());
            log.error("Generator crashed", e);
            showErr("Ошибка генератора: " + e.getMessage());
        }));

        genThread.start();

        // watcher
        new Thread(() -> {
            try { genThread.join(); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                genRunning = false;
                genStatusLabel.setText("Готово");
                appendLog("Генератор завершён.");
            });
        }, "gen-joiner").start();
    }

    private void stopGenerator() {
        if (!genRunning || genThread == null) {
            showErr("Генератор не запущен.");
            return;
        }
        appendLog("Остановка генератора...");
        genThread.interrupt();
        genRunning = false;
        genStatusLabel.setText("Остановлен");
    }

    // ===================== RECEIVER =====================

    private void recvResetReceiverCounters(int expected) {
        recvCount.set(0);
        recvBytes.set(0);

        recvProgressBar.setProgress(0);
        recvReceivedLabel.setText("Принято: 0");
        recvLossLabel.setText("Потери: " + expected);
    }


    private void startReceiver() {
        if (recvRunning) {
            showErr("Уже запущен приёмник. Дождитесь завершения.");
            return;
        }

        int port = parseInt(recvPortField.getText(), "Port");
        int expected = parseInt(recvExpectedField.getText(), "Ожидаемое кол-во");

        if (port < 1 || port > 65535) { showErr("Port должен быть 1..65535"); return; }
        if (expected < 1) { showErr("Ожидаемое кол-во должно быть > 0"); return; }

        recvResetReceiverCounters(expected);

        recvRunning = true;
        recvStatusLabel.setText("Запущен");
        appendLog("Приёмник старт: port=" + port + " expected=" + expected);

        TrafficReceiver receiver = new TrafficReceiver(
                port,
                expected,
                ps -> Platform.runLater(() -> {
                    int c = recvCount.incrementAndGet();
                    recvBytes.addAndGet(ps.getSizeBytes());
                    recvReceivedLabel.setText("Принято: " + c);
                    recvProgressBar.setProgress(Math.min(1.0, (double) c / expected));
                    appendLog("Принят пакет #" + ps.getSequence() + " (" + ps.getSizeBytes() + " bytes)");
                }),
                summary -> Platform.runLater(() -> {
                    recvRunning = false;
                    recvStatusLabel.setText("Готово");
                    recvLossLabel.setText("Потери: " + summary.getLost());
                    appendLog("Приёмник завершён: received=" + summary.getReceived() + ", lost=" + summary.getLost());
                })
        );

        recvThread = new Thread(receiver, "receiver-thread");
        recvThread.setDaemon(true);
        recvThread.setUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> {
            recvRunning = false;
            recvStatusLabel.setText("Ошибка");
            appendLog("Ошибка приёмника: " + e.getMessage());
            log.error("Receiver crashed", e);
            showErr("Ошибка приёмника: " + e.getMessage());
        }));

        recvThread.start();
    }

    private void stopReceiver() {
        if (!recvRunning || recvThread == null) {
            showErr("Приёмник не запущен.");
            return;
        }
        appendLog("Остановка приёмника...");
        recvThread.interrupt();
        recvRunning = false;
        recvStatusLabel.setText("Остановлен");
    }

    private void resetReceiverCounters(int expected) {
        recvCount.set(0);
        recvBytes.set(0);
        recvReceivedLabel.setText("Принято: 0");
        recvLossLabel.setText("Потери: 0");
        recvProgressBar.setProgress(0);
    }

    // ===================== HELPERS =====================

    private int parseInt(String s, String fieldName) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            showErr("Неверное число в поле: " + fieldName);
            throw e;
        }
    }

    private void appendLog(String msg) {
        if (logArea != null) {
            logArea.appendText(msg + "\n");
        }
        // дублируем в log4j тоже
        log.info(msg);
    }



    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.showAndWait();
    }
}
