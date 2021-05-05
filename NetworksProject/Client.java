import java.io.*;
import java.net.*;

class Client {

    public static void main(String args[]) throws Exception {
        String problem;
        String answer;
        Socket clientSocket = null;
        String name = args[0];

        // To receive math equation from user
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        try {
            //connect to server
            clientSocket = new Socket("127.0.0.1", 6789);
            // Establish sending stream to server
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            // Create input stream from user
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // send name to server
            outToServer.writeBytes(name + '\n');

            String nameAcknowledgment = inFromServer.readLine();
            // wait for server to acknowledge
            if (nameAcknowledgment.equals("Name received by server")) {
                // get math problem
                while (true) {
                    // prompt user for math problem
                    System.out.print("Enter a math problem: ");
                    problem = inFromUser.readLine();
                    // send math problem to server
                    outToServer.writeBytes(problem + '\n');
                    // Read answer from server, print
                    answer = inFromServer.readLine();
                    // end connection when requested
                    if (answer.equals("stopped")) {
                        break;
                    }

                    System.out.println("Answer: " + answer);

                    // close connection
                }
            }
            System.out.println("Stopping session. Session logs available in " + name + ".txt");
            inFromServer.close();
            inFromUser.close();
            outToServer.close();

        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
