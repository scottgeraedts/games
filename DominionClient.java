import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.*;    // Using Swing components and containers
import java.awt.event.WindowEvent;

public class DominionClient{
  public static boolean DEBUG=false;
  private BufferedReader input;
  private PrintWriter output;
  private DominionBoard board;
  
  public static void main(String [] args) throws IOException{
  
    String IP="";
    
    //read config file
    BufferedReader fr=null;
    try{
      fr=new BufferedReader(new FileReader("config.txt"));
      DEBUG=Boolean.parseBoolean(fr.readLine().split(" ")[0]);
      IP=fr.readLine().split(" ")[0];
      System.out.println(IP);
    }catch(FileNotFoundException ex){
      System.out.println("No config file found, going with defaults");
      DEBUG=false;
      IP="None";
    }
    
    BufferedReader stdin=new BufferedReader(new InputStreamReader(System.in));
    Socket socket=new Socket();
    if(!DEBUG){
      if(IP.equals("None")){
        System.out.println("enter IP of the server");
        IP=stdin.readLine(); 
      }
      try{
        socket=new Socket(IP,4444);
      }catch(ConnectException e){
        System.out.println("No Server is available");
        System.exit(0);
      }
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
      //check if there's any input to display
      if(watcher.isNew()){
        try{
          inputLine=watcher.getValue();
        }catch(NullPointerException ex){
          System.out.println("wtf its a null pointer exception "+watcher.isNew());
          ex.printStackTrace();
          break;
        }catch(NoSuchElementException ex){
          System.out.println("wtf its a null pointer exception "+watcher.isNew());
          ex.printStackTrace();
          break;
        }
        if(inputLine.equals("Terminate")) break;
        parseInput(inputLine);
      }else if(!board.lock){
        
        //check if there's any output to send
        //TODO sanity check on output
        outputLine=board.getOutput();
        if(outputLine.length()>0){
          output.println(outputLine);
          board.lock=true;
        }else{
            try{
              Thread.sleep(100);
            }catch(InterruptedException ex){
              System.out.println("interupted!");
            }                    
        }
      }else{
        try{
          Thread.sleep(100);
        }catch(InterruptedException ex){
          System.out.println("interupted!");
        }            
      }
      
    }
    board.kill();
    board.dispatchEvent(new WindowEvent(board, WindowEvent.WINDOW_CLOSING));
    
    System.out.println("Terminated by server");
  }
  private void initialize(String input){
//    String inputLine=input.readLine();
    //turn inputline into useful thing
    String [] parts=input.split("%");
    
    //initialize player
    ArrayList<DominionPlayer.Data> playerData=readArray(parts[0],new DominionPlayer.Data());
    //initialize supply
    ArrayList<Deck.SupplyData> supplyData=readArray(parts[1],new Deck.SupplyData());
    ArrayList<String> options=readArray(parts[4],"");
    ArrayList<String> options2=readArray(parts[5],"");

    board=new DominionBoard(playerData,supplyData,readArray(parts[3],new Integer(0)),
            Integer.parseInt(parts[2]),options,options2); // Let the constructor do the job
    
  }
  public void playAgain(String input){
    board.lock=false;
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
    ArrayList<String> options2=readArray(parts[4],"");
    board.reset(playerData,supplyData,Integer.parseInt(parts[2]),options,options2); // Let the constructor do the job
    
  }
  private void parseInput(String input){
    //System.out.println(input);
    int cut=0;
    try{
      cut=input.indexOf("%");
    }catch(NullPointerException ex){
      System.out.println(input);
    }
    String methodName=input.substring(0,cut);
    //System.out.println("client: "+methodName);
    input=input.substring(cut+1);
    
    //get name if method from the first argument
    java.lang.reflect.Method method;
    try {
      method = this.getClass().getMethod(methodName, String.class);
      method.invoke(this, input);
    }catch (NoSuchMethodException e) { 
      System.out.println("Unknown method name "+methodName+'\n'+input);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    
  }
  public void displayMatCards(String input){
//    System.out.println(input);
    String [] parts=input.split("%");
    board.refreshCardPanel(readArray(input,new DominionCard("copper")));
    
  }
  public void changePlayer(String input){
//    System.out.println(input);
    String [] parts=input.split("%");
//    System.out.println("changing player "+parts[0]+" "+parts[2]);
    board.changePlayer(Integer.parseInt(parts[0]),new DominionPlayer.Data(parts[1]),
        Integer.parseInt(parts[2]),new DominionPlayer.Data(parts[3]),readArray(parts[4],new Boolean(true)));
    
  }
  public void changePhase(String input){
    String [] parts=input.split("%");
    board.changePhase(Dominion.Phase.valueOf(parts[0]), Dominion.Phase.valueOf(parts[1]),readArray(parts[2],new Boolean(true)));
  }
  public void showScores(String input){
    board.showScores(new OptionData(input));
  }
  public void displayPlayer(String input){
    String [] parts=input.split("%");
    board.displayPlayer(Integer.parseInt(parts[0]),new DominionPlayer.Data(parts[1]),readArray(parts[2],new Boolean(true)));
  }
  public void optionPane(String input){
    board.lock=false;
    board.optionPane(new OptionData(input));
  }
  public void displayTrash(String input){
    board.displayTrash(new Deck.Data(input));
  }
  public void displaySupply(String input){
    board.displaySupply(new Deck.SupplyData(input));
  }
  public void updateSharedFields(String input){
     board.refreshSharedFields(new PairList<String, Integer>(input, String.class, Integer.class));
  }
  public void displayComment(String input){
    board.displayComment(input);
  }
  public void unlock(String input){
    board.lock=false;
  }
  public void changeController(String input){
    String [] parts=input.split("%");
    board.changeController(Integer.parseInt(parts[0]), Boolean.parseBoolean(parts[1]));
  }
  
  ///***THE PESKY READARRAYS***///
  static ArrayList<DominionPlayer.Data> readArray(String parts, DominionPlayer.Data temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<DominionPlayer.Data> x=new ArrayList<DominionPlayer.Data>(size);
    for(int i=0;i<size;i++){
      x.add(new DominionPlayer.Data(playerParts[i+1]));
    } 
    return x; 
  }
  static ArrayList<Deck.SupplyData> readArray(String parts, Deck.SupplyData temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<Deck.SupplyData> x=new ArrayList<Deck.SupplyData>(size);
    for(int i=0;i<size;i++){
      x.add(new Deck.SupplyData(playerParts[i+1]));
    }  
    return x;
  }
  static ArrayList<DominionCard> readArray(String parts, DominionCard temp){
    //initialize player
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<DominionCard> x=new ArrayList<DominionCard>(size);
    for(int i=0;i<size;i++){
      x.add(new DominionCard(playerParts[i+1],0));
    }
    return x;  
  }
  static ArrayList<String> readArray(String parts, String temp){
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<String> x=new ArrayList<String>(size);
    for(int i=0;i<size;i++){
      x.add(playerParts[i+1]);
    }
    return x;  
  }
  static ArrayList<Boolean> readArray(String parts, Boolean temp){
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<Boolean> x=new ArrayList<Boolean>(size);
    for(int i=0;i<size;i++){
      x.add(Boolean.parseBoolean(playerParts[i+1]));
    }
    return x;  
  }
  static ArrayList<Integer> readArray(String parts, Integer temp){
    String [] playerParts=parts.split("#");
    int size=Integer.parseInt(playerParts[0]);
    ArrayList<Integer> x=new ArrayList<>(size);
    for(int i=0;i<size;i++){
      x.add(Integer.parseInt(playerParts[i+1]));
    }
    return x;  
  }    
  /////**SERVER WATCHER***///
  //constantly checks to see if there's more information from the server
  //needs to be in its own thread so we don't block if no new information is coming
  public static class ServerWatcher implements Runnable{
    LinkedList<String> lines=new LinkedList<>();

    BufferedReader input;
    
    ServerWatcher(BufferedReader br){
      input=br;
    }
    public void run(){
      String out="";
      while(true){
        try{
          Thread.sleep(10);
        }catch(InterruptedException ex){
          System.out.println("watcher interrupted");
        }
        
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
