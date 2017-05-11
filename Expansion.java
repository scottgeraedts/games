import java.util.*;
public abstract class Expansion{
  String [] cards;
  String name;
  Dominion game;
  public Expansion(Dominion g){
    game=g;
  }
  public boolean hasCard(String cardName){
    return Arrays.asList(cards).contains(cardName);
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
        game.server.changePlayer(oldPlayer,game.players.get(oldPlayer).makeData(),i%game.players.size(),victim.makeData());

        //resolve possible reactions
        boolean moat=false;
        if(isAttack) reactions=game.reaction1Reveal(victim.hand,i);
        for(String r: reactions){
          if(r.equals("moat")){ moat=true;
          }else if(r.equals("diplomat")){
            if(victim.hand.size()>=5){
              victim.drawToHand(2);
              game.doWork("discard",3,3,i);
              game.selectedCards.clear();
              game.changePhase(attackPhase);
              game.displayPlayer(i);
            }
          }else if(r.equals("secretchamber")){
            victim.drawToHand(2);
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
        
        game.server.displayComment(i,comment);
        subStep(i,activePlayer);
        game.selectedCards.clear();
        game.server.displayComment(i,"");
        oldPlayer=i;
      }
      game.server.changePlayer(oldPlayer,game.players.get(oldPlayer).makeData(),activePlayer,game.players.get(activePlayer).makeData());
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

      game.changePhase("actions");
      game.server.displayComment(activePlayer,"");
      game.selectedCards.clear();
    }
    public void subWork(int activePlayer){}
  }
  
}
