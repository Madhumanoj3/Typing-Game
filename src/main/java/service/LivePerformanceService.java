package service;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletionStage;

/**
 * Streams live typing metrics to in-app listeners and, when configured, a WebSocket endpoint.
 */
public class LivePerformanceService {

    public record LiveMetric(String username, String context, double wpm, double accuracy,
                             int errors, int words, double progress, LocalDateTime at) {}
    public record LiveNotification(String username, String title, String message, LocalDateTime at) {}

    public interface MetricListener {
        void onMetric(LiveMetric metric);
    }

    public interface NotificationListener {
        void onNotification(LiveNotification notification);
    }

    private static LivePerformanceService instance;

    private final List<MetricListener> metricListeners = new CopyOnWriteArrayList<>();
    private final List<NotificationListener> notificationListeners = new CopyOnWriteArrayList<>();
    private WebSocket webSocket;

    private LivePerformanceService() {
        connectWebSocket(loadConfiguredEndpoint());
    }

    public static LivePerformanceService getInstance() {
        if (instance == null) instance = new LivePerformanceService();
        return instance;
    }

    public void connectWebSocket(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        try {
            HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(endpoint), new WebSocket.Listener() {
                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                            ws.request(1);
                            return null;
                        }
                    })
                    .thenAccept(ws -> webSocket = ws)
                    .exceptionally(ex -> null);
        } catch (Exception ignored) {
            webSocket = null;
        }
    }

    public void addMetricListener(MetricListener listener) {
        if (listener != null) metricListeners.add(listener);
    }

    public void removeMetricListener(MetricListener listener) {
        metricListeners.remove(listener);
    }

    public void addNotificationListener(NotificationListener listener) {
        if (listener != null) notificationListeners.add(listener);
    }

    public void removeNotificationListener(NotificationListener listener) {
        notificationListeners.remove(listener);
    }

    public void publishMetric(String username, String context, double wpm, double accuracy,
                              int errors, int words, double progress) {
        LiveMetric metric = new LiveMetric(username, context, wpm, accuracy, errors, words, progress, LocalDateTime.now());
        for (MetricListener listener : metricListeners) {
            Platform.runLater(() -> listener.onMetric(metric));
        }
        sendJson(String.format(
                "{\"type\":\"metric\",\"username\":\"%s\",\"context\":\"%s\",\"wpm\":%.1f,\"accuracy\":%.1f,\"errors\":%d,\"words\":%d,\"progress\":%.3f}",
                escape(username), escape(context), wpm, accuracy, errors, words, progress));
    }

    public void publishNotification(String username, String title, String message) {
        LiveNotification notification = new LiveNotification(username, title, message, LocalDateTime.now());
        for (NotificationListener listener : notificationListeners) {
            Platform.runLater(() -> listener.onNotification(notification));
        }
        sendJson(String.format(
                "{\"type\":\"notification\",\"username\":\"%s\",\"title\":\"%s\",\"message\":\"%s\"}",
                escape(username), escape(title), escape(message)));
    }

    private void sendJson(String json) {
        WebSocket ws = webSocket;
        if (ws != null) {
            try {
                ws.sendText(json, true);
            } catch (Exception ignored) {
                webSocket = null;
            }
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String loadConfiguredEndpoint() {
        try (var in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("live.websocket.url", "");
        } catch (Exception ignored) {
            return "";
        }
    }
}
