package space.poulter.poker.server;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                new PokerServer(Integer.parseInt(args[0]));
            } catch (NumberFormatException nfE) {
                System.err.println("Not a valid port number");
                System.exit(1);
            }
        } else {
            PokerServer server = new PokerServer();
        }

    }
}
