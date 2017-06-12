
import java.util.*;

public class DarkAges extends Expansion{
  public static String [] shelterNames={"hovel", "necropolis" , "overgrownestate"};
  static boolean hermitSwitch=true;
  static boolean urchinSwitch=false;
  static String [] ruinNames={"ruinedvillage","ruinedlibrary","ruinedmarket","abandonedmine","survivors"};
  static String [] knightNames={"dameanna", "damejosephine", "damemolly", "damenatalie", "damesylvia",
    "sirbailey", "sirdestry", "sirmartin", "sirmichael", "sirvander"};

  public DarkAges(Dominion g){
    super(g);
    cards=darkAgesCards;
  }
  //***SHELTERS***///
  class Hovel extends DominionCard{
    public Hovel(){
      super("hovel");
      cost=1;
      isReactionX=true;
      isShelter=true;
    }
  }
  class Necropolis extends DominionCard{
    public Necropolis(){
      super("necropolis");
      cost=1;
      isShelter=true;
      actions=2;
      isAction=true;
    }
  }
  class Overgrownestate extends DominionCard{
    public Overgrownestate(){
      super("overgrownestate");
      cost=1;
      isVictory=true;
      isShelter=true;
    }
    @Override
    public void onTrash(int ap){
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
    }
  }
  //***RUINS***///
  class Abandonedmine extends DominionCard{
    public Abandonedmine(){
      super("abandonedmine");
      cost=0;
      value=1;
      isAction=true;
      isRuins=true;
    }
  }
  class Ruinedlibrary extends DominionCard{
    public Ruinedlibrary(){
      super("ruinedlibrary");
      cost=0;
      cards=1;
      isAction=true;
      isRuins=true;
    }
  }
  class Ruinedmarket extends DominionCard{
    public Ruinedmarket(){
      super("ruinedmarket");
      cost=0;
      buys=1;
      isAction=true;
      isRuins=true;
    }
  }
  class Ruinedvillage extends DominionCard{
    public Ruinedvillage(){
      super("ruinedvillage");
      cost=0;
      actions=1;
      isAction=true;
      isRuins=true;
    }
  }
  class Survivors extends DominionCard{
    public Survivors(){
      super("survivors");
      cost=0;
      isAction=true;
      isRuins=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(2);
      OptionData o=new OptionData();
      for(DominionCard card : cards){
        o.add(card.getImage(), "image");
      }
      o.add("Discard", "textbutton");
      o.add("Put Back", "textbutton");
      String input=game.optionPane(ap, o);
      if(input.equals("Discard")) player.disc.put(cards);
      else player.deck.put(cards);
      game.displayPlayer(ap);
    }
  }
  //**SPOILS**//
  class Spoils extends DominionCard{
    public Spoils(){
      super("spoils");
      isMoney=true;
      value=3;
    }
    @Override
    public void work(int ap){
      lostTrack=true;
      game.matcards.remove(this);
    }
  }
  //***KINGDOM CARDS***///
  class Poorhouse extends DominionCard{
    public Poorhouse(){
      super("poorhouse");
      cost=1;
      isAction=true;
      value=4;
    }
    @Override
    public void work(int ap){
      int total=0;
      for(DominionCard card : game.players.get(ap).hand){
        if(card.isMoney) total++;
      }
      game.money-=Math.min(total, 4);
      game.updateSharedFields();
    }
  }
  class Beggar extends DominionCard{
    public Beggar(){
      super("beggar");
      cost=2;
      isAction=true;
      isReaction1=true;
    }
    @Override
    public void work(int ap){
      for(int i=0; i<3; i++) game.gainCard("copper", ap, "hand", true);
    }
  }
  class Squire extends DominionCard {
    public Squire() {
      super("squire");
      cost = 2;
      value=1;
      isAction = true;
    }

    @Override
    public void work(int ap) {
      String[] options = {"+2 Actions", "+2 Buys", "Gain Silver"};
      String input = game.optionPane(ap, new OptionData(options));
      if (input.equals(options[0])) {
        game.actions += 2;
        game.updateSharedFields();
      } else if (input.equals(options[1])) {
        game.buys += 2;
        game.updateSharedFields();
      } else {
        game.gainCard("silver", ap);
      }
    }

    @Override
    public void onTrash(int ap) {
      game.server.displayComment(ap, "Gain an attack");
      game.gainSpecial(ap, c -> c.isAttack);
    }
  }
  class Vagrant extends DominionCard{
    public Vagrant(){
      super("vagrant");
      actions=1;
      cards=1;
      isAction=true;
      cost=2;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      try {
        DominionCard card = player.getCard();
        if(card.isVictory || card.getName().equals("curse") || card.isShelter || card.isRuins) {
          player.hand.add(card);
          game.displayPlayer(ap);
        }else
          player.deck.put(card);
      }catch(OutOfCardsException ex){}
    }
  }
  class Forager extends RegularCard{
    public Forager(){
      super("forager");
      actions=1;
      buys=1;
      cost=3;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap);
      HashSet<DominionCard> cards=new HashSet<>();
      for(DominionCard card : game.trash){
        if(card.isMoney) cards.add(card);
      }
      game.money+=cards.size();
      game.updateSharedFields();
    }
  }
  class Hermit extends RegularCard{
    public Hermit(){
      super("hermit");
      cost=3;
    }
    @Override
    public void subWork(int ap){
      String [] options={"Trash from Discard Pile", "Trash from Hand", "Don't trash"};
      String input=game.optionPane(ap, new OptionData(options));

      if(input.equals(options[0])){
        OptionData o=new OptionData();
        Deck<DominionCard> deck=game.players.get(ap).disc;
        for(DominionCard card : deck){
          if(!card.isMoney) o.add(card.getImage(), "imagebutton");
        }
        o.add("Done", "textbutton");
        input=game.optionPane(ap, o);
        DominionCard card2;
        for(ListIterator<DominionCard> it=deck.listIterator(); it.hasNext(); ){
          card2=it.next();
          if(card2.getName().equals(input)){
            it.remove();
            game.trashCard(card2, ap);
            break;
          }
        }
      }else if(input.equals(options[1])){
        game.doWork("trash",0,1,ap, c -> !c.isMoney);
        game.selectedCards.clear();
      }

      //gain a card costuing up to 3
      game.gainNumber(ap, 3);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      if(hermitSwitch) {
        game.trashCard(this, ap);
        game.gainCardNoSupply(game.cardFactory("madman", "DarkAges"), ap, "discard");
      }
      return false;
    }
  }
  class Madman extends DominionCard{
    public Madman(){
      super("madman");
      actions=2;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      player.drawToHand(player.hand.size());
      game.displayPlayer(ap);
      //"return to the madman pile" means make it disappear
      game.matcards.remove(this);

    }
  }
  class Marketsquare extends DominionCard{
    public Marketsquare(){
      super("marketsquare");
      cards=1;
      actions=1;
      buys=1;
      cost=3;
      isReactionX=true;
      isAction=true;
    }
  }
  class Sage extends DominionCard{
    public Sage(){
      super("sage");
      cost=3;
      isAction=true;
      actions=1;
    }
    @Override
    public void work(int ap){
      DominionCard card;
      DominionPlayer player=game.players.get(ap);
      while(true){
        try {
          card = player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        if(game.cost2(card)>=3){
          player.hand.add(card);
          break;
        }else{
          player.disc.put(card);
        }
      }
      game.displayPlayer(ap);
    }
  }
  class Storeroom extends RegularCard{
    public Storeroom(){
      super("storeroom");
      cost=3;
      buys=1;
    }
    @Override
    public void subWork(int ap){
      game.server.displayComment(ap, "Discard cards to draw cards");
      game.doWork("discard",0,100, ap);
      game.players.get(ap).drawToHand(game.selectedCards.size());
      game.displayPlayer(ap);
      game.selectedCards.clear();
      game.server.displayComment(ap, "Discard cards for money");
      game.doWork("discard", 0, 100, ap);
      game.money+=game.selectedCards.size();
      game.updateSharedFields();
    }
  }
  class Urchin extends Attack{
    public Urchin(){
      super("urchin");
      cost=3;
      cards=1;
      actions=1;
    }
    @Override
    public void subStep(int ap, int atk){
      int x=game.players.get(ap).hand.size()-4;
      if(x>0) game.doWork("discard", x, x, ap);
    }
  }
  class Mercenary extends Attack {
    private boolean trashed;

    public Mercenary() {
      super("mercenary");
    }

    @Override
    public void subWork(int ap) {
      trashed = false;
      String[] options = {"Trash 2 Cards", "Done"};
      String input = game.optionPane(ap, new OptionData(options));
      if (input.equals(options[0])) {
        game.doWork("trash", 2, 2, ap);
        game.players.get(ap).drawToHand(2);
        game.displayPlayer(ap);
        game.money += 2;
        game.updateSharedFields();
        trashed = true;
      }
      game.selectedCards.clear();
    }

    @Override
    public void subStep(int ap, int atk) {
      if (trashed) {
        int x = game.players.get(ap).hand.size() - 3;
        System.out.println("mercenary: "+x);
        if (x > 0) game.doWork("discard", x, x, ap);
      }
    }
  }
  class Armory extends RegularCard{
    public Armory(){
      super("armory");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      Dominion.SupplyDeck deck;
      game.server.displayComment(ap, "gain a card costing up to 4");
      while(true) {
        game.doWork("selectDeck", 1, 1, ap);
        deck=game.supplyDecks.get(game.selectedDeck);
        if(deck.getCost()<=4 && deck.size()>0){
          game.gainCard(game.selectedDeck, ap, "topcard", true);
          break;
        }
      }
    }
  }
  class Deathcart extends RegularCard{
    public Deathcart(){
      super("deathcart");
      cost=4;
      value=5;
      isLooter=true;
    }
    @Override
    public void subWork(int ap){
      lostTrack=true;
      game.doWork("trash", 0, 1, ap,  c -> c.isAction);
      if(game.selectedCards.size()==0){
        game.matcards.remove(this);
        game.trashCard(this, ap);
      }
    }
    @Override
    public void onGain(int ap){
      for(int i=0; i<2; i++) game.gainCard("ruins", ap, "discard", true);
    }
  }
  class Feodum extends DominionCard{
    public Feodum(){
      super("feodum");
      cost=4;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      return Collections.frequency(cards, game.cardFactory("silver"))/3;
    }
    @Override
    public void onTrash(int ap){
      for(int i=0;i<3; i++) game.gainCard("silver", ap, "discard", true);
    }
  }
  class Fortress extends DominionCard{
    public Fortress(){
      super("fortress");
      cost=4;
      actions=2;
      cards=1;
      isAction=true;
    }
    @Override
    public void onTrash(int ap){
      game.trash.remove(this);
      game.players.get(ap).hand.add(this);
      game.displayPlayer(ap);
    }
  }
  class Ironmonger extends DominionCard{
    public Ironmonger(){
      super("ironmonger");
      cost=4;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      try {
        DominionCard card=player.getCard();
        String [] options={"Discard", "Put Back"};
        OptionData o=new OptionData(options);
        o.add(card.getImage(), "image");
        String input=game.optionPane(ap, o);
        if(input.equals(options[0])){
          player.disc.put(card);
        }else{
          player.deck.put(card);
        }
        if(card.isAction) game.actions++;
        if(card.isVictory){
          player.drawToHand(1);
          game.displayPlayer(ap);
        }
        if(card.isMoney) game.money++;
        game.updateSharedFields();
      } catch (OutOfCardsException e) {
      }
    }
  }
  class Marauder extends Attack{
    public Marauder(){
      super("marauder");
      cost=4;
      isLooter=true;
    }
    @Override
    public void subWork(int ap){
      game.gainCardNoSupply(game.cardFactory("spoils", "DarkAges"), ap, "discard");
    }
    @Override
    public void subStep(int ap, int atk){
      game.gainCard("ruins", ap);
    }
  }
  class Procession extends RegularCard{
    public Procession(){
      super("procession");
      cost=4;
    }
    @Override
    public void subWork(int ap){

      //this first part is basically throneroom
      game.server.displayComment(ap,"Choose a card to play twice");
      game.doWork("select",1,1,ap, c -> c.isAction);
      game.mask.clear();
      if(game.selectedCards.size()==0) return;
      DominionCard card=game.selectedCards.get(0);

      game.selectedCards.clear();
      game.changePhase("actions");
      game.server.displayComment(ap,"");

      //this card will never to go the mat
      game.playCard(card,ap,true);
      if(!card.lostTrack) game.playCard(card,ap,true);

      game.cardPlayed(ap);

      //now try to gain a card costing one more than this card
      if(!card.lostTrack) {
        game.trashCard(card, ap);
        int val = game.cost2(card) + 1;
        game.server.displayComment(ap, "Gain an action costing exactly " + val);
        game.gainSpecial(ap, c -> game.costCompare(c, card, 1)==0  && c.isAction );
      }
    }//subWork
  }
  class Rats extends RegularCard{
    public Rats(){
      super("rats");
      cost=4;
      actions=1;
      cards=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap, c -> !c.getName().equals("rats"));
    }
    @Override
    public void onTrash(int ap){
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
    }
  }
  class Scavenger extends DominionCard{
    public Scavenger(){
      super("scavenger");
      cost=4;
      value=2;
      isAction=true;
    }
    @Override
    public void work(int ap){
      String [] options={"Discard Deck", "Done"};
      DominionPlayer player=game.players.get(ap);
      DominionCard card;

      if(game.optionPane(ap, new OptionData(options)).equals(options[0])){
        player.disc.put(player.deck);
        player.deck.clear();
      }

      if(player.disc.size()>0){
        OptionData o=new OptionData(new String[0]);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          o.add(card.getName(),"imagebutton");
        }
        String input=game.optionPane(ap,o);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.deck.put(card);
            game.displayPlayer(ap);
            break;
          }
        }

      }//if discard>0
    }
  }
  class Wanderingminstrel extends DominionCard{
    public Wanderingminstrel(){
      super("wanderingminstrel");
      cost=4;
      actions=2;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(3);
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(!card.isAction){
          it.remove();
          player.disc.put(card);
        }
      }
      game.putBack(ap, cards);
    }
  }
  class Bandofmisfits extends CopyCard{
    DominionCard card=null;
    public Bandofmisfits(){
      super("bandofmisfits");
      cost=5;
    }
    @Override
    protected int getLimit(){
      return game.cost2(this)-1;
    }
  }
  class Banditcamp extends DominionCard{
    public Banditcamp(){
      super("banditcamp");
      cost=5;
      actions=2;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.gainCardNoSupply(game.cardFactory("spoils", "DarkAges"), ap, "discard");
    }
  }
  class Catacombs extends DominionCard{
    public Catacombs(){
      super("catacombs");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(3);
      String [] options={"Discard", "Put Back"};
      OptionData o=new OptionData(options);
      for(DominionCard card : cards){
        o.add(card.getImage(), "image");
      }
      String input=game.optionPane(ap, o);
      if(input.equals(options[0])){
        player.disc.put(cards);
      }else{
        game.putBack(ap, cards);
      }
      player.drawToHand(3);
      game.displayPlayer(ap);
    }
    @Override
    public void onTrash(int ap){
      game.gainSpecial(ap, c -> game.costCompare(c, this)<0);
    }
  }
  class Count extends RegularCard{
    public Count(){
      super("count");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      String [] options1={"Discard 2 Cards", "Put Card on Deck", "Gain Copper"};
      String [] options2={"+3 Money", "Trash Hand", "Gain Duchy"};

      String input=game.optionPane(ap, new OptionData(options1));
      if(input.equals(options1[0])){
        game.doWork("discard", 2, 2, ap);
      }else if(input.equals(options1[1])){
        game.doWork("topdeck", 1, 1, ap);
      }else{
        game.gainCard("copper", ap);
      }
      input=game.optionPane(ap, new OptionData(options2));
      if(input.equals(options2[0])){
        game.money+=3;
        game.updateSharedFields();
      }else if(input.equals(options2[1])){
        LinkedList<DominionCard> hand=game.players.get(ap).hand;
        for(DominionCard card : hand){
          game.trashCard(card, ap);
        }
        hand.clear();
        game.displayPlayer(ap);
      }else{
        game.gainCard("duchy", ap);
      }
      game.changePhase("actions");
    }
  }
  class Counterfeit extends DominionCard{
    public Counterfeit(){
      super("counterfeit");
      cost=5;
      value=1;
      buys=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      game.doWork("select", 0, 1, ap, c -> c.isMoney);
      game.mask.clear();
      game.changePhase("buys");
      DominionCard card=game.selectedCards.get(0);
      game.selectedCards.clear();

      game.playCard(card, ap, true);
      if(!card.lostTrack) game.playCard(card, ap, true);
      game.trashCard(card, ap);
    }
  }
  class Cultist extends Attack{
    public Cultist(){
      super("cultist");
      cost=5;
      cards=2;
      isLooter=true;
    }
    @Override
    public void subWork(int ap){

      game.doWork("select", 0, 1, ap, c -> c.equals(this));
      if(game.selectedCards.size()==0) return;
      DominionCard card=game.selectedCards.get(0);
      game.selectedCards.clear();
      game.playCard(card, ap);
    }
    @Override
    public void onTrash(int ap){
      game.players.get(ap).drawToHand(3);
      game.displayPlayer(ap);
    }
    @Override
    public void subStep(int ap, int atk){
      game.gainCard("ruins", ap);
    }
  }
  class Graverobber extends DominionCard{
    public Graverobber(){
      super("graverobber");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int ap){
      String [] options={"Gain from trash", "Trash and Gain"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.gainFromTrash(ap, "topcard", c -> game.costCompare(c, 6, 0,0) <=0 && game.cost2(c)>=3 );
      }else{
        game.doWork("trash", 1, 1, ap, c -> c.isAction);
        if(game.selectedCards.size()>0){
          game.gainSpecial(ap, c -> game.costCompare(c, game.selectedCards.get(0), 3)<=0);
        }
        game.selectedCards.clear();
      }
    }
  }
  class Junkdealer extends RegularCard{
    public Junkdealer(){
      super("junkdealer");
      cost=5;
      cards=1;
      actions=1;
      value=1;
      isKnight=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap);
    }
  }
  //*****KNIGHTS***///
  class Knight extends Attack{
    boolean knightTrashed=false;
    public Knight(String s){
      super(s);
      cost=5;
    }
    @Override
    public void subStep(int ap, int atk){

      extra(ap);

      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(2);
      if(cards.size()==0) return;
      OptionData o=new OptionData();
      boolean foundCard=false;
      for(DominionCard card : cards){
        if(game.cost2(card)<=6 && game.cost2(card)>=3 && card.debt==0 && card.potions==0){
          o.add(card.getImage(), "imagebutton");
          foundCard=true;
        }else{
          o.add(card.getImage(), "image");
        }
      }
      if(!foundCard) o.add("Done", "textbutton");
      String input=game.optionPane(ap, o);

      DominionCard card2;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card2=it.next();
        if(card2.getImage().equals(input)){
          it.remove();
          game.trashCard(card2, ap);
          if(card2.isKnight) knightTrashed=true;
          break;
        }
      }
      game.putBack(ap, cards);
    }
    @Override
    public void cleanup(int ap){
      if(knightTrashed){
        game.matcards.remove(this);
        game.trashCard(this, ap);
      }
    }
    public void extra(int ap){}
  }
  class Dameanna extends Knight{
    public Dameanna(){
      super("dameanna");
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 0, 2, ap);
    }
  }
  class Damejosephine extends Knight{
    public Damejosephine(){
      super("damejosephine");
      isVictory=true;
      vicPoints=2;
    }
  }
  class Damemolly extends Knight{
    public Damemolly(){
      super("damemolly");
      actions=2;
    }
  }
  class Damenatalie extends Knight{
    public Damenatalie(){
      super("damenatalie");
    }
    @Override
    public void subWork(int ap){
      game.gainNumber(ap, 3);
    }
  }
  class Damesylvia extends Knight{
    public Damesylvia(){
      super("damesylvia");
      value=2;
    }
  }
  class Sirbailey extends Knight{
    public Sirbailey(){
      super("sirbailey");
      cards=1;
      actions=1;
    }
  }
  class Sirdestry extends Knight{
    public Sirdestry(){
      super("sirdestry");
      cards=2;
    }
  }
  class Sirmartin extends Knight{
    public Sirmartin(){
      super("sirmartin");
      buys=2;
      cost=4;
    }
  }
  class Sirmichael extends Knight{
    public Sirmichael(){
      super("sirmichael");
    }
    @Override
    public void extra(int ap){
      int x=game.players.get(ap).hand.size()-3;
      game.doWork("discard", x, x, ap);
    }
  }
  class Sirvander extends Knight{
    public Sirvander(){
      super("sirvander");
    }
    @Override
    public void onTrash(int ap){
      game.gainCard("gold", ap);
    }
  }

  class Mystic extends RegularCard{
    public Mystic(){
      super("mystic");
      cost=5;
      value=2;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork("selectDeck2", 1, 1, ap);
      DominionPlayer player=game.players.get(ap);
      try {
        DominionCard card = player.getCard();
        if(card.getName().equals(game.supplyDecks.get(game.selectedDeck).card.getName())){
          player.hand.add(card);
          game.displayPlayer(ap);
        }else{
          player.deck.add(card);
        }
      }catch(OutOfCardsException ex){}
    }
  }
  class Pillage extends Attack{
    public Pillage(){
      super("pillage");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.trashCard(this, ap);
      game.matcards.remove(this);
      for(int i=0; i<2; i++){
        game.gainCardNoSupply(game.cardFactory("spoils", "DarkAges"), ap, "discard");
      }
    }
    @Override
    public void subStep(int ap, int atk){
      LinkedList<DominionCard> hand=game.players.get(ap).hand;
      OptionData o=new OptionData();
      o.add("Choose a card to discard: ", "text");
      for(DominionCard card : hand){
        o.add(card.getImage(), "imagebutton");
      }
      String input=game.optionPane(atk, o);
      DominionCard card2;
      for(ListIterator<DominionCard> it=hand.listIterator(); it.hasNext(); ){
        card2=it.next();
        if(card2.getName().equals(input)){
          it.remove();
          game.players.get(ap).disc.put(card2);
          break;
        }
      }
    }
  }
  class Rebuild extends RegularCard{
    public Rebuild(){
      super("rebuild");
      cost=5;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.server.displayComment(ap, "choose a card to not trash");
      game.doWork("selectDeck2", 1, 1, ap);
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionCard card=null;
      DominionPlayer player=game.players.get(ap);
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          player.disc.put(cards);
          return;
        }
        if(card.isVictory && !card.getName().equals(game.supplyDecks.get(game.selectedDeck).card.getName())){
          break;
        }else{
          cards.add(card);
        }
      }
      player.disc.put(cards);
      game.trashCard(card, ap);
      int gainLimit=game.cost2(card)+3;
      game.server.displayComment(ap, "gain a victory costing up to "+gainLimit);
      game.gainSpecial(ap, c -> game.costCompare(c, gainLimit, 0, 0)<=0 && c.isVictory);
    }
  }
  class Rogue extends Attack{
    private boolean attack=false;
    public Rogue(){
      super("rogue");
      cost=5;
      value=2;
    }
    @Override
    public void subWork(int ap){
      attack=!game.cardInTrash( c -> game.cost2(c)>=3 && game.costCompare(c, 6, 0, 0)<=0);
      if(!attack)
        game.gainFromTrash(ap, "discard", c -> game.cost2(c)>=3 && game.costCompare(c, 6, 0, 0)<=0);
    }
    @Override
    public void subStep(int ap, int atk){
      if(!attack) return;

      DominionPlayer player=game.players.get(ap);
      ArrayList<DominionCard> cards=player.draw(2);
      if(cards.size()==0) return;
      OptionData o=new OptionData();
      OptionData o2=new OptionData();
      boolean foundCard=false;
      for(DominionCard card : cards){
        if(game.costCompare(card, 6, 0, 0)<=0 && game.cost2(card)>=3){
          o.add(card.getImage(), "imagebutton");
          foundCard=true;
        }else{
          o.add(card.getImage(), "image");
        }
        o2.add(card.getImage(), "image");
      }
      if(!foundCard) o.add("Done", "textbutton");
      o2.add("Done", "textbutton");
      String input=game.optionPane(ap, o);

      DominionCard card2;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card2=it.next();
        if(card2.getImage().equals(input)){
          it.remove();
          game.trashCard(card2, ap);
          break;
        }
      }
      game.optionPane(atk, o2);
      game.putBack(ap, cards);
    }
  }
  class Altar extends RegularCard{
    public Altar(){
      super("altar");
      cost=6;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()>0){
        game.gainNumber(ap, 5);
      }
    }
  }
  class Huntinggrounds extends DominionCard{
    public Huntinggrounds(){
      super("huntinggrounds");
      cost=6;
      cards=4;
      isAction=true;
    }
    @Override
    public void onTrash(int ap){
      String [] options={"Gain a Duchy", "Gain 3 estates"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.gainCard("duchy", ap);
      }else{
        for(int i=0; i<3; i++) game.gainCard("estate", ap);
      }
    }
  }
}
