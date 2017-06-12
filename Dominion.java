
import java.io.*;
import java.util.*;
import java.util.function.Predicate;


public class Dominion{

  public DominionServer server;
  public ArrayList<DominionPlayer> players;
  LinkedHashMap<String, SupplyDeck> supplyDecks;
  public ArrayList<DominionCard> matcards;
  private ArrayList<DominionCard> durationHolder=new ArrayList<>();
  Deck<DominionCard> trash;
  private LinkedHashSet<String> gameOptions;
  private HashSet<String> playerOptions;
  private PairList<String, Integer> fields;

  public int money,actions,buys,potions;
  int emptyPiles;
  private boolean gameOver;
  private String phase;

  int gainLimit;

  //stuff for selections
  //cards that have been selected for trashing/discarding etc go here in case they need to be looked at by e.g. a forge
  String selectedDeck;
  private int maxSelection=10,minSelection=10;
  public ArrayList<DominionCard> selectedCards=new ArrayList<>(); 
  public ArrayList<Boolean> mask=new ArrayList<>();

  //specific card related counters
  //only counters that are relevant to multiple cards in different expansions should go hear
  //others should the static in the proper expansion
  int bridgeCounter=0; //counts cost reduction, also does highway and brige troll
  int conspiratorCounter=0; //counts total actions played, also does peddler
    
  private HashMap<String,Expansion> expansions=new HashMap<>();
  private Empires empires;
  
 //**********STUFF WHICH SETS UP THE GAME*********//
  public Dominion(DominionServer tserver, String supply){
    server=tserver;

    //players
    players=new ArrayList<DominionPlayer>();
    //make players
    for(int i=0;i<server.playerNames.size();i++){
      players.add(new DominionPlayer(server.playerNames.get(i)));

//      if(DominionServer.DEBUG)
//        for(int j=0;j<3;j++) players.get(i).deck.add(cardFactory("bordervillage"));
    }


    expansions.put("Core",new Core(this));
    expansions.put("Intrigue",new Intrigue(this));
//    expansions.put("Seaside",new Seaside(this));
//    expansions.put("Prosperity",new Prosperity(this));
//    expansions.put("Hinterlands",new Hinterlands(this));
//    expansions.put("Cornucopia",new Cornucopia(this));
//    expansions.put("Guilds",new Guilds(this));
//    expansions.put("DarkAges",new DarkAges(this));
//    //theres a bunch of useful methods in empires
    empires=new Empires(this);
    expansions.put("Empires", empires);
    int startingPlayer=startGame(supply);
    server.initialize(supplyData(), playerData(),startingPlayer, gameOptions, playerOptions);
    updateSharedFields();
    work(startingPlayer);
    
  }
  
  void reset(){
    //players need to be remade every time
    players=new ArrayList<>();
    for(int i=0;i<server.playerNames.size();i++){
      players.add(new DominionPlayer(server.playerNames.get(i)));
    }

    int startingPlayer=startGame("randomPreset");
    trash.clear();
    resetCardCounters();
    durationHolder.clear();
    selectedCards.clear();
    empires.setDefaults();
    for(DominionPlayer player : players) player.duration.clear();
    server.reset(supplyData(), playerData(), startingPlayer, gameOptions, playerOptions);
    work(startingPlayer);
  }
  
  //stuff which should run when a new game starts, whether or not its the first game
  private int startGame(String supply){
  
    matcards=new ArrayList<DominionCard>();
    money=0; actions=1; buys=1;
    phase="actions";
    gameOver=false;
    emptyPiles=0;
    maxSelection=1;

    //supplies
    gameOptions=new LinkedHashSet<>();
    playerOptions=new HashSet<>();
    gameOptions.add("Actions");
    gameOptions.add("Money");
    gameOptions.add("Buys");
    supplyDecks=new LinkedHashMap<>();    
    ArrayList<String> supplies=getSupplyCards(supply);
//    supplies.add("villa");
    System.out.println(supplies);
    boolean usePlatinums=Expansion.usePlatinums(supplies);
    //some cards need special behavior on setup, I deal with this here
    boolean addRuins=false;
    boolean addTax=false;
    boolean addObelisk=false;
    for(String s : supplies){
      if(s.equals("pirateship")) playerOptions.add("pirateship");
      if(s.equals("island")) playerOptions.add("island");
      if(s.equals("nativevillage")) playerOptions.add("nativevillage");
      if(cardFactory(s).isDuration) playerOptions.add("duration");
      if(s.equals("traderoute")) gameOptions.add("Trade Route");
      if(empires.gathererNames.containsKey(s)) gameOptions.add(empires.gathererNames.get(s));
      if(Expansion.vicTokens.contains(s)) playerOptions.add("victorytokens");
      if(Expansion.coinTokens.contains(s)) playerOptions.add("cointokens");
      if(cardFactory(s).isLooter) addRuins=true;
      if(Expansion.debtCards.contains(s)) playerOptions.add("debt");
      if(s.equals("baker")){
        for(DominionPlayer player : players){
          player.coinTokens=1;
        }
      }
      if(s.equals("tax")) addTax=true;
      if(s.equals("obelisk")) addObelisk=true;
    }
    if(addRuins) supplies.add("ruins");
    //set the shared fields
    fields=new PairList<>();
    for(String s: gameOptions){
      if(s.equals("Actions") || s.equals("Buys")){
        fields.put(s,1);
      }else fields.put(s, empires.gathererVals.getOrDefault(s, 0));
    }

    String [] tcards={"copper","silver","gold","estate","duchy","province","curse"};
    ArrayList<String> cards=new ArrayList<String>(Arrays.asList(tcards));
    if(usePlatinums){
      cards.add(3,"platinum");
      cards.add(7,"colony");
    }
    cards.addAll(supplies);
    
    //make the supplies
    for(int i=0;i<cards.size();i++){
      SupplyDeck deck=new SupplyDeck(cards.get(i));
      supplyDecks.put(cards.get(i), deck);
      if(addTax && !deck.card.isEvent && !deck.card.isLandmark) deck.tax++;
    }
    //now that we know what supplies there are we can deal with some landmark stuff
    if(addObelisk){
      Random ran=new Random();
      while(true) {
        Empires.obeliskSupply = supplies.get(ran.nextInt(supplies.size()));
        DominionCard card=supplyDecks.get(Empires.obeliskSupply).card;
        if(!card.isLandmark && !card.isEvent) break;
      }
    }
    empires.setDefiledshrineTokens();
    //based on what's in the supply, give each play estates or shelters
    //and then shuffle and draw 5 cards
    boolean shelters=Expansion.useShelters(supplies);
    for(DominionPlayer player : players){
      if(shelters){
        player.deck.put(cardFactory("hovel", "DarkAges"));
        player.deck.put(cardFactory("necropolis", "DarkAges"));
        player.deck.put(cardFactory("overgrownestate", "DarkAges"));
      }else{
        for(int i=0; i<3; i++) player.deck.put(cardFactory("estate"));
      }
      //for(int i=0; i<3; i++) player.deck.add(cardFactory("smallcastle", "Empires"));
      player.deck.shuffle();
      player.drawToHand(5);
    }

    //trash
    trash=new Deck<>();
    trash.backImage=Deck.blankBack;
    trash.faceup=true;  
    
    //specific card related stuff
    Prosperity.tradeRouteCards=new HashSet<>();
    
    Random ran=new Random();
    return ran.nextInt(players.size());
  }
  
  ///***STUFF WHICH PROGRESSES THE GAME***///
  public void work(int t){
    work(t,null);
  }

  //active card is used for the AI, every card that implements an AI response method should pass "this"
  //to all calls to doWork so the card knows what to do
  public void work(int t, DominionCard activeCard){
  
    int activePlayer=t;
    String input;
    String extraInput;
    DominionCard card;
    boolean doneSelection=false;
    if(maxSelection<=0) maxSelection=1;


    while(true){

      input=server.getUserInput(activePlayer, activeCard);
      
      System.out.println(input);
      
      //if input is a number, it represent the playing of a card from the active player's hand
      if(input.charAt(0)<='9' && input.charAt(0)>='0'){

        //always happens unless revealing a card
        if(!phase.equals("reveal")){
          if(mask.size()==players.get(activePlayer).hand.size()) mask.remove(Integer.parseInt(input));
          card=players.get(activePlayer).hand.remove(Integer.parseInt(input));
        }else
          card=players.get(activePlayer).hand.get(Integer.parseInt(input));
        
        //action specific stuff
        if(phase.equals("actions") && card.isAction){
          actions--; 
        }

        //if the card is going to go to matcard
        //display is done in playCard
        if(phase.equals("actions") || phase.equals("buys")) playCard(card, activePlayer);

        //check if we just ended the actions phase
        if(phase.equals("actions") && ( (card.isMoney && !card.isAction) || actions==0)){
          if(supplyDecks.containsKey("arena")) empires.arena(activePlayer);
          changePhase("buys");
        }
        // specific stuff for other phases
        if(phase.equals("discard")){
          players.get(activePlayer).disc.put(card);

          //tunnel
          if(card.getName().equals("tunnel")){
            String [] options={"Discard for Gold","pass"};
            OptionData o=new OptionData(options);
            o.add(card.getImage(),"image");
            extraInput=optionPane(activePlayer,o);
            if(extraInput.equals(options[0])){
              gainCard("gold",activePlayer);
            }
          }
        }
        if(Objects.equals(phase, "trash")) trashCard(card, activePlayer);
        if(Objects.equals(phase, "topdeck")) players.get(activePlayer).deck.put(card);
        
        //generic selection behavour
        if(phase.equals("discard") || phase.equals("trash") || phase.equals("topdeck") || phase.equals("select") || phase.equals("reveal")){
          selectedCards.add(card);
          displayPlayer(activePlayer);
        }
          
      }else if(input.charAt(0)=='G'){
      //if first character is G, we gained a card
        if(phase.equals("buys") || phase.equals("gain")){
          gainCard(input.substring(1),activePlayer);
        }
        else if(phase.equals("selectDeck") || phase.equals("selectDeck2")){
          selectedDeck=input.substring(1);
          break;
        }
      }else if(input.charAt(0)=='B'){
      //if first character is B, a button was pressed
        doneSelection=buttonManager(input.substring(1),activePlayer);
      }else{
        System.out.println("Bad input from client");
      }
      
      //check if the turn is over
      if(Objects.equals(phase, "buys") && (doneSelection || (buys==0 && players.get(activePlayer).debt==0))){
        activePlayer=endTurn(activePlayer);
        doneSelection=false;
      }
      System.out.println(doneSelection+" "+selectedCards.size()+" "+minSelection+" "+maxSelection);
      //check if a selection phase should be ended
      if(selectedCards.size()>=maxSelection 
          || (selectedCards.size()>=minSelection && doneSelection) 
          || ((phase.equals("trash") || phase.equals("discard") || phase.equals("topcard")
               || phase.equals("select") || phase.equals("reveal")) && players.get(activePlayer).hand.size()==0)){
        doneSelection=false;
        break;
      }
    }
    System.out.println("Exited loop");
  }
  //plays a card on behalf of a player
  void playCard(DominionCard card, int activePlayer){
    playCard(card,activePlayer,false);
  }
  void playCard(DominionCard card, int activePlayer, boolean throneRoom){
    System.out.println("played "+card.getName());

    if(card.isAction && phase.equals("actions")) conspiratorCounter++;
    if(supplyDecks.containsKey("peddler") && card.isAction) displaySupply("peddler");

    if(card.isAction && Empires.enchantressSwitch[activePlayer]){
      actions+=1;
      players.get(activePlayer).drawToHand(1);
    }else {
      money += card.value;
      actions += card.actions;
      buys += card.buys;
      players.get(activePlayer).drawToHand(card.cards);
    }
    if(card.getName().equals("silver")){
      money+=Core.merchantCounter;
      Core.merchantCounter=0;
    }
    if(card.getName().equals("copper")){
      money+=Intrigue.coppersmithCounter;
      if(supplyDecks.containsKey("grandmarket") && !supplyDecks.get("grandmarket").contraband){
        supplyDecks.get("grandmarket").contraband=true;
        displaySupply("grandmarket");
      }
    }


    if(!throneRoom){
      matcards.add(card);
    }


    cardPlayed(activePlayer);
    if(Empires.enchantressSwitch[activePlayer] && card.isAction){
      Empires.enchantressSwitch[activePlayer]=false;
    }else {
      card.work(activePlayer);
    }

  }
  public boolean gainCard(String supplyName, int activePlayer){
    return gainCard(supplyName,activePlayer, "discard", false);
  }
  public boolean gainCard(String supplyName, int activePlayer, String where){
    return gainCard(supplyName,activePlayer,where,false);
  }
  //supplyName is type of supply
  //activeplayers is the player that gets the card
  //where is "topcard", "discard", "hand": where to add the card
  //if skipBuy=true, the player will not be changed money or a buy even if its the buy phase
  @SuppressWarnings("unchecked")
  public boolean gainCard(String supplyName, int activePlayer, String where, boolean skipBuy){
    DominionPlayer player=players.get(activePlayer); 
    //we might try to gain a card that isn't in the supply(e.g. if jester reveals a knight)
    //so if this happens just fail quietly
    if(!supplyDecks.containsKey(supplyName)) return false;

    SupplyDeck deck=supplyDecks.get(supplyName);

    //can never gain a card if the supply is empty
    //if phase is buys you need a buy and enough money
    //if phase is gain you need the "gainLimit" set by the card you played to be big
    //if phase is something else there are no conditions since an action card got you here
    if(deck.size()>0 && ( 
           (phase.equals("buys") && money>=deck.getCost() && buys>0 && players.get(activePlayer).debt==0)
        || (phase.equals("gain") && gainLimit>=deck.getCost() && deck.card.debt==0)
        || (!phase.equals("gain") && !phase.equals("buys"))
        || skipBuy) ){

      if(phase.equals("buys") && !skipBuy) {

        if (deck.contraband) return false;
        buys--;
        money -= deck.getCost();
      }

      //we need to deduct money before gettng the card because of split piles
      DominionCard card=deck.topCard();

      if(phase.equals("buys") && !skipBuy) {

        players.get(activePlayer).debt += card.debt + deck.tax;
        deck.tax = 0;

        //for event cards thats all we have to do?
        if(card.isEvent){
          card.onGain(activePlayer);
          displayPlayer(activePlayer);
          return false;
        }

        //** CARD SPECIFIC BUYING STUFF **//
        //extra gains from talisman
        if(!card.isVictory){
          phase="gain";
          for(int i=0;i<Prosperity.talismanCounter; i++) gainCard(supplyName, activePlayer, "discard", true);
          phase="buys";
          selectedCards.clear();
        }
        //goons
        players.get(activePlayer).vicTokens+=Prosperity.goons;

        //hoard
        for(int i=0;i<Prosperity.hoard;i++){
          if(card.isVictory) gainCard("gold",activePlayer, "discard", true);
        }
        //embargo
        for(int i=0;i<deck.embargo;i++){
          gainCard("curse",activePlayer, "discard", true);
        }
        //treasury
        if(card.isVictory) Seaside.victoryBought=true;
        //haggler
        for(int i=0;i<Hinterlands.hagglerCounter;i++){
          gainLimit=deck.getCost()-1;
          server.displayComment(activePlayer, "gain a non-Victory card costing up to "+gainLimit);
          while(true){
            doWork("selectDeck",1,1,activePlayer,null, null);
            if(supplyDecks.get(selectedDeck).getCost()<=gainLimit && !supplyDecks.get(selectedDeck).card.isVictory){
              gainCard(selectedDeck,activePlayer);
              break;
            }
          }
          changePhase("buys");
          server.displayComment(activePlayer, "");
        }
        //merchantguild
        players.get(activePlayer).coinTokens+=Guilds.merchantguildCounter;
        //hermit
        DarkAges.hermitSwitch=false;
        //charm
        if(Empires.charmCounter>0 && isValidSupply(c -> costCompare(c,card)==0 && !c.equals(card) && c.debt==card.debt) ){
          String oldPhase=phase;
          for(int i=0; i<Empires.charmCounter; i++){
            server.displayComment(activePlayer, "gain a card with a different name costing "+cost2(card));
            SupplyDeck deck2;
            while(true){
              doWork("selectDeck", 1, 1, activePlayer);
              deck2=supplyDecks.get(selectedDeck);
              if(!deck2.card.equals(card) && costCompare(deck2.card, card)==0){
                gainCard(selectedDeck, activePlayer, "discard", true);
                break;
              }

            }
            server.displayComment(activePlayer, "");
          }
          changePhase(oldPhase);
          Empires.charmCounter=0;
        }
      }
      if(phase.equals("gain")) {
        selectedCards.add(card);
      }

      //trigger landmarks
      empires.landmarkGain(card, activePlayer);

      if(deck.size()==0){
        if(card.getName().equals("province") || card.getName().equals("colony")) gameOver=true;
        else if(emptyPiles<2) emptyPiles++;
        else gameOver=true;
      }

      //**stuff related to specific cards**//
      String input;
      DominionCard card2;
      //smuggler
      if(supplyDecks.get(supplyName).getCost()<=6) Seaside.smugglerCards1.add(supplyName);
      //traderoute
      if(card.isVictory) Prosperity.tradeRouteCards.add(card.getName());

      //duchess
      if(card.getName().equals("duchy") && supplyDecks.containsKey("duchess")){
        String [] options={"Gain Duchess","Pass"};
        input=optionPane(activePlayer,new OptionData(options));
        if(input.equals(options[0])){
          gainCard("duchess",activePlayer, "discard", true);
        }
      }
      //fools gold is the worst
      if(card.getName().equals("province") && supplyDecks.containsKey("foolsgold")){
        String [] options2={"Trash Fools Gold","Pass"};
        OptionData o=new OptionData(options2);
        o.add(supplyDecks.get("foolsgold").card.getImage(),"image");

        for(int i=(activePlayer+1)%players.size(); i!=activePlayer; i=(i+1)%players.size()){
          for(ListIterator<DominionCard> it=players.get(i).hand.listIterator(); it.hasNext(); ){
            card2=it.next();
            if(card2.getName().equals("foolsgold")){
              input=optionPane(i,o);
              if(input.equals(options2[0])){
                trashCard(card2, activePlayer);
                it.remove();
                gainCard("gold",activePlayer,"topcard",true);
              }
            }
          }//loop through hand
        }//loop through players
      }
      //hovel is also kind of a pain
      if(phase.equals("buys") && card.isVictory){
        String [] options2={"Trash Hovel","Pass"};
        OptionData o=new OptionData(options2);

        for(ListIterator<DominionCard> it=players.get(activePlayer).hand.listIterator(); it.hasNext(); ){
          card2=it.next();
          if(card2.getName().equals("hovel")){
            input=optionPane(activePlayer, o);
            if(input.equals(options2[0])){
              trashCard(card2, activePlayer);
              it.remove();
            }
          }
        }//loop through hand

      }
      //groundskeeper
      if(card.isVictory && Empires.groundskeeperCounter>0){
        players.get(activePlayer).vicTokens+=Empires.groundskeeperCounter;
        displayPlayer(activePlayer);
      }

      gainCardNoSupply(card, activePlayer, where);
      
      updateSharedFields();
      displaySupply(supplyName);

      //if this gain was successful increment the counter
      Empires.triumphCounter++;
      if(supplyName.equals("silver")) Empires.conquestCounter++;

      return true;
    }
    return false;
  }
  //gain cards that aren't in the supply
  public void gainCardNoSupply(DominionCard card, int activePlayer, String where){
    DominionPlayer player=players.get(activePlayer); 

    //royalseal
    if(Prosperity.royalSeal){
      String [] options={"Deck","Discard"};
      String input=optionPane(activePlayer,new OptionData(options));
      if(input.equals(options[0])) where="topcard";
    }

    //play reactions
    OptionData o;     
    ArrayList<String> reactions=reactionReveal(player.hand,activePlayer,card, c -> c.isReaction2);
    for(String r : reactions){
      if(r.equals("watchtower")){
        String [] options = {"Top Deck","trash"};
        o=new OptionData(options);
        o.add(card.getImage(),"image");
        if(optionPane(activePlayer,o).equals(options[0]))
          where="topcard";
        else{
          trashCard(card, activePlayer);
          return;
        }
      }
      if(r.equals("trader")){
        if(supplyDecks.containsKey(card.getName())){
          supplyDecks.get(card.getName()).put(card);
        }else{
          trashCard(card, activePlayer);
        }
        gainCard("silver",activePlayer,"discard",true);
        return;
      }
    }

    //add card on discard pile or (more rarely) top of deck
    if(where.equals("hand") || card.getName().equals("villa")) player.hand.add(card);
    else if(where.equals("topcard")) player.deck.put(card);
    else if(where.equals("discard")) player.disc.put(card);
    else{
      System.out.println("I don't know where to add this card! "+where);
    }

    //resolve ongain effects
    card.onGain(activePlayer);
      
    displayPlayer(activePlayer);
    
  }
  //is a card which satisfies the condition tester in the trash
  public boolean cardInTrash(Predicate<DominionCard> tester){
    for(DominionCard card : trash){
      if(tester.test(card)) return true;
    }
    return false;
  }
  public void gainFromTrash(int ap, String where, Predicate<DominionCard> tester){
    if(!cardInTrash(tester)){
      return;
    }
    OptionData o=new OptionData();
    for(DominionCard card : trash){
      if(tester.test(card)){
        o.add(card.getName(), "imagebutton");
      }else{
        o.add(card.getName(), "image");
      }
    }

    String input=optionPane(ap,o);
    DominionCard card;
    for(ListIterator<DominionCard> it=trash.listIterator(); it.hasNext(); ){
      card=it.next();
      if(card.getName().equals(input)){
        if(where.equals("discard")) players.get(ap).disc.put(card);
        else if(where.equals("topcard")) players.get(ap).disc.put(card);
        else{
          System.out.println("a card vanished "+where);
          System.exit(0);
        }
        it.remove();
        break;
      }
    }
    displayTrash();
    displayPlayer(ap);

  }
  void trashCard(DominionCard card, int ap){
    trash.put(card);
    card.onTrash(ap);

    ArrayList<String> cards=reactionReveal(players.get(ap).hand, ap, null,
            c -> c.getName().equals("marketsquare"));
    DominionCard card2;
    int counter=0;
    for(ListIterator<DominionCard> it=players.get(ap).hand.listIterator(); it.hasNext(); ){
      card2=it.next();
      if(card2.getName().equals("marketsquare")){
        players.get(ap).disc.put(card2);
        it.remove();
        counter++;
        gainCard("gold", ap, "discard", true);
        if(counter==cards.size()) break;
      }
    }
    if(supplyDecks.containsKey("tomb")){
      players.get(ap).vicTokens++;
      displayPlayer(ap);
    }
    displayTrash();
  }
  String cardToSupply(DominionCard card){
    String supplyName="";
    if(supplyDecks.containsKey(card.getName())){
      supplyName=card.getName();
    }else if(card.isKnight){
      supplyName="knight";
    }else if(card.isRuins) {
      supplyName = "ruins";
    }else if(card.isCastle){
      supplyName="castle";
    }else{
      boolean split=false;
      for(Map.Entry<String, String> e : Empires.splitPiles.entrySet()){
        if(card.getName().equals(e.getValue())){
          supplyName=e.getKey();
          split=true;
          break;
        }
        if(!split) {
          return null;
        }
      }
    }
    return supplyName;
  }
  void returnToSupply(DominionCard card, int ap){

    String s=cardToSupply(card);
    if(s==null){
      trashCard(card, ap);
    }else {
      supplyDecks.get(s).addCard(card);
      if (supplyDecks.get(s).size() == 1) emptyPiles--;
      displaySupply(s);
    }
  }

  //handles what happens if the player clicks on a button
  private boolean buttonManager(String input, int activePlayer){
    DominionCard card;
    DominionPlayer player;

    if(input.equals("buys")){
      return true;
    }else if(input.equals("actions")){
      if(supplyDecks.containsKey("arena")) empires.arena(activePlayer);
      changePhase("buys");
    }else if(input.equals("treasures")){
      if(phase.equals("actions") && supplyDecks.containsKey("arena")) empires.arena(activePlayer);
      changePhase("buys");
      player=players.get(activePlayer);

      for(ListIterator<DominionCard> it=player.hand.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getName().equals("copper") || card.getName().equals("silver") || card.getName().equals("gold") || card.getName().equals("platinum")){
          it.remove();
          playCard(card,activePlayer);
        }
      }
    }else if(input.equals("coin")) {
      if (players.get(activePlayer).coinTokens > 0) {
        players.get(activePlayer).coinTokens--;
        money++;
        displayPlayer(activePlayer);
        updateSharedFields();
      } else {
        System.out.println("Tried to play a coin token but there are none");
      }
    }else if(input.equals("debt")){
      if(money>0 && players.get(activePlayer).debt>0){
        if(players.get(activePlayer).debt>money){
          players.get(activePlayer).debt-=money;
          money=0;
        }else{
          money-=players.get(activePlayer).debt;
          players.get(activePlayer).debt=0;
        }
        updateSharedFields();
        displayPlayer(activePlayer);
      }
    }else if(input.equals("discard") || input.equals("trash") || input.equals("select") || input.equals("reveal") || input.equals("gain")){
      return true;
    }
    return false;
  }
  private int endTurn(int activePlayer){
    //****end the previous turn***//
    DominionCard card;
    DominionPlayer player=players.get(activePlayer);

    //check if any cards do things at the end of the turn
    ArrayList<DominionCard> schemeCards=new ArrayList<>();    
    for(ListIterator<DominionCard> it=matcards.listIterator(); it.hasNext(); ){
      card=it.next();
      //there might be a problem with cleanup being called twice on e.g. a hermit
      //fix would be to separate cleanup from a function which says whether you are going on the duration pile this turn
      if(!card.getName().equals("scheme") && card.cleanup(activePlayer,players.get(activePlayer))){
        it.remove();
      }else if(card.getName().equals("scheme")){
        schemeCards.add(card);
      }
    }
    //now use scheme to add a card on top of deck
    //the reason we add scheme cards in their own list is that scheme removes items from matcards
    for(DominionCard card2 : schemeCards ){
      card2.cleanup(activePlayer,players.get(activePlayer));
    }
        
    //add remaining matcards on discard pile and clear the mat
    players.get(activePlayer).disc.put(matcards);
    matcards.clear();
    //moves archive to the duration pile for its second go around
    for(ListIterator<DominionCard> it=durationHolder.listIterator(); it.hasNext(); ){
      card=it.next();
      if(card.getName().equals("archive") && card.cleanup(activePlayer, players.get(activePlayer))){
        it.remove();
      }
    }
    players.get(activePlayer).disc.put(durationHolder);
    durationHolder.clear();

    //specific cards stuff
    //grandmarket
    if(supplyDecks.containsKey("grandmarket")){
      supplyDecks.get("grandmarket").contraband=false;
      displaySupply("grandmarket");
    }
    //peddler
    if(supplyDecks.containsKey("peddler")) displaySupply("peddler");
    
    if(gameOver) endGame();
    
    //donate
    if(Empires.donateSwitch){
      player.drawToHand(player.deck.size()+player.disc.size());
      displayPlayer(activePlayer);
      doWork("trash", 0, 100, activePlayer);
      player.deck.put(player.hand);
      player.hand.clear();
      player.deck.shuffle();
      Empires.donateSwitch=false;
    }

    //check if the player add an outpost on their duration mat, and didn't play an outpost last time
    if(Seaside.outpost){
      Seaside.outpost=false;
    }else{
      for(DominionCard card2 : players.get(activePlayer).duration){
        if(card2.getName().equals("outpost")){
          Seaside.outpost=true;
          break;
        }
      }
    }
    int newPlayer;

    //if there is an outpost on the duration mat, that player gets to go again
    if(Seaside.outpost){
      newPlayer=activePlayer;
      players.get(activePlayer).disc.put(players.get(activePlayer).hand);
      players.get(activePlayer).hand.clear();
      players.get(activePlayer).drawToHand(3);
    }else{
      players.get(activePlayer).endTurn();       
      newPlayer=(activePlayer+1)%players.size();
    }

    //baths landmark
    if(supplyDecks.containsKey("baths")) empires.baths(activePlayer);
    //mountainpass
    if(Empires.mountainpassSwitch) empires.mountainPass(activePlayer);

    //****start the next turn ***///
    changePhase("actions");
    money=0;
    buys=1;
    actions=1;
    resetCardCounters();
    
    //pass on this info to board
    //have to do this before playing the duration cards because we're about to delete them all!
    changePlayer(activePlayer,newPlayer);

    //play duration cards
    for(DominionCard card2 : players.get(newPlayer).duration){
      for(int i=0;i<card2.throneroomed;i++) card2.duration(newPlayer);
      card2.throneroomed=1;
//      matcards.add(card2);
    }
    durationHolder=new ArrayList<>(players.get(newPlayer).duration);
    players.get(newPlayer).duration.clear();

    //play horse traders
    if(players.get(newPlayer).horseTraders.size()>0){
      players.get(newPlayer).hand.addAll(players.get(newPlayer).horseTraders);
      players.get(newPlayer).drawToHand(players.get(newPlayer).horseTraders.size());
      displayPlayer(newPlayer);
    }
    updateSharedFields();

    
    return newPlayer;
    
  }
  //resets specific card-related stuff
  @SuppressWarnings("unchecked")
  private void resetCardCounters(){
    if(bridgeCounter>0 || Prosperity.quarryCounter>0){
      bridgeCounter=0;
      //quarryCounter isn't zerod in its card because I don't want to call displaysupplies too many times
      Prosperity.quarryCounter=0;
      displaySupplies();
    } 
    conspiratorCounter=0;  
    
    Seaside.smugglerCards2=new ArrayList<>(Seaside.smugglerCards1);
    Seaside.smugglerCards1.clear();
    
    Seaside.victoryBought=false;

    DarkAges.hermitSwitch=true;
    DarkAges.urchinSwitch=false;

    Empires.triumphCounter=0;
    Empires.conquestCounter=0;

  }
  @SuppressWarnings("unchecked")
  private void endGame(){
    String temp;
    String name;
    int sum;
    PairList<Integer, String> scores=new PairList<>();
    Pair<Integer, String> landmark;
    Pair<Integer, String> core;
    for(int i=0;i<players.size();i++){
      core=players.get(i).victoryPoints();
      landmark=empires.landmarkScore(players.get(i).deck, supplyDecks);
      sum=Integer.sum(core.getA(),landmark.getA());
      temp=players.get(i).getName()+": "+sum+" ("+landmark.getB()+core.getB()+")";
      scores.add(sum, temp);
    }
    scores.sortByValue();
    server.showScores(scores);

    empires.setDefaults();

    //a hacky way to get out of the work loop
    maxSelection=0;
  }
  
  public ArrayList<String> reactionReveal(Collection<DominionCard> hand, int activePlayer, DominionCard attackCard,
                                          Predicate<DominionCard> tester){
    DominionCard card;
    String [] options={"Reveal","Pass"};
    OptionData o=new OptionData(options);
    ArrayList<String> out=new ArrayList<>();

    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
      card=it.next();
      if(tester.test(card)){
        o.add(card.getImage(),"image");
        if(attackCard != null) o.add(attackCard.getImage(),"image");
        if(optionPane(activePlayer,o).equals("Reveal")){
          out.add(card.getName());
        }
        o=new OptionData(options);
      }
    }
    return out;
  }
  //********DO THINGS THAT MULTIPLE ACTION CARDS NEED***///
  
  //give user a choice
  public String optionPane(int activePlayer, OptionData o){
    server.optionPane(activePlayer,o);
    return server.getUserInput(activePlayer, null);
  }
  //always use this to get the cost of cards
  public int cost2(DominionCard card){
    if(card.isAction) return Math.max(card.cost-bridgeCounter-Prosperity.quarryCounter,0);
    else return Math.max(card.cost-bridgeCounter,0);
  }
  //returns 0 if two cards are equal in cost, -1 if this card is less, 1 if this card is more
  int costCompare(DominionCard card1, DominionCard card2){ return costCompare(card1, cost2(card2), card2.debt, card2.potions); }
  int costCompare(DominionCard card1, DominionCard card2, int extra){ return costCompare(card1, cost2(card2)+extra, card2.debt, card2.potions); }
  int costCompare(DominionCard card1, int cost, int debt, int potions){
    if(cost==cost2(card1) && debt==card1.debt && potions==card1.potions) return 0;
    else if(card1.cost<=cost && card1.debt<=debt && card1.potions <= potions) return -1;
    else return 1;
  }

  //puts a card anywhere in the deck
  void putAnywhere(int activePlayer, DominionCard card){
    DominionPlayer player=players.get(activePlayer);
    String [] options=new String[0];
    OptionData o=new OptionData(options);
    o.add("Choose the position to add the card (0 is top)","text");
    for(int i=0;i<player.deck.size()+1;i++) o.add(Integer.toString(i),"textbutton");
    String input=optionPane(activePlayer,o);
    player.deck.add(Integer.parseInt(input),card);
    displayPlayer(activePlayer);
  }
  //puts a bunch of cards on top of the deck in user-specified order
  public void putBack(int activePlayer, ArrayList<DominionCard> cards){
    if(cards.size()==0) return;
    
    String [] options;
    OptionData o;
    DominionPlayer player=players.get(activePlayer);
    String input;
    server.displayComment(activePlayer,"Put the cards back in any order");
    
    while(cards.size()>1){
      options=new String[0];
      o=new OptionData(options);
      for(int i=0;i<cards.size();i++){
        o.add(cards.get(i).getImage(),"imagebutton");
      }
      input=optionPane(activePlayer,o);
//      input=server.getUserInput(activePlayer, null);
      for(int i=0;i<cards.size();i++){
        if(input.equals(cards.get(i).getName())){
          player.deck.put(cards.remove(i));
          break;
        }          
      }
      displayPlayer(activePlayer);
      
    }
    player.deck.put(cards.get(0));
    displayPlayer(activePlayer);
    server.displayComment(activePlayer,"");
  }
  //"gain a card costing exactly"
  void controlledGain(int activePlayer, int val){
    server.displayComment(activePlayer, "gain a card costing exactly "+val);
    gainSpecial(activePlayer, c -> costCompare(c,val,0,0)==0);

  }
  //find out if there are any cards in the supply that we can gain
  private boolean isValidSupply(Predicate<DominionCard> tester){
    boolean canGain=false;
    for(Map.Entry <String,SupplyDeck> entry : supplyDecks.entrySet()){
      if( entry.getValue().size()>0 && tester.test(entry.getValue().card)){
        canGain=true;
        break; 
      }     
    }
    return canGain;  
  }
  //gain a card subject to some conditions
  DominionCard  gainNumber(int ap, int val){
    System.out.println(val);
    server.displayComment(ap, "Gain a card costing up to "+val);
    DominionCard out=gainSpecial(ap, c -> costCompare(c, val, 0, 0)<=0);
    server.displayComment(ap, "");
    return out;
  }
  DominionCard gainSpecial(int ap, Predicate<DominionCard> tester){
    return gainSpecial(ap, tester, "discard");
  }
  DominionCard gainSpecial(int ap, Predicate<DominionCard> tester, String where){
    //quit if no valid card to gain
    System.out.println("is there a valid supply "+isValidSupply(tester));
    if (!isValidSupply(tester)) return null;

    String oldPhase=phase;
    DominionCard outcard=null;
    while (true) {
      doWork("selectDeck", 1, 1, ap);
      Dominion.SupplyDeck deck = supplyDecks.get(selectedDeck);
      if (tester.test(deck.card) && deck.size() > 0){
        outcard=deck.card;
        gainCard(selectedDeck, ap, where, true);
        Empires.triumphCounter++;
        if(deck.getName().equals("silver")) Empires.conquestCounter++;
        break;
      }
//      selectedCards.clear();
    }
    changePhase(oldPhase);
    return outcard;

  }
  //a typical request for the player to do something
  public void doWork(String p, int min, int max, int activePlayer){
    doWork(p,min,max,activePlayer, null, null);
  }
  public void doWork(String p, int min, int max, int activePlayer, Predicate<DominionCard> tester){
    doWork(p,min,max,activePlayer, null, tester);
  }
  public void doWork(String p, int min, int max, int activePlayer, DominionCard card, Predicate<DominionCard> tester){
    if(tester!=null) {
      mask = new ArrayList<>(players.get(activePlayer).hand.size());
      for (DominionCard aHand : players.get(activePlayer).hand) {
        mask.add(tester.test(aHand));
      }
      if (!mask.contains(true)){
        mask.clear();
        return;
      }
    }

    minSelection=min;
    if(!p.equals("gain") && !p.equals("selectDeck") && !p.equals("selectDeck2")
            && players.get(activePlayer).hand.size()==0) return;
    if(p.equals("gain")){
      server.displayComment(activePlayer, "gain a card costing up to "+gainLimit);
    }
    if(max<=0) max=1;
    maxSelection=max;

    String oldPhase=phase;
    changePhase(p);
    work(activePlayer, card);
    displayPlayer(activePlayer);
    if(tester!=null) mask.clear();
    changePhase(oldPhase);
  }

//  //***PRIVATE VARIABLES***///
  String getPhase(){
    return phase;
  }
  
//  //***STUFF WHICH GETS STATUS OF GAME***///
  ArrayList<Deck.SupplyData> supplyData(){
    ArrayList<Deck.SupplyData> out=new ArrayList<>(supplyDecks.size());
    for(Map.Entry<String, SupplyDeck> entry : supplyDecks.entrySet()){
      out.add(entry.getValue().makeData());
    }
    return out;
  }
  public ArrayList<DominionPlayer.Data> playerData(){
    ArrayList<DominionPlayer.Data> out=new ArrayList<>(players.size());
    for(Iterator<DominionPlayer> it=players.iterator(); it.hasNext(); ){
      out.add(it.next().makeData());
    }
    return out;
  }
  
  //***some simple wrappers for server functions ***///
  public void displayPlayer(int i){
    for( PlayerInterface connection : server.connections){
      connection.displayPlayer(i,players.get(i).makeData(),mask);
    }
  }
  public void changePlayer(int oldP, int newP){
    for( PlayerInterface connection : server.connections){
      connection.changePlayer(oldP,players.get(oldP).makeData(),newP,players.get(newP).makeData(),mask);
    }    
  }
  @SuppressWarnings("unchecked")
  public void updateSharedFields(){
    String s;
    for(int i=0; i<fields.size(); i++){
      s=fields.getKey(i);
      if(s.equals("Actions")) fields.put(s, actions);
      else if(s.equals("Money")) fields.put(s, money);
      else if(s.equals("Buys")) fields.put(s,buys);
      else if(s.equals("Trade Route")) fields.put(s,Prosperity.tradeRouteCards.size());
      else if(s.equals("Potions")) fields.put(s,potions);
      else if(empires.gathererVals.containsKey(s))
        fields.put(s, empires.gathererVals.get(s));
    }

    for( PlayerInterface connection : server.connections){
      connection.updateSharedFields(fields);
    }
  }
  public void changePhase(String newPhase){
    System.out.println(phase+" "+newPhase+" "+mask);
    for( PlayerInterface connection : server.connections){  
      connection.changePhase(phase,newPhase,mask);
    }
    phase=newPhase;
  }
  public void displaySupply(Deck.SupplyData data){
    for( PlayerInterface connection : server.connections){  
      connection.displaySupply(data);
    }
  }
  public void displaySupply(String name){
    for( PlayerInterface connection : server.connections){  
      connection.displaySupply(supplyDecks.get(name).makeData());
    }
  }
  public void displaySupplies(){
    for(Map.Entry<String,SupplyDeck> entry : supplyDecks.entrySet()){
      displaySupply(entry.getValue().makeData());
    }
  }
  public void cardPlayed(int activePlayer){
    displayPlayer(activePlayer);
    for( PlayerInterface connection : server.connections){
      connection.displayMatCards(matcards);      
    }
    updateSharedFields();
  }
  private void displayTrash(){
    for( PlayerInterface connection : server.connections){
      connection.displayTrash(trash.makeData());
    }
  }  
//  
  //****INNER CLASSES***///
  //can't be static because it uses cardFactory
  class SupplyDeck extends Deck<DominionCard>{
    private String name;
    int embargo=0;
    boolean contraband=false;
    public DominionCard card;
    private boolean varied;
    int tax=0;

    public SupplyDeck(String name){
      this.name=name;

      varied = name.equals("ruins") || name.equals("knight") || name.equals("castle")
              || Empires.splitPiles.containsKey(name);

      //decks that are all made of the same card
      if(!varied) {
        card=cardFactory(name);
        int nCards;
        if (name.equals("copper") || name.equals("silver") || name.equals("gold") || name.equals("platinum")) {
          nCards = 30;
        } else if (card.isVictory) {
          nCards = Math.min(4 * players.size(), 12);
        } else if (name.equals("curse")) {
          nCards = 10 * (players.size() - 1);
        } else if (name.equals("rats")) {
          nCards = 20;
        } else if (card.isLandmark || card.isEvent){
          nCards=1;
        } else {
          nCards = 10;
        }
        for (int i = 0; i < nCards; i++) {
          add(cardFactory(name));
        }
      }else{
        //decks that are made of different cards
        if(name.equals("ruins")) {
          Deck<DominionCard> bigdeck = new Deck<>();
          for (int i = 0; i < 10; i++) {
            for(String ruinName : DarkAges.ruinNames)
              bigdeck.put(cardFactory(ruinName, "DarkAges"));
          }
          bigdeck.shuffle();
          addAll(bigdeck.deal(10 * (players.size() - 1)));
        }else if(name.equals("knight")){
          for(String s : DarkAges.knightNames)
            put(cardFactory(s, "DarkAges"));
          shuffle();
        }else if(Empires.splitPiles.containsKey(name)){
          for(int i=0; i<5; i++) put(cardFactory(Empires.splitPiles.get(name), "Empires"));
          for(int i=0; i<5; i++) put(cardFactory(name));
        }else if(name.equals("castle")){
          for(int i=0; i<players.size()/3+1; i++)
            put(cardFactory("kingscastle", "Empires"));
          put(cardFactory("grandcastle", "Empires"));
          put(cardFactory("sprawlingcastle", "Empires"));
          for(int i=0; i<players.size()/3+1; i++)
            put(cardFactory("opulentcastle", "Empires"));
          put(cardFactory("hauntedcastle", "Empires"));
          for(int i=0; i<players.size()/3+1; i++)
            put(cardFactory("smallcastle", "Empires"));
          put(cardFactory("crumblingcastle", "Empires"));
          for(int i=0; i<players.size()/3+1; i++)
            put(cardFactory("humblecastle", "Empires"));

        }

        if(size()>0){
          card = get(0);
        }
        else card=null;

      }
      if(card==null) backImage=Deck.blankBack;
      else backImage=card.getImage();

    }
    public int getCost(){
      if(card==null) {
        return 0;
      }else if(name.equals("peddler")) {
          int temp = 0;
          return Math.max(0,cost2(card) - 2 * conspiratorCounter);
      }else{
        return cost2(card);
      }
    }
    public String getName(){return name;}
    public Deck.SupplyData makeData(){
      PairList<String, String> extra=new PairList<>();
      if(contraband) {
        extra.put("contraband", Boolean.toString(contraband));
      }
      if(embargo>0){
        extra.put("embargo", Integer.toString(embargo));
      }
      if(tax>0){
        extra.put("tax", Integer.toString(tax));
      }
      if(name.equals(Empires.obeliskSupply)){
        extra.put("obelisk", "true");
      }
      boolean isEvent=false;
      boolean isLandmark=false;
      if(card!=null){
        isEvent=card.isEvent;
        isLandmark=card.isLandmark;
      }
      if(name.equals(Cornucopia.bane)) extra.put("bane", "true");
      return new Deck.SupplyData(size(), backImage, getCost(), name, isEvent, isLandmark, extra);
    }
    @Override
    public DominionCard topCard(){
      if(card.isEvent) return cardFactory(name);

      DominionCard out=removeFirst();
      if(varied){
        if(size()>0) {
          card = get(0);
          backImage = card.getImage();
        }else{
          backImage=Deck.blankBack;
        }
      }
      return out;
    }
    void addCard(DominionCard card){
      put(card);
      this.card=card;
      backImage=card.getImage();
      displaySupply(name);
    }

  }

    //****CARD STUFF***//
  private ArrayList<String> getSupplyCards(String supply){
    ArrayList<String> allCards=new ArrayList<>();

    for(Map.Entry<String,Expansion> entry : expansions.entrySet()){
      Collections.addAll(allCards,entry.getValue().cards);
    }

    HashSet<String> included=new HashSet<>();

    Random ran=new Random();
    ArrayList<String> out=new ArrayList<>();

    int i;
    String s;
    String name;
    if(supply.equals("randomPreset")){
      File folder=new File("presets");
      File [] listOfFiles=folder.listFiles();
      i=ran.nextInt(listOfFiles.length);
      supply=listOfFiles[i].getName();
    }
    if(!supply.equals("random")){
      try{
        BufferedReader fr=new BufferedReader(new FileReader("presets/"+supply));
        while(true){
          try {
            s=fr.readLine();
            if(s==null) break;
            else if(s.charAt(0)!='#'){
              if(expansions.containsKey(s)){
                i=ran.nextInt(expansions.get(s).cards.length);
                name=expansions.get(s).cards[i];
                if(!included.contains(name)) {
                  out.add(expansions.get(s).cards[i]);
                  included.add(name);
                }
              }else if(allCards.contains(s)){
                if(!included.contains(s)) {
                  out.add(s);
                  included.add(s);
                }
              } else{
                System.out.println("Couldn't find supply "+s);
              }
            }
          }catch(IOException ex){
            break;
          }
        }
      }catch(FileNotFoundException ex){
        System.out.println("No supply file found, going with random");
        supply="random";
      }
    }
    while (out.size() < 10) {
      i = ran.nextInt(allCards.size());
      name=allCards.get(i);
      if (!included.contains(name)) {
        out.add(name);
        included.add(name);
      }
    }
    //add the bane card from young witch
    int cost;
    if(out.contains("youngwitch")){
      while(true){
        i=ran.nextInt(allCards.size());
        name=allCards.get(i);
        if(!included.contains(name)){
          cost=cost2(cardFactory(allCards.get(i)));
          if(cost>=2 && cost<=3){
            included.add(name);
            out.add(allCards.get(i));
            Cornucopia.bane=allCards.get(i);
            break;
          }
        }
      }
    }      
      
    out.sort((x, y) -> cost2(cardFactory(x))-cost2(cardFactory(y)));

    //make list of events
    allCards=new ArrayList<>();
    Collections.addAll(allCards,Empires.events);
    Collections.addAll(allCards,Empires.landmarks);
    //decide how many landmarks and events to do
    int nEvents=0;

    if(!supply.equals("random")){
      try{
        BufferedReader fr=new BufferedReader(new FileReader("presets/"+supply));
        while(true){
          try {
            s=fr.readLine();
            if(s==null) break;
            else if(s.charAt(0)=='#'){
              if(s.length()>1) out.add(s.substring(1));
              else nEvents++;
            }
          }catch(IOException ex){
            break;
          }
        }
      }catch(FileNotFoundException ex){
      }
    }else {
      //randomly determine how many landmark/events
      double cut0=0.4;
      double cut1=0.7;
      double r=ran.nextDouble();
      if(r<cut0) nEvents=0;
      else if(r<cut1) nEvents=1;
      else nEvents=2;

    }
    int eventCounter=0;
    while (eventCounter < nEvents) {
      i = ran.nextInt(allCards.size());
      name=allCards.get(i);
      if (!included.contains(name)) {
        out.add(allCards.get(i));
        included.add(name);
        eventCounter++;
      }
    }

    return out;
  }

  //a card factory for when the expansion is already known
  DominionCard cardFactory(String cardname, String expansion){
    Class c;
    
    try{
      c=Class.forName(expansion+"$"+cardname.substring(0,1).toUpperCase()+cardname.substring(1));
      return (DominionCard) c.getConstructors()[0].newInstance(expansions.get(expansion));      
    }
    catch(ClassNotFoundException e){
      return new DominionCard(cardname);
    }catch(Exception e){
      e.printStackTrace();
    }
    return new DominionCard(cardname);
  }

  //looks up the expansion in the expansion table
  DominionCard cardFactory(String cardname){
    for(Map.Entry<String,Expansion> entry : expansions.entrySet()){
      if(entry.getValue().hasCard(cardname)){
        return cardFactory(cardname,entry.getKey());
      }
      if (Arrays.asList(Empires.events).contains(cardname)) {
        return cardFactory(cardname, "Empires");
      }
    }
    return new DominionCard(cardname);
  }
}
