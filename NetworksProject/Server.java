import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Stack;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Server implements Runnable {
  static ServerSocket welcomeSocket;
  final Socket socket;
  String name;
  long startTime;
  BufferedReader inFromClient;
  DataOutputStream outToClient;
  FileWriter fWriter;
  PrintWriter writer;
  String state;
  SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z MMMM dd, yyyy");
  static int threadCount = 0;

  // server constructor
  public Server(Socket socket) throws IOException {
    // Initialize socket
    this.socket = socket;
    // Initialize input buffer
    this.inFromClient = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    // Initialize output buffer
    this.outToClient = new DataOutputStream(socket.getOutputStream());
    // set state of client to running
    this.state = "running";
  }

  // receives name from client, assigns to variable
  void receiveClientNameProtocol() throws IOException {
    String clientName = inFromClient.readLine();
    this.name = clientName;
  }

  // protocol: sends message to client
  void sendClientMessageProtocol(String msg) throws IOException {
    outToClient.writeBytes(msg + "\n");
  }

  // protocol: parses and solves the math problem provided)
  String solveMathExpressionProtocol(String exp) throws Exception {
    ScriptEngineManager sem = new ScriptEngineManager();
    ScriptEngine se;
    if (!(sem.getEngineByName("JavaScript") == null)) {
      se = sem.getEngineByName("JavaScript");
    }
    else {
      Stack<Double> numbers = new Stack<Double>();
      Stack<String> operators = new Stack<String>();

      while (!exp.equals("")) {
        //get first operator or operand
        int index = exp.indexOf(" ");

        //if end of exp reached
        if (index == -1) {
          numbers.push(Double.parseDouble(exp));
          exp = "";
          continue;
        }
        String str = exp.substring(0, index);
        exp = exp.substring(index + 1);
        try {
          //check if number
          double number = Double.parseDouble(str);

          //push number to number stack
          numbers.push(number);
        } catch (Exception e) {
          //since error, must be an operator
          String op = str;
          if (str.equals("*") || str.equals("/")) {
            double firstOperand = numbers.pop();
            double secondOperand;
            index = exp.indexOf(" ");
            if (index == -1) {
              secondOperand = Double.parseDouble(exp);
              exp = "";
            } else {
              //extract operator from expression
              str = exp.substring(0, index);
              //uodate expression
              exp = exp.substring(index + 1);
              secondOperand = Integer.parseInt(str);
            }
            double result;
            if(op.equals("*")) {
              result = firstOperand * secondOperand;
            }
            else {
             result = firstOperand / secondOperand;
            }
            numbers.push(result);
          }
          else {
            operators.push(op);
          }
        }
      }
      //run until only one operand left in stack
      while(!(numbers.size() == 1)) {
        double eval;
        double secondOperand = numbers.pop();
        double firstOperand = numbers.pop();
        String operator = operators.pop();
        //If operator is plus sign, add numbers
        if (operator.equals("+")) {
          eval = firstOperand + secondOperand;
        } else {
          //subtract numbers if minus sign
          eval = firstOperand - secondOperand;
        }
        //store result in operand stack
        numbers.push(eval);
      }
      return numbers.pop().toString();
    }
    String answer = se.eval(exp).toString();

    return answer;
  }

  // protocol: creates log files for each client
  void createFile() throws IOException {
    fWriter = new FileWriter(this.name + ".txt", true);
    writer = new PrintWriter(fWriter);
  }

  // gets the current unix epoch time (for logging purposes)
  long getCurrentTime() {
    return Instant.now().toEpochMilli();
  }

  // protocol: logs event information to corresponding client's file
  void logInfoProtocol(long unixTime, String action, String value) throws IOException {
    writer.write("[" + sdf.format(unixTime) + "]:\t" + this.name + " " + action + " " + value + "\n");
    fWriter.flush();
  }

  // close all existing connections
  void closeConnections() throws Exception {
    fWriter.close();
    writer.close();
    inFromClient.close();
    outToClient.close();
    socket.close();
    // decrement number of client threads running
    threadCount--;
  }

  // welcome new clients
  static void runWelcomingThread() {

    new Thread("Welcoming thread") {
      public void run() {
        while (true) {
          // Create new client connection
          try {
            // Welcoming connection
            Socket clientSocket = welcomeSocket.accept();
            // start new client thread
            new Thread(new Server(clientSocket)).start();
            // Increment number of client threads running
            threadCount++;
          } catch (IOException e) {
            break;
          }
        }
      }
    }.start();
  }

  // wait for stop server message and stop welcome thread
  static void runStoppingThread() {
    new Thread("Stopping thread") {
      public void run() {
        // create input buffer from console
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
          try {
            // if server told to stop by user
            if (inFromUser.readLine().equals("stop")) {
              // if no threads are running (no clients connected)
              if (threadCount == 0) {
                // close welcoming thread and exit
                welcomeSocket.close();
                break;
              } else {
                System.out.println("Can't stop server while clients still connected!");
              }
            }
          } catch (Exception e) {
            System.out.println(e);
          }

        }
      }
    }.start();
  }

  public static void main(String args[]) throws Exception {

    // Intialize server socket running on port 6789
    welcomeSocket = new ServerSocket(6789);

    // run thread to welcome incoming clients
    runWelcomingThread();

    // run thread to wait for stop request to stop welcome thread
    runStoppingThread();
  }

  @Override
  public void run() {
    // save time time of initial client connection to server
    this.startTime = getCurrentTime();
    try {
      // receive name from client
      receiveClientNameProtocol();

      // create logging file for user
      createFile();

      // write start time to log
      logInfoProtocol(this.startTime, "connected", "");

      // acknowlege received name
      sendClientMessageProtocol("Name received by server");
    } catch (IOException e) {
      System.out.println(e);
    }

    while (this.state.equals("running")) {
      String clientProb = "";
      String answer = "";

      // Read from client stream
      try {
        // Read from input buffer
        clientProb = inFromClient.readLine();
      } catch (IOException e) {
        System.out.println(e);
      }
      // when requested to end

      long currentTime = getCurrentTime();
      if (clientProb.equals("stop")) {
        try {
          // log client has requested to stop connection
          logInfoProtocol(currentTime, "requested", "stop");
          // log client disconnected and time connected for
          logInfoProtocol(currentTime, "disconnected",
              "(connected for " + (currentTime - this.startTime) / 1000 + " seconds)");

          // alert client to stop
          sendClientMessageProtocol("stopped");

          // close all existing connections
          closeConnections();

          // set client state to stopped
          this.state = "stopped";
        } catch (Exception e) {
          System.out.println(e);
        }

      } else {
        try {
          // log client requested math problem
          logInfoProtocol(currentTime, "requested", clientProb);
          // solve math expression
          answer = solveMathExpressionProtocol(clientProb);
        } catch (Exception e) {
          System.out.println(e);
        }

        try {
          // send answer back to client
          sendClientMessageProtocol(answer);

          // log answer to math problem received by client
          logInfoProtocol(currentTime, "received", answer);

        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
}
