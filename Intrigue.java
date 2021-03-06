import java.util.*;

public class Intrigue extends Expansion{

  public static int coppersmithCounter=0;
  
  public Intrigue(Dominion g){
    super(g);
    String [] t={"courtyard","lurker","masquerade","shantytown","pawn",
      "steward","swindler","wishingwell","baron","bridge","conspirator","diplomat","ironworks",
      "mill","miningvillage","secretpassage","courtier","duke","minion","patrol","replace",
      "torturer","tradingpost","upgrade","nobles","harem","secretchamber","greathall",
      "coppersmith","scout","saboteur","tribute"};
    cards=t;
  }
  private class Courtyard extends RegularCard{
    public Courtyard(){
      super("courtyard");
      cost=2;
      cards=3;
      comment="Top deck 1 card";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.TOP_DECK,1,1,activePlayer);
      game.displayPlayer(activePlayer);
    }
  }
  private class Lurker extends RegularCard{
    public Lurker(){
      super("lurker");
      cost=2;
      actions=1;
      comment="Gain an action from the trash, or trash a card from the supply";
    }
    @Override
    public void subWork(int activePlayer){

      String [] options={"trash from supply"};
      OptionData o=new OptionData(options);
      
      if(game.cardInTrash(c -> c.isAction)){
        o.add("Gain from Trash","textbutton");
      }else{
        o.add("No actions in trash","text");
      }
      String input=game.optionPane(activePlayer,o);
      DominionCard card;

      //if they are selecting a supply to game.trash from
      if(input.equals(options[0]) ){
        game.changePhase(Dominion.Phase.SELECT_DECK);
        Dominion.SupplyDeck deck;
        while(true){
          game.work(activePlayer);
          deck=game.supplyDecks.get(game.selectedDeck);
          if(deck.size()==0) continue;
          card=deck.peek();
          if(card.isAction){
            game.trashCard(deck.topCard(), activePlayer);
            if(deck.size()==0) game.emptyPiles++;
            break;
          }
        }
        game.displaySupply(deck.makeData());
      //if they are gaining an action from supply  
      }else{
        game.gainFromTrash(activePlayer, Dominion.GainTo.DISCARD, c -> c.isAction);
      }
    }
  }
  private class Pawn extends DominionCard{
    public Pawn(){
      super("pawn");
      cost=2;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      ArrayList<String> options=new ArrayList<>(4);
      options.add("+1 Card");
      options.add("+1 Action");
      options.add("+1 Money");
      options.add("+1 Buy");
      OptionData o=new OptionData(options.toArray(new String[4]));
      
      String input=game.optionPane(activePlayer,o);
      resolve(input, activePlayer);
      options.remove(input);
      o=new OptionData(options.toArray(new String[3]));
      resolve(game.optionPane(activePlayer,o), activePlayer);
      game.displayPlayer(activePlayer);
      game.updateSharedFields();
    }
    public void resolve(String input,int activePlayer){  
      if(input.equals("+1 Card")) game.players.get(activePlayer).drawToHand(1);
      if(input.equals("+1 Action")) game.actions++;
      if(input.equals("+1 Money")) game.money++;
      if(input.equals("+1 Buy")) game.buys++;        
    }
  }
  private class Masquerade extends Attack{
    HashMap<Integer,DominionCard> passedCards;
    public Masquerade(){
      super("masquerade");
      cost=3;
      isAttack=false;
      cards=2;
      comment="Pass a card to the left";
    }
    @Override
    public void cleanup(int activePlayer){
      //add the cards into the players hands
      DominionCard card;
      for(int i=0;i<game.players.size();i++){
        card=passedCards.get(i);
        if(card!=null){
          game.players.get(i).hand.add(card);
          game.displayPlayer(i);
        }
      }

      game.doWork(Dominion.Phase.TRASH,1,1,activePlayer);
    }
    @Override
    public void subWork(int activePlayer){
      passedCards=new HashMap<>();
      passCard(activePlayer,(activePlayer+1)%game.players.size());
      
    }
    @Override
    public void subStep(int i, int activePlayer){
      passCard(i,(i+1)%game.players.size());
    }
    //passes a cardfrom player i to player i+1
    public void passCard(int fromPlayer, int toPlayer){
      game.doWork(Dominion.Phase.SELECT,1,1,fromPlayer);
      passedCards.put(toPlayer,game.selectedCards.get(0));
      game.selectedCards.clear();
    }
  }
  private class Shantytown extends DominionCard{
    public Shantytown(){
      super("shantytown");
      cost=3;
      actions=2;
      isAction=true;
    }
    @Override 
    public void work(int activePlayer){
      boolean actions=false;
      for(Iterator<DominionCard> it=game.players.get(activePlayer).hand.iterator(); it.hasNext(); ){
        if(it.next().isAction){
          actions=true;
          break;
        }
      }
      if(!actions){
        game.players.get(activePlayer).drawToHand(2);
        game.displayPlayer(activePlayer);
      }
    }
  }
  private class Steward extends DominionCard{
    public Steward(){
      super("steward");
      cost=3;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      String [] options={"trash 2 cards", "+2 cards", "+2 money"};
      String input=game.optionPane(activePlayer,new OptionData(options));
      
      if(input.equals(options[0])){
        game.doWork(Dominion.Phase.TRASH,2,2,activePlayer);
        game.changePhase(Dominion.Phase.ACTIONS);
        game.selectedCards.clear();
      }else if(input.equals(options[1])){
        game.players.get(activePlayer).drawToHand(2);
        game.displayPlayer(activePlayer);
      }else{
        game.money+=2;
        game.updateSharedFields();
      }
    }
  }
  private class Swindler extends Attack{
    public Swindler(){
      super("swindler");
      cost=3;
      value=2;
    }
    @Override
    public void subStep(int victim, int attacker){
      try{
        //this code is basically game.gainSpecial but the attacker chooses the card to gain
        DominionCard card=game.players.get(victim).getCard();
        System.out.println("swindler trashed "+card.getName());
        int value=game.cost2(card);
        game.trashCard(card, victim);
        game.server.displayComment(attacker,"Trashed a "+card.getName()+", choose a card costing "+value);

        if(!game.isValidSupply(c -> game.costCompare(c, card)==0)) return;
        Dominion.Phase oldPhase=game.getPhase();
        while (true) {
          game.doWork(Dominion.Phase.SELECT_DECK, 1, 1, attacker);
          Dominion.SupplyDeck deck = game.supplyDecks.get(game.selectedDeck);
          if (game.costCompare(deck.card, card)==0 && deck.size() > 0){
            game.gainCard(game.selectedDeck, victim, Dominion.GainTo.DISCARD, true);
            Empires.triumphCounter++;
            if(deck.getName().equals("silver")) Empires.conquestCounter++;
            break;
          }
//      selectedCards.clear();
        }
        game.changePhase(oldPhase);
      }catch(OutOfCardsException e){
      }
    }
  }
  private class Wishingwell extends RegularCard{
    public Wishingwell(){
      super("wishingwell");
      cost=3;
      cards=1;
      actions=1;
    }
    @Override
    public void subWork(int activePlayer){
      try{
        game.changePhase(Dominion.Phase.SELECT_DECK2);
        game.work(activePlayer);
        String card1=game.supplyDecks.get(game.selectedDeck).card.getName();
        DominionCard card2=game.players.get(activePlayer).getCard();
        
        //show the drawn card
        OptionData o=new OptionData(new String[0]);
        o.add(card2.getImage(),"image");
        o.add("Continue","textbutton");
        game.optionPane(activePlayer,o);        
        
        if(card1.equals(card2.getName())){
          game.players.get(activePlayer).hand.add(card2);
        }else{
          game.players.get(activePlayer).deck.put(card2);
        }
        game.displayPlayer(activePlayer);
      }catch(OutOfCardsException ex){}
        
    }
  }
  private class Baron extends RegularCard{
    public Baron(){
      super("baron");
      cost=4;
      buys=1;
    }
    @Override 
    public void subWork(int activePlayer){
      DominionPlayer player=game.players.get(activePlayer);
      game.mask=makeMask(player.hand);
      game.doWork(Dominion.Phase.DISCARD,0,1,activePlayer);
      
      if(game.selectedCards.size()>0){
        game.money+=4;
      }else{
        game.changePhase(Dominion.Phase.ACTIONS);
        game.gainCard("estate",activePlayer);
      }
      game.updateSharedFields();
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals("estate");
    }     
  }
  private class Bridge extends DominionCard{
    public Bridge(){
      super("bridge");
      cost=4;
      buys=1;
      value=1;
      isAction=true;
    }
    @Override
    public void work(int x){
      game.bridgeCounter++;
      game.displaySupplies();
    }
  }
  private class Conspirator extends DominionCard{
    public Conspirator(){
      super("conspirator");
      cost=4;
      value=2;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      if(game.conspiratorCounter>=3){
        game.players.get(activePlayer).drawToHand(1);
        game.actions++;
      }
      game.displayPlayer(activePlayer);
      game.updateSharedFields();
    }
  }
  private class Diplomat extends DominionCard{
    public Diplomat(){
      super("diplomat");
      cost=4;
      cards=2;
      isAction=true;
      isReaction1=true;
    }
    @Override
    public void work(int activePlayer){
      if(game.players.get(activePlayer).hand.size()<=5){
        game.actions+=2;
        game.updateSharedFields();
      }
    }
  }
  private class Ironworks extends RegularCard{
    public Ironworks(){
      super("ironworks");
      cost=4;
    }
    @Override
    public void subWork(int activePlayer){
      DominionCard card=game.gainNumber(activePlayer, 4);
      
      if(card.isAction){
        game.actions++;
        game.updateSharedFields();        
      }
      if(card.isMoney){ 
        game.money++;
        game.updateSharedFields();
      }
      if(card.isVictory){
        game.players.get(activePlayer).drawToHand(1);
        game.displayPlayer(activePlayer);
      }
    }
  }
  private class Mill extends RegularCard{
    public Mill(){
      super("mill");
      cost=4;
      isVictory=true;
      vicPoints=1;
    }
    @Override
    public void subWork(int activePlayer){
      game.actions++;
      game.players.get(activePlayer).drawToHand(1);
      game.displayPlayer(activePlayer);
      
      String [] options={"Discard 2 cards", "Done"};
      String input=game.optionPane(activePlayer,new OptionData(options));
      if(input.equals(options[0])){
        game.doWork(Dominion.Phase.DISCARD,2,2,activePlayer);
        game.money+=2;
        game.updateSharedFields();
      }
    }    
  }
  private class Miningvillage extends DominionCard{
    public Miningvillage(){
      super("miningvillage");
      cost=4;
      actions=2;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      String [] options={"trash for +2","Done"};
      if(game.optionPane(activePlayer,new OptionData(options)).equals(options[0])){
        if(game.matcards.remove(this)){
          game.trashCard(this, activePlayer);
          game.money+=2;
          game.cardPlayed(activePlayer);
        }
      }
    }
  }
  private class Secretpassage extends RegularCard{
    public Secretpassage(){
      super("secretpassage");
      cost=4;
      actions=1;
      cards=2;
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.SELECT,1,1,activePlayer);
      game.putAnywhere(activePlayer,game.selectedCards.get(0));
    }
  }
  private class Courtier extends RegularCard{
    public Courtier(){
      super("courtier");
      cost=5;
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.SELECT,1,1,activePlayer);
      DominionCard card=game.selectedCards.get(0);
      game.players.get(activePlayer).hand.add(card);
      game.displayPlayer(activePlayer);
      int picks=0;
      if(card.isAction) picks++;
      if(card.isVictory) picks++;
      if(card.isMoney) picks++;
      if(card.isAttack) picks++;
      if(card.isReaction()) picks++;
      if(card.isDuration) picks++;
      if(card.isShelter) picks++;
      if(card.isLooter) picks++;
      if(card.isRuins) picks++;
      if(card.isKnight) picks++;
      if(card.isCastle) picks++;
      if(card.isGathering) picks++;
      if(card.isReserve) picks++;
      if(card.isTraveller) picks++;

      ArrayList<String> options=new ArrayList<>(4);
      ArrayList<String> choices=new ArrayList<>(picks);
      options.add("+1 Action");
      options.add("+1 Buy");
      options.add("+3 Money");
      options.add("Gain Gold");
      String input;
      Dominion.SupplyDeck deck=game.supplyDecks.get("gold");
      while(picks>0){
        input=game.optionPane(activePlayer,new OptionData(options.toArray(new String[options.size()])));
        picks--;
        options.remove(input);
        choices.add(input);
      }
      for(String choice : choices){
        if(choice.equals("+1 Action")) game.actions++;
        else if(choice.equals("+1 Buy")) game.buys++;
        else if(choice.equals("+3 Money")) game.money+=3;
        else{
          game.gainCard("gold",activePlayer);
        }        
      }
      game.updateSharedFields();
          
    }
  }
  private class Duke extends DominionCard{
    public Duke(){
      super("duke");
      cost=5;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      int nDuchies=0;
      for(DominionCard card : cards){
        if(card.getName().equals("duchy")) nDuchies++;
      }
      return nDuchies;
    }
  }
  private class Minion extends Attack{
    public String choice;
    public final String [] options={"+2 Money", "Discard and Draw 4"};
    public Minion(){
      super("minion");
      cost=5;
      actions=1;
    }
    @Override
    public void subWork(int activePlayer){
      choice=game.optionPane(activePlayer,new OptionData(options));
      if(choice.equals(options[1])){
        redraw(activePlayer);
        game.displayPlayer(activePlayer);
      }else{
        game.money+=2;
        game.updateSharedFields();
      }        
    }
    @Override 
    public void subStep(int victim, int attacker){
      if(choice.equals(options[1])) redraw(victim);
    }
    public void redraw(int victim){
      DominionPlayer player=game.players.get(victim);
      player.disc.put(player.hand);
      player.hand.clear();
      player.drawToHand(4);
    }
  }
  private class Patrol extends DominionCard{
    public Patrol(){
      super("patrol");
      cost=5;
      isAction=true;
      cards=3;
    }
    @Override
    public void work(int activePlayer){
      ArrayList<DominionCard> cards=new ArrayList<>(4);
      DominionCard card;
      for(int i=0;i<4;i++){
        try{
          card=game.players.get(activePlayer).getCard();
          if(card.isVictory || card.getName().equals("curse")){
            game.players.get(activePlayer).hand.add(card);
          }else{
            cards.add(card);
          }
        }catch(OutOfCardsException ex){
          break;
        }
      }
      game.displayPlayer(activePlayer);
      game.putBack(activePlayer,cards);
    }
  }
  private class Replace extends Attack{
    private boolean curse=false;
    public Replace(){
      super("replace");
      cost=5;
      comment="trash a card, gain a card costing up to 2 more than it";
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.TRASH,1,1,activePlayer);

      if(game.selectedCards.size()==0) return;


      DominionCard card=game.gainSpecial(activePlayer, c -> game.costCompare(c, game.selectedCards.get(0), 2)<=0);
      game.selectedCards.clear();
      if(card.isAction || card.isMoney){
        curse=false;
        game.players.get(activePlayer).deck.put(game.players.get(activePlayer).disc.topCard());
      }
      if(card.isVictory){
        curse=true;
      }
    }  
    @Override
    public void subStep(int victim, int attacker){
      if(curse) game.gainCard("curse",victim);
    }
  }
  private class Torturer extends Attack{
    private final String [] options={"Discard 2 cards", "Gain curse in hand"};
    private OptionData o;
    public Torturer(){
      super("torturer");
      cost=5;
      cards=3;
      o=new OptionData(options);
    }
    @Override
    public void subStep(int victim, int attacker){
      if(game.optionPane(victim,o).equals(options[0])){
        game.doWork(Dominion.Phase.DISCARD,2,2,victim);
      }else{
        game.gainCard("curse",victim,Dominion.GainTo.HAND);
      }
    }
  }
  private class Tradingpost extends RegularCard{
    private final String [] options={"trash 2 cards for a Silver", "Done"};
    private OptionData o;
    public Tradingpost(){
      super("tradingpost");
      cost=5;
      o=new OptionData(options);
    }
    @Override
    public void subWork(int activePlayer){
      if(game.optionPane(activePlayer,o).equals(options[0])){
        game.doWork(Dominion.Phase.TRASH,2,2,activePlayer);
        game.gainCard("silver",activePlayer,Dominion.GainTo.HAND);
      }
    }
  }
  private class Upgrade extends RegularCard{
    public Upgrade(){
      super("upgrade");
      cards=1;
      actions=1;
      cost=5;
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.TRASH,1,1,activePlayer);
      game.displayPlayer(activePlayer);
      game.gainSpecial(activePlayer, c -> game.costCompare(c, game.selectedCards.get(0), 1)==0);
      game.selectedCards.clear();
    }
  }
  private class Nobles extends DominionCard{
    private final String [] options={"+3 cards", "+2 actions"};
    private OptionData o; 
    public Nobles(){
      super("nobles");
      cost=6;
      isAction=true;
      isVictory=true;
      o=new OptionData(options);
    }
    @Override
    public void work(int activePlayer){
      String input=game.optionPane(activePlayer,o);
      if(input.equals(options[0])){
        game.players.get(activePlayer).drawToHand(3);
        game.displayPlayer(activePlayer);
      }else{
        game.actions+=2;
        game.updateSharedFields();
      }
    }
  }
  private class Secretchamber extends RegularCard{
    public Secretchamber(){
      super("secretchamber");
      cost=2;
      isAction=true;
      isReaction1=true;
    }
    @Override
    public void subWork(int activePlayer){
      game.doWork(Dominion.Phase.DISCARD,0,100,activePlayer);
      game.money+=game.selectedCards.size();
      game.updateSharedFields();
      game.displayPlayer(activePlayer);
    }
  }
  private class Coppersmith extends DominionCard{
    public Coppersmith(){
      super("coppersmith");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      coppersmithCounter++;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      coppersmithCounter=0;
      return false;
    }
  }
  private class Scout extends DominionCard{
    public Scout(){
      super("scout");
      cost=4;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      ArrayList<DominionCard> cards=new ArrayList<>(4);
      DominionCard card;
      for(int i=0;i<4;i++){
        try{
          card=game.players.get(activePlayer).getCard();
          if(card.isVictory) game.players.get(activePlayer).hand.add(card);
          else cards.add(card);
        }catch(OutOfCardsException ex){
          break;
        }
      }
      game.putBack(activePlayer,cards);
    }
  }
  private class Saboteur extends Attack{
    public Saboteur(){
      super("saboteur");
      cost=5;
    }
    @Override
    public void subStep(int victim, int attacker){
      while(true){
        try{
          DominionCard card=game.players.get(victim).getCard();
          if(game.costCompare(card,3,0,0)<0) game.players.get(victim).disc.put(card);
          else{
            game.trashCard(card, victim);
            game.gainSpecial(victim, c -> game.costCompare(c, card, -2)<=0);
            break;
          }
        }catch(OutOfCardsException ex){
          break;
        }
      }
    }
  }
  private class Tribute extends DominionCard{
    public Tribute(){
      super("tribute");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int activePlayer){
      HashSet<DominionCard> cards=new HashSet<>();
      DominionCard  card;
      for(int i=0;i<2;i++){
        try{
          card=game.players.get( (activePlayer+1)%game.players.size()).getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        cards.add(card);
      }
      game.players.get( (activePlayer+1)%game.players.size()).disc.put(cards);
      System.out.println("size in tribute "+cards.size());
      OptionData o=new OptionData();      
      for(DominionCard card2 : cards){
        if(card2.isAction) game.actions+=2;
        if(card2.isVictory) game.players.get(activePlayer).drawToHand(2);
        if(card2.isMoney) game.money+=2;
        o.add(card2.getImage(),"image");
      }
      o.add("Done","textbutton");
      game.optionPane(activePlayer,o);
      game.updateSharedFields();
      game.displayPlayer(activePlayer);
      game.displayPlayer( (activePlayer+1)%game.players.size());
    }
  }
}
