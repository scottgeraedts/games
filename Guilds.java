import java.util.*;

public class Guilds extends Expansion{

  static int merchantguildCounter=0;

  Guilds(Dominion g){
    super(g);
    String [] t={"candlestickmaker","stonemason","advisor","baker","butcher","doctor","herald",
        "journeyman", "masterpiece", "merchantguild", "plaza", "soothsayer", "taxman"};
    cards=t;
  }
  public static int overpay(Dominion g, int ap){
    String [] options={"Overpay","Done"};
    OptionData o=new OptionData(options);
    o.put("Overpayed 0", "text");
    String input;
    int out=0;
    while(g.money>0){
      input=g.optionPane(ap,o);
      if(input.equals(options[0])){
        out++;
        g.money--;
        g.updateSharedFields();
        o=new OptionData(options);
        o.put("Overpayed "+out, "text");
      }else{
        break;
      }
    }
    return out;
  }  
  public class Candlestickmaker extends DominionCard{
    public Candlestickmaker(){
      super("candlestickmaker");
      cost=2;
      actions=1;
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.players.get(ap).coinTokens++;
      game.displayPlayer(ap);
    }
  }
  public class Stonemason extends RegularCard{
    public Stonemason(){
      super("stonemason");
      cost=2;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()==0) return;
      game.gainLimit=game.cost2(game.selectedCards.get(0))-1;
      if(game.gainLimit<0) return;
      game.selectedCards.clear();
      game.server.displayComment(ap, "gain 2 cards costing up to "+game.gainLimit);
      game.doWork("gain",0,2,ap);
    }
    @Override
    public void onGain(int ap){
      int over=overpay(game,ap);
      game.server.displayComment(ap, "Gain 2 cards costing "+over);
      int counter=0;

      while(counter<2){

        if(!game.isValidSupply(ap,over, (DominionCard c) -> c.isAction)) break;
        
        game.doWork("selectDeck",1,1,ap);
        System.out.println("cost: "+game.supplyDecks.get(game.selectedDeck).getCost());
        if(game.supplyDecks.get(game.selectedDeck).card.isAction && 
            game.supplyDecks.get(game.selectedDeck).getCost()==over){
          
          counter++;
          game.gainCard(game.selectedDeck,ap);
        }
      }
      game.changePhase("buys");
      game.selectedCards.clear();
      game.server.displayComment(ap, "");
    }
  }
  class Doctor extends RegularCard{
    public Doctor(){
      super("doctor");
      cost=3;
    }
    @Override
    public void subWork(int ap){
      game.server.displayComment(ap, "click on a supply pile to trash matches");
      game.doWork("selectDeck",1,1,ap);
      String name=game.supplyDecks.get(game.selectedDeck).getName();
      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(3);
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getName().equals(name)){
          it.remove();
          game.trash.put(card);
          game.displayTrash();
        }
      }
      game.putBack(ap, cards);
    }
    @Override
    public void onGain(int ap){
      int over=overpay(game, ap);
      String [] options={"Trash", "Discard", "Put Back"};
      OptionData o;
      DominionCard card;
      DominionPlayer player=game.players.get(ap);
      String input;
      for(int i=0; i<over; i++){
        try {
          card = player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        o=new OptionData(options);
        o.put(card.getImage(), "image");
        input=game.optionPane(ap, o);
        if(input.equals(options[0])){
          game.trash.put(card);
          game.displayTrash();
        }else if(input.equals(options[0])){
          player.disc.put(card);
          game.displayPlayer(ap);
        }else{
          player.deck.put(card);
          game.displayPlayer(ap);
        }
      }
    }
  }
  class Masterpiece extends DominionCard{
    public Masterpiece(){
      super("masterpiece");
      cost=3;
      value=1;
      isMoney=true;
    }
    @Override
    public void onGain(int ap){
      int over=overpay(game, ap);
      for(int i=0; i<over; i++) game.gainCard("silver", ap);
    }
  }
  class Advisor extends DominionCard{
    public Advisor(){
      super("advisor");
      cost=4;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(3);
      if (cards.size()==0) return;
      OptionData o=new OptionData();
      for(DominionCard card : cards){
        o.put(card.getImage(), "imagebutton");
      }
      String input=game.optionPane((ap+1)%game.players.size(), o);
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getImage().equals(input)){
          it.remove();
          player.disc.put(card);
          break;
        }
      }
      player.hand.addAll(cards);
      game.displayPlayer(ap);
    }
  }
  class Plaza extends RegularCard{
    public Plaza(){
      super("plaza");
      cost=4;
      actions=2;
      cards=1;
    }
    @Override
    public void subWork(int ap){
      DominionPlayer player=game.players.get(ap);
      game.mask=makeMask(player.hand);
      game.doWork("discard",0,1,ap);
      if(game.selectedCards.size()>0) player.coinTokens++;
      game.displayPlayer(ap);
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
  }
  class Taxman extends Attack{
    String cardName;
    public Taxman(){
      super("taxman");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      DominionPlayer player=game.players.get(ap);
      game.mask=makeMask(player.hand, c -> c.isMoney);
      game.doWork("trash", 0, 1, ap);
      cardName=game.selectedCards.get(0).getName();
      int gainLimit=game.cost2(game.selectedCards.get(0))+3;
      game.selectedCards.clear();
      Dominion.SupplyDeck deck;
      game.server.displayComment(ap, "gain a treasure costing up to "+gainLimit);
      while(true){
        game.doWork("selectDeck", 1, 1, ap);
        deck=game.supplyDecks.get(game.selectedDeck);
        if(deck.size()>0 && deck.getCost()<=gainLimit && deck.card.isMoney){
          game.gainCard(game.selectedDeck, ap, "topcard", true);
          break;
        }
      }
    }
    @Override
    public void subStep(int ap, int atk){
      DominionPlayer player=game.players.get(ap);
      game.mask=makeMask(player.hand, c -> c.getName().equals(cardName));
      int count=Collections.frequency(game.mask,true);
      if(count==1){
        player.deck.add(player.hand.remove(game.mask.indexOf(true)));
      }else if(count>1){
        game.doWork("discard",1,1,ap);
      }
    }
  }
  class Herald extends DominionCard{
    public Herald(){
      super("herald");
      cost=4;
      actions=1;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      try{
        DominionCard card=game.players.get(ap).getCard();
        if(card.isAction) game.playCard(card, ap);
        else{
          game.players.get(ap).deck.put(card);
          game.displayPlayer(ap);
        }
      }catch(OutOfCardsException ex){}
    }
    @Override
    public void onGain(int ap){
      int over=overpay(game, ap);
      DominionPlayer player=game.players.get(ap);
      DominionCard card;

      for(int i=0; i<over; i++) {
        if (player.disc.size() > 0) {
          OptionData o = new OptionData(new String[0]);
          for (DominionCard aDisc : player.disc) {
            card = aDisc;
            o.put(card.getName(), "imagebutton");
          }
          String input = game.optionPane(ap, o);
          for (DominionCard aDisc : player.disc) {
            card = aDisc;
            if (card.getName().equals(input)) {
              player.disc.remove(card);
              player.deck.put(card);
              game.displayPlayer(ap);
              break;
            }
          }
        }else{
          break;
        }
      }//for loop in over
    }
  }
  class Baker extends DominionCard{
    public Baker(){
      super("baker");
      cost=5;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.players.get(ap).coinTokens++;
      game.displayPlayer(ap);
    }
  }
  class Butcher extends RegularCard{
    public Butcher(){
      super("butcher");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      DominionPlayer player=game.players.get(ap);
      player.coinTokens+=2;
      game.doWork("trash", 0, 1, ap);
      String [] options={"Play Coin Token", "Done"};
      String input;
      OptionData o=new OptionData(options);
      int extra=0;
      if(game.selectedCards.size()>0){
        //play any number of coin tokens
        while(player.coinTokens>0){
          input=game.optionPane(ap, o);
          if(input.equals(options[0])){
            player.coinTokens--;
            extra++;
            game.displayPlayer(ap);
          }else{
            break;
          }
        }//while coin tokens

        //gain card
        game.server.displayComment(ap, "gain a card costing up to "+game.gainLimit);
        game.gainLimit=game.cost2(game.selectedCards.get(0))+extra;
        game.selectedCards.clear();
        game.doWork("gain", 1, 1, ap);
      }
    }
  }
  class Journeyman extends RegularCard{
    public Journeyman(){
      super("journeyman");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.doWork("selectDeck", 1, 1, ap);
      int counter=0;
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      while(counter<3){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        if(card.getName().equals(game.selectedDeck)){
          player.disc.put(card);
        }else{
          player.hand.add(card);
          counter++;
        }
      }
      game.displayPlayer(ap);
    }
  }
  class Merchantguild extends DominionCard{
    private boolean inPlay=false;
    public Merchantguild(){
      super("merchantguild");
      cost=5;
      value=1;
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(!inPlay){
        inPlay=true;
        merchantguildCounter++;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      inPlay=false;
      merchantguildCounter=0;
      return false;
    }
  }
  class Soothsayer extends Attack{
    public Soothsayer(){
      super("soothsayer");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.gainCard("gold", ap);
    }
    @Override
    public void subStep(int ap, int atk){
      boolean gained=game.gainCard("curse", ap);
      if(gained) game.players.get(ap).drawToHand(1);
    }
  }
}
