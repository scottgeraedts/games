import java.util.*;

public class Core extends Expansion{

  public static int merchantCounter=0;
  
  public Core(Dominion g){
    super(g);
    String [] temp={"adventurer", "bureaucrat", "cellar", "chancellor", "chapel", 
      "councilroom", "feast", "festival", "laboratory", "library", "market", "militia", 
      "mine","moneylender","remodel", "smithy", "spy", "thief", "throneroom", "village", "witch",
      "woodcutter","workshop","gardens","harbinger","merchant","vassal","bandit","poacher","sentry","artisan"};
    cards=temp;
  }
  private class Adventurer extends DominionCard{ 
  
    public Adventurer(){
      super("adventurer");
      cost=6;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      int counter=0;
      
      DominionCard card;
      
      while (counter<2){
        try{
          card=game.players.get(activePlayer).getCard();
        }catch(OutOfCardsException e){
          return;
        }
        if(card.isMoney){
          game.money+=card.value;
          game.matcards.add(card);
          counter++;
        }else{
          game.players.get(activePlayer).disc.put(card);
        }
      }
      game.cardPlayed(activePlayer);
    }
  }
  private class Artisan extends RegularCard{
    public Artisan(){
      super("artisan");
      cost=6;
      comment="Gain a card costing up to 5, then put a card on top of your deck";          
    }
    @Override
    public void subWork(int activePlayer){
      game.gainLimit=5;
      game.doWork("gain",0,1,activePlayer);
      game.players.get(activePlayer).hand.add(game.players.get(activePlayer).disc.topCard());
      game.displayPlayer(activePlayer);
      game.selectedCards.clear();
      game.doWork("topdeck",1,1,activePlayer);
      
    }
    
  }
  private class Bandit extends Attack{
    public Bandit(){
      super("bandit");
      cost=5;
    }
    @Override
    public void subWork(int activePlayer){
      game.gainCard("gold",activePlayer);
    }
    @Override
    public void subStep(int victim, int attacker){
      DominionPlayer player=game.players.get(victim);
      DominionCard card1,card2;
      OptionData o=new OptionData(new String[0]);
      boolean doneButton=true;

      ArrayList<DominionCard> cards=player.draw(2);
      for(DominionCard card : cards){
        if(test(card)){
          o.put(card.getImage(),"imagebutton");
          doneButton=false;
        }else{
          o.put(card.getImage(),"image");
        }
      }       
      if(doneButton) o.put("Done","textbutton");
      String input=game.optionPane(attacker,o);

      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(input.equals(card.getName())){
          game.trash.put(card);
          it.remove();
          break;
        }
      }
      game.displayTrash();
      player.disc.put(cards);
    }
    private boolean test(DominionCard card){
      return !card.getName().equals("copper") && card.isMoney;
    }
  }
  private class Bureaucrat extends Attack{
    public Bureaucrat(){
      super("bureaucrat");
      cost=4;
      attackPhase="topdeck";
    }
    @Override
    public void subWork(int activePlayer){
      game.gainCard("silver",activePlayer,"topcard");
    }
    @Override
    public void subStep(int activePlayer, int attacker){

      LinkedList<DominionCard> hand;
      DominionCard card;
      hand=game.players.get(activePlayer).hand;
      DominionPlayer player=game.players.get(activePlayer);

      game.mask=makeMask(hand);

      int count=Collections.frequency(game.mask,true);
      if(count==1){
        player.deck.add(player.hand.remove(game.mask.indexOf(true)));
      }else if(count>1){
        game.doWork("topdeck",1,1,activePlayer);
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isVictory;
    }
  }  
  
  private class Cellar extends RegularCard{
    public Cellar(){
      super("cellar");
      cost=2;
      actions=1;
      comment="Discard cards. +1 card per card discarded";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork("discard",0,1000,activePlayer);
      game.players.get(activePlayer).drawToHand(game.selectedCards.size());
      game.displayPlayer(activePlayer);
    }
  }
  private class Chancellor extends DominionCard{
    public Chancellor(){
      super("chancellor");
      cost=3;
      value=2;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      String[] options={"Discard Deck","Done"};
      OptionData o=new OptionData(options);
      String input=game.optionPane(activePlayer,o);
      DominionPlayer player=game.players.get(activePlayer);
      if(input.equals(options[0])){
        player.disc.put(player.deck.deal(player.deck.size()));
      }
    }    
  }
  private class Chapel extends RegularCard{
    public Chapel(){
      super("chapel");
      cost=2;
      comment="Trash up to 4 cards";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork("trash",0,4,activePlayer);
      game.displayTrash();
      game.displayPlayer(activePlayer);
    }
  }
  private class Councilroom extends DominionCard{
    public Councilroom(){
      super("councilroom");
      cost=5;
      isAction=true;
      cards=4;
      buys=1;
    }
    @Override
    public void work(int activePlayer){
      for(int i=activePlayer+1;i<activePlayer+game.players.size();i++){
        game.players.get( i%game.players.size() ).drawToHand(1);
        game.displayPlayer(i%game.players.size());
      }
    }
  }
  private class Harbinger extends DominionCard{
    public Harbinger(){
      super("harbinger");
      cost=3;
      actions=1;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      DominionPlayer player=game.players.get(activePlayer);
      DominionCard card;
      
      if(player.disc.size()>0){
        OptionData o=new OptionData(new String[0]);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          o.put(card.getName(),"imagebutton");
        }
        String input=game.optionPane(activePlayer,o);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.deck.put(card);
            game.displayPlayer(activePlayer);
            break;
          }
        }         
        
      }
    }
  }
  private class Feast extends RegularCard{
    public Feast(){
      super("feast");
      cost=4;
      comment="Gain a card costing up to 5";
    }
    @Override
    public void subWork(int activePlayer){
      game.gainLimit=5;
      game.doWork("gain",0,1,activePlayer);
      game.trash.put(game.matcards.remove(game.matcards.size()-1));
      game.displayTrash();
    }
    
  }
  private class Library extends DominionCard{
    public Library(){
      super("library");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      DominionPlayer player=game.players.get(activePlayer);
      DominionCard card;
      ArrayList<DominionCard> aside=new ArrayList<>();
      String [] options={"Keep","Set Aside"};
      OptionData o=new OptionData(options);
      String out;
      
      while(player.hand.size()<7){
        try{
          card=player.getCard();
          o.put(card.getImage(),"image");
        }catch(OutOfCardsException e){
          break;
        }
        if(card.isAction){
          game.displayPlayer(activePlayer);
          out=game.optionPane(activePlayer,o);
          if(out.equals(options[0])) player.hand.add(card);
          else aside.add(card);
        }else{
          player.hand.add(card);
        }
        o.remove(card.getImage()); 
      }
      player.disc.put(aside);
      game.displayPlayer(activePlayer);
    }
  }  
  private class Merchant extends DominionCard{
    public Merchant(){
      super("merchant");
      cost=3;
      cards=1;
      actions=1;
      isAction=true;
   }
   @Override 
   public void work(int activePlayer){
    merchantCounter++;
   }
   @Override
   public boolean cleanup(int ap, DominionPlayer player){
    merchantCounter=0;
    return false;
   }
  }
  private class Militia extends Attack{
    public Militia(){
      super("militia");
      cost=4;
      value=2;
      attackPhase="discard";
    }
    @Override
    public void subStep(int activePlayer, int attacker){
      game.server.displayComment(activePlayer,"Discard down to three cards");
      int n=game.players.get(activePlayer).hand.size()-3;
      if(n>=1) game.doWork("discard",n,n,activePlayer);
    }
  }
  private class Mine extends RegularCard{
    public Mine(){
      super("mine");
      cost=5;
      comment="Trash a treasure and gain one costing up to 3 more";
    }
    @Override
    public void subWork(int activePlayer){
      game.mask=makeMask(game.players.get(activePlayer).hand);
      game.doWork("trash",0,1,activePlayer);
      int cost=game.selectedCards.get(0).cost;

      game.changePhase("selectDeck");
      
      DominionCard card;
      Dominion.SupplyDeck deck;
      while(true){
        game.work(activePlayer);
        deck=game.supplyDecks.get(game.selectedDeck);
        if(deck.size()==0) continue;
        
        card=deck.peek();
        if(card.isMoney && deck.getCost()<=game.cost2(game.selectedCards.get(0))+3){
          game.gainCard(deck.getName(),activePlayer,"hand");
//          game.players.get(activePlayer).hand.add(deck.topCard());
//          game.server.cardGained(actions,game.money,buys,activePlayer,game.players.get(activePlayer).makeData(),deck.makeData());          
          break;
        }   
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
  }
  private class Moneylender extends RegularCard{
    public Moneylender(){
      super("moneylender");
      cost=4;
      comment="Trash a copper for +3 money";
    }
    @Override
    public void subWork(int activePlayer){
      game.mask=makeMask(game.players.get(activePlayer).hand);
      game.displayPlayer(activePlayer);
      game.doWork("trash",0,1,activePlayer);
      if(game.selectedCards.size()>0){
        game.money+=3;
        game.updateSharedFields();
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName()=="copper";
    }
  }
  private class Poacher extends RegularCard{
    public Poacher(){
      super("poacher");
      cost=4;
      cards=1;
      value=1;
      actions=1;
    } 
    @Override
    public void subWork(int activePlayer){
      if(game.emptyPiles>0){
        game.doWork("discard",game.emptyPiles,Math.max(1,game.emptyPiles),activePlayer);
      }
    }
  }
  private class Remodel extends RegularCard{
    public Remodel(){
      super("remodel");
      cost=4;
      comment="Trash a card, gain a card costing up to 2 more than it";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork("trash",1,1,activePlayer);
      game.displayTrash();

      game.gainLimit=game.cost2(game.selectedCards.get(0))+2;
      game.doWork("gain",1,1,activePlayer);      
    }    
  }
  private class Sentry extends DominionCard{
    public Sentry(){
      super("sentry");
      cost=5;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      ArrayList<DominionCard> cards=new ArrayList<>(2);
      DominionPlayer player=game.players.get(activePlayer);
      DominionCard card;
      String [] options={"Trash", "Discard", "Put back"};
      OptionData o=new OptionData(options);
      String input;
      
      for(int i=0;i<2;i++){
        try{
          card=player.getCard();
          o.put(card.getImage(),"image");
          input=game.optionPane(activePlayer,o);
          if(input.equals(options[0])){
            game.trash.put(card);
            game.displayTrash();
          }else if(input.equals(options[1])){
            player.disc.put(card);
          }else{
            cards.add(card);
          }
          game.displayPlayer(activePlayer);
          o.remove(card.getImage());
        }catch(OutOfCardsException ex){
          break;
        }
      }
      if(cards.size()==0) return;

      game.putBack(activePlayer,cards);      
    }
  }
  private class Spy extends Attack{
    OptionData o;
    String [] options={"Keep","Discard"};

    public Spy(){
      super("spy");
      cost=4;
      actions=1;
      cards=1;
      o=new OptionData(options);
    }
    @Override
    public void subWork(int activePlayer){
      discardOrKeep(activePlayer, activePlayer);
    }
    public void discardOrKeep(int activePlayer, int attacker){
      try{
        DominionCard card=game.players.get(activePlayer).getCard();
        o.put(card.getImage(),"image");
        String result=game.optionPane(attacker,o);
        if(result.equals(options[0])) game.players.get(activePlayer).deck.put(card);
        else game.players.get(activePlayer).disc.put(card);
        game.displayPlayer(activePlayer);
        o.remove(card.getImage());
      }catch(OutOfCardsException e){
      }
    }
    @Override
    public void subStep(int victim, int attacker){
      discardOrKeep(victim, attacker);
    }
  }
  private class Thief extends Attack{
    OptionData o=new OptionData();
    public Thief(){
      super("thief");
      cost=4;
    }
    @Override
    public void subStep(int victim, int attacker){
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionCard card=new DominionCard("copper");
      for(int i=0;i<2;i++){
        try{
          card=game.players.get(victim).getCard();
        }catch(OutOfCardsException e){}
        
        cards.add(card);
        if(card.isMoney)
          o.put(card.getImage(),"imagebutton");
        else
          o.put(card.getImage(),"image");
      }
      o.put("Done","textbutton");
      String out=game.optionPane(attacker,o);
      if(out.equals(cards.get(0).getName())){
        game.players.get(victim).disc.put(cards.get(1));
        card=cards.get(0);
      }else if(out.equals(cards.get(1).getName())){
        game.players.get(victim).disc.put(cards.get(0));
        card=cards.get(1);
      }else{
        game.players.get(victim).disc.put(cards);
        return;
      }
      
      for(int i=0;i<2;i++){
        if(out.equals(cards.get(i).getImage())){
          card=cards.get(i);
          break;
        }
      }
      
      o.clear();
      o.put(card.getImage(),"image");
      o.put("Keep","textbutton");
      o.put("Trash","textbutton");
      out=game.optionPane(attacker,o);
      if(out.equals("Keep")){
        game.players.get(attacker).disc.put(card);
      }else{
        game.trash.put(card);
        game.displayTrash();       
      }
      o.clear();
    }
  }
  private class Throneroom  extends DominionCard{
    public Throneroom(){
      super("throneroom");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      game.server.displayComment(activePlayer,"Choose a card to play twice");
      Collection<DominionCard> hand=game.players.get(activePlayer).hand;
      
      game.mask=makeMask(hand);
      
      if(game.mask.contains(true)){
        game.doWork("select",1,1,activePlayer);
        game.mask.clear();
        DominionCard card=game.selectedCards.get(0);

        game.selectedCards.clear();
        game.changePhase("actions");
        game.server.displayComment(activePlayer,"");
        
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
  private class Vassal extends DominionCard{
    public Vassal(){
      super("vassal");
      cost=3;
      value=2;
      isAction=true;
    }
    public void work(int activePlayer){
      DominionCard card;
      DominionPlayer player=game.players.get(activePlayer);
      try{
        card=player.getCard();
        if(card.isAction){
          String [] options={"Play", "Discard"};
          OptionData o=new OptionData(options);
          o.put(card.getName(), "image");
          String input=game.optionPane(activePlayer,o);
          if(input.equals(options[0])){
            game.playCard(card,activePlayer);
          }else{
            player.disc.put(card);
          }
        }else{
          player.disc.put(card);
        }
        game.displayPlayer(activePlayer);
      }catch(OutOfCardsException ex){}
    }
  }
  private class Witch extends Attack{
    public Witch(){
      super("witch");
      cost=5;
      cards=2;
      attackPhase="actions";
    }
    @Override
    public void subStep(int activePlayer, int attacker){
      game.gainCard("curse",activePlayer);
    }
  }  
  private class Workshop extends RegularCard{
    public Workshop(){
      super("workshop");
      cost=3;
      comment="Gain a card costing up to 4";
    }
    @Override
    public void subWork(int activePlayer){
      game.gainLimit=4;
      game.doWork("gain",0,1,activePlayer);      
    }
  }
  private class Gardens extends DominionCard{
    public Gardens(){
      super("gardens");
      cost=4;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      return cards.size()/10;
    }
  }
  
}
