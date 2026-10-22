package pousada;

public class Main {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) throw new IllegalStateException("DB_URL não definida");
        String portEnv = System.getenv("PORT");
        int port = portEnv == null || portEnv.isBlank() ? 7070 : Integer.parseInt(portEnv.trim());

        Store store = new Store(Database.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        new Api(new ReservationService(store), new Reports(store)).start(port);
    }
}
