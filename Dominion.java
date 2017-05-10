import java.util.*;


public class Dominion{

  private DominionServer server;
  public ArrayList<DominionPlayer> players;
  private LinkedHashMap<String, SupplyDeck> supplyDecks;
  public ArrayList<DominionCard> matcards;
  private Deck<DominionCard> trash;

  private int money,actions,buys;
  private int nPlayers;
  private int emptyPiles;
  private boolean gameOver;
  private String phase;

  private int gainLimit;
  private int minGain=0;

  //stuff for selections
  //cards that have been selected for trashing/discarding etc go here in case they need to be looked at by e.g. a forge
  private String selectedDeck;
  private int maxSelection=10,minSelection=10;
  private ArrayList<DominionCard> selectedCards=new ArrayList<>(); 

  //specific card related counters
  private int merchantCounter;
  private int bridgeCounter; //counts cost reduction, also does highway and brige troll
  private int conspiratorCounter; //counts total actions played, also does peddler
  private int coppersmithCounter;
    
  
  public static String [] coreCards={"adventurer", "bureaucrat", "cellar", "chancellor", "chapel", 
      "councilroom", "feast", "festival", "laboratory", "library", "market", "militia", 
      "mine","moneylender","remodel", "smithy", "spy", "thief", "throneroom", "village", "witch",
      "woodcutter","workshop","gardens","harbinger","merchant","vassal","bandit","poacher","sentry","artisan"};
  public static String [] intrigueCards={"courtyard","lurker","masquerade","shantytown","pawn",
      "steward","swindler","wishingwell","baron","bridge","conspirator","diplomat","ironworks",
      "mill","miningvillage","secretpassage","courtier","duke","minion","patrol","replace",
      "torturer","tradingpost","upgrade","nobles","harem","secretchamber","greathall",
      "coppersmith","scout","saboteur","tribute"};
  
 //**********8STUFF WHICH SETS UP THE GAME*********//
  public Dominion(ArrayList<String> names, DominionServer tserver){  
    server=tserver;        
    int startingPlayer=startGame(names);      
    server.initialize(supplyData(), playerData(),startingPlayer);    
    work(startingPlayer);
  }
  
  public void reset(ArrayList<String> names){
    int startingPlayer=startGame(names);
    resetCardCounters();
    server.reset(supplyData(), playerData(), startingPlayer);
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

    resetCardCounters();
    
    //players
    players=new ArrayList<DominionPlayer>();    
    //make players
    for(int i=0;i<names.size();i++){
      players.add(new DominionPlayer(names.get(i)));
//      for(int j=0;j<3;j++) players.get(i).deck.put(cardFactory("secretchamber"));
    }
    nPlayers=names.size();

    //supplies
    supplyDecks=new LinkedHashMap<>();    
    String [] tcards={"copper","silver","gold","estate","duchy","province","curse"};
    ArrayList<String> cards=new ArrayList<String>(Arrays.asList(tcards));
    cards.addAll(randomSupply());
    
    for(int i=0;i<cards.size();i++){
      supplyDecks.put(cards.get(i), new SupplyDeck( cards.get(i) ));
    } 
    
    //trash
    trash=new Deck<>();
    trash.backImage=Deck.blankBack;
    trash.faceup=true;  
    
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

    while(true){

      input=server.getUserInput(activePlayer);
      
      System.out.println(input);
      
      //if input is a number, it represent the playing of a card from the active player's hand
      if(input.charAt(0)<='9' && input.charAt(0)>='0'){

        //always happens
        card=players.get(activePlayer).hand.remove(Integer.parseInt(input));

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
        if(phase=="discard" || phase.equals("trash") || phase=="topdeck" || phase=="select"){
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
      if(selectedCards.size()>=maxSelection || (selectedCards.size()>=minSelection && doneSelection)){
        doneSelection=false;
        break;
      }
    }
    System.out.println("Exited loop");
  }
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
      if(phase=="buys"){
        buys--;
        money-=deck.getCost();
      }
      DominionCard card=deck.topCard();

      if(phase=="gain"){
        selectedCards.add(card);
      }
            
      //put card on discard pile or (more rarely) top of deck
      if(where.equals("topcard")) player.deck.put(card);
      if(where.equals("hand")) player.hand.add(card);
      else player.disc.put(card);
      
      System.out.println("End: "+gameOver+" "+emptyPiles);
      if(deck.size()==0){
        if(card.getName()=="province" || card.getName()=="colony") gameOver=true;
        else if(emptyPiles<2) emptyPiles++;
        else gameOver=true;
      }
      
      server.cardGained(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),deck.makeData());
      
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
    }else if(input.equals("discard") || input.equals("trash") || input.equals("select")){
      return true;
    }
    return false;
  }
  public void changePhase(String newPhase){
    server.changePhase(phase,newPhase);
    phase=newPhase;
  }
  public int endTurn(int activePlayer){
    int newPlayer=(activePlayer+1)%players.size();
    players.get(activePlayer).disc.put(matcards);
    players.get(activePlayer).endTurn();   
    if(gameOver) endGame();
    money=0;
    buys=1;
    actions=1; 
    matcards.clear();
    changePhase("actions");

    resetCardCounters();
    
    //pass on this info to board
    server.changePlayer(activePlayer,players.get(activePlayer).makeData(),newPlayer,players.get(newPlayer).makeData());
    
    return newPlayer;
    
  }
  public void resetCardCounters(){
    merchantCounter=0;
    if(bridgeCounter>0){
      bridgeCounter=0;
      displaySupplies();
    } 
    conspiratorCounter=0;  
    coppersmithCounter=0; 
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
  
  public ArrayList<String> reaction1Reveal(Collection<DominionCard> hand, int activePlayer){
    DominionCard card;
    String [] options={"Reveal","Pass"};
    OptionData o=new OptionData(options);
    ArrayList<String> out=new ArrayList<>();

    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
      card=it.next();
      if(card.isReaction1){
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
    return Math.max(card.cost-bridgeCounter,0);
  }
  //puts a card anywhere in the deck
  public void putAnywhere(int activePlayer, DominionCard card){
    DominionPlayer player=players.get(activePlayer);
    String [] options=new String[0];
    OptionData o=new OptionData(options);
    o.put("Choose the position to put the card (1 is top)","text");
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
    maxSelection=max;
    changePhase(p);
    work(activePlayer);
//    displayPlayer(activePlayer);
//    if(p.equals("trash")) displayTrash();
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
    server.updateSharedFields(actions,money,buys);
  }
  public void displaySupplies(){
    for(Map.Entry<String,SupplyDeck> entry : supplyDecks.entrySet()){
      server.displaySupply(entry.getValue().makeData());
    }
  }
  public void cardPlayed(int activePlayer){
    for( DominionServer.HumanPlayer connection : server.connections){
      connection.cardPlayed(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),matcards);
    }
  }
  public void displayTrash(){
    for( DominionServer.HumanPlayer connection : server.connections){
      connection.displayTrash(trash.makeData());
    }
  }    
//  
  //****INNER CLASSES***///
  class SupplyDeck extends Deck<DominionCard>{
    private int cost;
    private String name;
    public SupplyDeck(String name){
      this.name=name;
      DominionCard card=cardFactory(name);
      cost=card.cost;
      backImage=card.getImage();
      
      int nCards;
      if(card.getName()=="copper" || card.getName()=="silver" || card.getName()=="gold" || card.getName()=="platinum"){
        nCards=30;
      }else if(card.isVictory){
        nCards=1;//=Math.min(4*players.size(),12);
      }else if(card.getName()=="curse"){
        nCards=10*(players.size()-1);
      }else{
        nCards=10;
      }
      for(int i=0;i<nCards;i++){
        add(cardFactory(name));
      }
    }
    public int getCost(){return Math.max(cost-bridgeCounter,0);}
    public String getName(){return name;}
    public Deck.SupplyData makeData(){
      return new Deck.SupplyData(size(), backImage, getCost(), name);
    }
    
  }
  
  public static class OptionData extends PairList<String,String>{
    public OptionData(String input){
      String [] parts=input.split("@");
      int size=Integer.parseInt(parts[0]);
      String [] t=parts[1].split("!");
      for(int i=0; i<size; i++){
        names.add(t[i]);
      }
      t=parts[2].split("!");
      for(int i=0; i<size; i++){
        types.add(t[i]);
      }
    }
    public OptionData(String [] options){
      for(int i=0;i<options.length;i++){
        names.add(options[i]);
        types.add("textbutton");
      }
    }
    public OptionData(){}
  }

  
    //****CARD STUFF***//
  public ArrayList<String> randomSupply(){
    ArrayList<String> allCards=new ArrayList<>(coreCards.length);
    
    Collections.addAll(allCards,coreCards);
    Collections.addAll(allCards,intrigueCards);
    
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
    try{
      c=Class.forName("Dominion$"+cardname.substring(0,1).toUpperCase()+cardname.substring(1));
      return (DominionCard) c.getConstructors()[0].newInstance(this);      
    }
    catch(ClassNotFoundException e){
    }catch(Exception e){
      e.printStackTrace();
    }
    return new DominionCard(cardname);
  }
  private abstract class Attack extends DominionCard{
    protected String attackPhase="other";
    protected String comment="";
    public Attack(String name){
      super(name);
      isAction=true;
      isAttack=true;
    }
    @Override
    public final void work(int activePlayer){
      subWork(activePlayer);
      changePhase(attackPhase);

      ArrayList<DominionCard> oldMat=new ArrayList<>(matcards);
      int oldPlayer=activePlayer;
      int oldMoney=money;
      int oldActions=Dominion.this.actions;
      int oldBuys=Dominion.this.buys;

      server.displayComment(activePlayer,comment);
      ArrayList<String> reactions;
      DominionPlayer victim;

      for(int i=(activePlayer+1)%players.size(); i!=activePlayer; i=(i+1)%players.size()){

        victim=players.get(i);
        server.changePlayer(oldPlayer,players.get(oldPlayer).makeData(),i%players.size(),victim.makeData());

        //resolve possible reactions
        boolean moat=false;
        reactions=reaction1Reveal(victim.hand,i);
        for(String r: reactions){
          if(r.equals("moat")){ moat=true;
          }else if(r.equals("diplomat")){
            if(victim.hand.size()>=5){
              victim.drawToHand(2);
              doWork("discard",3,3,i);
              selectedCards.clear();
              changePhase(attackPhase);
              displayPlayer(i);
            }
          }else if(r.equals("secretchamber")){
            victim.drawToHand(2);
            doWork("topdeck",2,2,i);
            selectedCards.clear();
            changePhase(attackPhase);
            displayPlayer(i);            
          }
        }
        if(moat){
          oldPlayer=i;
          continue;
        }
        
        server.displayComment(i,comment);
        subStep(i,activePlayer);
        selectedCards.clear();
        server.displayComment(i,"");
        oldPlayer=i;
      }
      server.changePlayer(oldPlayer,players.get(oldPlayer).makeData(),activePlayer,players.get(activePlayer).makeData());
      money=oldMoney;
      Dominion.this.actions=oldActions;
      Dominion.this.buys=oldBuys;
      matcards=oldMat;       
      
      cleanup(activePlayer);

//      server.updateSharedFields(Dominion.this.actions,money,Dominion.this.buys);
      cardPlayed(activePlayer);
      server.displayComment(activePlayer,"");
      changePhase("actions");
      selectedCards.clear();
    }
    public void subStep(int x, int y){}
    public void subWork(int x){}
    public void cleanup(int x){}
  }
  private abstract class RegularCard extends DominionCard{
    protected String comment="";
    public RegularCard(String name){
      super(name);
      isAction=true;
    }
    @Override
    public final void work(int activePlayer){
      server.displayComment(activePlayer,comment);

      subWork(activePlayer);

      if(maxSelection<=0) maxSelection=1;
      changePhase("actions");
      server.displayComment(activePlayer,"");
      selectedCards.clear();
    }
    public void subWork(int activePlayer){}
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
          card=players.get(activePlayer).getCard();
        }catch(OutOfCardsException e){
          return;
        }
        if(card.isMoney){
          money+=card.value;
          matcards.add(card);
          counter++;
        }else{
          players.get(activePlayer).disc.put(card);
        }
      }
      cardPlayed(activePlayer);
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
      gainLimit=5;
      doWork("gain",0,1,activePlayer);
      players.get(activePlayer).hand.add(players.get(activePlayer).disc.topCard());
      displayPlayer(activePlayer);
      selectedCards.clear();
      doWork("topdeck",1,1,activePlayer);
      
    }
    
  }
  private class Bandit extends Attack{
    public Bandit(){
      super("bandit");
      cost=5;
    }
    @Override
    public void subWork(int activePlayer){
      gainCard("gold",activePlayer);
    }
    @Override
    public void subStep(int victim, int attacker){
      DominionPlayer player=players.get(victim);
      DominionCard card1,card2;

      //handling cases where there is no card to get
      try{
        card1=player.getCard();
        
        try{
          card2=player.getCard();

          //if both cards can be trashed have to let player pick
          if(test(card1) && test(card2)){
            OptionData o=new OptionData(new String[0]);
            o.put(card1.getName(),"imagebutton");
            o.put(card2.getName(),"imagebutton");
            server.optionPane(attacker,o);
            String input=server.getUserInput(attacker);
            if(input.equals(card1.getName())){
              trash.put(card1);
              player.disc.put(card2);
            }else{
              trash.put(card2);
              player.disc.put(card1);
            }
          }else if(test(card1)){
            trash.put(card1);
            player.disc.put(card2);
          }else if(test(card2)){
            trash.put(card2);
            player.disc.put(card1);
          }else{
            player.disc.put(card2);
            player.disc.put(card1);        
          }
        //if there is no second card just result first card
        }catch(OutOfCardsException ex){
          if(test(card1)) trash.put(card1);
          else player.disc.put(card1);
        }
        
      }catch(OutOfCardsException ex){
        return;
      }
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
      gainCard("silver",activePlayer,"topcard");
      minSelection=1;
      maxSelection=1;
    }
    @Override
    public void subStep(int activePlayer, int attacker){

      boolean mask[];
      int firstVictory=0,count,j;
      LinkedList<DominionCard> hand;
      DominionCard card;

      count=0;
      j=0;
      
      //a custom mask creation because if there is only one victory card, we just topdeck it and are done
      hand=players.get(activePlayer).hand;
      mask=new boolean[hand.size()];
      Arrays.fill(mask,false);
      for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        card=it.next();
        if(card.isVictory){
          mask[j]=true;
          count++;
          firstVictory=j;
        }
        j++;
      }
      if(count==1){
        //we found the only victory card, skip selection step
        players.get(activePlayer).disc.put(hand.remove(firstVictory));
        displayPlayer(activePlayer);
      }else if(count>0){
        server.setMask(activePlayer, mask);
        displayPlayer(activePlayer);
        Dominion.this.work(activePlayer);
      }else{
        //no victory cards, nothing to be done
      }
    
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
      doWork("discard",0,1000,activePlayer);
      players.get(activePlayer).drawToHand(selectedCards.size());
      displayPlayer(activePlayer);
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
      server.optionPane(activePlayer,o);
      String input=server.getUserInput(activePlayer);
      DominionPlayer player=players.get(activePlayer);
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
      doWork("trash",0,4,activePlayer);
      displayTrash();
      displayPlayer(activePlayer);
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
      for(int i=activePlayer+1;i<activePlayer+players.size();i++){
        players.get( i%players.size() ).drawToHand(1);
        displayPlayer(i%players.size());
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
      DominionPlayer player=players.get(activePlayer);
      DominionCard card;
      
      if(player.disc.size()>0){
        OptionData o=new OptionData(new String[0]);
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          o.put(card.getName(),"imagebutton");
        }
        server.optionPane(activePlayer,o);
        String input=server.getUserInput(activePlayer);   
        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.deck.put(card);
            displayPlayer(activePlayer);
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
      gainLimit=5;
      doWork("gain",0,1,activePlayer);
      trash.put(matcards.remove(matcards.size()-1));
      displayTrash();
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
      DominionPlayer player=players.get(activePlayer);
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
          displayPlayer(activePlayer);
          server.optionPane(activePlayer,o);
          out=server.getUserInput(activePlayer);
          if(out.equals(options[0])) player.hand.add(card);
          else aside.add(card);
        }else{
          player.hand.add(card);
        }
        o.remove(card.getImage()); 
      }
      player.disc.put(aside);
      displayPlayer(activePlayer);
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
      server.displayComment(activePlayer,"Discard down to three cards");
      minSelection=players.get(activePlayer).hand.size()-3;
      maxSelection=minSelection;    
      if(maxSelection>0) Dominion.this.work(activePlayer);
      if(maxSelection==0){
        maxSelection=1;//note that if maxSelection is ever set to zero and work is called the program will crash
        return;
      }
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
      server.setMask(activePlayer,makeMask(players.get(activePlayer).hand));
      doWork("trash",0,1,activePlayer);
      displayTrash();
      int cost=selectedCards.get(0).cost;
      changePhase("selectDeck");
      
      DominionCard card;
      SupplyDeck deck;
      while(true){
        Dominion.this.work(activePlayer);
        deck=supplyDecks.get(selectedDeck);
        if(deck.size()==0) continue;
        
        card=deck.peek();
        if(card.isMoney && deck.getCost()<=cost2(selectedCards.get(0))+3){
          gainCard(deck.getName(),activePlayer,"hand");
//          players.get(activePlayer).hand.add(deck.topCard());
//          server.cardGained(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),deck.makeData());          
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
      server.setMask(activePlayer,makeMask(players.get(activePlayer).hand));
      doWork("trash",0,1,activePlayer);
      if(selectedCards.size()>0){
        money+=3;
        server.updateSharedFields(Dominion.this.actions,money,Dominion.this.buys);
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
      if(emptyPiles>0){
        doWork("discard",emptyPiles,Math.max(1,minSelection),activePlayer);
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
      doWork("trash",1,1,activePlayer);
      displayTrash();

      gainLimit=cost2(selectedCards.get(0))+2;
      doWork("gain",0,1,activePlayer);      
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
      DominionPlayer player=players.get(activePlayer);
      DominionCard card;
      String [] options={"Trash", "Discard", "Put back"};
      OptionData o=new OptionData(options);
      String input;
      
      for(int i=0;i<2;i++){
        try{
          card=player.getCard();
          o.put(card.getImage(),"image");
          server.optionPane(activePlayer,o);
          input=server.getUserInput(activePlayer);
          if(input.equals(options[0])){
            trash.put(card);
            displayTrash();
          }else if(input.equals(options[1])){
            player.disc.put(card);
          }else{
            cards.add(card);
          }
          displayPlayer(activePlayer);
          o.remove(card.getImage());
        }catch(OutOfCardsException ex){
          break;
        }
      }
      if(cards.size()==0) return;

      putBack(activePlayer,cards);      
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
        DominionCard card=players.get(activePlayer).getCard();
        o.put(card.getImage(),"image");
        server.optionPane(attacker,o);
        String result=server.getUserInput(attacker);
        if(result.equals(options[0])) players.get(activePlayer).deck.put(card);
        else players.get(activePlayer).disc.put(card);
        displayPlayer(activePlayer);
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
      DominionCard card=cardFactory("copper");
      for(int i=0;i<2;i++){
        try{
          card=players.get(victim).getCard();
        }catch(OutOfCardsException e){}
        
        cards.add(card);
        if(card.isMoney)
          o.put(card.getImage(),"imagebutton");
        else
          o.put(card.getImage(),"image");
      }
      o.put("Done","textbutton");
      server.optionPane(attacker,o);
      String out=server.getUserInput(attacker);
      if(out.equals(cards.get(0).getName())){
        players.get(victim).disc.put(cards.get(1));
        card=cards.get(0);
      }else if(out.equals(cards.get(1).getName())){
        players.get(victim).disc.put(cards.get(0));
        card=cards.get(1);
      }else{
        players.get(victim).disc.put(cards);
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
      server.optionPane(attacker,o);
      out=server.getUserInput(attacker);
      if(out.equals("Keep")){
        players.get(attacker).disc.put(card);
      }else{
        trash.put(card);
        displayTrash();       
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
      server.displayComment(activePlayer,"Choose a card to play twice");
      Collection<DominionCard> hand=players.get(activePlayer).hand;
      boolean foundAction=false;
      for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        if(maskCondition(it.next())){
          foundAction=true;
          break;
        }
      }
      if(foundAction==false){
        server.displayComment(activePlayer,"");
        return;
      }
      
      server.setMask(activePlayer,makeMask(hand));
      doWork("select",1,1,activePlayer);
      DominionCard card=selectedCards.get(0);

      selectedCards.clear();
      changePhase("actions");
      server.displayComment(activePlayer,"");
      
      playCard(card,activePlayer,true);
      playCard(card,activePlayer,true);

      //displayPlayer(activePlayer);
      matcards.add(card);
      cardPlayed(activePlayer);
    }
    @Override 
    public boolean maskCondition(DominionCard card){
      return card.isAction;
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
      DominionPlayer player=players.get(activePlayer);
      try{
        card=player.getCard();
        if(card.isAction){
          String [] options={"Play", "Discard"};
          OptionData o=new OptionData(options);
          o.put(card.getName(), "image");
          server.optionPane(activePlayer,o);
          String input=server.getUserInput(activePlayer);
          if(input.equals(options[0])){
            playCard(card,activePlayer);
          }else{
            player.disc.put(card);
          }
        }else{
          player.disc.put(card);
        }
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
      gainCard("curse",activePlayer);
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
      gainLimit=4;
      doWork("gain",0,1,activePlayer);      
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
  
  //**********INTRIGUE CARDS***************////
  private class Courtyard extends RegularCard{
    public Courtyard(){
      super("courtyard");
      cost=2;
      cards=3;
      comment="Top deck 1 card";
    }
    @Override
    public void subWork(int activePlayer){
      doWork("topdeck",1,1,activePlayer);
      displayPlayer(activePlayer);
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
      boolean actionInTrash=false;
      for(Iterator<DominionCard> it=trash.iterator(); it.hasNext(); ){
        if(it.next().isAction){
          actionInTrash=true;
          break;
        }
      }

      String [] options={"Trash from supply"};
      OptionData o=new OptionData(options);
      
      if(actionInTrash){
        o.put("Gain from Trash","textbutton");
      }else{
        o.put("No actions in trash","text");
      }
      String input=optionPane(activePlayer,o);
      DominionCard card;

      //if they are selecting a supply to trash from
      if(input.equals(options[0]) ){
        changePhase("selectDeck");
        SupplyDeck deck;
        while(true){
          Dominion.this.work(activePlayer);
          deck=supplyDecks.get(selectedDeck);
          if(deck.size()==0) continue;
          card=deck.peek();
          if(card.isAction){
            trash.put(deck.topCard());
            break;
          }
        }
        displayTrash();
        server.displaySupply(deck.makeData()); 
      //if they are gaining an action from supply  
      }else{
        options=new String[0];
        o=new OptionData(options);
        for(Iterator<DominionCard> it=trash.iterator(); it.hasNext(); ){
          o.put(it.next().getName(), "imagebutton");
        }
        while(true){
          input=optionPane(activePlayer,o);
          if(cardFactory(input).isAction) break;
        }
        for(Iterator<DominionCard> it=trash.iterator(); it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            players.get(activePlayer).disc.put(card);
            trash.remove(card);
            break;
          }
        }
        displayTrash();
        displayPlayer(activePlayer);
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
      
      String input=optionPane(activePlayer,o);
      resolve(input, activePlayer);
      options.remove(input);
      o=new OptionData(options.toArray(new String[3]));
      resolve(optionPane(activePlayer,o), activePlayer);
      displayPlayer(activePlayer);
      server.updateSharedFields(Dominion.this.actions,money,Dominion.this.buys);
    }
    public void resolve(String input,int activePlayer){  
      if(input.equals("+1 Card")) players.get(activePlayer).drawToHand(1);
      if(input.equals("+1 Action")) Dominion.this.actions++;
      if(input.equals("+1 Money")) money++;
      if(input.equals("+1 Buy")) Dominion.this.buys++;        
    }
  }
  private class Masquerade extends Attack{
    public Masquerade(){
      super("masquerade");
      cost=3;
      isAttack=false;
      cards=2;
      comment="Pass a card to the left";
    }
    @Override
    public void cleanup(int activePlayer){
      doWork("trash",1,1,activePlayer);
      displayTrash();   
    }
    @Override
    public void subWork(int activePlayer){
      passCard(activePlayer,(activePlayer+1)%players.size());
      
    }
    @Override
    public void subStep(int i, int activePlayer){
      passCard(i,(i+1)%players.size());
    }
    //passes a cardfrom player i to player i+1
    public void passCard(int fromPlayer, int toPlayer){
      doWork("select",1,1,fromPlayer);
      players.get(toPlayer).hand.add(selectedCards.get(0));
      displayPlayer(fromPlayer);
      displayPlayer(toPlayer);
      selectedCards.clear();
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
      for(Iterator<DominionCard> it=players.get(activePlayer).hand.iterator(); it.hasNext(); ){
        if(it.next().isAction){
          actions=true;
          break;
        }
      }
      if(!actions){
        players.get(activePlayer).drawToHand(2);
        displayPlayer(activePlayer);
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
      String [] options={"Trash 2 cards", "+2 cards", "+2 money"};
      String input=optionPane(activePlayer,new OptionData(options));
      
      if(input.equals(options[0])){
        minSelection=2;
        maxSelection=2;
        changePhase("trash");
        Dominion.this.work(activePlayer);
        changePhase("actions");
        selectedCards.clear();
      }else if(input.equals(options[1])){
        players.get(activePlayer).drawToHand(2);
        displayPlayer(activePlayer);
      }else{
        money+=2;
        server.updateSharedFields(Dominion.this.actions,money,Dominion.this.buys);
      }
    }
  }
  private class Swindler extends Attack{
    public Swindler(){
      super("swindler");
      cost=3;
      value=2;
      attackPhase="selectDeck";
    }
    @Override
    public void subStep(int victim, int attacker){
      try{
        DominionCard card=players.get(victim).getCard();
        int value=card.value;
        SupplyDeck deck;
        while(true){
          Dominion.this.work(attacker);
          deck=supplyDecks.get(selectedDeck);
          if(deck.getCost()==value && deck.size()>0){
            gainCard(deck.getName(),victim);
            break;
          }
        }        
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
        changePhase("selectDeck");
        Dominion.this.work(activePlayer);
        DominionCard card1=cardFactory(supplyDecks.get(selectedDeck).getName());
        DominionCard card2=players.get(activePlayer).getCard();
        
        //show the drawn card
        OptionData o=new OptionData(new String[0]);
        o.put(card2.getImage(),"image");
        o.put("Continue","textbutton");
        optionPane(activePlayer,o);        
        
        if(card1.equals(card2)){
          players.get(activePlayer).hand.add(card2);
        }else{
          players.get(activePlayer).deck.put(card2);
        }
        displayPlayer(activePlayer);
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
      DominionPlayer player=players.get(activePlayer);
      server.setMask(activePlayer,makeMask(player.hand));
      doWork("discard",0,1,activePlayer);
      
      if(selectedCards.size()>0){
        money+=4;
      }else{
        changePhase("actions");
        gainCard("estate",activePlayer);
      }
      displayPlayer(activePlayer);
      updateSharedFields();
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
      bridgeCounter++;
      displaySupplies();
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
      if(conspiratorCounter>=3){
        players.get(activePlayer).drawToHand(1);
        Dominion.this.actions++;
      }
      displayPlayer(activePlayer);
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
      if(players.get(activePlayer).hand.size()<=5) Dominion.this.actions+=2;
    }
  }
  private class Ironworks extends RegularCard{
    public Ironworks(){
      super("ironworks");
      cost=4;
    }
    @Override
    public void subWork(int activePlayer){
      gainLimit=4;
      doWork("gain",0,1,activePlayer);
      DominionCard card=selectedCards.get(0);
      
      if(card.isAction) Dominion.this.actions++;
      if(card.isMoney) money++;
      if(card.isVictory) players.get(activePlayer).drawToHand(1);
      updateSharedFields();
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
      Dominion.this.actions++;
      players.get(activePlayer).drawToHand(1);
      displayPlayer(activePlayer);
      
      String [] options={"Discard 2 cards", "Done"};
      String input=optionPane(activePlayer,new OptionData(options));
      if(input.equals(options[0])){
        doWork("discard",2,2,activePlayer);
        money+=2;
        updateSharedFields();
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
      String [] options={"Trash for +2","Done"};
      if(optionPane(activePlayer,new OptionData(options)).equals(options[0])){
        if(matcards.remove(this)){
          trash.put(this);
          money+=2;
          cardPlayed(activePlayer);
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
      doWork("select",1,1,activePlayer);
      putAnywhere(activePlayer,selectedCards.get(0));
    }
  }
  private class Courtier extends RegularCard{
    public Courtier(){
      super("courtier");
      cost=5;
    }
    @Override
    public void subWork(int activePlayer){
      doWork("select",1,1,activePlayer);
      DominionCard card=selectedCards.get(0);
      players.get(activePlayer).hand.add(card);
      displayPlayer(activePlayer);
      int picks=0;
      if(card.isAction) picks++;
      if(card.isVictory) picks++;
      if(card.isMoney) picks++;
      if(card.isAttack) picks++;
      if(card.isReaction()) picks++;
      
      ArrayList<String> options=new ArrayList<>(4);
      ArrayList<String> choices=new ArrayList<>(picks);
      options.add("+1 Action");
      options.add("+1 Buy");
      options.add("+3 Money");
      options.add("Gain Gold");
      String input;
      SupplyDeck deck=supplyDecks.get("gold");
      while(picks>0){
        input=optionPane(activePlayer,new OptionData(options.toArray(new String[options.size()])));
        picks--;
        options.remove(input);
        choices.add(input);
      }
      for(String choice : choices){
        if(choice.equals("+1 Action")) Dominion.this.actions++;
        else if(choice.equals("+1 Buy")) Dominion.this.buys++;
        else if(choice.equals("+3 Money")) money+=3;
        else{
          gainCard("gold",activePlayer);
        }        
      }
      updateSharedFields();
          
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
      choice=optionPane(activePlayer,new OptionData(options));
      if(choice.equals(options[1])){
        redraw(activePlayer);
        displayPlayer(activePlayer);
      }else{
        money+=2;
        updateSharedFields();
      }        
    }
    @Override 
    public void subStep(int victim, int attacker){
      if(choice.equals(options[1])) redraw(victim);
    }
    public void redraw(int victim){
      DominionPlayer player=players.get(victim);
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
          card=players.get(activePlayer).getCard();
          if(card.isVictory || card.getName().equals("curse")){
            players.get(activePlayer).hand.add(card);
          }else{
            cards.add(card);
          }
        }catch(OutOfCardsException ex){
          break;
        }
      }
      putBack(activePlayer,cards);
    }
  }
  private class Replace extends Attack{
    private boolean curse=false;
    public Replace(){
      super("remodel");
      cost=5;
      comment="Trash a card, gain a card costing up to 2 more than it";
      attackPhase="actions";
    }
    @Override
    public void subWork(int activePlayer){
      doWork("trash",1,1,activePlayer);
      displayTrash();

      changePhase("gain");
      gainLimit=cost2(selectedCards.get(0))+2;
      selectedCards.clear();
      minSelection=0;
      Dominion.this.work(activePlayer);
      DominionCard card=selectedCards.get(0);
      if(card.isAction || card.isMoney){
        curse=false;
        players.get(activePlayer).deck.put(players.get(activePlayer).disc.topCard());
      }else{
        curse=true;
      }
    }  
    @Override
    public void subStep(int victim, int attacker){
      if(curse) gainCard("curse",victim);
    }
  }
  private class Torturer extends Attack{
    private final String [] options={"Discard 2 cards", "Gain curse in hand"};
    private OptionData o;
    public Torturer(){
      super("torturer");
      cost=5;
      cards=3;
      attackPhase="discard";
      o=new OptionData(options);
    }
    @Override
    public void subStep(int victim, int attacker){
      if(optionPane(victim,o).equals(options[0])){
        doWork("discard",2,2,victim);
      }else{
        gainCard("curse",victim,"hand");
      }
    }
  }
  private class Tradingpost extends RegularCard{
    private final String [] options={"Trash 2 cards for a Silver", "Done"};
    private OptionData o;
    public Tradingpost(){
      super("tradingpost");
      cost=5;
      o=new OptionData(options);
    }
    @Override
    public void subWork(int activePlayer){
      if(optionPane(activePlayer,o).equals(options[0])){
        doWork("trash",2,2,activePlayer);
        gainCard("silver",activePlayer,"hand");
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
      doWork("trash",1,1,activePlayer);
      displayPlayer(activePlayer);
      displayTrash();
      minGain=cost2(selectedCards.get(0))+1;
      gainLimit=minGain;
      selectedCards.clear();
      controlledGain(activePlayer);
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
      String input=optionPane(activePlayer,o);
      if(input.equals(options[0])) players.get(activePlayer).drawToHand(3);
      else Dominion.this.actions+=2;
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
      doWork("discard",0,100,activePlayer);
      money+=selectedCards.size();
      updateSharedFields();
      displayPlayer(activePlayer);
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
          card=players.get(activePlayer).getCard();
          if(card.isVictory) players.get(activePlayer).hand.add(card);
          else cards.add(card);
        }catch(OutOfCardsException ex){
          break;
        }
      }
      putBack(activePlayer,cards);
    }
  }
  private class Saboteur extends Attack{
    public Saboteur(){
      super("saboteur");
      cost=5;
    }
    @Override
    public void subStep(int victim, int attacker){
      DominionCard card;
      while(true){
        try{
          card=players.get(victim).getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        if(cost2(card)<3) players.get(victim).disc.put(card);
        else{
          trash.put(card);
          gainLimit=cost2(card)-2;
          if(gainLimit>=0) doWork("gain",1,1,victim);
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
      HashSet<String> cards=new HashSet<>();
      DominionCard  card;
      for(int i=0;i<2;i++){
        try{
          card=players.get( (activePlayer+1)%players.size()).getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        cards.add(card.getName());
      }
      for(String t : cards){
        card=cardFactory(t);
        if(card.isAction) Dominion.this.actions+=2;
        if(card.isVictory) players.get(activePlayer).drawToHand(2);
        if(card.isMoney) money+=2;
      }
      updateSharedFields();
      displayPlayer(activePlayer);
      displayPlayer( (activePlayer+1)%players.size());
    }
  }
}
