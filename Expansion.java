import java.util.*;
public abstract class Expansion{
  String [] cards;
  String name;
  Dominion game;
  public static ArrayList<String> vicTokens;
  
  public static String [] prosperityCards={"loan","traderoute","watchtower","bishop","quarry",
      "talisman","city","contraband","countinghouse","mint","mountebank","rabble"};
  
  public Expansion(Dominion g){
    game=g;
    String [] vic={"bishop","goons","monument"};
    vicTokens=new ArrayList<>(Arrays.asList(vic));
  }
  public boolean hasCard(String cardName){
    return Arrays.asList(cards).contains(cardName);
  }
  public static boolean usePlatinums(ArrayList<String> supplyCards){
    Random ran=new Random();
    double weight=0.05;

    ArrayList<String> pCards=new ArrayList<>(Arrays.asList(prosperityCards));
    for(String card : supplyCards){
      if(pCards.contains(card)) weight+=0.15;
    }
    if(ran.nextDouble()<weight) return true;
    else return false;
  }
  
  protected abstract class Attack extends DominionCard{
    protected String attackPhase="other";
    protected String comment="";
    public Attack(String name){
      super(name);
      isAction=true;
      isAttack=true;
    }
    @Override
    public final void work(int activePlayer){
      subWork(activePlayer);
      game.changePhase(attackPhase);

      ArrayList<DominionCard> oldMat=new ArrayList<>(game.matcards);
      int oldPlayer=activePlayer;
      int oldmoney=game.money;
      int oldActions=game.actions;
      int oldBuys=game.buys;

      game.server.displayComment(activePlayer,comment);
      ArrayList<String> reactions=new ArrayList<>();
      DominionPlayer victim;

      for(int i=(activePlayer+1)%game.players.size(); i!=activePlayer; i=(i+1)%game.players.size()){

        victim=game.players.get(i);
        game.changePlayer(oldPlayer,i%game.players.size());

        //resolve possible reactions
        boolean moat=false;
        if(isAttack) reactions=game.reactionReveal(victim.hand,i,1);
        for(String r: reactions){
          if(r.equals("moat")){ moat=true;
          }else if(r.equals("diplomat")){
            if(victim.hand.size()>=5){
              victim.drawToHand(2);
              game.displayPlayer(i);
              game.doWork("discard",3,3,i);
              game.selectedCards.clear();
              game.changePhase(attackPhase);
              game.displayPlayer(i);
            }
          }else if(r.equals("secretchamber")){
            victim.drawToHand(2);
            game.displayPlayer(i);
            game.doWork("topdeck",2,2,i);
            game.selectedCards.clear();
            game.changePhase(attackPhase);
            game.displayPlayer(i);            
          }
        }
        //check for lighthouse
        for(DominionCard card : victim.duration){
          if(card.getName().equals("lighthouse")) moat=true;
        }
        
        if(moat){
          oldPlayer=i;
          continue;
        }
        
        game.mask.clear();
        game.server.displayComment(i,comment);
        subStep(i,activePlayer);
        game.selectedCards.clear();
        game.server.displayComment(i,"");
        oldPlayer=i;
      }
      game.changePlayer(oldPlayer,activePlayer);
      game.money=oldmoney;
      game.actions=oldActions;
      game.buys=oldBuys;
      game.matcards=oldMat;       
      
      cleanup(activePlayer);

//      game.server.updateSharedFields(game.actions,game.money,game.buys);
      game.cardPlayed(activePlayer);
      game.server.displayComment(activePlayer,"");
      game.changePhase("actions");
      game.selectedCards.clear();
    }
    public void subStep(int x, int y){}
    public void subWork(int x){}
    public void cleanup(int x){}
  }
  protected abstract class RegularCard extends DominionCard{
    protected String comment="";
    public RegularCard(String name){
      super(name);
      isAction=true;
    }
    @Override
    public final void work(int activePlayer){
      game.server.displayComment(activePlayer,comment);

      subWork(activePlayer);

      game.mask.clear();
      game.changePhase("actions");
      game.server.displayComment(activePlayer,"");
      game.selectedCards.clear();
    }
    public void subWork(int activePlayer){}
  }
  
}
