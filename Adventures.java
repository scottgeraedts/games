import java.util.*;
import java.util.function.Predicate;

public class Adventures extends Expansion{

  static int treasureHunterCounter=0;

  static ArrayList<String> turnStart;

  static boolean almsSwitch=false;
  static boolean borrowSwitch=false;
  static boolean saveSwitch=false;
  static boolean travellingFairSwitch=false;
  static boolean missionSwitch1=false;
  static boolean missionSwitch2=false;
  static boolean pilgrimageSwitch=false;

  private static boolean portLock=false;

  public Adventures(Dominion g){
    super(g);
    String [] t={"coinoftherealm", "page", "peasant", "ratcatcher", "raze", "amulet", "caravanguard", "dungeon", "gear",
            "guide", "duplicate", "magpie", "messenger", "miser", "port", "ranger", "transmogrify",
            "artificer", "bridgetroll", "distantlands", "giant", "hauntedwoods", "lostcity",
            "relic", "royalcarriage", "storyteller", "swamphag", "treasuretrove", "winemerchant",
            "hireling", "alms", "borrow", "save", "quest", "scoutingparty", "travellingfair",
            "bonfire", "expedition", "ferry", "plan", "mission", "pilgrimage", "ball", "raid",
            "seaway", "trade", "lostarts", "training", "pathfinding"};
    cards=t;
    turnStart=new ArrayList<>();
    turnStart.add("teacher");
    turnStart.add("ratcatcher");
    turnStart.add("guide");
    turnStart.add("transmogrify");
  }
  ///actions related to various cards on the tavern mat
  void tavern(int ap, DominionCard incard, Predicate<DominionCard> tester){
    LinkedList<DominionCard> cards=new LinkedList<>();
    for(DominionCard card : game.players.get(ap).tavern){
      if(tester.test(card)) cards.add(card);
    }
    while(cards.size()>0){
      OptionData o=new OptionData();
      for(DominionCard card : cards){
        o.add(card.getImage(), "imagebutton");
      }
      o.add("Done", "textbutton");
      String input=game.optionPane(ap, o);
      if(input.equals("Done")) break;

      DominionCard card=Dominion.remove(cards, c -> c.getImage().equals(input));
      game.players.get(ap).tavern.remove(card);
      card.tavern(ap, incard);
      game.players.get(ap).disc.put(card);
      System.out.println(cards);
    }
  }

  //what to do when a traveller is going to be discarded
  //can't put this in a superclass because Dominioncard doesn't have access to Dominion
  //and some travellers are attacks and there's no multiple inheritance
  private boolean exchangeTraveller(int ap, DominionCard card, String newName){
    DominionCard newCard=game.cardFactory(newName, "Adventures");
    String [] options={"Exchange", "Keep"};
    OptionData o=new OptionData(options);
    o.add(card.getImage(), "image");
    o.add(newCard.getImage(), "image");
    String input=game.optionPane(ap, o);
    if(input.equals(options[0])){
      game.returnToSupply(card, ap);
      game.gainCardNoSupply(newCard, ap, Dominion.GainTo.DISCARD);
      return true;
    }
    return false;

  }

  class Coinoftherealm extends DominionCard{
    public Coinoftherealm(){
      super("coinoftherealm");
      cost=2;
      value=1;
      isMoney=true;
      isReserve=true;
      //this means it cant be throneroomed
      lostTrack=true;
    }
    @Override
    void tavern(int ap, DominionCard incard){
      game.actions+=2;
      game.updateSharedFields();
    }
  }

  class Page extends DominionCard{
    public Page(){
      super("page");
      cards=1;
      actions=1;
      isAction=true;
      isTraveller=true;
      cost=2;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "treasurehunter" );
    }
  }
  class Treasurehunter extends DominionCard{
    public Treasurehunter(){
      super("treasurehunter");
      cost=3;
      actions=1;
      value=1;
      isAction=true;
      isTraveller=true;
    }
    @Override
    public void work(int ap){
      for(int i=0; i<treasureHunterCounter; i++) game.gainCard("silver", ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "warrior" );
    }
  }
  class Warrior extends Attack{
    int counter;
    public Warrior(){
      super("warrior");
      cost=4;
      cards=2;
      isTraveller=true;
    }
    @Override
    public void subWork(int ap){
      counter=0;
      for(DominionCard card : game.matcards){
        if(card.isTraveller) counter++;
      }
    }
    @Override
    public void subStep(int ap, int atk){
      DominionCard card=null;
      for(int i=0; i<counter; i++){
        try{
          card=game.players.get(ap).getCard();
        }catch (OutOfCardsException ex){
          break;
        }
        if(game.costCompare(card,4,0,0)<=0 && game.cost2(card)>=3) game.trashCard(card, ap);
        else game.players.get(ap).disc.put(card);
        game.displayPlayer(ap);
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "hero" );
    }
  }
  class Hero extends DominionCard{
    public Hero(){
      super("hero");
      cost=5;
      value=2;
      isAction=true;
      isTraveller=true;
    }
    @Override
    public void work(int ap){
      game.server.displayComment(ap, "Gain a treasure");
      game.gainSpecial(ap, c -> c.isMoney);
      game.server.displayComment(ap, "");
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "champion" );
    }
  }
  class Champion extends DominionCard{
    public Champion(){
      super("champion");
      cost=6;
      actions=1;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void work(int ap){
      game.players.get(ap).champions++;
    }
  }
  class Peasant extends DominionCard{
    public Peasant(){
      super("peasant");
      cost=2;
      value=1;
      buys=1;
      isAction=true;
      isTraveller=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "soldier" );
    }
  }
  class Soldier extends Attack{
    public Soldier(){
      super("soldier");
      cost=3;
      value=2;
      isTraveller=true;
    }
    @Override
    public void subWork(int ap){
      //start at -1 so as to not count itself
      int counter=-1;
      for(DominionCard card : game.matcards){
        if(card.isAttack) counter++;
      }
      game.money+=counter;
      game.updateSharedFields();
    }
    @Override
    public void subStep(int ap, int atk){
      if(game.players.get(ap).hand.size()>=4)
        game.doWork(Dominion.Phase.DISCARD, 1, 1, ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "fugitive" );
    }
  }
  class Fugitive extends RegularCard{
    public Fugitive(){
      super("fugitive");
      cost=4;
      cards=2;
      actions=1;
      isTraveller=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.DISCARD, 1, 1, ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "disciple" );
    }
  }
  class Disciple extends RegularCard {
    public Disciple() {
      super("disciple");
      cost = 5;
      isTraveller=true;
    }

    @Override
    public void subWork(int ap) {

      //this first part is basically throneroom
      game.server.displayComment(ap, "Choose a card to play twice");
      game.doWork(Dominion.Phase.SELECT, 0, 1, ap, c -> c.isAction);
      if (game.selectedCards.size() == 0) return;
      DominionCard card = game.selectedCards.get(0);

      game.selectedCards.clear();
      game.changePhase(Dominion.Phase.ACTIONS);
      game.server.displayComment(ap, "");

      //this card will never to go the mat
      game.playCard(card, ap, true);
      if (!card.lostTrack) game.playCard(card, ap, false);

      //game.cardPlayed(ap);
      game.gainCard(card.getName(), ap);
    }//subWork
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      return exchangeTraveller(ap, this, "teacher" );
    }
  }
  class Teacher extends DominionCard{
    public Teacher(){
      super("teacher");
      cost=6;
      isAction=true;
      isReserve=true;
    }
    @Override
    void tavern(int ap, DominionCard incard){
      String [] options={"card", "money", "buy", "action"};
      String type=game.optionPane(ap, new OptionData(options));
      game.placeToken(ap, type, true);
    }
  }
  class Ratcatcher extends DominionCard{
    public Ratcatcher(){
      super("ratcatcher");
      cards=1;
      actions=1;
      cost=2;
      isAction=true;
      isReserve=true;
    }
    @Override
    void tavern(int ap, DominionCard incard){
      game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
      game.selectedCards.clear();
    }
  }
  class Raze extends RegularCard{
    public Raze(){
      super("raze");
      cost=2;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      String [] options={"Trash This", "Trash from hand"};
      String input=game.optionPane(ap, new OptionData(options));
      int x=0;
      if(input.equals(options[0])){
        game.matcards.remove(this);
        lostTrack=true;
        game.trashCard(this, ap);
        x=2;
      }else{
        game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
        if(game.selectedCards.size()>0) x=game.cost2(game.selectedCards.get(0));
      }
      if(x>0) {
        DominionPlayer player = game.players.get(ap);
        List<DominionCard> cards = player.draw(x);
        OptionData o = new OptionData();
        for (DominionCard card : cards) {
          o.add(card.getImage(), "imagebutton");
        }
        String input2 = game.optionPane(ap, o);
        player.hand.add(Dominion.remove(cards, c -> c.getName().equals(input2)));
        player.disc.put(cards);
        game.displayPlayer(ap);
      }
    }
  }
  class Amulet extends RegularCard{
    public Amulet(){
      super("amulet");
      cost=3;
      isDuration=true;
    }
    @Override
    public void subWork(int ap){
      choose(ap);
    }
    @Override
    public void duration(int ap){
      choose(ap);
    }
    private void choose(int ap){
      String [] options={"Trash card", "+1 Money", "Gain silver"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
      }else if(input.equals(options[1])){
        game.money++;
        game.updateSharedFields();
      }else{
        game.gainCard("silver", ap, Dominion.GainTo.DISCARD, true);
      }
      game.selectedCards.clear();
    }
  }
  class Caravanguard extends RegularCard{
    public Caravanguard(){
      super("caravanguard");
      cost=3;
      actions=1;
      cards=1;
      isReaction1=true;
      isDuration=true;
    }
    @Override
    public void duration(int ap){
      game.money++;
      game.updateSharedFields();
    }
  }
  class Dungeon extends RegularCard{
    public Dungeon(){
      super("dungeon");
      cost=3;
      actions=1;
      isDuration=true;
    }
    @Override
    public void subWork(int ap){
      game.players.get(ap).drawToHand(2);
      game.displayPlayer(ap);
      game.doWork(Dominion.Phase.DISCARD, 2, 2, ap);
      game.selectedCards.clear();
    }
    @Override
    public void duration(int ap){
      subWork(ap);
    }
  }
  class Gear extends RegularCard{
    List<DominionCard> savedCards;
    public Gear(){
      super("gear");
      cost=3;
      cards=2;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.SELECT, 0, 2, ap);
      savedCards=game.selectedCards;
    }
    @Override
    public void duration(int ap){
      game.players.get(ap).hand.addAll(savedCards);
      game.displayPlayer(ap);
    }
  }
  class Guide extends DominionCard {
    public Guide() {
      super("guide");
      cost = 3;
      cards = 1;
      actions = 1;
      isAction = true;
      isReserve = true;
    }

    @Override
    void tavern(int ap, DominionCard incard) {
      DominionPlayer player = game.players.get(ap);
      player.disc.put(player.hand);
      player.hand.clear();
      player.drawToHand(5);
      game.displayPlayer(ap);
    }
  }
  class Duplicate extends DominionCard{
    public Duplicate(){
      super("duplicate");
      isAction=true;
      cost=4;
      isReserve=true;
    }
    @Override
    public void tavern(int ap, DominionCard incard){
      game.gainCard(incard.getName(), ap, Dominion.GainTo.DISCARD, true);
    }
  }
  class Magpie extends DominionCard{
    public Magpie(){
      super("magpie");
      cost=4;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      try{
        DominionCard card=player.getCard();
        if(card.isMoney){
          player.hand.add(card);
        }else{
          player.deck.put(card);
        }
        game.displayPlayer(ap);
        if(card.isAction || card.isVictory) game.gainCard("magpie", ap);
      }catch (OutOfCardsException ex){
      }
    }
  }
  class Messenger extends DominionCard{
    public Messenger(){
      super("messenger");
      cost=4;
      value=2;
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      String[] options={"Discard Deck","Done"};
      OptionData o=new OptionData(options);
      String input=game.optionPane(ap,o);
      DominionPlayer player=game.players.get(ap);
      if(input.equals(options[0])){
        player.disc.put(player.deck.deal(player.deck.size()));
      }
      game.displayPlayer(ap);
    }
    @Override
    public void onGain(int ap){
      if(Empires.triumphCounter==0){
        DominionCard card=game.gainNumber(ap, 4);
        for(int i=(ap+1)%game.players.size(); i != ap; i=(i+1)%game.players.size()){
          game.gainCard(card.getName(), i, Dominion.GainTo.DISCARD, true);
        }
      }
    }
  }
  class Miser extends DominionCard{
    public Miser(){
      super("miser");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(int ap){
      String [] options={"Put copper on mat", "Take money"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.doWork(Dominion.Phase.SELECT, 1, 1, ap, c -> c.getName().equals("copper"));
        if(game.selectedCards.size()>0){
          game.players.get(ap).miser++;
          game.displayPlayer(ap);
          game.selectedCards.clear();
        }
      }else{
        game.money+=game.players.get(ap).miser;
        game.updateSharedFields();
      }
    }
  }
  class Port extends DominionCard{
    public Port(){
      super("port");
      cards=1;
      actions=2;
      isAction=true;
      cost=4;
    }
    @Override
    public void onGain(int ap){
      if(!portLock) {
        portLock=true;
        game.gainCard("port", ap, Dominion.GainTo.DISCARD, true);
        portLock=false;
      }
    }
  }
  class Ranger extends DominionCard{
    public Ranger(){
      super("ranger");
      cost=4;
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      if(player.journey){
        player.drawToHand(5);
      }
      player.journey=!player.journey;
      game.displayPlayer(ap);
    }
  }
  class Transmogrify extends DominionCard{
    public Transmogrify(){
      super("transmogrify");
      cost=4;
      actions=1;
      isAction=true;
      isReserve=true;
    }
    @Override
    void tavern(int ap, DominionCard card){
      game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
      if(game.selectedCards.size()==0) return;
      game.gainSpecial(ap, c -> game.costCompare(c, game.selectedCards.get(0), 1)<=0);
      game.selectedCards.clear();
    }

  }
  class Artificer extends RegularCard{
    public Artificer(){
      super("artificer");
      cost=5;
      cards=1;
      actions=1;
      value=1;
      isAction=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.DISCARD, 0, 100, ap);
      OptionData o=new OptionData();
      o.put("Gain a card costing "+game.selectedCards.size(), "textbutton");
      o.put("Pass", "textbutton");
      String input=game.optionPane(ap, o);
      if(!input.equals("Pass")){
        game.gainSpecial(ap, c -> game.costCompare(c, game.selectedCards.size(), 0, 0)==0, Dominion.GainTo.TOP_CARD);
      }
    }
  }
  class Bridgetroll extends Attack{
    public Bridgetroll(){
      super("bridgetroll");
      cost=5;
      isDuration=true;
    }
    @Override
    public void subStep(int ap, int atk){
      game.players.get(ap).minusMoneyToken=true;
    }
    @Override
    public void subWork(int ap){
      game.bridgeCounter++;
      game.buys++;
      game.displaySupplies();
      game.updateSharedFields();
    }
    @Override
    public void duration(int ap){
      subWork(ap);
    }
  }
  class Distantlands extends DominionCard{
    public Distantlands(){
      super("distantlands");
      cost=5;
      isVictory=true;
      isReserve=true;
      isAction=true;
    }
  }
  class Giant extends Attack{
    private boolean attack;
    public Giant(){
      super("giant");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      DominionPlayer player=game.players.get(ap);
      attack=player.journey;
      if(player.journey) game.money+=5;
      else game.money+=1;
      game.updateSharedFields();
      player.journey=!player.journey;
      game.displayPlayer(ap);
    }
    @Override
    public void subStep(int ap, int atk){
      if(attack){
        try{
          DominionCard card=game.players.get(ap).getCard();
          if(game.costCompare(card, 6, 0, 0) <=0 && game.cost2(card)>=3){
            game.trashCard(card, ap);
          }else{
            game.players.get(ap).disc.put(card);
            game.gainCard("curse", ap);
          }
        }catch (OutOfCardsException ex){

        }
      }
    }
  }
  class Hauntedwoods extends Attack{
    public Hauntedwoods(){
      super("hauntedwoods");
      cost=5;
      isDuration=true;
    }
    @Override
    public void subStep(int ap, int atk){
      game.players.get(ap).hauntedWoods=true;
    }
    @Override
    public void duration(int ap){
      game.players.get(ap).drawToHand(3);
      game.displayPlayer(ap);
    }
  }
  class Lostcity extends DominionCard{
    public Lostcity(){
      super("lostcity");
      cost=5;
      cards=2;
      actions=2;
      isAction=true;
    }
    @Override
    public void onGain(int ap){
      for(int i=(ap+1)%game.players.size(); i!=ap; i=(i+1)%game.players.size()){
        game.players.get(i).drawToHand(1);
        game.displayPlayer(i);
      }
    }
  }
  class Relic extends Attack{
    public Relic(){
      super("relic");
      cost=5;
      value=2;
      isAction=false;
      isMoney=true;
    }
    @Override
    public void subStep(int ap, int atk){
      game.players.get(ap).minusCardToken=true;
      game.displayPlayer(ap);
    }
  }
  class Royalcarriage extends DominionCard{
    public Royalcarriage(){
      super("royalcarriage");
      cost=5;
      actions=1;
      isAction=true;
      isReserve=true;
    }
    @Override
    void tavern(int ap, DominionCard card){
      game.playCard(card, ap, true);
    }
  }
  class Storyteller extends RegularCard{
    public Storyteller(){
      super("storyteller");
      cost=5;
      value=1;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.SELECT, 0, 3, ap, c -> c.isMoney);
      for(DominionCard card : game.selectedCards){
        game.playCard(card, ap);
      }
      game.players.get(ap).drawToHand(game.money);
      game.money=0;
      game.displayPlayer(ap);
      game.updateSharedFields();
    }
  }
  class Swamphag extends Attack{
    public Swamphag(){
      super("swamphag");
      cost=5;
      isDuration=true;
    }
    @Override
    public void subStep(int ap, int atk){
      game.players.get(ap).swampHag=true;
    }
    @Override
    public void duration(int ap){
      game.money+=3;
      game.updateSharedFields();
    }
  }
  class Treasuretrove extends DominionCard{
    public Treasuretrove(){
      super("treasuretrove");
      cost=5;
      value=2;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      game.gainCard("copper", ap, Dominion.GainTo.DISCARD, true);
      game.gainCard("gold", ap, Dominion.GainTo.DISCARD, true);
    }
  }
  class Winemerchant extends DominionCard{
    public Winemerchant(){
      super("winemerchant");
      cost=5;
      value=4;
      buys=1;
      isAction=true;
    }
  }
  class Hireling extends DominionCard {
    public Hireling() {
      super("hireling");
      cost = 6;
      isDuration = true;
      isAction = true;
    }

    @Override
    public void duration(int ap) {
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
    }

    @Override
    public boolean cleanup(int ap, DominionPlayer player) {
      //stays in tne duration pile forever
      player.duration.add(this);
      return true;
    }
  }
  class Alms extends DominionCard{
    public Alms(){
      super("alms");
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      if(!almsSwitch){
        boolean gain=true;
        for(DominionCard card : game.matcards){
          if(card.isMoney){
            gain=false;
            break;
          }
        }
        if(gain) game.gainNumber(ap, 4);
      }
      almsSwitch=true;
    }
  }
  class Borrow extends DominionCard{
    public Borrow(){
      super("borrow");
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.buys++;
      if(!borrowSwitch && game.players.get(ap).minusMoneyToken){
        game.players.get(ap).minusMoneyToken=true;
        game.money++;
        game.displayPlayer(ap);
      }
      borrowSwitch=true;
      game.updateSharedFields();
    }
  }
  class Quest extends DominionCard{
    public Quest(){
      super("quest");
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      ArrayList<String> options=new ArrayList<>(3);
      List<DominionCard> hand=game.players.get(ap).hand;
      if(hand.size()>=6) options.add("Discard 6 cards");
      int nCurses=0;
      int nAttacks=0;
      for(DominionCard card : hand){
        if(card.isAttack) nAttacks++;
        if(card.getName().equals("curse")) nCurses++;
      }
      if(nAttacks>0) options.add("Discard Attack");
      if(nCurses>1) options.add("Discard 2 curses");

      //can't do any of the choices so don't get a gold
      if(options.size()==0) return;

      String input=game.optionPane(ap, new OptionData(options.toArray(new String[options.size()])));
      if(input.equals("Discard 6 cards")){
        game.doWork(Dominion.Phase.DISCARD, 6, 6, ap);
      }else if(input.equals("Discard Attack")){
        game.doWork(Dominion.Phase.DISCARD, 1, 1, ap, c -> c.isAttack);
      }else{
        game.doWork(Dominion.Phase.DISCARD, 2, 2, ap, c -> c.getName().equals("curse"));
      }
      game.gainCard("gold", ap, Dominion.GainTo.DISCARD, true);
      game.selectedCards.clear();
    }
  }
  class Save extends DominionCard{
    public Save(){
      super("save");
      cost=1;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.buys++;
      game.updateSharedFields();
      if(!saveSwitch){
        game.doWork(Dominion.Phase.SELECT, 1, 1, ap);
        if(game.selectedCards.size()>0){
          game.players.get(ap).saveCard=game.selectedCards.get(0);
          game.selectedCards.clear();
        }
      }
      saveSwitch=true;
    }
  }
  class Scoutingparty extends DominionCard{
    public Scoutingparty(){
      super("scoutingparty");
      isEvent=true;
      cost=2;
    }
    @Override
    public void onGain(int ap){
      DominionPlayer player=game.players.get(ap);
      List<DominionCard> cards=player.draw(5);
      OptionData o;
      for(int i=0; i<Math.min(3, cards.size()); i++){
        o=new OptionData();
        o.add("Choose cards to discard: ", "text");
        for(DominionCard card : cards){
          o.add(card.getImage(), "imagebutton");
        }
        String input=game.optionPane(ap, o);
        player.disc.put(Dominion.remove(cards, c -> c.getName().equals(input)));
      }
      game.putBack(ap, cards);
      game.buys++;
      game.updateSharedFields();
    }
  }
  class Travellingfair extends DominionCard{
    public Travellingfair(){
      super("travellingfair");
      cost=2;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.buys+=2;
      game.updateSharedFields();
      travellingFairSwitch=true;
    }
  }
  class Bonfire extends DominionCard{
    public Bonfire(){
      super("bonfire");
      isEvent=true;
      cost=3;
    }
    @Override
    public void onGain(int ap){
      OptionData o;
      for(int i=0; i<Math.min(2, game.matcards.size()); i++){
        o=new OptionData();
        o.add("Choose cards to trash: ", "text");
        for(DominionCard card : game.matcards){
          o.add(card.getImage(), "imagebutton");
        }
        String input=game.optionPane(ap, o);
        game.trashCard(Dominion.remove(game.matcards, c -> c.getName().equals(input)), ap);
      }
    }
  }
  class Expedition extends DominionCard{
    public Expedition(){
      super("expedition");
      cost=3;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.players.get(ap).expedition=true;
    }
  }
  class Ferry extends DominionCard{
    public Ferry(){
      super("ferry");
      cost=3;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.placeToken(ap, "ferry", false);
    }
  }
  class Plan extends DominionCard{
    public Plan(){
      super("plan");
      cost=3;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.placeToken(ap, "trash", false);
    }
  }
  class Mission extends DominionCard{
    public Mission(){
      super("mission");
      cost=4;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      //missionSwitch1 says that the next turn will be a mission turn
      //missionSwitch2 says that the current turn is a mission turn
      if(!missionSwitch2)
        missionSwitch1=true;
    }
  }
  class Pilgrimage extends DominionCard{
    public Pilgrimage(){
      super("pilgrimage");
      cost=4;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      DominionCard card2;
      if(!pilgrimageSwitch){
        if(game.players.get(ap).journey){
          LinkedList<DominionCard> cards=new LinkedList<>(game.matcards);
          OptionData o;
          for(int i=0; i<3; i++){
            if(cards.size()==0) break;
            o=new OptionData();
            o.add("Choose a card to gain a copy of", "text");
            for(DominionCard card : cards){
              o.add(card.getImage(), "imagebutton");
            }
            String input=game.optionPane(ap, o);
            card2=Dominion.remove(cards, c -> c.getName().equals(input));
            game.gainCard(card2.getName(), ap, Dominion.GainTo.DISCARD, true);
          }
        }
        game.players.get(ap).journey=!game.players.get(ap).journey;
      }
    }
  }
  class Ball extends DominionCard{
    public Ball(){
      super("ball");
      cost=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.players.get(ap).minusMoneyToken=true;
      for(int i=0; i<2; i++) game.gainNumber(ap, 4);
    }
  }
  class Raid extends DominionCard{
    public Raid(){
      super("raid");
      cost=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      int x=Collections.frequency(game.matcards, game.cardFactory("silver"));
      for(int i=0; i<x; i++) game.gainCard("silver", ap, Dominion.GainTo.DISCARD, true);
    }
  }
  class Seaway extends DominionCard{
    public Seaway(){
      super("seaway");
      cost=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      DominionCard card=game.gainNumber(ap, 4);
      game.players.get(ap).adventureTokens.put("buy", game.cardToSupply(card));
      game.displayPlayer(ap);
    }
  }
  class Trade extends DominionCard{
    public Trade(){
      super("trade");
      cost=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.doWork(Dominion.Phase.TRASH, 0, 2, ap);
      for(int i=0; i<game.selectedCards.size(); i++){
        game.gainCard("silver", ap, Dominion.GainTo.DISCARD, true);
      }
      game.selectedCards.clear();
    }
  }
  class Lostarts extends DominionCard{
    public Lostarts(){
      super("lostarts");
      cost=6;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.placeToken(ap, "action", false);
    }
  }
  class Training extends DominionCard{
    public Training(){
      super("training");
      cost=6;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.placeToken(ap, "money", false);
    }
  }
  class Pathfinding extends DominionCard{
    public Pathfinding(){
      super("pathfinding");
      cost=8;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.placeToken(ap, "card", false);
    }
  }
}
