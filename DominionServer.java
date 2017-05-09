import java.util.*;
import java.io.*;
import java.net.*;

public class DominionServer{

  public ArrayList<HumanPlayer> connections;
  private int [] player;

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
    
//    try(ServerSocket connectionSocket=new ServerSocket(portNumber,100,InetAddress.getByName("173.71.122.168"));)
    try(ServerSocket connectionSocket=new ServerSocket(portNumber);)
    {      
      //get the connections and all the player names
      while(true){
        clients.add(connectionSocket.accept());
        input.add(new BufferedReader(new InputStreamReader(clients.get(nPlayers).getInputStream())));
        output.add(new PrintWriter(clients.get(nPlayers).getOutputStream(), true));
        
        output.get(nPlayers).println("!Please enter your name:");
        inputLine=input.get(nPlayers).readLine();
        playerNames.add(inputLine);
        output.get(nPlayers).println("Welcome "+inputLine);
        System.out.println(inputLine+" has joined the game");
        
        nPlayers++;
        
        output.get(0).println("!Are there more players coming? (yes/no)");
        inputLine=input.get(0).readLine();

        if(!inputLine.equals("yes")) break;
      }
      for(int i=0;i<nPlayers;i++) output.get(i).println("break");
        
      //playerNames.add("bot");
      
      Dominion game=new Dominion(playerNames, new DominionServer(input,output));
      System.out.println("game over");
      //asking for new games
      boolean playAgain=true;
      while(true){
        for(int i=0;i<input.size();i++){
          output.get(i).println("playAgain%x");
          inputLine=input.get(i).readLine();
          if(inputLine.equals("Quit")){
            playAgain=false;
            break;
          }
        }
        if(playAgain){
          game.reset(playerNames);
        }else{
          break;
        }
      }
      for(int i=0;i<output.size();i++){
        output.get(i).println("Terminate");
      }
      System.out.println("Application ended normally");
    }catch(NullPointerException e){
      System.out.println("A client terminated their application!");
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  public DominionServer(ArrayList<BufferedReader> br, ArrayList<PrintWriter> pw){
    int size=br.size();
    if(size!=pw.size()) System.out.println("size mismatch");
    player=new int[2];
    player[0]=0;
    player[1]=1;
    connections=new ArrayList<HumanPlayer>(size);
    for(int i=0;i<size;i++)
      connections.add( new HumanPlayer(br.get(i),pw.get(i)));
  }
  //pass initial string to all players
  public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer){
    for(int i=0;i<connections.size();i++){
      connections.get(i).initialize(supplyData,playerData,startingPlayer,player[i]);
    }
  }
  public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer){
    for(int i=0;i<connections.size();i++){
      connections.get(i).reset(supplyData,playerData,startingPlayer);
    }
  }
  public void cardPlayed(int actions, int money, int buys, int playerNum, DominionPlayer.Data player, ArrayList<DominionCard> matcards){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().cardPlayed(actions,money,buys,playerNum,player,matcards);
    }
  }
  public void cardGained(int actions, int money, int buys, int playerNum, DominionPlayer.Data player, Deck.SupplyData deck){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().cardGained(actions,money,buys,playerNum, player, deck);
    }
  }
  public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().changePlayer(oldPlayerNum,oldPlayer,newPlayerNum,newPlayer);
    }
  }
  public void changePhase(String oldPhase, String newPhase){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().changePhase(oldPhase,newPhase);
    }
  }
  public void showScores(PairList<String,Integer> scores){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().showScores(scores);
    }
  }
  public void displayPlayer(int playerNum, DominionPlayer.Data player){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().displayPlayer(playerNum,player);
    }
  }
  public void optionPane(int playerNum, Dominion.OptionData o){
    connections.get(player[playerNum]).optionPane(o);
  }
  public void displayTrash(Deck.Data dat){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().displayTrash(dat);
    }
  }
  public void updateSharedFields(int a, int b, int c){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().updateSharedFields(a,b,c);
    }
  }  
  public void displayComment(int playerNum, String text){
    connections.get(player[playerNum]).displayComment(text);
  }
  public void displaySupply(Deck.SupplyData data){
    for(Iterator<HumanPlayer> it=connections.iterator(); it.hasNext(); ){
      it.next().displaySupply(data);
    }
  }    
  public void setMask(int playerNum, boolean [] mask){
    connections.get(player[playerNum]).setMask(mask);
  }
  
  public String getUserInput(int i){
    System.out.println("requesting input from player "+player[i]);
    return connections.get(player[i]).getUserInput();
  }
  public static class HumanPlayer{
    private BufferedReader input;
    private PrintWriter output;
    public HumanPlayer(BufferedReader br,PrintWriter pw){
      input=br;
      output=pw;
    }
    //make initial string
    //pass initial string to all players
    public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, int num){
     String out="initialize%";
     out+=toArray(playerData);
//     out+=playerData.size();
//     for(int i=0;i<playerData.size();i++){
//      out+="#"+playerData.get(i).toString();
//     }
     out+="%"+supplyData.size();
     for(int i=0;i<supplyData.size();i++){
      out+="#"+supplyData.get(i).toString();
     }
     out+="%"+startingPlayer+"%"+num;
      output.println(out);
      output.flush();
    }
    //reset for a new game
    public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer){
     String out="reset%";
     out+=toArray(playerData);
     out+="%"+supplyData.size();
     for(int i=0;i<supplyData.size();i++){
      out+="#"+supplyData.get(i).toString();
     }
     out+="%"+startingPlayer;
      output.println(out);
      output.flush();
    }
    public void cardPlayed(int actions, int money, int buys, int playerNum, DominionPlayer.Data player, ArrayList<DominionCard> matcards){
      String out="cardPlayed%"+actions+"%"+money+"%"+buys+"%"+playerNum+"%"+player.toString()+"%"+matcards.size();
      for(int i=0; i<matcards.size(); i++){
        out+="#"+matcards.get(i).toString();
      }
      output.println(out);
    }
    public void cardGained(int actions, int money, int buys, int playerNum, DominionPlayer.Data player, Deck.SupplyData deck){
      String out="cardGained%"+actions+"%"+money+"%"+buys+"%"+playerNum+"%"+player.toString()+"%"+deck.toString();
      output.println(out);
    }
    public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer){
      String out="changePlayer%"+oldPlayerNum+"%"+oldPlayer.toString();
      out+="%"+newPlayerNum+"%"+newPlayer.toString();
      output.println(out);
    }
    public void changePhase(String oldPhase, String newPhase){
      output.println("changePhase%"+oldPhase+"%"+newPhase);
    }
    public void showScores(PairList<String,Integer> scores){
      output.println("showScores%"+scores.toString());
    } 
    public void displayPlayer(int playerNum, DominionPlayer.Data player){
      output.println("displayPlayer%"+playerNum+"%"+player.toString());
    }
    public void optionPane(Dominion.OptionData o){
      output.println("optionPane%"+o.toString());
    }
    public void displayTrash(Deck.Data dat){
      output.println("displayTrash%"+dat.toString());
    }
    public void displaySupply(Deck.SupplyData dat){
      output.println("displaySupply%"+dat.toString());
    }
    public void setMask(boolean [] mask){
      String out="setMask%"+mask.length;
      for(int i=0;i<mask.length;i++) out+="#"+mask[i];
      output.println(out);
    }
    public void displayComment(String text){
      output.println("displayComment%"+text);
    }
    public void updateSharedFields(int actions, int money, int buys){
      output.println("updateSharedFields%"+actions+"%"+money+"%"+buys);
    }
     
    public String getUserInput(){
      try{
        return input.readLine();
      }catch(IOException ex){
        ex.printStackTrace();
        return "";
      }
    }
  }
  public static <T extends Object> String toArray(ArrayList<T> x){
    String out="";
    out+=x.size();
    for(int i=0;i<x.size();i++) out+="#"+x.get(i).toString();
    return out;
  }
}
