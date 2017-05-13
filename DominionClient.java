import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.*;    // Using Swing components and containers
import java.awt.event.WindowEvent;

public class DominionClient{
  public static final boolean DEBUG=true;
  private BufferedReader input;
  private PrintWriter output;
  private DominionBoard board;
  
  public static void main(String [] args) throws IOException{
  
    BufferedReader stdin=new BufferedReader(new InputStreamReader(System.in));
    Socket socket;
    if(!DEBUG){
      System.out.println("enter IP of the server");
      String IP=stdin.readLine();  
      socket=new Socket(IP,4444);
    }else{
      socket=new Socket("localhost",4444);
    }
    BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter output=new PrintWriter(socket.getOutputStream(),true);

    String inputLine,name;    
    while(true){
      inputLine=input.readLine();
      System.out.println(inputLine);
      if(inputLine.equals("break")){
        break;
      }
      if(inputLine.charAt(0)=='!'){
        if(!DEBUG){
          name=stdin.readLine();
          output.println(name);
        }else{
          output.println("no");
        }
      }
    }
    new DominionClient(output,input);
   }
   public DominionClient(PrintWriter output, BufferedReader input) throws IOException{
    this.output=output;
    this.input=input;
    String start=input.readLine();
    initialize(start.substring(start.indexOf("%")+1));
    work();
    
  }
  public void work() throws IOException{
    String inputLine;
    String outputLine;
    ServerWatcher watcher=new ServerWatcher(input);
    Thread thread=new Thread(watcher);
    thread.start(); 
    
    while(true){
      try{
        Thread.sleep(1);
      }catch(InterruptedException ex){
        System.out.println("interupted!");
      }
      
      //check if there's any input to display
      if(watcher.isNew()){
        inputLine=watcher.getValue();
        if(inputLine.equals("Terminate")) break;
        parseInput(inputLine);
      }
      
      //check if there's any output to send
      //TODO sanity check on output
      outputLine=board.getOutput();
      if(outputLine.length()>0){
        output.println(outputLine);
      }
      
    }
    board.kill();
    board.dispatchEvent(new WindowEvent(board, WindowEvent.WINDOW_CLOSING));
    
    System.out.println("Terminated by server");
  }
  public void initialize(String input){
//    String inputLine=input.readLine();
    //turn inputline into useful thing
    String [] parts=input.split("%");
    
    //initialize player
    ArrayList<DominionPlayer.Data> playerData=readArray(parts[0],new DominionPlayer.Data());
    //initialize supply
    ArrayList<Deck.SupplyData> supplyData=readArray(parts[1],new Deck.SupplyData());
    ArrayList<String> options=readArray(parts[4],"");
    board=new DominionBoard(playerData,supplyData,Integer.parseInt(parts[3]),Integer.parseInt(parts[2]),options); // Let the constructor do the job
    
  }
  public void playAgain(String input){
    board.playAgain();
  }
  public void reset(String input){
//    String inputLine=input.readLine();
    //turn inputline into useful thing
    String [] parts=input.split("%");
    
    //initialize player
    ArrayList<DominionPlayer.Data> playerData=readArray(parts[0],new DominionPlayer.Data());
    //initialize supply
    ArrayList<Deck.SupplyData> supplyData=readArray(parts[1],new Deck.SupplyData());
    ArrayList<String> options=readArray(parts[3],"");
    board.reset(playerData,supplyData,Integer.parseInt(parts[2]),options); // Let the constructor do the job
    
  }
  public void parseInput(String input){
    //System.out.println(input);
    int cut=0;
    try{
      cut=input.indexOf("%");
    }catch(NullPointerException ex){
      System.out.println(input);
    }
    String methodName=input.substring(0,cut);
    System.out.println("client: "+methodName);
    input=input.substring(cut+1);
    
    //get name if method from the first argument
    java.lang.reflect.Method method;
    try {
      method = this.getClass().getMethod(methodName, String.class);
      method.invoke(this, input);
    }catch (NoSuchMethodException e) { 
      System.out.println("Unknown method name");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    
  }
  public void cardPlayed(String input){
//    System.out.println(input);
    String [] parts=input.split("%");
    board.refreshCardPanel(readArray(parts[2],new DominionCard("copper")));
    board.displayPlayer(Integer.parseInt(parts[0]),new DominionPlayer.Data(parts[1]));
    
  }
  public void changePlayer(String input){
//    System.out.println(input);
    String [] parts=input.split("%");
    System.out.println("changing player "+parts[0]+" "+parts[2]);
    board.changePlayer(Integer.parseInt(parts[0]),new DominionPlayer.Data(parts[1]),
        Integer.parseInt(parts[2]),new DominionPlayer.Data(parts[3]));
    
  }
  public void changePhase(String input){
    String [] parts=input.split("%");
    board.changePhase(parts[0],parts[1]);
  }
  public void showScores(String input){
    board.showScores(new OptionData(input));
  }
  public void displayPlayer(String input){
    String [] parts=input.split("%");
    board.displayPlayer(Integer.parseInt(parts[0]),new DominionPlayer.Data(parts[1]));
  }
  public void optionPane(String input){
    board.optionPane(new OptionData(input));
  }
  public void displayTrash(String input){
    board.displayTrash(new Deck.Data(input));
  }
  public void displaySupply(String input){
    board.displaySupply(new Deck.SupplyData(input));
  }
  public void updateSharedFields(String input){
//    System.out.println(input);
    String [] parts=input.split("%");
     board.refreshSharedFields(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),
        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
  }
  public void displayComment(String input){
    board.displayComment(input);
  }
  public void setMask(String input){
    String [] parts=input.split("#");
    int size=Integer.parseInt(parts[0]);
    boolean [] out=new boolean[size];
    for(int i=0;i<size;i++) out[i]=Boolean.parseBoolean(parts[i+1]);
    board.setMask(out);
  }
  
  ///***THE PESKY READARRAYS***///
  public static ArrayList<DominionPlayer.Data> readArray(String parts, DominionPlayer.Data temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<DominionPlayer.Data> x=new ArrayList<DominionPlayer.Data>(size);
    for(int i=0;i<size;i++){
      x.add(new DominionPlayer.Data(playerParts[i+1]));
    } 
    return x; 
  }
  public static ArrayList<Deck.SupplyData> readArray(String parts, Deck.SupplyData temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<Deck.SupplyData> x=new ArrayList<Deck.SupplyData>(size);
    for(int i=0;i<size;i++){
      x.add(new Deck.SupplyData(playerParts[i+1]));
    }  
    return x;
  }
  public static ArrayList<DominionCard> readArray(String parts, DominionCard temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<DominionCard> x=new ArrayList<DominionCard>(size);
    for(int i=0;i<size;i++){
      x.add(new DominionCard(playerParts[i+1],0));
    }
    return x;  
  }
  public static ArrayList<String> readArray(String parts, String temp){
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<String> x=new ArrayList<String>(size);
    for(int i=0;i<size;i++){
      x.add(playerParts[i+1]);
    }
    return x;  
  }

    
  /////**SERVER WATCHER***///
  //constantly checks to see if there's more information from the server
  //needs to be in its own thread so we don't block if no new information is coming
  public static class ServerWatcher implements Runnable{
    LinkedList<String> lines=new LinkedList<>();

    BufferedReader input;
    
    public ServerWatcher(BufferedReader br){
      input=br;
    }
    public void run(){
      String out="";
      while(true){
        try{
          Thread.sleep(10);
        }catch(InterruptedException ex){}
        
        try{
          out=input.readLine();
        }catch(IOException ex){
          ex.printStackTrace();
        }
        lines.add(out);
      }
    }
    public String getValue(){
      return lines.remove();
    }
    public boolean isNew(){ return lines.size()>0; }
  }
}
