import java.util.*;
import java.io.*;
import java.net.*;

public class DominionServer{
  public static void main(String [] args) throws IOException{
    int portNumber=4444;

    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    ArrayList<Socket> clients=new ArrayList<>();
    ArrayList<PrintWriter> output=new ArrayList<>();
    ArrayList<BufferedReader> input=new ArrayList<>();
    ArrayList<String> playerNames=new ArrayList<>();

    String inputLine;
    //try to set up a server
    int nPlayers=0;
    ServerSocket connectionSocket=new ServerSocket(portNumber);
    
    //get the connections and all the player names
    while(true){
      clients.add(connectionSocket.accept());
      input.add(new BufferedReader(new InputStreamReader(clients.get(nPlayers).getInputStream())));
      output.add(new PrintWriter(clients.get(nPlayers).getOutputStream(), true));
      
      output.get(nPlayers).println("!Please enter your name:");
      inputLine=input.get(nPlayers).readLine();
      playerNames.add(inputLine);
      output.get(nPlayers).println("Welcome "+inputLine);
      
      nPlayers++;
      
      output.get(0).println("!Are there more players coming? (yes/no)");
      inputLine=input.get(0).readLine();

      if(!inputLine.equals("yes")) break;
    }
    
    //done with getting new players
    for(int i=0;i<nPlayers;i++){
      output.get(i).println("break");
    }
    
    
  }
}
