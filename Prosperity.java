import java.util.*;

public class Prosperity extends Expansion{
  public static int quarryCounter=0; 
  public static HashSet<String> tradeRouteCards;
  public static int talismanCounter=0;
  public static boolean royalSeal=false;
  public static int goons=0;
  public static int hoard=0;

  public Prosperity(Dominion g){
    super(g);
    cards=prosperityCards;
  }
  public class Loan extends DominionCard{
    public Loan(){
      super("loan");
      cost=3;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      DominionCard card;
      DominionPlayer player=game.players.get(ap);
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          return;
        }
        if(card.isMoney) break;
        else player.disc.put(card);
      }
      String [] options={"Trash","Discard"};
      OptionData o=new OptionData(options);
      o.put(card.getImage(),"image");
      String input=game.optionPane(ap,o);
      if(input.equals(options[0])){
        game.trash.put(card);
        game.displayTrash();
      }else{
        player.disc.put(card);
      }
      game.displayPlayer(ap);
    }
  }
  public class Traderoute extends RegularCard{
    public Traderoute(){
      super("traderoute");
      cost=3;
      buys=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      game.money+=tradeRouteCards.size();
      game.updateSharedFields();
    }
  }
  public class Watchtower extends DominionCard{
    public Watchtower(){
      super("watchtower");
      cost=3;
      isAction=true;
      isReaction2=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      player.drawToHand(6-player.hand.size());
      game.displayPlayer(ap);
    }
  }
  public class Bishop extends Attack{
    public Bishop(){
      super("bishop");
      cost=4;
      value=1;
      isAttack=false;
    }
    @Override
    public void subWork(int ap){
      game.players.get(ap).vicTokens++;
      game.doWork("trash",1,1,ap);
      game.players.get(ap).vicTokens+=game.selectedCards.get(0).cost/2;
      game.updateSharedFields();
    }
    @Override
    public void subStep(int ap, int attacker){
      game.doWork("trash",0,1,ap);
    }
  }
  public class Monument extends DominionCard{
    public Monument(){
      super("monument");
      cost=4;
      value=2;
    }
    @Override
    public void work(int ap){
      game.players.get(ap).vicTokens++;      
    }
  }
  public class Quarry extends DominionCard{
    public Quarry(){
      super("quarry");
      cost=4;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      quarryCounter+=2;
      game.displaySupplies();
    }
  }
  public class Talisman extends DominionCard{
    public Talisman(){
      super("talisman");
      cost=4;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      talismanCounter++;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      talismanCounter=0;
      return false;
    }
  }
  public class City extends DominionCard{
    public City(){
      super("city");
      cost=5;
      actions=2;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(game.emptyPiles>=1){
        game.players.get(ap).drawToHand(1);
        game.displayPlayer(ap);
      }
      if(game.emptyPiles>=2){
        game.money++;
        game.buys++;
        game.updateSharedFields();
      }
    }
  }
  public class Contraband extends RegularCard{
    String deck;
    public Contraband(){
      super("contraband");
      cost=5;
      buys=1;
      value=3;
      isAction=false;
      isMoney=true;
    }
    @Override
    public void subWork(int ap){
      game.server.displayComment((ap+1)%game.players.size(),"choose a deck that the player can't gain from");
      game.doWork("selectDeck",1,1,(ap+1)%game.players.size());
      game.changePhase("buys");
      deck=game.selectedDeck;
      System.out.println("selected "+deck+" for contraband");
      game.supplyDecks.get(deck).contraband=true;
      game.displaySupply(deck);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      System.out.println("contraband called");
      game.supplyDecks.get(deck).contraband=false;
      game.displaySupply(deck);
      return false;
    }
  }
  public class Countinghouse extends DominionCard{
    public Countinghouse(){
      super("countinghouse");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      OptionData o=new OptionData();
      ArrayList<DominionCard> cards=new ArrayList<>();
      for(ListIterator<DominionCard> it=player.disc.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getName().equals("copper")){
          it.remove();
          o.put(card.getImage(), "imagebutton");
          cards.add(card);
        }
      }
      String all="Select all";
      o.put(all,"textbutton");
      o.put("Done","textbutton");
      String input;
      while(cards.size()>0){
        input=game.optionPane(ap,o);
        if(input.equals("copper")){
          o.remove("copper");
          player.hand.add(cards.remove(0));
        }else if(input.equals(all)){
          player.hand.addAll(cards);
          break;
        }else{
          player.disc.put(cards);
          break;
        }
      }
      game.displayPlayer(ap);
    }
  }
  public class Mint extends RegularCard{
    public Mint(){
      super("mint");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("reveal",1,1,ap);
      if(game.selectedCards.size()>0){
        game.gainCard(game.selectedCards.get(0).getName(),ap);
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
    @Override
    public void onGain(int ap){
      DominionCard card;
      for(ListIterator<DominionCard> it=game.matcards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.isMoney){
          game.trash.put(card);
          it.remove();
        }
      }
      game.displayTrash();
    }
  }
  public class Mountebank extends Attack{
    public Mountebank(){
      super("mountebank");
      cost=5;
      value=2;
    }
    @Override
    public void subStep(int vic, int ap){
      game.mask=makeMask(game.players.get(vic).hand);
      game.server.displayComment(ap,"you may discard a curse to block the attack, or press 'Done Discarding' ");
      game.doWork("discard",0,1,vic);
      if(game.selectedCards.size()==0){
        game.gainCard("curse",vic);
        game.gainCard("copper",vic);
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals("curse");
    }
  }
  public class Rabble extends Attack{
    public Rabble(){
      super("rabble");
      cost=5;
      cards=3;
    }
    @Override
    public void subStep(int vic, int ap){
      DominionPlayer player=game.players.get(vic);
      ArrayList<DominionCard> cards=player.draw(3);
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.isAction || card.isMoney){
          player.disc.put(card);
          it.remove();
        }       
      }
      if(cards.size()>0) game.putBack(vic,cards);
    }
  }
  public class Royalseal extends DominionCard{
    public Royalseal(){
      super("royalseal");
      cost=5;
      value=2;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      royalSeal=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      royalSeal=false;
      return false;
    }
  }
  public class Vault extends Attack{
    public Vault(){
      super("vault");
      cost=5;
      cards=2;
      isAttack=false;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",0,100,ap);
      game.money+=game.selectedCards.size();
      game.updateSharedFields();
    }
    @Override
    public void subStep(int ap, int atk){
      game.selectedCards.clear();
      String [] options={"Discard 2 cards","pass"};
      String input=game.optionPane(ap,new OptionData(options));
      if(input.equals(options[0])){
        game.doWork("discard",2,2,ap);
        game.players.get(ap).drawToHand(1);
        game.displayPlayer(ap);
      }
    }
  }
  public class Venture extends DominionCard{
    public Venture(){
      super("venture");
      cost=5;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      DominionCard card;
      DominionPlayer player=game.players.get(ap);
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          return;
        }
        if(card.isMoney){
          game.playCard(card,ap);
          break;
        }else player.disc.put(card);
      }
      game.displayPlayer(ap);
    }
  }
  private class Goons extends Attack{
    private boolean inPlay=false;
    public Goons(){
      super("goons");
      cost=6;
      value=2;
      buys=1;
    }
    @Override
    public void subStep(int activePlayer, int attacker){
      game.server.displayComment(activePlayer,"Discard down to three cards");
      int n=game.players.get(activePlayer).hand.size()-3;
      if(n>=1) game.doWork("discard",n,n,activePlayer);
    }
    @Override
    public void subWork(int ap){
      if(!inPlay) goons++;
      inPlay=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      goons--;
      inPlay=false;
      return false;
    }
  }
  private class Hoard extends DominionCard{
    private boolean inPlay=false;
    public Hoard(){
      super("hoard");
      cost=6;
      value=2;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      if(!inPlay) hoard++;
      inPlay=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      inPlay=false;
      hoard--;
      return false;
    }   
  }  
  private class Bank extends DominionCard{
    public Bank(){
      super("bank");
      cost=7;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      int temp=0;
      for(DominionCard card : game.matcards){
        if(card.isMoney) temp++;
      }
      game.money+=temp;
      game.updateSharedFields();
    }
  }
  private class Expand extends RegularCard{
    public Expand(){
      super("expand");
      cost=7;
      comment="Trash a card, gain a card costing up to 3 more than it";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork("trash",1,1,activePlayer);
      game.displayTrash();

      game.gainLimit=game.cost2(game.selectedCards.get(0))+3;
      game.doWork("gain",1,1,activePlayer);      
    }    
  }
  private class Forge extends RegularCard{
    public Forge(){
      super("forge");
      cost=7;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",0,100,ap);
      int temp=0;
      for(DominionCard card : game.selectedCards){
        temp+=game.cost2(card);
      }
      game.server.displayComment(ap,"Gain a card costing exactly "+temp);
      game.selectedCards.clear();
      game.controlledGain(ap,temp);
    }
  }
  private class Kingscourt extends DominionCard{
    public Kingscourt(){
      super("kingscourt");
      cost=7;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      game.server.displayComment(activePlayer,"Choose a card to play thrice");
      Collection<DominionCard> hand=game.players.get(activePlayer).hand;
      
      game.mask=makeMask(hand);
      
      if(game.mask.contains(true)){
        game.doWork("select",1,1,activePlayer);
        DominionCard card=game.selectedCards.get(0);

        game.selectedCards.clear();
        game.changePhase("actions");
        game.server.displayComment(activePlayer,"");
        
        game.playCard(card,activePlayer,true);
        game.playCard(card,activePlayer,true);
        game.playCard(card,activePlayer,false);

        //if you throne room a duration card, send this to the duration mat also
        if(card.isDuration) isDuration=true;
        card.throneroomed++;
        
        //game.displayPlayer(activePlayer);
        //game.matcards.add(card);
        game.cardPlayed(activePlayer);
      }
    }
    @Override 
    public boolean maskCondition(DominionCard card){
      return card.isAction;
    }
    @Override
    public void duration(int ap){
      isDuration=false;
    }
  }

}









