import java.net.InetSocketAddress;
import java.net.Socket;

public class TestSocket {
    public static void main(String[] args) throws Exception {
        // Test 1: DNS resolution
        System.out.println("=== DNS ===");
        for (var addr : java.net.InetAddress.getAllByName("api.deepseek.com")) {
            System.out.println(addr);
        }

        // Test 2: TCP socket connect to port 443
        System.out.println("\n=== TCP connect ===");
        try (var sock = new Socket()) {
            sock.connect(new InetSocketAddress("api.deepseek.com", 443), 10000);
            System.out.println("TCP connected!");
        } catch (Exception e) {
            System.out.println("TCP FAIL: " + e.getClass().getName() + " - " + e.getMessage());
        }

        // Test 3: Can we reach a normal site?
        System.out.println("\n=== TCP to httpbin.org ===");
        try (var sock = new Socket()) {
            sock.connect(new InetSocketAddress("httpbin.org", 443), 10000);
            System.out.println("httpbin.org connected!");
        } catch (Exception e) {
            System.out.println("httpbin.org FAIL: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
}
