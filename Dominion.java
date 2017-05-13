import java.util.*;


public class Dominion{

  public DominionServer server;
  public ArrayList<DominionPlayer> players;
  public LinkedHashMap<String, SupplyDeck> supplyDecks;
  public ArrayList<DominionCard> matcards;
  public Deck<DominionCard> trash;
  public HashSet<String> startingOptions;

  public int money,actions,buys,potions;
  private int nPlayers;
  public int emptyPiles;
  private boolean gameOver;
  private String phase;

  public int gainLimit;
  public int minGain=0;

  //stuff for selections
  //cards that have been selected for trashing/discarding etc go here in case they need to be looked at by e.g. a forge
  public String selectedDeck;
  private int maxSelection=10,minSelection=10;
  public ArrayList<DominionCard> selectedCards=new ArrayList<>(); 

  //specific card related counters
  public int bridgeCounter=0; //counts cost reduction, also does highway and brige troll
  public int quarryCounter=0; 
  public int conspiratorCounter=0; //counts total actions played, also does peddler
  public int coppersmithCounter=0;
  public int merchantCounter=0;
  private ArrayList<String> smugglerCards1=new ArrayList<>();
  public ArrayList<String> smugglerCards2=new ArrayList<>();
  private boolean outpost;
  public boolean victoryBought=false; //did we gain a victory card this turn (for treasury)
  public HashSet<String> tradeRouteCards;
  public int talismanCounter=0;
  public HashSet<String> contrabandDecks=new HashSet<>();
    
  private HashMap<String,Expansion> expansions=new HashMap<>();
  
 //**********8STUFF WHICH SETS UP THE GAME*********//
  public Dominion(ArrayList<String> names, DominionServer tserver){  
    server=tserver;   

    expansions.put("Core",new Core(this));
    expansions.put("Intrigue",new Intrigue(this));
    expansions.put("Seaside",new Seaside(this));
    expansions.put("Prosperity",new Prosperity(this));
    int startingPlayer=startGame(names);      
    server.initialize(supplyData(), playerData(),startingPlayer, startingOptions);    
    work(startingPlayer);
    
  }
  
  public void reset(ArrayList<String> names){
    int startingPlayer=startGame(names);
    trash.clear();
    resetCardCounters();
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
      //for(int j=0;j<3;j++) players.get(i).deck.put(cardFactory("nativevillage"));
    }
    nPlayers=names.size();

    //supplies
    startingOptions=new HashSet<>(); 
    supplyDecks=new LinkedHashMap<>();    
    ArrayList<String> supplies=randomSupply();
    supplies.add("nativevillage");
    System.out.println(supplies);
    boolean usePlatinums=Expansion.usePlatinums(supplies);
    for(String s : supplies){
      if(s.equals("pirateship")) startingOptions.add("pirateship");
      if(s.equals("island")) startingOptions.add("island");
      if(s.equals("nativevillage")) startingOptions.add("nativevillage");
      if(cardFactory(s).isDuration) startingOptions.add("duration");
      if(s.equals("traderoute")) startingOptions.add("traderoute");
      if(Expansion.vicTokens.contains(s)) startingOptions.add("victorytokens");
    }
    String [] tcards={"copper","silver","gold","estate","duchy","province","curse"};
    ArrayList<String> cards=new ArrayList<String>(Arrays.asList(tcards));
    if(usePlatinums){
      cards.add(3,"platinum");
      cards.add(7,"colony");
    }
    cards.addAll(supplies);
    
    for(int i=0;i<cards.size();i++){
      supplyDecks.put(cards.get(i), new SupplyDeck( cards.get(i) ));
    } 
    
    //trash
    trash=new Deck<>();
    trash.backImage=Deck.blankBack;
    trash.faceup=true;  
    
    //specific card related stuff
    tradeRouteCards=new HashSet<>();
    
    Random ran=new Random();
    int startingPlayer=ran.nextInt(players.size());  
    return startingPlayer;
  }
  
  ///***STUFF WHICH PROGRESSES THE GAME***///
  public void work(int t){
  
    int activePlayer=t;
    String input;
    DominionCard card;
    boolean doneSelection=false;
    if(maxSelection<=0) maxSelection=1;


    while(true){

      input=server.getUserInput(activePlayer);
      
      System.out.println(input);
      
      //if input is a number, it represent the playing of a card from the active player's hand
      if(input.charAt(0)<='9' && input.charAt(0)>='0'){

        //always happens unless revealing a card
        if(!phase.equals("reveal"))
          card=players.get(activePlayer).hand.remove(Integer.parseInt(input));
        else
          card=players.get(activePlayer).hand.get(Integer.parseInt(input));
        
        //action specific stuff
        if(phase=="actions" && card.isAction){
          actions--; 
          conspiratorCounter++;
        }

        //if the card is going to go to matcard
        //display is done in playCard
        if(phase=="actions" || phase=="buys") playCard(card, activePlayer);

        //check if we just ended the actions phase
        if(phase=="actions" && ( card.isMoney || actions==0)){
          changePhase("buys");
        }
        //discard specific stuff
        if(phase=="discard") players.get(activePlayer).disc.put(card);
        if(phase=="trash") trash.put(card);
        if(phase=="topdeck") players.get(activePlayer).deck.put(card);
        
        //generic selection behavour
        if(phase=="discard" || phase.equals("trash") || phase=="topdeck" || phase=="select" || phase=="reveal"){
          selectedCards.add(card);
          displayPlayer(activePlayer);
        }
          
      }else if(input.charAt(0)=='G'){
      //if first character is G, we gained a card
        if(phase=="buys" || phase=="gain") gainCard(input.substring(1),activePlayer);
        else if(phase=="selectDeck"){
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
      if(phase=="buys" && (doneSelection || buys==0)){
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
      money+=card.value;
      actions+=card.actions;
      buys+=card.buys;
      if(card.getName().equals("silver")){
        money+=merchantCounter;
       merchantCounter=0;
      }
      if(card.getName().equals("copper")) money+=coppersmithCounter;
      
      players.get(activePlayer).drawToHand(card.cards);
      if(!throneRoom){
        matcards.add(card);
      }
      cardPlayed(activePlayer);
      card.work(activePlayer);
  }
  public boolean gainCard(String supplyName, int activePlayer){
    return gainCard(supplyName,activePlayer, "discard");
  }
  public boolean gainCard(String supplyName, int activePlayer, String where){
    DominionPlayer player=players.get(activePlayer);
    SupplyDeck deck=supplyDecks.get(supplyName);

    //can never gain a card if the supply is empty
    //if phase is buys you need a buy and enough money
    //if phase is gain you need the "gainLimit" set by the card you played to be big
    //if phase is something else there are no conditions since an action card got you here
    if(deck.size()>0 && ( 
           (phase=="buys" && money>=deck.getCost() && buys>0) 
        || (phase=="gain" && gainLimit>=deck.getCost() && minGain<=deck.getCost()) 
        || (phase!="gain" && phase != "buys") ) ){
      DominionCard card=deck.topCard();

      if(phase=="buys"){
      
        if(contrabandDecks.contains(supplyName)) return false;
        buys--;
        money-=deck.getCost();
        
        //extra gains from talisman
        if(!card.isVictory){
          phase="actions"; //so buy and money aren't depleted
          for(int i=0;i<talismanCounter; i++) gainCard(supplyName, activePlayer, where);
          phase="buys";
        }
                
      }

      if(phase=="gain"){
        selectedCards.add(card);
      }

      //check for embargo-type effects
      if(phase.equals("buys")){
        phase="actions";
        for(int i=0;i<deck.curses;i++){
          gainCard("curse",activePlayer);
        }
        phase="buys";
      }
      
      System.out.println("End: "+gameOver+" "+emptyPiles);
      if(deck.size()==0){
        if(card.getName()=="province" || card.getName()=="colony") gameOver=true;
        else if(emptyPiles<2) emptyPiles++;
        else gameOver=true;
      }

      //stuff related to specific cards      
      if(supplyDecks.get(supplyName).getCost()<=6) smugglerCards1.add(supplyName);
      if(phase.equals("buys") && card.isVictory) victoryBought=true;
      if(card.isVictory) tradeRouteCards.add(card.getName());

      //play reactions
      OptionData o;     
      ArrayList<String> reactions=reactionReveal(player.hand,activePlayer,2);
      for(String r : reactions){
        if(r.equals("watchtower")){
          String [] options = {"Top Deck","trash"};
          o=new OptionData(options);
          o.put(card.getImage(),"image");
          if(optionPane(activePlayer,o).equals(options[0]))
            player.deck.put(card);
          else{
            trash.put(card);
            displayTrash();
          }
        }
      }

      //resolve ongain effects
      card.onGain(activePlayer);
      
      //put card on discard pile or (more rarely) top of deck
      if(where.equals("topcard")) player.deck.put(card);
      if(where.equals("hand")) player.hand.add(card);
      else player.disc.put(card);      

      cardGained(activePlayer,supplyName);
      
      return true;
    }
    return false;
  }
  public boolean buttonManager(String input, int activePlayer){
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
        if(card.isMoney){
          it.remove();
          playCard(card,activePlayer);
        }
      }
    }else if(input.equals("discard") || input.equals("trash") || input.equals("select") || input.equals("reveal")){
      return true;
    }
    return false;
  }
  public void changePhase(String newPhase){
    server.changePhase(phase,newPhase);
    phase=newPhase;
  }
  public int endTurn(int activePlayer){
    //****end the previous turn***//
    DominionCard card;

    //check if any cards do things at the end of the turn    
    for(ListIterator<DominionCard> it=matcards.listIterator(); it.hasNext(); ){
      card=it.next();
      if(card.cleanup(activePlayer,players.get(activePlayer))){
        it.remove();
      }
    }
    
    //put remaining matcards on discard pile and clear the mat
    players.get(activePlayer).disc.put(matcards);
    matcards.clear();
    
    if(gameOver) endGame();
    

    int newPlayer;
    //check if the player put an outpost on their duration mat, and didn't play an outpost last time
    if(outpost){
      outpost=false;
    }else{
      for(DominionCard card2 : players.get(activePlayer).duration){
        if(card2.getName().equals("outpost")){
          outpost=true;
          break;
        }
      }
    }
    //if there is an outpost on the duration mat, that player gets to go again
    if(outpost){
      newPlayer=activePlayer;
      players.get(activePlayer).disc.put(players.get(activePlayer).hand);
      players.get(activePlayer).hand.clear();
      players.get(activePlayer).drawToHand(3);
    }else{
      players.get(activePlayer).endTurn();       
      newPlayer=(activePlayer+1)%players.size();
    }
    
    //start the next turn
    changePhase("actions");
    money=0;
    buys=1;
    actions=1; 
    resetCardCounters();
    
    //play duration cards
    for(DominionCard card2 : players.get(newPlayer).duration){
      for(int i=0;i<card2.throneroomed;i++) card2.duration(newPlayer);
      card2.throneroomed=1;
      matcards.add(card2);
      players.get(newPlayer).duration.remove(card2);
    }
    players.get(newPlayer).duration.clear();

    //pass on this info to board
    server.changePlayer(activePlayer,players.get(activePlayer).makeData(),newPlayer,players.get(newPlayer).makeData());
    updateSharedFields();

    
    return newPlayer;
    
  }
  //resets specific card-related stuff
  public void resetCardCounters(){
    if(bridgeCounter>0 || quarryCounter>0){
      bridgeCounter=0;
      quarryCounter=0;
      displaySupplies();
    } 
    conspiratorCounter=0;  
    merchantCounter=0;
    coppersmithCounter=0;
    
    smugglerCards2=new ArrayList<>(smugglerCards1);
    smugglerCards1.clear();
    
    victoryBought=false;
    talismanCounter=0;
    contrabandDecks.clear();
  }
  public void endGame(){
    int temp;
    PairList<String,Integer> points=new PairList<>();
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
  
  public ArrayList<String> reactionReveal(Collection<DominionCard> hand, int activePlayer, int type){
    DominionCard card;
    String [] options={"Reveal","Pass"};
    OptionData o=new OptionData(options);
    ArrayList<String> out=new ArrayList<>();

    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
      card=it.next();
      if( (type==1 && card.isReaction1) || (type==2 && card.isReaction2)){
        o.put(card.getImage(),"image");
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
    return server.getUserInput(activePlayer);
  }
  //always use this to get the cost of cards
  public int cost2(DominionCard card){
    if(card.isAction) return Math.max(card.cost-bridgeCounter-quarryCounter,0);
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
    String [] options;
    OptionData o;
    DominionPlayer player=players.get(activePlayer);
    String input;
    
    while(cards.size()>1){
      options=new String[0];
      o=new OptionData(options);
      for(int i=0;i<cards.size();i++){
        o.put(cards.get(i).getImage(),"imagebutton");          
      }
      server.optionPane(activePlayer,o);
      input=server.getUserInput(activePlayer);
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
  }
  //"gain a card costing exactly"
  public void controlledGain(int activePlayer){
    //find out if there are any cards in the supply that we can gain
    boolean canGain=false;
    for(Map.Entry <String,SupplyDeck> entry : supplyDecks.entrySet()){
      if(entry.getValue().getCost()>=minGain && entry.getValue().getCost()<=gainLimit){
        canGain=true;
        break; 
      }     
    }
    if(!canGain) return;
    
    //if so, let the player pick one
    changePhase("gain");
    Dominion.this.work(activePlayer);
    minGain=0;     
  }
  //a typical request for the player to do something
  public void doWork(String p, int min, int max, int activePlayer){
    minSelection=min;
    if(max==0) max=1;
    maxSelection=max;
    changePhase(p);
    work(activePlayer);
    displayPlayer(activePlayer);
    if(p.equals("trash")) displayTrash();
  }
  //a variant that doesn't change the phase, important whenever there is a mask
  public void doWork(int min, int max, int activePlayer){
    minSelection=min;
    maxSelection=max;
    if(max==0) max=1;
    work(activePlayer);
    displayPlayer(activePlayer);
  }
//  //***SETTING PRIVATE VARIABLES***///

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
  
  //some simple wrappers for server functions 
  public void displayPlayer(int i){
    for( DominionServer.HumanPlayer connection : server.connections){
      connection.displayPlayer(i,players.get(i).makeData());
    }
  }
  public void updateSharedFields(){
    for( DominionServer.HumanPlayer connection : server.connections){  
      connection.updateSharedFields(actions,money,buys,tradeRouteCards.size(),potions);
    }
  }
  public void displaySupply(Deck.SupplyData data){
    for( DominionServer.HumanPlayer connection : server.connections){  
      connection.displaySupply(data);
    }
  }
  public void displaySupply(String name){
    for( DominionServer.HumanPlayer connection : server.connections){  
      connection.displaySupply(supplyDecks.get(name).makeData());
    }
  }
  public void displaySupplies(){
    for(Map.Entry<String,SupplyDeck> entry : supplyDecks.entrySet()){
      displaySupply(entry.getValue().makeData());
    }
  }
  public void cardPlayed(int activePlayer){
    for( DominionServer.HumanPlayer connection : server.connections){
      connection.cardPlayed(activePlayer,players.get(activePlayer).makeData(),matcards);      
    }
    updateSharedFields();
  }
  public void cardGained(int activePlayer, String supplyDeck){
    displayPlayer(activePlayer);
    displaySupply(supplyDeck);
    updateSharedFields();
  }
  public void displayTrash(){
    for( DominionServer.HumanPlayer connection : server.connections){
      connection.displayTrash(trash.makeData());
    }
  }    
//  
  //****INNER CLASSES***///
  //can't be static because it uses cardFactory
  class SupplyDeck extends Deck<DominionCard>{
    private int cost;
    private String name;
    public int curses=0;
    private DominionCard card;
    public SupplyDeck(String name){
      this.name=name;
      card=cardFactory(name);
      cost=card.cost;
      backImage=card.getImage();
      
      int nCards;
      if(card.getName()=="copper" || card.getName()=="silver" || card.getName()=="gold" || card.getName()=="platinum"){
        nCards=30;
      }else if(card.isVictory){
        nCards=Math.min(4*players.size(),12);
      }else if(card.getName()=="curse"){
        nCards=10*(players.size()-1);
      }else{
        nCards=10;
      }
      for(int i=0;i<nCards;i++){
        add(cardFactory(name));
      }
    }
    public int getCost(){return cost2(card);}
    public String getName(){return name;}
    public Deck.SupplyData makeData(){
      return new Deck.SupplyData(size(), backImage, getCost(), name);
    }
    
  }

    //****CARD STUFF***//
  public ArrayList<String> randomSupply(){
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
    Collections.sort(out, new Comparator<String>(){
        public int compare(String x, String y){
          return cost2(cardFactory(x))-cost2(cardFactory(y));
        }
    });
    return out;
  }
  
  public DominionCard cardFactory(String cardname){
    Class c=null;
    
    for(Map.Entry<String,Expansion> entry : expansions.entrySet()){
      if(entry.getValue().hasCard(cardname)){
        try{
          c=Class.forName(entry.getKey()+"$"+cardname.substring(0,1).toUpperCase()+cardname.substring(1));
          return (DominionCard) c.getConstructors()[0].newInstance(entry.getValue());      
        }
        catch(ClassNotFoundException e){
          return new DominionCard(cardname);
        }catch(Exception e){
          e.printStackTrace();
        }
      }
    }
    return new DominionCard(cardname);
  }
}
