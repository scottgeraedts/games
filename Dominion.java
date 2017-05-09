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

  //stuff for selections
  private int maxSelection=10,minSelection=10;
  //cards that have been selected for trashing/discarding etc go here in case they need to be looked at by e.g. a forge
  private ArrayList<DominionCard> selectedCards=new ArrayList<>(); 
  private DominionCard currentCard;
  private int initialPlayer;
  private int gainLimit;
  private String selectedDeck;

  //specific card related counters
  private int merchantCounter;
  private int bridgeCounter; //counts cost reduction, also does highway and brige troll
  private int conspiratorCounter; //counts total actions played, also does peddler
    
  
  public static String [] coreCards={"adventurer", "bureaucrat", "cellar", "chancellor", "chapel", 
      "councilroom", "feast", "festival", "laboratory", "library", "market", "militia", 
      "mine","moneylender","remodel", "smithy", "spy", "thief", "throneroom", "village", "witch",
      "woodcutter","workshop","gardens","harbinger","merchant","vassal","bandit","poacher","sentry","artisan"};
  public static String [] intrigueCards={"courtyard","lurker","masquerade","shantytown","pawn","steward","swindler"};
  
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

    merchantCounter=0;
    bridgeCounter=0;

    //players
    players=new ArrayList<DominionPlayer>();    
    //make players
    for(int i=0;i<names.size();i++){
      players.add(new DominionPlayer(names.get(i)));
      for(int j=0;j<3;j++) players.get(i).deck.put(cardFactory("miningvillage"));
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
          server.displayPlayer(activePlayer,players.get(activePlayer).makeData());
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
      
      players.get(activePlayer).drawToHand(card.cards);
      if(!throneRoom){
        matcards.add(card);
      }
      server.cardPlayed(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),matData());
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
    //if phase is actions there are no conditions since an action card got you here
    if(deck.size()>0 && ( (phase=="buys" && money>=deck.getCost() && buys>0) || (phase=="gain" && gainLimit>=deck.getCost()) || phase=="actions") ){
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
  public String optionPane(int activePlayer, OptionData o){
    server.optionPane(activePlayer,o);
    return server.getUserInput(activePlayer);
  }
  public int cost2(DominionCard card){
    return Math.max(card.cost-bridgeCounter,0);
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
  public ArrayList<DominionCard> matData(){
    ArrayList<DominionCard> out=new ArrayList<>(matcards.size());
    for(Iterator<DominionCard> it=matcards.iterator(); it.hasNext(); ){
      out.add(it.next());
    }
    return out;
  }
  
  //some simple wrappers for server functions 
  public void displayPlayer(int i){
    server.displayPlayer(i,players.get(i).makeData());
  }
  public void updateSharedFields(){
    server.updateSharedFields(actions,money,buys);
  }
  public void displaySupplies(){
    for(Map.Entry<String,SupplyDeck> entry : supplyDecks.entrySet()){
      server.displaySupply(entry.getValue().makeData());
    }
  }
  public Deck.Data trashData(){ return trash.makeData(); }
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
        cards.add(cardFactory(name));
      }
    }
    public int getCost(){return Math.max(cost-bridgeCounter,0);}
    public String getName(){return name;}
    public Deck.SupplyData makeData(){
      return new Deck.SupplyData(cards.size(), backImage, getCost(), name);
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
          if(r.equals("moat")) moat=true;
          else if(r.equals("diplomat")){
            if(victim.hand.size()>=5){
              victim.drawToHand(2);
              maxSelection=3;
              minSelection=3;
              changePhase("discard");              
              Dominion.this.work(i);
              selectedCards.clear();
              changePhase(attackPhase);
            }
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
      server.cardPlayed(Dominion.this.actions,money,Dominion.this.buys, activePlayer, players.get(activePlayer).makeData(), matData());
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
      server.cardPlayed(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),matData());
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
      changePhase("gain");
      gainLimit=5;
      maxSelection=1;
      minSelection=0;
      Dominion.this.work(activePlayer);
      players.get(activePlayer).hand.add(players.get(activePlayer).disc.topCard());
      displayPlayer(activePlayer);
      changePhase("topdeck");
      selectedCards.clear();
      minSelection=1;
      Dominion.this.work(activePlayer);
      
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
        server.displayPlayer(activePlayer,players.get(activePlayer).makeData());
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
      changePhase("discard");
      minSelection=0;
      maxSelection=1000;
      Dominion.this.work(activePlayer);
      players.get(activePlayer).drawToHand(selectedCards.size());
      server.displayPlayer(activePlayer,players.get(activePlayer).makeData());
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
        server.displayPlayer(activePlayer, player.makeData());
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
      changePhase("trash");
      minSelection=0;
      maxSelection=4;
      Dominion.this.work(activePlayer);
      server.displayTrash(trashData());
      server.displayPlayer(activePlayer,players.get(activePlayer).makeData());
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
        server.displayPlayer(i%players.size(), players.get(i%players.size()).makeData());
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
      changePhase("gain");
      initialPlayer=activePlayer;
      minSelection=0;
      maxSelection=1;
      gainLimit=5;
      Dominion.this.work(activePlayer);
      trash.put(matcards.remove(matcards.size()-1));
      server.displayTrash(trashData());
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
          server.displayPlayer(activePlayer,player.makeData());
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
      server.displayPlayer(activePlayer,player.makeData());
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
      minSelection=1;
      maxSelection=1;
      changePhase("trash");

      server.setMask(activePlayer,makeMask(players.get(activePlayer).hand));
      Dominion.this.work(activePlayer);
      server.displayTrash(trashData());
      int cost=selectedCards.get(0).cost;
      changePhase("selectDeck");
      
      DominionCard card;
      SupplyDeck deck;
      while(true){
        Dominion.this.work(activePlayer);
        deck=supplyDecks.get(selectedDeck);
        if(deck.size()==0) continue;
        
        card=deck.peekTop();
        if(card.isMoney && deck.getCost()<=cost2(selectedCards.get(0))+3){
          players.get(activePlayer).hand.add(deck.topCard());
          server.cardGained(actions,money,buys,activePlayer,players.get(activePlayer).makeData(),deck.makeData());          
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
      minSelection=0;
      maxSelection=1;
      changePhase("trash");
      server.setMask(activePlayer,makeMask(players.get(activePlayer).hand));
      Dominion.this.work(activePlayer);
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
        changePhase("discard");
        minSelection=emptyPiles;
        maxSelection=Math.max(1,minSelection);
        Dominion.this.work(activePlayer);
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
      minSelection=1;
      maxSelection=1;
      changePhase("trash");
      Dominion.this.work(activePlayer);
      server.displayTrash(trashData());

      changePhase("gain");
      gainLimit=cost2(selectedCards.get(0))+2;
      minSelection=0;
      Dominion.this.work(activePlayer);
      
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
            server.displayTrash(trashData());
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
        server.displayTrash(trash.makeData());        
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
      
      changePhase("select");
      server.setMask(activePlayer,makeMask(hand));
      minSelection=1;
      maxSelection=1;

      Dominion.this.work(activePlayer);
      DominionCard card=selectedCards.get(0);

      selectedCards.clear();
      changePhase("actions");
      server.displayComment(activePlayer,"");
      
      playCard(card,activePlayer,true);
      playCard(card,activePlayer,true);

      //displayPlayer(activePlayer);
      matcards.add(card);
      server.cardPlayed(Dominion.this.actions,money,Dominion.this.buys,activePlayer,players.get(activePlayer).makeData(),matData());
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
      changePhase("gain");
      gainLimit=4;
      maxSelection=1;
      minSelection=0;
      Dominion.this.work(activePlayer);
      
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
      changePhase("topdeck");
      minSelection=1;
      maxSelection=1;
      Dominion.this.work(activePlayer);
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
          card=deck.peekTop();
          if(card.isAction){
            trash.put(deck.topCard());
            break;
          }
        }
        server.displayTrash(trashData());
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
        server.displayTrash(trashData());
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
      changePhase("trash");
      minSelection=1;
      maxSelection=1;
      Dominion.this.work(activePlayer); 
      server.displayTrash(trashData());   
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
      changePhase("select");
      minSelection=1;
      maxSelection=1;
      Dominion.this.work(fromPlayer);
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
    }
    @Override
    public void subStep(int victim, int attacker){
      try{
        DominionCard card=players.get(victim).getCard();
        int value=card.value;
        SupplyDeck deck;
        changePhase("selectDeck");
        while(true){
          Dominion.this.work(attacker);
          deck=supplyDecks.get(selectedDeck);
          if(deck.getCost()==value && deck.size()>0){
            players.get(victim).disc.put(deck.topCard());
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
      changePhase("discard");
      minSelection=0;
      maxSelection=1;
      server.setMask(activePlayer,makeMask(player.hand));
      Dominion.this.work(activePlayer);
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
      minSelection=0;
      maxSelection=1;
      changePhase("gain");
      gainLimit=4;
      Dominion.this.work(activePlayer);
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
        minSelection=2;
        maxSelection=2;
        changePhase("discard");
        Dominion.this.work(activePlayer);
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
          updateSharedFields();
        }
      }
    }
  }
}
