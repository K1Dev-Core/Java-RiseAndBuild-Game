public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("server")) {
            server.GameServer.main(args);
        } else {
            client.GameClient.main(args);
        }
    }
}
