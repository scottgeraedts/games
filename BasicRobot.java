import java.util.*;

class BasicRobot implements PlayerInterface{

  protected int money;
  protected int buys;
  protected int numPlayer;
  protected int activePlayer;
  protected String phase;
  protected ArrayList<Boolean> mask;
  protected boolean optionPane=false;
  protected ArrayList<DominionCard> hand;
  
  public BasicRobot(){
  
  }
  public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, ArrayList<Integer> num, HashSet<String> o){
    if(num.size()!=1){
      System.out.println("a bot can only control one player");
      System.exit(0);
    }
    numPlayer=num.get(0);
    activePlayer=startingPlayer;
    phase="actions";
    hand=playerData.get(numPlayer).hand;
  }
  public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer, HashSet<String> o){
    activePlayer=startingPlayer;
    phase="actions";
    hand=playerData.get(numPlayer).hand;
  }
  public void displayMatCards(ArrayList<DominionCard> matcards){
  }
  public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer, ArrayList<Boolean> mask){
    activePlayer=newPlayerNum;
  }
  public void changePhase(String oldPhase, String newPhase, ArrayList<Boolean> mask){
    phase=newPhase;
    this.mask=mask;
  }
  public void showScores(PairList<String,String> scores){
  }
  public void displayPlayer(int playerNum, DominionPlayer.Data player, Collection<Boolean> mask){
    this.mask=new ArrayList<>(mask);
    if(playerNum==numPlayer) hand=player.hand;
  }
  public void optionPane(OptionData o){
    optionPane=true;
  }
  public void displayTrash(Deck.Data dat){
  }
  public void displaySupply(Deck.SupplyData dat){
  }
  public void displayComment(String text){
  }
  public void updateSharedFields(int actions, int money, int buys, int tradeRoute, int potions){
    this.money=money;
    this.buys=buys;
  }
  public boolean playAgain(){
    return true;
  }
  public void terminate(){
  }
  public String getUserInput(){
    if(optionPane){
      optionPane=false;
      return "Done";
    }else if(phase.equals("actions")){
      //never play actions
      return "Btreasures";
    }else if(phase.equals("buys")){
      //if its the buy phase, just buy a province, silver, gold or copper
      if(money>=8) return "Gprovince";
      else if(money>=6) return "Ggold";
      else if(money>=3) return "Gsilver";
      else return "Gcopper";
    }else if(phase.equals("discard") || phase.equals("topdeck")){
      //always discard or topdeck victories if possible
      for(int i=0;i<hand.size();i++){     
        if(hand.get(i).isVictory) return ""+i;
      }
      return 0+"";
    }else{
      //never trash, reveal or select cards
      return "B"+phase;
    }
  }
  
  
}
