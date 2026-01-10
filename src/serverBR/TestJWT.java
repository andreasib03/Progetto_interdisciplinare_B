public class TestJWT {
    public static void main(String[] args) {
        try {
            Class.forName("it.uninsubria.server.util.JWTUtil");
            System.out.println("✓ JWTUtil loaded successfully");
            System.out.println("✓ ExceptionInInitializerError should be fixed");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
