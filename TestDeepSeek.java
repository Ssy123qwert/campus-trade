import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestDeepSeek {
    public static void main(String[] args) throws Exception {
        String body = "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":10}";
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer sk-27739e888630412bb44d30bd6f3a1094")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status: " + resp.statusCode());
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
}
