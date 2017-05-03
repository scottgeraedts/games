import java.util.*;


public class Dominion{

  public ArrayList<DominionPlayer> players=new ArrayList<DominionPlayer>();
  private LinkedHashMap<String, SupplyDeck> supplyDecks=new LinkedHashMap<>();
  public ArrayList<DominionCard> matcards=new ArrayList<DominionCard>();
  private Deck<DominionCard> trash=new Deck<>();

  private int money=0,actions=1,buys=1;
  private int nPlayers;
  private DominionBoard board;
  private int emptyPiles=0;
  private boolean gameOver=false;
  private String phase="actions";
  private int activePlayer=0;

  //stuff for selections
  private int maxSelection=0,minSelection=0;
  //cards that have been selected for trashing/discarding etc go here in case they need to be looked at by e.g. a forge
  private ArrayList<DominionCard> selectedCards=new ArrayList<>(); 
  private DominionCard currentCard;
  private int initialPlayer;
  private int gainLimit;
  
  private Stack<Integer> throneRoomCounter=new Stack<>();
  private Stack<DominionCard> throneRoomCard=new Stack<>();
  
  public static String [] coreCards={"adventurer", "bureaucrat", "cellar", "chancellor", "chapel", 
      "councilroom", "feast", "festival", "laboratory", "library", "market", "militia", 
      "mine","moneylender","remodel", "smithy", "spy", "thief", "throneroom", "village", "witch"};
  
  public Dominion(ArrayList<String> names){    
    //make players
    for(int i=0;i<names.length;i++){
      players.add(new DominionPlayer(names.get(i)));
     // for(int j=0;j<3;j++) players.get(i).deck.put(cardFactory("witch"));
    }
    nPlayers=names.length;
    
    //supplies
    String [] tcards={"copper","silver","gold","estate","duchy","province","curse"};
    ArrayList<String> cards=new ArrayList<String>(Arrays.asList(tcards));
    cards.addAll(randomSupply());
    cards.add("gardens");
    
    for(int i=0;i<cards.size();i++){
      supplyDecks.put(cards.get(i), new SupplyDeck( cards.get(i) ));
    } 
    
    //trash
    trash.backImage=Deck.blankBack;
    trash.faceup=true;
  }
  public void setBoard(DominionBoard tboard){
    board=tboard;
  }
  
  ///***STUFF WHICH PROGRESSES THE GAME***///
  public void playCard(int cardNum){
    playCard(players.get(activePlayer).hand.remove(cardNum), false);
  }
  public void playCard(DominionCard card, boolean fake){
    if(phase=="actions" && !fake) actions--;

    DominionPlayer player=players.get(activePlayer);
  
    if(phase=="buys" || phase=="actions"){
      currentCard=card;
      if(!fake) matcards.add(card);
      money+=card.value;
      actions+=card.actions;
      buys+=card.buys;
      player.drawToHand(card.cards);
        
      //do special behavior of the card
      initialPlayer=activePlayer;
      card.work();
      //this is to make sure that even easily-resolved action cards go through step selection
      if(card.isAction && card.simple) stepSelection(true);
    }else{
      //do phase related behavior
      if(phase=="topdeck") player.deck.put(card);
      if(phase=="discard") player.disc.put(card);
      if(phase=="trash"){
        trash.put(card);
        board.displayTrash(trash.makeData());
      }
      selectedCards.add(card);
      
      //check if the phase is over
      if(selectedCards.size()>=maxSelection)
        stepSelection(true);
      
    }
    //if the current card left us still in the action phase, check if we're out of actions
    if(phase=="actions" && actions==0){
      changePhase("buys");
    }
    
  }
  
  //all card plays come through this function  
  public void stepSelection(boolean autoSucceed){
  
    DominionCard tempCard;
    
    //see if its ok to end the selection phase
    if(selectedCards.size()<minSelection && !autoSucceed) return;
    
    //call card behavior for end of selection phase
    boolean done=currentCard.wrapup();

    //reset selection related stuff
    selectedCards.clear();
    
    //progress the player counter and/or end the selection phase
    if(activePlayer==initialPlayer && done){
      if(!throneRoomCounter.empty()){
        changePhase("actions");
        int temp=throneRoomCounter.pop();
        if(temp==1){
          tempCard=throneRoomCard.pop();
          matcards.add(tempCard);
        }else{
          throneRoomCounter.push(temp-1);
          tempCard=throneRoomCard.peek();
        }
        playCard(tempCard,true);

      }else changePhase("actions");
      if(actions<=0 && phase=="actions") changePhase("buys");
      return;
    }
    
    //call card behavior for beginning of next selection phase
    currentCard.step();
  }

  public void stepPlayer(){
    int newPlayer=(activePlayer+1)%players.size();
    board.changePlayer(activePlayer,newPlayer);
    activePlayer=newPlayer;
  }
  public void changePhase(String newPhase){
    board.changePhase(phase,newPhase);
    phase=newPhase;
    if(newPhase=="buys"){
      throneRoomCard.clear();
      throneRoomCounter.clear();
    }
  }
  public void endTurn(){
    int newPlayer=(activePlayer+1)%players.size();
    players.get(activePlayer).disc.put(matcards);
    players.get(activePlayer).endTurn();   
    if(gameOver) endGame();
    money=0;
    buys=1;
    actions=1; 
    matcards.clear();
    phase="actions";

    //pass on this info to board
    board.changePlayer(activePlayer,newPlayer);
    activePlayer=newPlayer;
    
  }
  public boolean gainCard(String deckName, boolean buying, boolean topdeck){
    DominionPlayer player=players.get(activePlayer);
    SupplyDeck deck=supplyDecks.get(deckName);
    if(deck.size()>0 && buys>0 && ( (buying && money>=supplyCost(deckName)) || (!buying && gainLimit>=supplyCost(deckName)) ) ){
      if(buying){
        buys--;
        money-=supplyCost(deckName);
      }
      DominionCard card=deck.topCard();
      

      //put card on discard pile or (more rarely) top of deck
      if(topdeck) player.deck.put(card);
      else player.disc.put(card);
      
      if(deck.size()==0){
        if(card.getName()=="province" || card.getName()=="colony") gameOver=true;
        else if(emptyPiles<3) emptyPiles++;
        else gameOver=true;
      }
      return true;
    }
    return false;
  }
  public void endGame(){
    int temp;
    LinkedHashMap<String,Integer> points=new LinkedHashMap<>();
    String name;
    
    for(int i=0;i<players.size();i++){
      temp=players.get(i).victoryPoints();
      name=players.get(i).getName();
      points.put(name,temp);
    }
    //sort the values of the array and return a sorted list
    LinkedList<Map.Entry<String,Integer>> list=new LinkedList<>(points.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String,Integer>>(){
      public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b){
        return b.getValue()-a.getValue();
      }
    });
    
    points.clear();
    for(Map.Entry<String,Integer> entry: list){
      points.put(entry.getKey(), entry.getValue());
    }      
    board.showScores(points);
  }
  
  public static boolean moatReveal(Collection<DominionCard> hand, DominionBoard board){
    DominionCard card;
    String [] options={"Reveal Moat","Pass"};
    OptionData o=new OptionData(options);

    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
      card=it.next();
      if(card.getName()=="moat"){
        o.put(card.getImage(),"image");
        return board.optionPane(o)=="Reveal Moat";        
      }
    }
    return false;
  }
  //***SETTING PRIVATE VARIABLES***///
  public void setPhase(String newphase){ phase=newphase; }
  
  //***STUFF WHICH GETS STATUS OF GAME***///
  public int supplyCost(String deckName){
    return supplyDecks.get(deckName).getCost();
  }
  public Deck.Data supplyData(String deckName){
    return supplyDecks.get(deckName).makeData();
  }
  public Set<String> getSupplyNames(){
    return supplyDecks.keySet();
  }
  public int getMoney(){ return money; }
  public int getBuys(){ return buys; }
  public int getActions(){ return actions; }
  public String getPhase(){ return phase; }
  public int getActivePlayer(){ return activePlayer; }
  public Deck.Data trashData(){ return trash.makeData(); }
  
  //****INNER CLASSES***///
  class SupplyDeck extends Deck<DominionCard>{
    private int cost;
    public SupplyDeck(String name){
      DominionCard card=cardFactory(name);
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
        cards.add(cardFactory(name));
      }
    }
    public int getCost(){return cost;}
  }
  
  public static class OptionData extends PairList<String,String>{
    public OptionData(String [] options){
      for(int i=0;i<options.length;i++){
        names.add(options[i]);
        types.add("textbutton");
      }
    }
    public OptionData(){}
  }

  
    //****CARD STUFF***//
  public static ArrayList<String> randomSupply(){
    boolean [] included=new boolean[coreCards.length];
    Arrays.fill(included,false);
    
    Random ran=new Random();
    ArrayList<String> out=new ArrayList<>();
    int i;
    while(out.size()<10){
      i=ran.nextInt(coreCards.length);
      if(!included[i]){
        included[i]=true;
        out.add(coreCards[i]);
      }
    }
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
  private class Attack extends DominionCard{
    protected String attackPhase="other";
    public Attack(String name){
      super(name);
      isAction=true;
      isAttack=true;
    }
    @Override
    public void work(){
      subWork();
      if(attackPhase=="other") phase="other";
      else changePhase(attackPhase);
      stepPlayer();
      step();  
        
    }
    @Override
    public boolean wrapup(){
      stepPlayer();
      return true;    
    }
    @Override
    public void step(){
      if(moatReveal(players.get(activePlayer).hand,board)){
        stepSelection(true);
        return;
      }
      subStep();    
    }
    
    public void subWork(){}
    public void subStep(){}
  }
  private class Adventurer extends DominionCard{ 
  
    public Adventurer(){
      super("adventurer");
      cost=6;
      isAction=true;
      simple=true;
    }
    @Override
    public void work(){
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
    }
  }

  private class Bureaucrat extends Attack{
    public Bureaucrat(){
      super("bureaucrat");
      cost=4;
      attackPhase="topdeck";
    }
    @Override
    public void subWork(){
      if(gainCard("silver",false,true))
        board.displaySupply("silver",supplyDecks.get("silver").makeData());
      minSelection=1;
      maxSelection=1;
    }
    @Override
    public void subStep(){
      boolean mask[];
      int firstVictory=0,count,j;
      LinkedList<DominionCard> hand;
      DominionCard card;

      count=0;
      j=0;
      
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
      if(count==-1){
        players.get(activePlayer).disc.put(hand.remove(firstVictory));
        stepSelection(true);//we found the only victory card, skip selection step
      }else if(count>0){
        board.setMask(mask);
      }else{
        stepSelection(true);
      }
    
    }
  }  
  
  private class Cellar extends DominionCard{
    public Cellar(){
      super("cellar");
      cost=2;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(){
      changePhase("discard");
      minSelection=0;
      maxSelection=1000;
    }
    @Override
    public boolean wrapup(){
      players.get(activePlayer).drawToHand(selectedCards.size());
      return true;
    }
  }
  private class Chancellor extends DominionCard{
    public Chancellor(){
      super("chancellor");
      cost=3;
      value=2;
      isAction=true;
      simple=true;
    }
    @Override
    public void work(){
      String[] options={"Discard Deck","Done"};
      OptionData o=new OptionData(options);
      String input=board.optionPane(o);
      DominionPlayer player=players.get(activePlayer);
      if(input==options[0]) player.disc.put(player.deck.deal(player.deck.size()));
    }    
  }
  private class Chapel extends DominionCard{
    public Chapel(){
      super("chapel");
      cost=2;
      isAction=true;
    }
    @Override
    public void work(){
      changePhase("trash");
      minSelection=0;
      maxSelection=4;
      initialPlayer=activePlayer;
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
    public void work(){
      for(int i=activePlayer+1;i<activePlayer+players.size();i++){
        players.get( i%players.size() ).drawToHand(1);
        board.displayPlayer(i%players.size());
      }
    }
  }
  private class Feast extends DominionCard{
    public Feast(){
      super("feast");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(){
      changePhase("gain");
      initialPlayer=activePlayer;
      minSelection=0;
      maxSelection=1;
      gainLimit=5;
      trash.put(matcards.remove(matcards.size()-1));
      board.displayTrash(trash.makeData());
      board.refreshCardPanel();
    }
    
  }
  private class Library extends DominionCard{
    public Library(){
      super("library");
      cost=5;
      isAction=true;
      simple=true;
    }
    @Override
    public void work(){
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
          out=board.optionPane(o);
          if(out==options[0]) player.hand.add(card);
          else aside.add(card);
        }else{
          player.hand.add(card);
        }
        o.remove(card.getImage()); 
      }
      player.disc.put(aside);
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
    public void subStep(){
      minSelection=players.get(activePlayer).hand.size()-3;
      maxSelection=minSelection;    
      if(maxSelection==0) stepSelection(true);
    }
  }
  private class Mine extends DominionCard{
    private boolean firstPass=true;
    public Mine(){
      super("mine");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(){
      minSelection=1;
      maxSelection=1;
      changePhase("trash");
      firstPass=true;

      board.setMask(makeMask(players.get(activePlayer).hand));
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
    @Override
    public boolean wrapup(){
      if (firstPass){
        changePhase("gain");
        gainLimit=selectedCards.get(0).cost+3;
        firstPass=false;
        minSelection=0;
        return false;
      }else{
        DominionCard card=players.get(activePlayer).disc.topCard();
        if(card.isMoney){
          players.get(activePlayer).hand.add(card);
          return true;
        }else{
          supplyDecks.get(card.getName()).put(card);
          board.displaySupply(card.getName(),supplyDecks.get(card.getName()).makeData());
          return false;
        }
      }
    }
  }
  private class Moneylender extends DominionCard{
    public Moneylender(){
      super("moneylender");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(){
      minSelection=1;
      maxSelection=1;
      changePhase("trash");
      board.setMask(makeMask(players.get(activePlayer).hand));
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName()=="copper";
    }
    @Override
    public boolean wrapup(){
      if(selectedCards.size()>0) money+=3;
      return true;
    }
  }
  private class Remodel extends DominionCard{
    private boolean firstPass;
    public Remodel(){
      super("remodel");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(){
      firstPass=true;
      minSelection=1;
      maxSelection=1;
      changePhase("trash");
    }
    @Override
    public boolean wrapup(){
      if (firstPass){
        changePhase("gain");
        gainLimit=selectedCards.get(0).cost+2;
        firstPass=false;
        minSelection=0;
        return false;
      }else{
        return true;
      }
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
    public void subWork(){
      board.displayPlayer(activePlayer);
      discardOrKeep();
    }
    public void discardOrKeep(){
      try{
        DominionCard card=players.get(activePlayer).getCard();
        o.put(card.getImage(),"image");
        String result=board.optionPane(o);
        if(result==options[0]) players.get(activePlayer).deck.put(card);
        else players.get(activePlayer).disc.put(card);
        board.displayPlayer(activePlayer);
        o.remove(card.getImage());
      }catch(OutOfCardsException e){
      }
    }
    @Override
    public void subStep(){
      discardOrKeep();
      stepSelection(true);
    }
  }
  private class Thief extends Attack{
    OptionData o=new OptionData();
    public Thief(){
      super("thief");
      cost=4;
    }
    @Override
    public void subStep(){
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionCard card=cardFactory("copper");
      for(int i=0;i<2;i++){
        try{
          card=players.get(activePlayer).getCard();
        }catch(OutOfCardsException e){}
        
        cards.add(card);
        if(card.isMoney)
          o.put(card.getImage(),"imagebutton");
        else
          o.put(card.getImage(),"image");
      }
      o.put("Done","textbutton");
      String out=board.optionPane(o);
      if(out=="Done"){
        stepSelection(true);
        return;
      }
      
      for(int i=0;i<2;i++){
        if(out==cards.get(i).getImage()){
          card=cards.get(i);
          break;
        }
      }
      
      o.clear();
      o.put(card.getImage(),"image");
      o.put("Keep","textbutton");
      o.put("Trash","textbutton");
      out=board.optionPane(o);
      if(out=="Keep"){
        players.get(initialPlayer).disc.put(card);
      }else{
        trash.put(card);
        board.displayTrash(trash.makeData());        
      }
      o.clear();
      stepSelection(true);
    }
  }
  private class Throneroom  extends DominionCard{
    public Throneroom(){
      super("throneroom");
      cost=4;
      isAction=true;
    }
    @Override
    public void work(){
      Collection<DominionCard> hand=players.get(activePlayer).hand;
      for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        if(maskCondition(it.next())){
          changePhase("throneroom");
          board.setMask(makeMask(hand));
          minSelection=1;
          maxSelection=1;
          break;
        }
      }
    }
    @Override 
    public boolean maskCondition(DominionCard card){
      return card.isAction;
    }
    @Override
    public boolean wrapup(){
      throneRoomCounter.push(2);
      throneRoomCard.push(selectedCards.get(0));
      changePhase("actions");
      return true;
    }
  }
  private class Witch extends Attack{
    public Witch(){
      super("witch");
      cost=5;
      cards=2;
    }
    @Override
    public void subStep(){
      gainCard("curse",false,false);
      stepSelection(true);
      board.displaySupply("curse",supplyDecks.get("curse").makeData());
    }
  }  
  private class Workshop extends DominionCard{
    public Workshop(){
      super("workshop");
      cost=3;
      isAction=true;
    }
    @Override
    public void work(){
      changePhase("gain");
      gainLimit=4;
      maxSelection=0;
      minSelection=1;
      initialPlayer=activePlayer;
      board.refreshCardPanel();
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
}
