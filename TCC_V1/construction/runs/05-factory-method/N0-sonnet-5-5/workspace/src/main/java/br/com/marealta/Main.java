package br.com.marealta;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) throw new IllegalStateException("DB_URL não definida");
        Db db = Db.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

        String port = System.getenv("PORT");
        int p = port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
        new Api(new Service(db)).create().start(p);
    }
}
