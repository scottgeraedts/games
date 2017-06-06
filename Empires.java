import java.util.*;

public class Empires extends Expansion {
  public static boolean [] enchantressSwitch;
  private int farmersSupply=0;
  public Empires(Dominion g) {
    super(g);
    String[] t = {"engineer", "cityquarter", "overlord", "royalblacksmith", "encampment",
      "patrician", "settlers", "castle"};
    cards = t;
    enchantressSwitch=new boolean[g.players.size()];
    Arrays.fill(enchantressSwitch, false);
  }

  class Engineer extends RegularCard {
    public Engineer() {
      super("engineer");
      debt = 4;
    }

    @Override
    public void subWork(int ap) {
      game.gainLimit = 4;
      game.doWork("gain", 1, 1, ap);
      String[] options = {"Trash this card", "Done"};
      String input = game.optionPane(ap, new OptionData(options));
      if (input.equals(options[0])) {
        game.trashCard(this, ap);
        game.matcards.remove(this);
        game.gainLimit = 4;
        game.doWork("gain", 1, 1, ap);
        game.selectedCards.clear();
      }
    }
  }

  class Cityquarter extends DominionCard {
    public Cityquarter() {
      super("cityquarter");
      debt = 8;
      actions = 2;
      isAction = true;
    }

    @Override
    public void work(int ap) {
      int x = 0;
      for (DominionCard card : game.players.get(ap).hand) {
        if (card.isAction) x++;
        ;
      }
      game.players.get(ap).drawToHand(x);
      game.displayPlayer(ap);
    }
  }

  class Overlord extends RegularCard {
    DominionCard card = null;

    public Overlord() {
      super("overlord");
      debt = 8;
    }

    @Override
    public void subWork(int ap) {
      Dominion.SupplyDeck deck;
      while (true) {
        game.doWork("selectDeck", 1, 1, ap);
        deck = game.supplyDecks.get(game.selectedDeck);
        if (deck.getCost() <= 5 && deck.card.isAction && deck.card.debt == 0) {
          card = game.cardFactory(deck.card.getName());
          game.playCard(card, ap, true);
          break;
        }
      }
    }

    @Override
    public boolean maskCondition(DominionCard card2) {
      if (card == null) return false;
      return card.maskCondition(card2);
    }

    @Override
    public boolean cleanup(int ap, DominionPlayer player) {
      card = null;
      return false;
    }
  }

  class Royalblacksmith extends DominionCard {
    public Royalblacksmith() {
      super("royalblacksmith");
      debt = 8;
      cards = 5;
      isAction = true;
    }

    @Override
    public void work(int ap) {
      DominionCard card;
      for (ListIterator<DominionCard> it = game.players.get(ap).hand.listIterator(); it.hasNext(); ) {
        card = it.next();
        if (card.getName().equals("copper")) {
          it.remove();
          game.players.get(ap).disc.put(card);
        }
      }
      game.displayPlayer(ap);
    }
  }

  class Encampment extends RegularCard {
    public Encampment() {
      super("encampment");
      cost = 2;
      actions = 2;
      cards = 2;
    }

    @Override
    public void subWork(int ap) {
      game.mask = makeMask(game.players.get(ap).hand,
              c -> c.getName().equals("gold") || c.getName().equals("plunder"));
      game.doWork("reveal", 0, 1, ap);
      if (game.selectedCards.size() == 0) {
        game.returnToSupply(this, ap);
        game.matcards.remove(this);
      }
    }
  }

  class Plunder extends DominionCard {
    public Plunder() {
      super("plunder");
      cost = 5;
      value = 2;
    }

    @Override
    public void work(int ap) {
      game.players.get(ap).vicTokens++;
    }
  }
  class Patrician extends DominionCard{
    public Patrician(){
      super("patrician");
      cost=2;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      try{
        DominionCard card=player.getCard();
        OptionData o=new OptionData();
        o.put(card.getImage(), "image");
        o.put("Done", "textbutton");
        game.optionPane(ap, o);
        if(game.cost2(card)>=5) player.hand.add(card);
        else player.deck.put(card);
        game.displayPlayer(ap);
      }catch (OutOfCardsException ex ){}
    }
  }
  class Emporium extends DominionCard{
    public Emporium(){
      super("emporium");
      cost=5;
      cards=1;
      actions=1;
      isAction=true;
      value=1;
    }
    @Override
    public void onGain(int ap){
      int x=0;
      DominionPlayer player=game.players.get(ap);
      for(DominionCard card : game.matcards){
        if(card.isAction) x++;
      }
      if(x>=5) player.vicTokens+=2;
    }
  }
  class Settlers extends DominionCard{
    public Settlers(){
      super("settlers");
      cost=2;
      cards=1;
      actions=1;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      DominionCard card;

      if(player.disc.size()>0){
        OptionData o=new OptionData(new String[0]);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals("copper"))
            o.put(card.getImage(),"imagebutton");
          else
            o.put(card.getImage(), "image");
        }
        o.put("Done", "textbutton");
        String input=game.optionPane(ap,o);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.hand.add(card);
            game.displayPlayer(ap);
            break;
          }
        }

      }
    }
  }
  class Bustlingvillage extends DominionCard{
    public Bustlingvillage(){
      super("bustlingvillage");
      cost=5;
      cards=1;
      actions=3;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      DominionCard card;

      if(player.disc.size()>0){
        OptionData o=new OptionData(new String[0]);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals("settlers"))
            o.put(card.getImage(),"imagebutton");
          else
            o.put(card.getImage(), "image");
        }
        o.put("Done", "textbutton");
        String input=game.optionPane(ap,o);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.hand.add(card);
            game.displayPlayer(ap);
            break;
          }
        }

      }
    }
  }
  //**CASTLES***//
  class Humblecastle extends DominionCard{
    public Humblecastle(){
      super("humblecastle");
      cost=3;
      value=1;
      isVictory=true;
      isMoney=true;
      isCastle=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      int x=0;
      for(DominionCard card : cards){
        if(card.isCastle) x++;
      }
      return x;
    }
  }
  class Crumblingcastle extends DominionCard{
    public Crumblingcastle(){
      super("crumblingcastle");
      cost=4;
      isCastle=true;
      isVictory=true;
      vicPoints=1;
    }
    @Override
    public void onGain(int ap){
      stuff(ap);
    }
    @Override
    public void onTrash(int ap){
      stuff(ap);
    }
    public void stuff(int ap){
      game.players.get(ap).vicTokens++;
      game.gainCard("silver", ap, "discard", true);
    }
  }
  class Smallcastle extends DominionCard{
    public Smallcastle(){
      super("smallcastle");
      cost=5;
      isCastle=true;
      isAction=true;
      isVictory=true;
      vicPoints=2;
    }
    @Override
    public void work(int ap){
      String [] options={"Trash this", "Trash castle from hand"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.matcards.remove(this);
        game.trashCard(this, ap);
        game.gainCard("castle", ap);
      }else{
        game.mask=makeMask(game.players.get(ap).hand, c -> c.isCastle);
        game.doWork("trash", 1, 1, ap);
        if(game.selectedCards.size()>0) game.gainCard("castle", ap);
      }
    }
  }
  class Hauntedcastle extends DominionCard{
    public Hauntedcastle(){
      super("hauntedcastle");
      cost=6;
      isCastle=true;
      isVictory=true;
      vicPoints=2;
    }
    @Override
    public void onGain(int ap){
      game.gainCard("gold", ap, "discard", true);
      for(int i=(ap+1)%game.players.size(); i!=ap; i=(i+1)%game.players.size()){
        if(game.players.get(i).hand.size()>=5){
          game.doWork("topdeck", 2, 2, i);
        }
        game.selectedCards.clear();
      }//player loop
    }
  }
  class Opulentcastle extends RegularCard{
    public Opulentcastle(){
      super("opulentcastle");
      cost=7;
      isCastle=true;
      isVictory=true;
      vicPoints=3;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand, c -> c.isVictory);
      game.doWork("discard", 0, 100, ap);
      game.money+=2*game.selectedCards.size();
      game.updateSharedFields();
    }
  }
  class Sprawlingcastle extends DominionCard{
    public Sprawlingcastle(){
      super("sprawlingcastle");
      cost=8;
      isCastle=true;
      isVictory=true;
      vicPoints=4;
    }
    @Override
    public void onGain(int ap){
      String [] options={"Gain a Duchy", "Gain 3 estates"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.gainCard("duchy", ap,  "discard", true);
      }else{
        for(int i=0; i<3; i++){
          game.gainCard("estate", ap, "discard", true);
        }
      }
    }
  }
  class Grandcastle extends DominionCard{
    public Grandcastle(){
      super("grandcastle");
      cost=9;
      isCastle=true;
      isVictory=true;
    }
    @Override
    public void onGain(int ap){
      int x=0;
      for(DominionCard card : game.matcards){
        if(card.isVictory) x++;
      }
      for(DominionCard card : game.players.get(ap).hand){
        if(card.isVictory) x++;
      }
      game.players.get(ap).vicTokens+=x;
      game.displayPlayer(ap);
    }
  }
  class Kingscastle extends DominionCard{
    public Kingscastle(){
      super("kingscastle");
      cost=10;
      isCastle=true;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      int x=0;
      for(DominionCard card : cards){
        if(card.isCastle) x++;
      }
      return x*2;
    }
  }


  class Catapult extends Attack{
    DominionCard card;
    public Catapult(){
      super("catapult");
      cost=3;
      value=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap);
      if(game.selectedCards.size()>0) card=game.selectedCards.get(0);
      else card=null;
    }
    @Override
    public void subStep(int ap, int atk){
      if(card==null) return;
      if(game.cost2(card)>=3) game.gainCard("curse", ap);
      if(card.isMoney){
        int x=game.players.get(ap).hand.size()-3;
        game.doWork("discard", x, x, ap);
      }
    }
  }
  class Rocks extends DominionCard{
    public Rocks(){
      super("rocks");
      cost=4;
      value=1;
      isMoney=true;
    }
    private void stuff(int ap){
      if(game.getPhase().equals("buys")) game.gainCard("silver", ap, "deck", true);
      else game.gainCard("silver", ap, "hand", true);
    }
    @Override
    public void onGain(int ap){ stuff(ap); }
    @Override
    public void onTrash(int ap){ stuff(ap); }
  }
  class Chariotrace extends DominionCard{
    public Chariotrace(){
      super("chariotrace");
      cost=3;
      actions=1;
    }
    @Override
    public void work(int ap){
      DominionCard card1=null;
      DominionCard card2=null;
      try{
        card1=game.players.get(ap).getCard();
      }catch(OutOfCardsException ex){return; }
      game.players.get(ap).hand.add(card1);
      game.displayPlayer(ap);
      int i=(ap+1)%game.players.size();
      try{
        card2=game.players.get(i).getCard();
      }catch(OutOfCardsException ex){return; }

      game.players.get(i).deck.put(card2);
      OptionData o=new OptionData();
      o.put(card1.getImage(), "image");
      o.put(card2.getImage(), "image");
      o.put("Done", "textbutton");
      if(game.cost2(card1)>game.cost2(card2)){
        game.money++;
        game.players.get(ap).vicTokens++;
        game.displayPlayer(ap);
        game.updateSharedFields();
      }
    }
  }
  class Enchantress extends Attack{
    public Enchantress(){
      super("enchantress");
      cost=3;
      isDuration=true;
    }
    @Override
    public void subStep(int ap, int atk){
      enchantressSwitch[ap]=true;
    }
    @Override
    public void duration(int ap){
      game.players.get(ap).drawToHand(2);
      game.displayPlayer(ap);
    }
  }
  class Farmersmarket extends DominionCard{
    public Farmersmarket(){
      super("farmersmarket");
      cost=3;
      buys=3;
      isAction=true;
      isGathering=true;
    }
    @Override
    public void work(int ap){
      if(farmersSupply>=4){
        game.players.get(ap).vicTokens+=farmersSupply;
        farmersSupply=0;
        game.matcards.remove(this);
        game.trashCard(this, ap);
      }else{
        farmersSupply++;
        game.money+=farmersSupply;
        game.updateSharedFields();
      }
    }
  }
}
