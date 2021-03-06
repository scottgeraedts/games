import java.util.*;
import java.io.*;
import java.net.*;

public class DominionServer{ 

  //an interface is something that can return an instruction, like 'buy a card', 'play a card', etc
  //there are two kinds: clients which provide a display interface so humans can play
  //and robots which just return a value
  ArrayList<PlayerInterface> connections=new ArrayList<>();
  //there may be more players than interfaces (if we are testing or doing hotseat)
  //this is a map from player to interface
  private HashMap<Integer,Integer> controllers=new HashMap<>();
  public static boolean DEBUG;
  ArrayList<String> playerNames=new ArrayList<>();

  public static void main(String [] args) throws IOException{
  
    DominionServer server=new DominionServer();
    
    //read config file
    BufferedReader fr=null;
    String supply;
    try{
      fr=new BufferedReader(new FileReader("server_config.txt"));
      DEBUG=Boolean.parseBoolean(fr.readLine().split(" ")[0]);
      supply=fr.readLine().split(" ")[0];
    }catch(FileNotFoundException ex){
      System.out.println("No config file found, going with defaults");
      DEBUG=false;
      supply="random";
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
      int x;
      while(true){
        try{
          output0.println("!How many computer players?");
          inputLine=input0.readLine();
          x=Integer.parseInt(inputLine);
          break;
        }catch(NumberFormatException ex){
          System.out.println("please enter an integer");
        }
      }
        
       
      for(int i=0;i<x;i++) server.addRobot();
      output0.println("break");
        
      //add a second human controlled by the first interface for testing purposes
      if(DEBUG) server.addHuman("tester",0);
      
      Dominion game=new Dominion(server, supply);
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
  private void addHuman(BufferedReader br, PrintWriter pw, String name){
    controllers.put(playerNames.size(),connections.size());
    connections.add( new HumanPlayer(br,pw));
    playerNames.add(name);
  }
  //add a human player in hotest
  private void addHuman(String name, int display){
    controllers.put(playerNames.size(), display);
    playerNames.add(name);
  }
  //add a Robot player
  private void addRobot(){
    controllers.put(playerNames.size(),connections.size());
    connections.add(new BasicRobot());
    playerNames.add("Robot");
  }
  
  //pass initial string to all players
  void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer,
                         LinkedHashSet<String> o, HashSet<String> o2){
    ArrayList<Integer> isControlled=new ArrayList<>();
    for(int i=0;i<connections.size();i++){
      for(Map.Entry<Integer,Integer> entry : controllers.entrySet()){
        if(entry.getValue()==i) isControlled.add(entry.getKey());
      }
      connections.get(i).initialize(supplyData,playerData,startingPlayer,isControlled,o,o2);
      isControlled.clear();
    }    
  }
  void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData,
                    int startingPlayer, LinkedHashSet<String> o, HashSet<String> o2){
    for (PlayerInterface connection : connections) {
      connection.reset(supplyData, playerData, startingPlayer, o, o2);
    }
  }
  void showScores(PairList<Integer,String> scores){
    for (PlayerInterface connection : connections) {
      connection.showScores(scores);
    }
  }
  public void optionPane(int playerNum, OptionData o){
    connections.get(controllers.get(playerNum)).optionPane(o);
  }  
  public void displayComment(int playerNum, String text){
    //System.out.println("displayed "+text);
    connections.get(controllers.get(playerNum)).displayComment(text);
  }

  String getUserInput(int i, DominionCard card){
    System.out.println("requesting input from player "+i);
    return connections.get(controllers.get(i)).getUserInput(card);
  }
  //gives control of a player to a different player
  void changeController(int oldPlayer, int interfaceNum){
    System.out.println("giving control of "+oldPlayer+" to "+interfaceNum);
    if(controllers.get(oldPlayer) != interfaceNum) {
      connections.get(interfaceNum).changeController(oldPlayer, true);
      connections.get(controllers.get(oldPlayer)).changeController(oldPlayer, false);
      controllers.put(oldPlayer, interfaceNum);
    }
  }
  //returns the controller of a player
  int getController(int x){
    return controllers.get(x);
  }

  public static class HumanPlayer implements PlayerInterface{
    private BufferedReader input;
    private PrintWriter output;
    HumanPlayer(BufferedReader br,PrintWriter pw){
      input=br;
      output=pw;
    }
    //make initial string
    //pass initial string to all players
    public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData,
                           int startingPlayer, ArrayList<Integer> controlled,
                           LinkedHashSet<String> gameOptions, HashSet<String> playerOptions){
     StringBuilder out= new StringBuilder("initialize%");
     out.append(toArray(playerData));
     out.append("%").append(supplyData.size());
      for (Deck.SupplyData aSupplyData : supplyData) {
        out.append("#").append(aSupplyData.toString());
      }
     out.append("%").append(startingPlayer).append("%").append(toArray(controlled));
     out.append("%").append(toArray(gameOptions));
     out.append("%").append(toArray(playerOptions));
     output.println(out);
     output.flush();
   }
    //reset for a new game
    public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData,
                      int startingPlayer, LinkedHashSet<String> gameOptions, HashSet<String> playerOptions){
      StringBuilder out=new StringBuilder();
      out.append("reset%");
      out.append(toArray(playerData));
      out.append("%").append(supplyData.size());
      for (Deck.SupplyData aSupplyData : supplyData) {
        out.append("#").append(aSupplyData.toString());
      }
      out.append("%").append(startingPlayer);
      out.append("%").append(toArray(gameOptions));
      out.append("%").append(toArray(playerOptions));
      output.println(out);
      output.flush();
    }
    public void displayMatCards(ArrayList<DominionCard> matcards){
      StringBuilder out=new StringBuilder();
      out.append("displayMatCards%").append(matcards.size());
      for (DominionCard matcard : matcards) {
        out.append( "#").append(matcard.toString());
      }
      output.println(out);
    }
    public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer, ArrayList<Boolean> mask){
      String out="changePlayer%"+oldPlayerNum+"%"+oldPlayer.toString();
      out+="%"+newPlayerNum+"%"+newPlayer.toString()+"%"+toArray(mask);
      output.println(out);
    }
    public void changePhase(Dominion.Phase oldPhase, Dominion.Phase newPhase, ArrayList<Boolean> mask){
      output.println("changePhase%"+oldPhase.name()+"%"+newPhase.name()+"%"+toArray(mask));
    }
    public void showScores(PairList<?,?> scores){
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
    public void updateSharedFields(PairList<String, Integer> fields){
      output.println("updateSharedFields%"+fields.toString());
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
    public void changeController(int player, boolean control){
      output.println("changeController%"+player+"%"+control);
    }
    public void terminate(){
      output.println("Terminate");
    }
    
    public String getUserInput(DominionCard card){
      output.println("unlock%");
      try{
        return input.readLine();
      }catch(IOException ex){
        ex.printStackTrace();
        return "";
      }
    }
  }
  static <T> String toArray(Collection<T> x){
    StringBuilder out=new StringBuilder();
    out.append(x.size());
    for(T y : x) out.append("#").append(y.toString());
    return out.toString();
  }
}
