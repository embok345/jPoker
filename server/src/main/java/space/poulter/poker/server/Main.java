package space.poulter.poker.server;

public class Main {
    public static void main(String[] args) {
        PokerServer server = new PokerServer(args);
        server.start();
    }
}
