import java.util.*;

class Empires extends Expansion {
  static boolean [] enchantressSwitch=null;
  //these are the names the game will display for the different gatherers
  HashMap<String, String> gathererNames;
  //these are the value the gatherer is at
  HashMap<String, Integer> gathererVals;

  static HashMap<String, String> splitPiles=new HashMap<>();

  static int charmCounter=0;

  private boolean fortuneSwitch=false;

  static int groundskeeperCounter=0;

  static String [] events={"triumph", "annex", "donate", "advance", "tax", "delve", "banquet",
      "ritual", "salttheearth", "wedding", "windfall", "conquest", "dominate"};
  static String [] landmarks={"aqueduct", "arena", "banditfort", "basilica", "baths", "battlefield",
    "colonnade", "defiledshrine", "fountain",  "keep", "labyrinth", "mountainpass", "museum",
    "obelisk", "orchard", "palace", "tomb", "tower", "triumphalarch", "wall", "wolfden"};

  static int triumphCounter=0;

  static boolean donateSwitch=false;

  static int conquestCounter=0;

  static String obeliskSupply="";

  private HashMap<String,Integer> aqueductMoney=new HashMap<>();

  private HashMap<String,Integer> defiledshrineTokens=new HashMap<>();

  private boolean mountainpassPlayed=false;
  static boolean mountainpassSwitch=false;

  public Empires(Dominion g) {
    super(g);
    String[] t = {"engineer", "cityquarter", "overlord", "royalblacksmith", "encampment",
      "patrician", "settlers", "castle", "catapult", "chariotrace", "enchantress", "gladiator",
            "farmersmarket", "sacrifice", "temple", "villa", "archive", "capital", "charm",
      "crown", "forum", "groundskeeper", "legionary", "wildhunt"};
    cards = t;


    enchantressSwitch = new boolean[game.players.size()];
    Arrays.fill(enchantressSwitch, false);

    //split piles
    splitPiles.put("encampment", "plunder");
    splitPiles.put("patrician", "emporium");
    splitPiles.put("settlers", "bustlingvillage");
    splitPiles.put("catapult", "rocks");
    splitPiles.put("gladiator", "fortune");

    setDefaults();

  }
  private void addGatherer(String cardName, String displayName, Integer i){
    gathererNames.put(cardName, displayName);
    gathererVals.put(displayName, i);
  }
  void setDefaults() {
    gathererNames = new HashMap<>();
    gathererVals = new HashMap<>();
    //all the gatherer cards
    addGatherer("farmersmarket", "Farmer's Market", 0);
    addGatherer("temple", "Temple", 0);
    addGatherer("wildhunt", "Wild Hunt", 0);
    addGatherer("aqueduct", "Aqueduct", 0);
    addGatherer("basilica", "Basilica", 6 * game.players.size());
    addGatherer("battlefield", "Battlefield", 6 * game.players.size());
    addGatherer("colonnade", "Colonnade", 6 * game.players.size());
    addGatherer("defiledshrine", "Defiled Shrine", 0);
    addGatherer("labyrinth", "Labyrinth", 6 * game.players.size());
    addGatherer("arena", "Arena", 6 * game.players.size());
    addGatherer("baths", "Baths", 6 * game.players.size());

    aqueductMoney.put("silver", 8);
    aqueductMoney.put("gold", 8);

    mountainpassPlayed=false;
    mountainpassSwitch=false;
  }
  void setDefiledshrineTokens(){
    defiledshrineTokens=new HashMap<>();
    for(Map.Entry<String, Dominion.SupplyDeck> e : game.supplyDecks.entrySet()){
      if(!e.getValue().card.isGathering && e.getValue().card.isAction)
        defiledshrineTokens.put(e.getKey(), 2);
    }

  }
  //a number of landmarks give extra points, this counts those points
  Pair<Integer, String> landmarkScore(Collection<DominionCard> cards,
                                             LinkedHashMap<String, Dominion.SupplyDeck> supplies){
    Pair<Integer, String> out=new Pair<>(0,"");
    int x;
    HashSet<DominionCard> cardSet;

    //banditfort
    if(supplies.containsKey("banditfort")){
      x=-2*(Collections.frequency(cards,game.cardFactory("silver"))
              +Collections.frequency(cards,game.cardFactory("gold")));
      updatePair(out,x,"Bandit fort");
    }
    //fountain
    if(supplies.containsKey("fountain")){
      if(Collections.frequency(cards,game.cardFactory("copper"))>=10) x=15;
      else x=0;
      updatePair(out, x, "Fountain");
    }
    //keep
    if(supplies.containsKey("keep")){
      x=0;
      int max;
      int temp;
      for(Map.Entry<String, Dominion.SupplyDeck> e : supplies.entrySet()){
        if(e.getValue().card.isMoney){
          max=0;
          for(DominionPlayer player : game.players){
            temp=Collections.frequency(player.deck, e.getValue().card);
            if(temp>=max) max=temp;
          }
          if(Collections.frequency(cards, e.getValue().card)==max) x+=5;
        }
      }
      updatePair(out, x, "Keep");
    }
    if(supplies.containsKey("museum")){
      cardSet=new HashSet<>(cards);
      updatePair(out, 2*cardSet.size(), "Museum");
    }
    if(supplies.containsKey("obelisk")){
      x=0;
      for(DominionCard card : cards){
        if(game.cardToSupply(card)!=null && game.cardToSupply(card).equals(obeliskSupply)) x+=2;
      }
      updatePair(out, x, "Obelisk");
    }
    if(supplies.containsKey("orchard")){
      x=0;
      cardSet=new HashSet<>();
      for(DominionCard card : cards){
        if(card.isAction && !cardSet.contains(card)){
          if(Collections.frequency(cards,card)>=3) x+=4;
          cardSet.add(card);
        }
      }
      updatePair(out, x, "Orchard");
    }
    if(supplies.containsKey("palace")){
      x=Math.min(Collections.frequency(cards, game.cardFactory("copper")),
              Collections.frequency(cards, game.cardFactory("silver")));
      x=Math.min(x,Collections.frequency(cards,game.cardFactory("gold")));
      System.out.println("palace details: ");
      System.out.println("copper: "+Collections.frequency(cards, game.cardFactory("copper")));
      System.out.println("silver: "+Collections.frequency(cards, game.cardFactory("silver")));
      System.out.println("gold: "+Collections.frequency(cards, game.cardFactory("gold")));
      updatePair(out, 3*x, "Palace");
    }
    if(supplies.containsKey("tower")){
      cardSet=new HashSet<>();
      for(Map.Entry<String, Dominion.SupplyDeck> e : supplies.entrySet()){
        if(e.getValue().size()==0 && !e.getValue().card.isVictory){
          cardSet.add(e.getValue().card);
        }
      }
      x=0;
      for(DominionCard card : cardSet){
        x+= Collections.frequency(cards, card);
      }
      updatePair(out, x, "Tower");
    }
    if(supplies.containsKey("triumphalarch")){
      cardSet=new HashSet<>(cards);
      int max=0,max2=0;
      for(DominionCard card : cardSet){
        if(!card.isAction) continue;
        x=Collections.frequency(cards, card);
        if(x>max){
          max2=max;
          max=x;
        }else if(x>max2){
          max2=x;
        }
      }
      updatePair(out,3*max2, "Triumphal Arch");
    }
    if(supplies.containsKey("wall")){
      if(cards.size()>15) updatePair(out, 15-cards.size(), "Wall");
    }
    if(supplies.containsKey("wolfden")){
      x=0;
      System.out.println("wolfden details: ");
      for(DominionCard card : cards){
        if(Collections.frequency(cards, card)==1){
          x+=1;
          System.out.println(card.getName());
        }
      }
      updatePair(out, -3*x, "Wolf Den");
    }
    return out;
  }
  private void updatePair(Pair<Integer, String > in, int x, String out){
    in.setA(Integer.sum(in.getA(),x));
    in.setB(in.getB()+out+": "+x+", ");
  }

  public void landmarkGain(DominionCard card, int ap){
    int x=0;
    String s;
    LinkedHashMap<String, Dominion.SupplyDeck> supplies=game.supplyDecks;
    if(supplies.containsKey("aqueduct")){
      s=card.getName();
      if(s.equals("silver") || s.equals("gold")){
        if(aqueductMoney.get(s)>0){
          stepGatherer("Aqueduct", 1);
          aqueductMoney.put(s, aqueductMoney.get(s)-1);
        }
      }else if(card.isVictory){
        x+=takeGatherer("Aqueduct");
      }
    }
    if(supplies.containsKey("basilica")){
      if(game.getPhase().equals("buys") && game.money>=2){
        x+=stepGatherer("Basilica", -2);
      }
    }
    if(supplies.containsKey("colonnade")){
      if(game.getPhase().equals("buys") && card.isAction && game.matcards.contains(card)){
        x+=stepGatherer("Colonnade", -2);
      }
    }
    if(supplies.containsKey("battlefield")){
      if(card.isVictory){
        x+=stepGatherer("Battlefield", -2);
      }
    }
    if(supplies.containsKey("defiledshrine")){
      s=card.getName();
      if(card.isAction && defiledshrineTokens.get(s)>0) {
        stepGatherer("Defiled Shrine", 1);
        defiledshrineTokens.put(s, defiledshrineTokens.get(s) - 1);
      }
      if(s.equals("curse")){
        x+=takeGatherer("Defiled Shrine");
      }
    }
    if(supplies.containsKey("labyrinth")){
      if(triumphCounter>=2){
        x+=2;
        stepGatherer("Labyrinth", -2);
      }
    }
    if(supplies.containsKey("mountainpass")){
      if(card.getName().equals("province") && !mountainpassPlayed){
        mountainpassPlayed=true;
        mountainpassSwitch=true;
      }
    }
    game.players.get(ap).vicTokens+=x;
    game.displayPlayer(ap);
    game.updateSharedFields();
  }

  //if x is positive, just increase x and return 0
  //if x is negative, reduce by x if possible or reduce to zero
  //return the amount you reduced by
  private int stepGatherer(String s, int x){
    int out=0;
    if(x<0 && gathererVals.get(s)<=-x){
      return takeGatherer(s);
    }else{
      out=-x;
    }
    gathererVals.put(s, gathererVals.get(s)+x);
    return out;
  }
  private int takeGatherer(String s){
    int out=gathererVals.get(s);
    gathererVals.put(s,0);
    return out;
  }
  //some annoying landmarks that don't activate at an ordinary time
  void arena(int ap){
    if(gathererVals.get("Arena")==0) return;

    game.server.displayComment(ap,
            "Discard an Action to take from the Arena");
    game.doWork("discard", 0, 1, ap, c -> c.isAction);
    if(game.selectedCards.size()>0){
      game.players.get(ap).vicTokens+=stepGatherer("Arena", -2);
      game.displayPlayer(ap);
      game.updateSharedFields();
      game.selectedCards.clear();
    }
    game.server.displayComment(ap, "");
  }
  void baths(int ap){
    if(DarkAges.hermitSwitch){
      game.players.get(ap).vicTokens+=stepGatherer("Baths", -2);
      game.displayPlayer(ap);
    }
  }
  void mountainPass(int ap){
    OptionData o;
    int j;
    int winner=ap;
    int price=-1;
    String input;
    for(int i=0; i<game.players.size(); i++){
      j=(ap+i+1)%game.players.size();
      o=new OptionData();
      for(int k=price+1; k<=40; k++){
        o.put(Integer.toString(k), "textbutton");
      }
      input=game.optionPane(j, o);
      if(!input.equals("Done")){
        price=Integer.parseInt(input);
        winner=j;
      }
    }
    game.players.get(winner).debt+=price;
    game.players.get(winner).vicTokens+=8;
    game.displayPlayer(winner);
    mountainpassSwitch=false;
  }
  //***FINALLY WE CAN START THE CARDS***///
  class Engineer extends RegularCard {
    public Engineer() {
      super("engineer");
      debt = 4;
    }

    @Override
    public void subWork(int ap) {
      game.gainNumber(ap, 4);
      String[] options = {"Trash this card", "Done"};
      String input = game.optionPane(ap, new OptionData(options));
      if (input.equals(options[0])) {
        game.trashCard(this, ap);
        game.matcards.remove(this);
        game.gainNumber(ap, 4);
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

  class Overlord extends CopyCard {


    public Overlord() {
      super("overlord");
      debt = 8;
    }
    @Override
    protected int getLimit(){ return 5; }

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
      boolean putBack=true;
      game.doWork("reveal", 0, 1, ap,c -> c.getName().equals("gold") || c.getName().equals("plunder"));
      if (game.selectedCards.size() > 0) putBack=false;
      if(putBack){
        game.returnToSupply(this, ap);
        game.matcards.remove(this);
        lostTrack=true;
      }
    }
  }

  class Plunder extends DominionCard {
    public Plunder() {
      super("plunder");
      cost = 5;
      value = 2;
      isMoney=true;
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
        o.add(card.getImage(), "image");
        o.add("Done", "textbutton");
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
      isAction=true;
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
            o.add(card.getImage(),"imagebutton");
          else
            o.add(card.getImage(), "image");
        }
        o.add("Done", "textbutton");
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
      isAction=true;
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
            o.add(card.getImage(),"imagebutton");
          else
            o.add(card.getImage(), "image");
        }
        o.add("Done", "textbutton");
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
      String [] options={"Trash this"};
      OptionData o=new OptionData(options);
      for(DominionCard card : game.players.get(ap).hand) {
        if (card.isCastle) {
          o.put("Trash castle from hand", "textbutton");
          break;
        }
      }
      String input=game.optionPane(ap, o);
      if(input.equals(options[0])){
        game.matcards.remove(this);
        game.trashCard(this, ap);
        game.gainCard("castle", ap);
      }else{
        game.doWork("trash", 1, 1, ap, c -> c.isCastle);
        if(game.selectedCards.size()>0) game.gainCard("castle", ap);
      }
      game.selectedCards.clear();
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
      String oldPhase=game.getPhase();
      game.gainCard("gold", ap, "discard", true);
      //game.mask.clear();
      int oldPlayer=ap;
      for(int i=(ap+1)%game.players.size(); i!=ap; i=(i+1)%game.players.size()){
        if(game.players.get(i).hand.size()>=5){
          game.changePlayer(oldPlayer,i);
          game.doWork("topdeck", 2, 2, i);
        }
        game.selectedCards.clear();
        oldPlayer=i;
      }//player loop
      game.changePlayer(oldPlayer, ap);
      game.changePhase(oldPhase);
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
      game.doWork("discard", 0, 100, ap, c -> c.isVictory);
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
      comment="discard down to 3";
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap);
      if(game.selectedCards.size()>0) card=game.selectedCards.get(0);
      else card=null;
      game.selectedCards.clear();
    }
    @Override
    public void subStep(int ap, int atk){
      if(card==null) return;
      if(game.cost2(card)>=3) game.gainCard("curse", ap);
      if(card.isMoney){
        int x=game.players.get(ap).hand.size()-3;
        if(x>0) game.doWork("discard", x, x, ap);
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
      isAction=true;
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
      o.add(card1.getImage(), "image");
      o.add(card2.getImage(), "image");
      o.add("Done", "textbutton");
      game.optionPane(ap, o);
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
    private String s="Farmer's Market";
    public Farmersmarket(){
      super("farmersmarket");
      cost=3;
      buys=1;
      isAction=true;
      isGathering=true;
    }
    @Override
    public void work(int ap){
      int x=gathererVals.get(s);
      if(x>=4){
        game.players.get(ap).vicTokens+=x;
        gathererVals.put(s, 0);
        game.matcards.remove(this);
        game.trashCard(this, ap);
        game.displayPlayer(ap);
        game.updateSharedFields();
      }else{
        gathererVals.put(s,x+1);
        game.money+=gathererVals.get(s);
        game.updateSharedFields();
      }
    }
  }
  class Gladiator extends RegularCard{
    public Gladiator(){
      super("gladiator");
      cost=3;
      value=2;
    }
    @Override
    public void subWork(int ap){
      game.doWork("reveal", 1, 1, ap);
      if(game.selectedCards.size()==0) return;
      DominionCard card1=game.selectedCards.get(0);
      game.selectedCards.clear();

      game.doWork("reveal", 1, 1, (ap+1)%game.players.size(), c -> c.equals(card1));
      if(game.selectedCards.size()==0 || game.cost2(card1)>game.cost2(game.selectedCards.get(0))) {
        if(game.supplyDecks.get("gladiator").size()>5) game.trashCard(game.supplyDecks.get("gladiator").topCard(), ap);
        game.money++;
        game.updateSharedFields();
      }
      game.displaySupply("gladiator");
    }
  }
  class Fortune extends DominionCard{
    public Fortune(){
      super("fortune");
      cost=8;
      debt=8;
      isMoney=true;
      buys=1;
    }
    @Override
    public void work(int ap){
      if(!fortuneSwitch){
        game.money*=2;
        game.updateSharedFields();
        fortuneSwitch=true;
      }
    }
    @Override
    public void onGain(int ap){
      for(int i=0; i<Collections.frequency(game.matcards, game.cardFactory("gladiator")); i++){
        game.gainCard("gold", ap, "discard", true);
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      fortuneSwitch=false;
      return false;
    }
  }
  class Sacrifice extends RegularCard{
    public Sacrifice(){
      super("sacrifice");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash", 1, 1, ap);
      if(game.selectedCards.size()==0) return;

      DominionCard card=game.selectedCards.get(0);
      if(card.isAction){
        game.actions+=2;
        game.players.get(ap).drawToHand(2);
        game.displayPlayer(ap);
        game.updateSharedFields();
      }
      if(card.isMoney){
        game.money+=2;
        game.updateSharedFields();
      }
      if(card.isVictory){
        game.players.get(ap).vicTokens+=2;
        game.displayPlayer(ap);
      }
    }
  }
  class Temple extends RegularCard{
    String s="Temple";
    public Temple(){
      super("temple");
      cost=4;
      isGathering=true;
    }
    @Override
    public void subWork(int ap){
      game.players.get(ap).vicTokens+=1;
      HashSet<DominionCard> cards=new HashSet<>();
      game.doWork("trash", 1, 1, ap);
      for(int i=0; i<2; i++){
        cards.addAll(game.selectedCards);
        game.selectedCards.clear();
        game.doWork("trash", 0, 1, ap, c -> !cards.contains(c));
        if(game.selectedCards.size()==0) break;
      }
      gathererVals.put(s, gathererVals.get(s)+1);
    }
    @Override
    public void onGain(int ap){
      game.players.get(ap).vicTokens+=gathererVals.get(s);
      gathererVals.put(s, 0);
    }
  }
  class Villa extends DominionCard{
    public Villa(){
      super("villa");
      cost=4;
      actions=2;
      buys=1;
      value=1;
      isAction=true;
    }
    @Override
    public void onGain(int ap){
      game.actions++;
      game.updateSharedFields();
      game.changePhase("actions");
    }
  }
  class Archive extends DominionCard{
    LinkedList<DominionCard> cards=new LinkedList<>();
    public Archive(){
      super("archive");
      cost=5;
      actions=1;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void work(int ap){
      cards=game.players.get(ap).draw(3);
      pickup(ap);
    }
    @Override
    public void duration(int ap){
      pickup(ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      if(cards.size()>0){
        player.duration.add(this);
        return true;
      }else{
        return false;
      }
    }
    private void pickup(int ap){
      if(cards.size()==0) return;

      OptionData o=new OptionData();
      for(DominionCard card : cards){
        System.out.println(card.getImage());
        o.add(card.getImage(), "imagebutton");
      }
      String input=game.optionPane(ap, o);

      DominionCard card2;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card2=it.next();
        if(card2.getImage().equals(input)){
          it.remove();
          game.players.get(ap).hand.add(card2);
          break;
        }
      }
      game.displayPlayer(ap);
    }
  }
  class Capital extends DominionCard{
    public Capital(){
      super("capital");
      cost=5;
      value=6;
      buys=1;
      isMoney=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      player.debt+=6;
      if(game.money<player.debt){
        player.debt-=game.money;
        game.money=0;
      }else{
        player.debt=0;
      }
      return false;
    }
  }
  class Charm extends DominionCard{
    public Charm(){
      super("charm");
      cost=5;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      String [] options={"+2 Money +1 Buy", "Gain a Card with same cost"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.money+=2;
        game.buys+=1;
        game.updateSharedFields();
      }else{
        charmCounter++;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      charmCounter=0;
      return false;
    }
  }
  class Crown extends DominionCard{
    public Crown(){
      super("crown");
      cost=5;
      isMoney=true;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.server.displayComment(ap,"Choose a card to play twice");
      Collection<DominionCard> hand=game.players.get(ap).hand;

      String oldPhase=game.getPhase();
      game.doWork("select",1,1,ap,
              c -> c.isMoney && oldPhase.equals("buys") || c.isAction && oldPhase.equals("actions"));
      if(game.selectedCards.size()==0) return;
      game.mask.clear();
      DominionCard card=game.selectedCards.get(0);

      game.selectedCards.clear();
      game.changePhase(oldPhase);
      game.server.displayComment(ap,"");

      game.playCard(card,ap,false);
      if(!card.lostTrack) game.playCard(card,ap,true);

      //if you throne room a duration card, send this to the duration mat also
      if(card.isDuration) isDuration=true;
      card.throneroomed++;

      game.cardPlayed(ap);
      game.changePhase(oldPhase);
      game.server.displayComment(ap, "");
    }
  }
  class Forum extends RegularCard{
    public Forum(){
      super("forum");
      cost=5;
      actions=1;
      cards=3;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard", 2, 2, ap);
    }
    @Override
    public void onGain(int ap){
      game.buys++;
      game.updateSharedFields();
    }
  }
  class Groundskeeper extends DominionCard{
    private boolean inPlay=false;
    public Groundskeeper(){
      super("groundskeeper");
      cost=5;
      actions=1;
      isAction=true;
      cards=1;
    }
    @Override
    public void work(int ap){
      if(!inPlay){
        groundskeeperCounter++;
        inPlay=true;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      inPlay=false;
      groundskeeperCounter=0;
      return false;
    }
  }
  class Legionary extends Attack{
    private boolean active=false;
    public Legionary(){
      super("legionary");
      cost=5;
      value=3;
    }
    @Override
    public void subWork(int ap){
      active=false;
      game.doWork("reveal", 0, 1, ap, c -> c.getName().equals("gold"));
      if(game.selectedCards.size()>0){
        active=true;
      }
      game.selectedCards.clear();
    }
    @Override
    public void subStep(int ap, int atk){
      if(active) {
        int x = game.players.get(ap).hand.size() - 2;
        game.doWork("discard", x, x, ap);
        game.players.get(ap).drawToHand(1);
        game.displayPlayer(ap);
      }
    }
  }
  class Wildhunt extends DominionCard{
    private String s="Wild Hunt";
    public Wildhunt(){
      super("wildhunt");
      cost=5;
      isGathering=true;
      isAction=true;
    }
    @Override
    public void work(int ap){
      String [] options={"+3 Cards", "Take tokens and estate"};
      String input=game.optionPane(ap, new OptionData(options));
      DominionPlayer player=game.players.get(ap);
      if(input.equals(options[0])){
        player.drawToHand(3);
        gathererVals.put(s,gathererVals.get(s)+1);
      }else{
        player.vicTokens+=gathererVals.get(s);
        gathererVals.put(s,0);
      }
      game.displayPlayer(ap);
      game.updateSharedFields();
    }
  }
  class Triumph extends DominionCard{
    public Triumph(){
      super("triumph");
      debt=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      String oldPhase=game.getPhase();
      //change the phase so the estate doesn't trigger hovel
      game.changePhase("gain");
      if(game.gainCard("estate", ap, "discard", true)){
        game.players.get(ap).vicTokens+=triumphCounter;
        game.displayPlayer(ap);
      }
      game.changePhase(oldPhase);
    }
  }
  class Annex extends DominionCard{
    public Annex(){
      super("annex");
      debt=8;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      DominionPlayer player=game.players.get(ap);
      OptionData o;
      String input;
      DominionCard card2;
      while(player.disc.size()>5){
        o=new OptionData();
        for(DominionCard card : player.disc){
          o.put(card.getImage(), "imagebutton");
        }
        input=game.optionPane(ap, o);
        for(ListIterator<DominionCard> it=player.disc.listIterator(); it.hasNext(); ){
          card2=it.next();
          if(card2.getName().equals(input)){
            it.remove();
            player.deck.put(card2);
          }
        }
      }
      player.deck.shuffle();
      game.displayPlayer(ap);
    }
  }
  class Donate extends DominionCard{
    public Donate(){
      super("donate");
      debt=8;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      donateSwitch=true;
    }
  }
  class Advance extends DominionCard{
    public Advance(){
      super("advance");
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.doWork("trash", 0, 1, ap, c -> c.isAction);
      game.selectedCards.clear();
      game.gainSpecial(ap, c -> c.isAction && game.costCompare(c,6,0,0)<=0);
    }
  }
  class Delve extends DominionCard{
    public Delve(){
      super("delve");
      cost=2;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.gainCard("silver", ap, "discard", true);
      game.buys++;
      game.updateSharedFields();
    }
  }
  class Tax extends DominionCard{
    public Tax(){
      super("tax");
      cost=2;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.server.displayComment(ap, "Choose a pile to tax");
      game.doWork("selectDeck", 1, 1, ap);
      game.supplyDecks.get(game.selectedDeck).tax+=2;
      game.displaySupply(game.selectedDeck);
      game.changePhase("buys");
      game.server.displayComment(ap, "");
    }
  }
  class Banquet extends DominionCard{
    public Banquet(){
      super("banquet");
      cost=3;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      for(int i=0; i<2; i++) game.gainCard("copper", ap, "discard", true);
      game.gainSpecial(ap, c -> !c.isVictory && game.costCompare(c,5,0,0)<=0);
    }
  }
  class Ritual extends DominionCard{
    public Ritual(){
      super("ritual");
      cost=4;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      if(game.gainCard("curse", ap, "discard", true)){
        game.doWork("trash", 1, 1, ap);
        if(game.selectedCards.size()==0) return;
        game.players.get(ap).vicTokens=game.cost2(game.selectedCards.get(0));
        game.displayPlayer(ap);
        game.changePhase("buys");
        game.selectedCards.clear();
      }
    }
  }
  class Salttheearth extends DominionCard{
    public Salttheearth(){
      super("salttheearth");
      cost=4;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.players.get(ap).vicTokens++;
      game.displayPlayer(ap);
      game.changePhase("selectDeck");
      Dominion.SupplyDeck deck;
      while(true){
        game.work(ap);
        deck=game.supplyDecks.get(game.selectedDeck);
        if(deck.size()==0) continue;
        if(deck.card.isVictory){
          game.trashCard(deck.topCard(), ap);
          if(deck.size()==0) game.emptyPiles++;
          break;
        }
      }
      game.displaySupply(deck.makeData());
      game.changePhase("buys");
    }
  }
  class Wedding extends DominionCard{
    public Wedding(){
      super("wedding");
      cost=4;
      debt=3;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      game.players.get(ap).vicTokens+=1;
      game.gainCard("gold", ap, "discard", true);
    }
  }
  class Windfall extends DominionCard{
    public Windfall(){
      super("windfall");
      cost=5;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      if(game.players.get(ap).deck.size()==0 && game.players.get(ap).disc.size()==0){
        for(int i=0; i<3; i++) game.gainCard("gold", ap, "discard", true);
      }
    }
  }
  class Conquest extends DominionCard{
    public Conquest(){
      super("conquest");
      cost=6;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      for(int i=0; i<2; i++) game.gainCard("silver", ap, "discard", true);
      game.players.get(ap).vicTokens+=conquestCounter;
      game.displayPlayer(ap);
    }
  }
  class Dominate extends DominionCard{
    public Dominate(){
      super("dominate");
      cost=14;
      isEvent=true;
    }
    @Override
    public void onGain(int ap){
      if(game.gainCard("province", ap, "discard", true)){
        game.players.get(ap).vicTokens+=9;
        game.displayPlayer(ap);
      }
    }
  }
}
