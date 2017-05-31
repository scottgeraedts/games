import java.util.*;
import java.io.*;
import java.net.*;

public class DominionServer{ 

  //an interface is something that can return an instruction, like 'buy a card', 'play a card', etc
  //there are two kinds: clients which provide a display interface so humans can play
  //and robots which just return a value
  public ArrayList<PlayerInterface> connections=new ArrayList<>();
  //there may be more players than interfaces (if we are testing or doing hotseat)
  //this is a map from player to interface
  private HashMap<Integer,Integer> controllers=new HashMap<>();
  public static boolean DEBUG;
  public ArrayList<String> playerNames=new ArrayList<>();

  public static void main(String [] args) throws IOException{
  
    DominionServer server=new DominionServer();
    
    //read config file
    BufferedReader fr=null;
    try{
      fr=new BufferedReader(new FileReader("server_config.txt"));
      DEBUG=Boolean.parseBoolean(fr.readLine().split(" ")[0]);
    }catch(FileNotFoundException ex){
      System.out.println("No config file found, going with defaults");
      DEBUG=false;
    }    

    int portNumber=4444;

    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    ArrayList<Socket> clients=new ArrayList<>();
    PrintWriter output;
    PrintWriter output0=null;
    BufferedReader input;
    BufferedReader input0=null;
    ArrayList<String> playerNames=new ArrayList<>();

    String inputLine;
    //try to set up a server
    int nPlayers=0;
    
//    try(ServerSocket connectionSocket=new ServerSocket(portNumber,100,InetAddress.getByName("173.71.122.168"));)
    try(ServerSocket connectionSocket=new ServerSocket(portNumber);)
    {      
      //get the connections and all the player names
      while(true){
        clients.add(connectionSocket.accept());
        input=new BufferedReader(new InputStreamReader(clients.get(nPlayers).getInputStream()));
        output=new PrintWriter(clients.get(nPlayers).getOutputStream(), true);
        if(input0==null){
          input0=input;
          output0=output;
        }
        
        output.println("!Please enter your name:");
        inputLine=input.readLine();
        playerNames.add(inputLine);
        output.println("Welcome "+inputLine);
        System.out.println(inputLine+" has joined the game");
        output0.println(inputLine+" has joined the game");

        server.addHuman(input,output,inputLine);

        //if this isn't the first player they are done with the text UI
        if(output!=output0) output.println("break");
        nPlayers++;
        
        output0.println("!Are there more players coming? (yes/no)");
        inputLine=input0.readLine();

        if(!inputLine.equals("yes")) break;
      }
      output0.println("!How many computer players?");
      inputLine=input0.readLine();
      int x=Integer.parseInt(inputLine);
      for(int i=0;i<x;i++) server.addRobot();
      output0.println("break");
        
      //add a second human controlled by the first interface for testing purposes
      if(DEBUG) server.addHuman("tester",0);
      
      Dominion game=new Dominion(server);
      System.out.println("game over");
      //asking for new games
      boolean playAgain=true;
      while(true){
        for(PlayerInterface c : server.connections){
          playAgain= playAgain && c.playAgain();
        }
        if(playAgain){
          game.reset();
        }else{
          break;
        }
      }
      for(PlayerInterface c : server.connections)
        c.terminate();
        
      System.out.println("Application ended normally");
    }catch(NullPointerException e){
      e.printStackTrace();
      System.out.println("A client terminated their application!");
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  public DominionServer(){
    
  }
  //add a human player with a new input stream
  public void addHuman(BufferedReader br, PrintWriter pw, String name){
    controllers.put(playerNames.size(),connections.size());
    connections.add( new HumanPlayer(br,pw));
    playerNames.add(name);
  }
  //add a human player in hotest
  public void addHuman(String name, int display){
    controllers.put(playerNames.size(), display);
    playerNames.add(name);
  }
  //add a Robot player
  public void addRobot(){
    controllers.put(playerNames.size(),connections.size());
    connections.add(new BasicRobot());
    playerNames.add("Robot");
  }
  
  //pass initial string to all players
  public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, HashSet<String> o){
    ArrayList<Integer> isControlled=new ArrayList<>();
    for(int i=0;i<connections.size();i++){
      for(Map.Entry<Integer,Integer> entry : controllers.entrySet()){
        if(entry.getValue()==i) isControlled.add(entry.getKey());
      }
      connections.get(i).initialize(supplyData,playerData,startingPlayer,isControlled,o);
      isControlled.clear();
    }    
  }
  public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, HashSet<String> o){
    for(int i=0;i<connections.size();i++){
      connections.get(i).reset(supplyData,playerData,startingPlayer,o);
    }
  }
  public void showScores(PairList<String,String> scores){
    for(Iterator<PlayerInterface> it=connections.iterator(); it.hasNext(); ){
      it.next().showScores(scores);
    }
  }
  public void optionPane(int playerNum, OptionData o){
    connections.get(controllers.get(playerNum)).optionPane(o);
  }  
  public void displayComment(int playerNum, String text){
    System.out.println("displayed "+text);
    connections.get(controllers.get(playerNum)).displayComment(text);
  }

  public String getUserInput(int i){
    System.out.println("requesting input from player "+i);
    return connections.get(controllers.get(i)).getUserInput();
  }
  public static class HumanPlayer implements PlayerInterface{
    private BufferedReader input;
    private PrintWriter output;
    public HumanPlayer(BufferedReader br,PrintWriter pw){
      input=br;
      output=pw;
    }
    //make initial string
    //pass initial string to all players
    public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, ArrayList<Integer> controlled, HashSet<String> o){
     String out="initialize%";
     out+=toArray(playerData);
     out+="%"+supplyData.size();
     for(int i=0;i<supplyData.size();i++){
      out+="#"+supplyData.get(i).toString();
     }
     out+="%"+startingPlayer+"%"+toArray(controlled);
     out+="%"+toArray(o);
     output.println(out);
     output.flush();
   }
    //reset for a new game
    public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, HashSet<String> o){
     String out="reset%";
     out+=toArray(playerData);
     out+="%"+supplyData.size();
     for(int i=0;i<supplyData.size();i++){
      out+="#"+supplyData.get(i).toString();
     }
     out+="%"+startingPlayer;
     out+="%"+toArray(o);
      output.println(out);
      output.flush();
    }
    public void displayMatCards(ArrayList<DominionCard> matcards){
      String out="displayMatCards%"+matcards.size();
      for(int i=0; i<matcards.size(); i++){
        out+="#"+matcards.get(i).toString();
      }
      output.println(out);
    }
    public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer, ArrayList<Boolean> mask){
      String out="changePlayer%"+oldPlayerNum+"%"+oldPlayer.toString();
      out+="%"+newPlayerNum+"%"+newPlayer.toString()+"%"+toArray(mask);
      output.println(out);
    }
    public void changePhase(String oldPhase, String newPhase, ArrayList<Boolean> mask){
      output.println("changePhase%"+oldPhase+"%"+newPhase+"%"+toArray(mask));
    }
    public void showScores(PairList<String,String> scores){
      output.println("showScores%"+scores.toString());
    } 
    public void displayPlayer(int playerNum, DominionPlayer.Data player, Collection<Boolean> mask){
      output.println("displayPlayer%"+playerNum+"%"+player.toString()+"%"+toArray(mask));
    }
    public void optionPane(OptionData o){
      output.println("optionPane%"+o.toString());
    }
    public void displayTrash(Deck.Data dat){
      output.println("displayTrash%"+dat.toString());
    }
    public void displaySupply(Deck.SupplyData dat){
      output.println("displaySupply%"+dat.toString());
    }
    public void displayComment(String text){
      output.println("displayComment%"+text);
    }
    public void updateSharedFields(int actions, int money, int buys, int tradeRoute, int potions){
      output.println("updateSharedFields%"+actions+"%"+money+"%"+buys+"%"+tradeRoute+"%"+potions);
    }
    public boolean playAgain(){
      output.println("playAgain%");
      try{
        String inputLine=input.readLine();
        if(inputLine.equals("Quit")){
          output.println("Terminate");
          return false;
        }else return true;
      }catch(IOException ex){
        return false;
      }
    }    
    public void terminate(){
      output.println("Terminate");
    }
    
    public String getUserInput(){
      output.println("unlock%");
      try{
        return input.readLine();
      }catch(IOException ex){
        ex.printStackTrace();
        return "";
      }
    }
  }
  public static <T extends Object> String toArray(Collection<T> x){
    String out="";
    out+=x.size();
    for(T y : x) out+="#"+y.toString();
    return out;
  }
}
