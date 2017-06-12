import java.util.*;
public abstract class Expansion{
  String [] cards;
  String name;
  Dominion game;
  static ArrayList<String> vicTokens;
  static ArrayList<String> coinTokens;
  static ArrayList<String> debtCards;

  static String [] prosperityCards={"loan","traderoute","watchtower","bishop","quarry",
      "talisman","city","contraband","countinghouse","mint","mountebank","rabble","royalseal",
      "vault","venture","goons","hoard","grandmarket","bank","expand","forge","kingscourt",
      "peddler","monument"};
  static String [] darkAgesCards={"altar", "armory", "banditcamp", "bandofmisfits", "beggar",
          "catacombs", "count", "counterfeit", "cultist", "deathcart", "feodum", "forager", "fortress",
          "graverobber", "hermit", "huntinggrounds",
          "ironmonger","junkdealer", "knight", "marauder", "marketsquare", "mystic", "pillage",
          "poorhouse", "procession", "rats", "rebuild", "rogue", "sage", "scavenger",
          "squire","storeroom","urchin", "vagrant", "wanderingminstrel"};

  public Expansion(Dominion g){
    game=g;
    String [] vic={"bishop","goons","monument","patrician", "chariotrace", "farmersmarket",
      "sacrifice", "temple", "groundskeeper", "wildhunt","castle","sacrifice", "triumph"};
    vicTokens=new ArrayList<>(Arrays.asList(vic));
    String [] coin={"candlestickmaker","plaza","baker","butcher","merchantguild"};
    coinTokens=new ArrayList<>(Arrays.asList(coin));
    String [] debt={"engineer", "cityquarter", "overlord", "royalblacksmith", "fortune" ,"capital",
            "triumph","annex","donate","tax", "mountainpass"};
    debtCards=new ArrayList<>(Arrays.asList(debt));
  }
  boolean hasCard(String cardName){
    return Arrays.asList(cards).contains(cardName);
  }
  static boolean usePlatinums(ArrayList<String> supplyCards){
    Random ran=new Random();
    double weight=0.05;

    ArrayList<String> pCards=new ArrayList<>(Arrays.asList(prosperityCards));
    for(String card : supplyCards){
      if(pCards.contains(card)) weight+=0.15;
    }
    if(ran.nextDouble()<weight) return true;
    else return false;
  }
  static boolean useShelters(ArrayList<String> supplyCards){
    Random ran=new Random();
    double weight=0.05;

    ArrayList<String> pCards=new ArrayList<>(Arrays.asList(darkAgesCards));
    for(String card : supplyCards){
      if(pCards.contains(card)) weight+=0.15;
    }
    if(ran.nextDouble()<weight) return true;
    else return false;
  }
  protected abstract class Attack extends DominionCard{
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

      //see if we can trash an urchin
      DominionCard card;
      String [] urchinOptions={"Trash Urchin", "Done"};
      if(DarkAges.urchinSwitch) {
        OptionData o = new OptionData(urchinOptions);
        String urchinInput;
        for (ListIterator<DominionCard> it = game.matcards.listIterator(); it.hasNext(); ) {
          card = it.next();

          //skip the last one, which might be an urchin we just played
          if(!it.hasNext()) break;

          if (card.getName().equals("urchin")) {
            urchinInput = game.optionPane(activePlayer, o);
            if (urchinInput.equals(urchinOptions[0])) {
              it.remove();
              game.trashCard(card, activePlayer);
              game.gainCardNoSupply(game.cardFactory("mercenary", "DarkAges"), activePlayer, "discard");
            }
          }
        }
      }else{
        if(isAttack) DarkAges.urchinSwitch=true;
      }
      game.changePhase(attackPhase);

      ArrayList<DominionCard> oldMat=new ArrayList<>(game.matcards);
      int oldPlayer=activePlayer;
      int oldmoney=game.money;
      int oldActions=game.actions;
      int oldBuys=game.buys;

      game.server.displayComment(activePlayer,comment);
      ArrayList<String> reactions=new ArrayList<>();
      DominionPlayer victim;

      for(int i=(activePlayer+1)%game.players.size(); i!=activePlayer; i=(i+1)%game.players.size()){

        victim=game.players.get(i);
        game.changePlayer(oldPlayer,i%game.players.size());

        DominionCard card2;

        //resolve possible reactions
        boolean moat=false;
        if(isAttack) reactions=game.reactionReveal(victim.hand,i,this, c -> c.isReaction1);
        for(String r: reactions){
          if(r.equals("moat")){ 
            moat=true;
          }else if(r.equals("diplomat")){
            victim.drawToHand(2);
            game.displayPlayer(i);
            game.doWork("discard",3,3,i);
            game.selectedCards.clear();
            game.changePhase(attackPhase);
            game.displayPlayer(i);
          }else if(r.equals("secretchamber")){
            victim.drawToHand(2);
            game.displayPlayer(i);
            game.server.displayComment(i,"Put 2 cards on top of the deck");
            game.doWork("topdeck",2,2,i);
            game.selectedCards.clear();
            game.changePhase(attackPhase);
            game.displayPlayer(i);            
          }else if(r.equals("horsetrader")){
            for(ListIterator<DominionCard> it=victim.hand.listIterator(); it.hasNext(); ){
              card2=it.next();
              if(card2.getName().equals("horsetrader")){
                victim.horseTraders.add(card2);
                it.remove();
                break;
              }
            }
          }else if(r.equals("beggar")){
            String [] options={"Discard Beggar", "Done"};
            String input=game.optionPane(i, new OptionData(options));
            if(input.equals(options[0])){
              for(ListIterator<DominionCard> it=victim.hand.listIterator(); it.hasNext(); ){
                card2=it.next();
                if(card2.getName().equals("beggar")){
                  victim.disc.put(card2);
                  it.remove();
                  game.gainCard("silver", i, "topcard", true);
                  game.gainCard("silver", i);
                }
              }
            }
          }
        }
        //check for lighthouse
        if(isAttack){
          for(DominionCard card3 : victim.duration){
            if(card3.getName().equals("lighthouse")) moat=true;
          }
        }
        
        if(moat){
          oldPlayer=i;
          continue;
        }
        
        game.mask.clear();
        if(comment.length()>0) game.server.displayComment(i,comment);
        subStep(i,activePlayer);
        game.selectedCards.clear();
        game.server.displayComment(i,"");
        oldPlayer=i;
      }
      game.changePlayer(oldPlayer,activePlayer);
      game.money=oldmoney;
      game.actions=oldActions;
      game.buys=oldBuys;
      game.matcards=oldMat;       
      
      cleanup(activePlayer);

//      game.server.updateSharedFields(game.actions,game.money,game.buys);
      game.cardPlayed(activePlayer);
      game.server.displayComment(activePlayer,"");
      game.changePhase("actions");
      game.selectedCards.clear();
    }
    public void subStep(int x, int y){}
    public void subWork(int x){}
    public void cleanup(int x){}
  }
  protected abstract class RegularCard extends DominionCard{
    protected String comment="";
    public RegularCard(String name){
      super(name);
      isAction=true;
    }
    @Override
    public final void work(int activePlayer){
      boolean displayedComment=false;
      if(comment.length()>0){
        game.server.displayComment(activePlayer,comment);
        displayedComment=true;
      }

      subWork(activePlayer);

      game.mask.clear();
      if(isAction) game.changePhase("actions");
      if(displayedComment) game.server.displayComment(activePlayer,"");
      game.selectedCards.clear();
    }
    public void subWork(int activePlayer){}
  }
  //used for band of misfits and overlord
  //these cards are complicated!
  protected abstract class CopyCard extends RegularCard{
    protected DominionCard card;
    public CopyCard(String name){
      super(name);
    }
    @Override
    public void subWork(int ap) {
      Dominion.SupplyDeck deck;
      while (true) {
        game.doWork("selectDeck", 1, 1, ap);
        deck = game.supplyDecks.get(game.selectedDeck);
        if (deck.getCost() <= getLimit() && deck.card.isAction && deck.card.debt == 0) {
          card = game.cardFactory(deck.card.getName());
          break;
        }
      }
      game.playCard(card, ap, true);
      isDuration=card.isDuration;

      //be careful with cards that trash themselves
      if(card.getName().equals("deathcart") || card.getName().equals("feast")
              || card.getName().equals("embargo")){
        game.trash.remove(card);
        game.trashCard(this, ap);
      }else if(card.getName().equals("island")){
        DominionPlayer player=game.players.get(ap);
        player.island.remove(card);
        player.island.add(this);
      }
    }

    @Override
    public boolean maskCondition(DominionCard card2) {
      if (card == null) return false;
      return card.maskCondition(card2);
    }

    @Override
    public void duration(int ap){
      card.duration(ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      card.cleanup(ap, player);
      if(card.getName().equals("hermit")){
        game.trash.remove(card);
        game.trashCard(this, ap);
      }
      if(isDuration){
        player.duration.add(this);
        return true;
      }else{
        card=null;
        return false;
      }
    }
    protected int getLimit(){
      return 100;
    }
  }
}
