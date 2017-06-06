
import java.util.*;
import java.util.function.Predicate;


public class Dominion{

  public DominionServer server;
  public ArrayList<DominionPlayer> players;
  LinkedHashMap<String, SupplyDeck> supplyDecks;
  public ArrayList<DominionCard> matcards;
  private ArrayList<DominionCard> durationHolder=new ArrayList<>();
  Deck<DominionCard> trash;
  public HashSet<String> startingOptions;

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
  public int bridgeCounter=0; //counts cost reduction, also does highway and brige troll
  public int conspiratorCounter=0; //counts total actions played, also does peddler
    
  private HashMap<String,Expansion> expansions=new HashMap<>();
  
 //**********STUFF WHICH SETS UP THE GAME*********//
  public Dominion(DominionServer tserver){  
    server=tserver;   

    expansions.put("Core",new Core(this));
//    expansions.put("Intrigue",new Intrigue(this));
//    expansions.put("Seaside",new Seaside(this));
//    expansions.put("Prosperity",new Prosperity(this));
//    expansions.put("Hinterlands",new Hinterlands(this));
//    expansions.put("Cornucopia",new Cornucopia(this));
//    expansions.put("Guilds",new Guilds(this));
//    expansions.put("DarkAges",new DarkAges(this));
    expansions.put("Empires", new Empires(this));
    int startingPlayer=startGame(server.playerNames);
    server.initialize(supplyData(), playerData(),startingPlayer, startingOptions);    
    work(startingPlayer);
    
  }
  
  public void reset(){
    int startingPlayer=startGame(server.playerNames);
    trash.clear();
    resetCardCounters();
    durationHolder.clear();
    selectedCards.clear();
    for(DominionPlayer player : players) player.duration.clear();
    server.reset(supplyData(), playerData(), startingPlayer, startingOptions);
    work(startingPlayer);
  }
  
  //stuff which should run when a new game starts, whether or not its the first game
  public int startGame(ArrayList<String> names){
  
    matcards=new ArrayList<DominionCard>();
    money=0; actions=1; buys=1;
    phase="actions";
    gameOver=false;
    emptyPiles=0;
    maxSelection=1;

    //resetCardCounters();
    
    //players
    players=new ArrayList<DominionPlayer>();    
    //make players
    for(int i=0;i<names.size();i++){
      players.add(new DominionPlayer(names.get(i)));

//      if(DominionServer.DEBUG)
//        for(int j=0;j<3;j++) players.get(i).deck.put(cardFactory("bordervillage"));
    }

    //supplies
    startingOptions=new HashSet<>(); 
    supplyDecks=new LinkedHashMap<>();    
    ArrayList<String> supplies=randomSupply();
 //   supplies.add("engineer");
    System.out.println(supplies);
    boolean usePlatinums=Expansion.usePlatinums(supplies);
    //some cards need special behavior on setup, I deal with this here
    boolean addRuins=false;
    for(String s : supplies){
      if(s.equals("pirateship")) startingOptions.add("pirateship");
      if(s.equals("island")) startingOptions.add("island");
      if(s.equals("nativevillage")) startingOptions.add("nativevillage");
      if(cardFactory(s).isDuration) startingOptions.add("duration");
      if(s.equals("traderoute")) startingOptions.add("traderoute");
      if(cardFactory(s).isGathering) startingOptions.add(s);
      if(Expansion.vicTokens.contains(s)) startingOptions.add("victorytokens");
      if(Expansion.coinTokens.contains(s)) startingOptions.add("cointokens");
      if(cardFactory(s).isRuins) addRuins=true;
      if(Expansion.debtCards.contains(s)) startingOptions.add("debt");
      if(s.equals("baker")){
        for(DominionPlayer player : players){
          player.coinTokens=1;
        }
      }
    }
    if(addRuins) supplies.add("ruins");

    String [] tcards={"copper","silver","gold","estate","duchy","province","curse"};
    ArrayList<String> cards=new ArrayList<String>(Arrays.asList(tcards));
    if(usePlatinums){
      cards.add(3,"platinum");
      cards.add(7,"colony");
    }
    cards.addAll(supplies);
    
    //make the supplies
    for(int i=0;i<cards.size();i++){
      supplyDecks.put(cards.get(i), new SupplyDeck( cards.get(i) ));
    } 

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
      //for(int i=0; i<3; i++) player.deck.put(cardFactory("remake"));
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
    int startingPlayer=ran.nextInt(players.size());  
    return startingPlayer;
  }
  
  ///***STUFF WHICH PROGRESSES THE GAME***///
  public void work(int t){
    work(t,null);
  }
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
        if(phase=="actions" && card.isAction){
          actions--; 
        }

        //if the card is going to go to matcard
        //display is done in playCard
        if(phase=="actions" || phase=="buys") playCard(card, activePlayer);

        //check if we just ended the actions phase
        if(phase=="actions" && ( card.isMoney || actions==0)){
          changePhase("buys");
        }
        // specific stuff for other phases
        if(phase=="discard"){
          players.get(activePlayer).disc.put(card);

          //tunnel
          if(card.getName().equals("tunnel")){
            String [] options={"Discard for Gold","pass"};
            OptionData o=new OptionData(options);
            o.put(card.getImage(),"image");
            extraInput=optionPane(activePlayer,o);
            if(extraInput.equals(options[0])){
              gainCard("gold",activePlayer);
            }
          }
        }
        if(phase=="trash") trashCard(card, activePlayer);
        if(phase=="topdeck") players.get(activePlayer).deck.put(card);
        
        //generic selection behavour
        if(phase=="discard" || phase.equals("trash") || phase=="topdeck" || phase=="select" || phase=="reveal"){
          selectedCards.add(card);
          displayPlayer(activePlayer);
        }
          
      }else if(input.charAt(0)=='G'){
      //if first character is G, we gained a card
        if(phase=="buys" || phase=="gain") gainCard(input.substring(1),activePlayer);
        else if(phase=="selectDeck" || phase=="selectDeck2"){
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
      if(phase=="buys" && (doneSelection || (buys==0 && players.get(activePlayer).debt==0))){
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
  public void playCard(DominionCard card, int activePlayer){
    playCard(card,activePlayer,false);
  }
  public void playCard(DominionCard card, int activePlayer, boolean throneRoom){
    System.out.println("played "+card.getName());

    if(phase.equals("actions") && card.isAction) conspiratorCounter++;
    if(supplyDecks.containsKey("peddler") && card.isAction) displaySupply("peddler");

    if(Empires.enchantressSwitch[activePlayer] && card.isAction){
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
      if(supplyDecks.containsKey("grandmarket")){
        supplyDecks.get("grandmarket").contraband=true;
        displaySupply("grandmarket");
      }
    }


    if(!throneRoom){
      matcards.add(card);
    }


    cardPlayed(activePlayer);
    if(!Empires.enchantressSwitch[activePlayer] || !card.isAction)
      card.work(activePlayer);
  }
  public boolean gainCard(String supplyName, int activePlayer){
    return gainCard(supplyName,activePlayer, "discard", false);
  }
  public boolean gainCard(String supplyName, int activePlayer, String where){
    return gainCard(supplyName,activePlayer,where,false);
  }
  //supplyName is type of supply
  //activeplayers is the player that gets the card
  //where is "topcard", "discard", "hand": where to put the card
  //if skipBuy=true, the player will not be changed money or a buy even if its the buy phase
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
           (phase=="buys" && money>=deck.getCost() && buys>0 && players.get(activePlayer).debt==0)
        || (phase=="gain" && gainLimit>=deck.getCost() && deck.card.debt==0)
        || (phase!="gain" && phase != "buys")
        || skipBuy ) ){
      DominionCard card=deck.topCard();

      if(phase=="buys" && !skipBuy){
      
        if(deck.contraband) return false;
        buys--;
        money-=deck.getCost();
        players.get(activePlayer).debt+=deck.card.debt;
        
        //** CARD SPECIFIC BUYING STUFF **//
        //extra gains from talisman
        if(!card.isVictory){
          for(int i=0;i<Prosperity.talismanCounter; i++) gainCard(supplyName, activePlayer, "discard", true);
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
        DominionCard hagglerCard;
        for(int i=0;i<Hinterlands.hagglerCounter;i++){
          gainLimit=deck.getCost()-1;
          server.displayComment(activePlayer, "gain a non-Victory card costing up to "+gainLimit);
          while(true){
            doWork("selectDeck",1,1,activePlayer,null);
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
       
      }

      if(phase=="gain"){
        selectedCards.add(card);
      }

      
      System.out.println("End: "+gameOver+" "+emptyPiles);
      if(deck.size()==0){
        if(card.getName()=="province" || card.getName()=="colony") gameOver=true;
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
        o.put(supplyDecks.get("foolsgold").card.getImage(),"image");

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

      gainCardNoSupply(card, activePlayer, where);
      
      updateSharedFields();
      displaySupply(supplyName);
      
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
        o.put(card.getImage(),"image");
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

    //put card on discard pile or (more rarely) top of deck
    if(where.equals("topcard")) player.deck.put(card);
    else if(where.equals("hand")) player.hand.add(card);
    else if(where.equals("discard")) player.disc.put(card);      
    else{
      System.out.println("I don't know where to put this card! "+where);
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
        o.put(card.getName(), "imagebutton");
      }else{
        o.put(card.getName(), "image");
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
  public void trashCard(DominionCard card, int ap){
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
        gainCard("gold", ap);
        if(counter==cards.size()) break;
      }
    }
    displayTrash();
  }
  public void returnToSupply(DominionCard card, int ap){
    String supplyName="";
    if(supplyDecks.containsKey(card.getName())){
      supplyName=card.getName();
    }else if(card.isKnight){
      supplyName="knight";
    }else if(card.isRuins) {
      supplyName = "ruins";
    }else if(card.isCastle){
      supplyName="castle";
    }else if(card.getName().equals("plunder")){
      supplyName="encampment";
    }else if(card.getName().equals("emporium")){
      supplyName="patrician";
    }else if(card.getName().equals("bustlingvillage")){
      supplyName="settlers";
    }else if(card.getName().equals("rocks")){
      supplyName="catapult";
    }else{
      trashCard(card, ap);
      return;
    }
    supplyDecks.get(supplyName).put(card);
  }

  //handles what happens if the player clicks on a button
  private boolean buttonManager(String input, int activePlayer){
    DominionCard card;
    DominionPlayer player;

    if(input.equals("buys")){
      return true;
    }else if(input.equals("actions")){
      changePhase("buys");
    }else if(input.equals("treasures")){
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

    //check if any cards do things at the end of the turn
    ArrayList<DominionCard> schemeCards=new ArrayList<>();    
    for(ListIterator<DominionCard> it=matcards.listIterator(); it.hasNext(); ){
      card=it.next();
      if(!card.getName().equals("scheme") && card.cleanup(activePlayer,players.get(activePlayer))){
        it.remove();
      }else if(card.getName().equals("scheme")){
        schemeCards.add(card);
      }
    }
    //now use scheme to put a card on top of deck
    //the reason we put scheme cards in their own list is that scheme removes items from matcards
    for(DominionCard card2 : schemeCards ){
      card2.cleanup(activePlayer,players.get(activePlayer));
    }
        
    //put remaining matcards on discard pile and clear the mat
    players.get(activePlayer).disc.put(matcards);
    matcards.clear();
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
    

    //check if the player put an outpost on their duration mat, and didn't play an outpost last time
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
  }
  private void endGame(){
    String temp;
    PairList<String,String> points=new PairList<>();
    String name;
    
    for(int i=0;i<players.size();i++){
      temp=players.get(i).victoryPoints();
      name=players.get(i).getName();
      points.put(name,temp);
    }
    points.sortByValue();
    server.showScores(points);
    
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
        o.put(card.getImage(),"image");
        if(attackCard != null) o.put(attackCard.getImage(),"image");
        if(optionPane(activePlayer,o).equals("Reveal")){
          out.add(card.getName());
        }
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
  //puts a card anywhere in the deck
  public void putAnywhere(int activePlayer, DominionCard card){
    DominionPlayer player=players.get(activePlayer);
    String [] options=new String[0];
    OptionData o=new OptionData(options);
    o.put("Choose the position to put the card (0 is top)","text");
    for(int i=0;i<player.deck.size()+1;i++) o.put(Integer.toString(i),"textbutton");
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
        o.put(cards.get(i).getImage(),"imagebutton");          
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
    gainSpecial(activePlayer, c -> cost2(c)==val && c.debt==0);

  }
  //find out if there are any cards in the supply that we can gain
  boolean isValidSupply(int activePlayer, Predicate<DominionCard> tester){
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
  void gainSpecial(int ap, Predicate<DominionCard> tester){
    gainSpecial(ap, tester, "discard");
  }
  void gainSpecial(int ap, Predicate<DominionCard> tester, String where){
    //quit if no valid card to gain
    if (!isValidSupply(ap, tester)) return;
    while (true) {
      doWork("selectDeck", 1, 1, ap);
      Dominion.SupplyDeck deck = supplyDecks.get(selectedDeck);
      if (tester.test(deck.card) && deck.size() > 0){
        gainCard(selectedDeck, ap, where, true);
        break;
      }
      selectedCards.clear();
    }
  }
  //a typical request for the player to do something
  public void doWork(String p, int min, int max, int activePlayer){
    doWork(p,min,max,activePlayer, null);
  }
  public void doWork(String p, int min, int max, int activePlayer, DominionCard card){
    minSelection=min;
    if(!p.equals("gain") && !p.equals("selectDeck") && !p.equals("selectDeck2")
            && players.get(activePlayer).hand.size()==0) return;
    if(p.equals("gain")){
      server.displayComment(activePlayer, "gain a card costing up to "+gainLimit);
    }
    if(max<=0) max=1;
    maxSelection=max;
    changePhase(p);
    work(activePlayer, card);
    displayPlayer(activePlayer);
  }
//  //***PRIVATE VARIABLES***///
  public String getPhase(){
    return phase;
  }
  
//  //***STUFF WHICH GETS STATUS OF GAME***///
  public ArrayList<Deck.SupplyData> supplyData(){
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
  public void updateSharedFields(){
    for( PlayerInterface connection : server.connections){  
      connection.updateSharedFields(actions,money,buys,Prosperity.tradeRouteCards.size(),potions);
    }
  }
  public void changePhase(String newPhase){
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
    
    public SupplyDeck(String name){
      this.name=name;

      if(name.equals("ruins") || name.equals("knight") || name.equals("encampment")
              || name.equals("patrician") || name.equals("settlers") || name.equals("castle")
              || name.equals("catapult")) varied=true;
      else varied=false;

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
        } else if (name.equals("rats")){
          nCards=20;
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
        }else if(name.equals("encampment")){
          for(int i=0; i<5; i++) put(cardFactory("plunder", "Empires"));
          for(int i=0; i<5; i++) put(cardFactory("encampment"));
        }else if(name.equals("patrician")){
          for(int i=0; i<5; i++) put(cardFactory("emporium", "Empires"));
          for(int i=0; i<5; i++) put(cardFactory("patrician"));
        }else if(name.equals("settlers")){
          for(int i=0; i<5; i++) put(cardFactory("bustlingvillage", "Empires"));
          for(int i=0; i<5; i++) put(cardFactory("settlers"));
        }else if(name.equals("catapult")){
          for(int i=0; i<5; i++) put(cardFactory("rocks", "Empires"));
          for(int i=0; i<5; i++) put(cardFactory("catapult"));
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

        if(size()>0) card = get(0);
        else card=null;

      }
      if(card==null) backImage=Deck.blankBack;
      else backImage=card.getImage();

    }
    public int getCost(){
      if(name.equals("peddler")) {
        int temp = 0;
        return cost2(card) - 2 * conspiratorCounter;
      }else if(card==null){
        return 0;
      }else{
        return cost2(card);
      }
    }
    public String getName(){return name;}
    public Deck.SupplyData makeData(){
      return new Deck.SupplyData(size(), backImage, getCost(), name, embargo, contraband);
    }
    @Override
    public DominionCard topCard(){
      DominionCard out=removeFirst();
      if(varied){
        card=get(0);
        backImage=card.getImage();
      }
      return out;
    }
    
  }

    //****CARD STUFF***//
  private ArrayList<String> randomSupply(){
    ArrayList<String> allCards=new ArrayList<>();
    
    for(Map.Entry<String,Expansion> entry : expansions.entrySet()){
      Collections.addAll(allCards,entry.getValue().cards);  
    }
    
    boolean [] included=new boolean[allCards.size()];
    Arrays.fill(included,false);
    
    Random ran=new Random();
    ArrayList<String> out=new ArrayList<>();
    int i;
    while(out.size()<10){
      i=ran.nextInt(allCards.size());
      if(!included[i]){
        included[i]=true;
        out.add(allCards.get(i));
      }
    }
    //add the bane card from young witch
    int cost;
    if(out.contains("youngwitch")){
      while(true){
        i=ran.nextInt(allCards.size());
        if(!included[i]){
          cost=cost2(cardFactory(allCards.get(i)));
          if(cost>=2 && cost<=3){
            included[i]=true;
            out.add(allCards.get(i));
            Cornucopia.bane=allCards.get(i);
            break;
          }
        }
      }
    }      
      
    Collections.sort(out, new Comparator<String>(){
        public int compare(String x, String y){
          return cost2(cardFactory(x))-cost2(cardFactory(y));
        }
    });
    return out;
  }

  //a card factory for when the expansion is already known
  public DominionCard cardFactory(String cardname, String expansion){
    Class c=null;
    
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
  public DominionCard cardFactory(String cardname){
    for(Map.Entry<String,Expansion> entry : expansions.entrySet()){
      if(entry.getValue().hasCard(cardname)){
        return cardFactory(cardname,entry.getKey());
      }
    }
    return new DominionCard(cardname);
  }
}
