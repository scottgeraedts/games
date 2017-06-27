import java.util.*;

public class Alchemy extends Expansion{

  //this is true when, at the end of the phase, we need to start a possession phase
  static boolean possessionSwitch=false;
  //this is true when we are in a possession phase
  static boolean possessed=false;
  //this stuff stores a players material for when possession happens
  static LinkedList<DominionCard> possessionCards=new LinkedList<>();
  private static int possessor;
  static int possessee;
  private static int possesseeInterface;


  static String [] potionCost={"transmute", "vineyard", "apothecary", "scryingpool", "university",
          "alchemist", "familiar", "philosphersstone", "golem", "possession"};
  public Alchemy(Dominion g){
    super(g);
    String [] t={"transmute", "vineyard", "herbalist","apothecary", "scryingpool", "university",
            "alchemist", "familiar", "philosophersstone", "golem", "apprentice", "possession"};
    cards=t;
  }
  class Potion extends DominionCard{
    public Potion(){
      super("potion");
      cost=4;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      game.potions++;
      game.updateSharedFields();
    }
  }
  class Transmute extends RegularCard{
    public Transmute(){
      super("transmute");
      potions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
      if(game.selectedCards.size()>0){
        DominionCard card=game.selectedCards.get(0);
        if(card.isAction) game.gainCard("duchy", ap);
        if(card.isMoney) game.gainCard("transmute", ap);
        if(card.isVictory) game.gainCard("gold", ap);
      }
    }
  }
  class Vineyard extends DominionCard{
    public Vineyard(){
      super("vineyard");
      potions=1;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      int count=0;
      for(DominionCard card : cards){
        if(card.isAction) count++;
      }
      return count/3;
    }
  }
  class Herbalist extends DominionCard{
    public Herbalist(){
      super("herbalist");
      cost=2;
      buys=1;
      value=1;
      isAction=true;
    }
    //this is just scheme with actions changed to money
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      game.server.displayComment(ap, "Choose a treasure to put on top of your deck");
      OptionData o=new OptionData();
      for(DominionCard card : game.matcards){
        if(card.isMoney) o.add(card.getImage(), "imagebutton");
      }
      o.add("Done", "textbutton");
      String input=game.optionPane(ap,o);
      DominionCard card;
      for(ListIterator<DominionCard> it=game.matcards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(input.equals(card.getImage())){
          player.deck.put(card);
          it.remove();
          break;
        }
      }//loop through cards
      game.server.displayComment(ap, "");
      return false;
    }
  }
  class Apothecary extends DominionCard{
    public Apothecary(){
      super("apothecary");
      cost=2;
      potions=1;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      LinkedList<DominionCard> cards=player.draw(4);
      if(cards.size()==0) return;
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getName().equals("copper") || card.getName().equals("potion")){
          it.remove();
          player.hand.add(card);
        }
      }
      game.putBack(ap, cards);
    }
  }
  class Scryingpool extends Attack{
    String [] options={"Keep","Discard"};
    public Scryingpool(){
      super("scryingpool");
      cost=2;
      actions=1;
      potions=1;
      isAction=true;
    }
    @Override
    public void subWork(int ap){
      discardOrKeep(ap, ap);
    }
    @Override
    public void subStep(int ap, int atk){
      discardOrKeep(ap, atk);
    }
    @Override
    public void cleanup(int ap){
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      while(true){
        try {
          card=player.getCard();
          cards.add(card);
          if(card.isAction) break;
        }catch (OutOfCardsException ex){
          break;
        }
      }
      player.hand.addAll(cards);
      game.displayPlayer(ap);
    }
    private void discardOrKeep(int activePlayer, int attacker){
      OptionData o=new OptionData(options);
      try{
        DominionCard card=game.players.get(activePlayer).getCard();
        o.add(card.getImage(),"image");
        String result=game.optionPane(attacker,o);
        if(result.equals(options[0])) game.players.get(activePlayer).deck.put(card);
        else game.players.get(activePlayer).disc.put(card);
        game.displayPlayer(activePlayer);
      }catch(OutOfCardsException e){
      }
    }
  }
  class University extends DominionCard{
    public University(){
      super("university");
      cost=2;
      potions=1;
      isAction=true;
      actions=2;
    }
    @Override
    public void work(int ap){
      game.server.displayComment(ap, "Gain an action costing up to 5");
      game.gainSpecial(ap, c -> c.isAction && game.costCompare(c, 5, 0,0)<=0);
      game.server.displayComment(ap, "");
    }
  }
  class Alchemist extends DominionCard{
    public Alchemist(){
      super("alchemist");
      cost=2;
      potions=1;
      isAction=true;
      cards=2;
      actions=1;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      boolean out=false;
      String [] options={"Put on Deck", "Discard"};
      if(game.matcards.contains(game.cardFactory("potion", "Alchemy"))){
        String input=game.optionPane(ap, new OptionData(options));
        if(input.equals(options[0])){
          game.players.get(ap).deck.put(this);
          out=true;
        }
      }
      return out;
    }
  }
  class Familiar extends Attack{
    public Familiar(){
      super("familiar");
      cost=3;
      potions=1;
      cards=1;
      actions=1;
    }
    @Override
    public void subStep(int ap, int atk){
      game.gainCard("curse", ap);
    }
  }
  class Philosophersstone extends DominionCard{
    public Philosophersstone(){
      super("philosophersstone");
      cost=3;
      potions=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      game.money+=(game.players.get(ap).deck.size()+game.players.get(ap).disc.size())/3;
      game.updateSharedFields();
    }
  }
  class Golem extends DominionCard{
    public Golem(){
      super("golem");
      cost=4;
      potions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      LinkedList<DominionCard> discards=new LinkedList<>();
      LinkedList<DominionCard> actions=new LinkedList<>();

      DominionCard card;
      DominionPlayer player=game.players.get(ap);
      while(actions.size()<2){
        try{
          card=player.getCard();
        }catch (OutOfCardsException ex){
          break;
        }
        if(card.isAction && !card.getName().equals("golem")) actions.add(card);
        else discards.add(card);
      }
      player.disc.put(discards);
      OptionData o;
      while(actions.size()>0){
        o=new OptionData();
        for(DominionCard card2 : actions){
          o.put(card2.getImage(), "imagebutton");
        }
        String input=game.optionPane(ap, o);
        game.playCard(Dominion.remove(actions, c -> c.getImage().equals(input)), ap);
      }
    }
  }
  class Apprentice extends RegularCard{
    public Apprentice(){
      super("apprentice");
      cost=5;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.TRASH, 1, 1, ap);
      if(game.selectedCards.size()>0){
        game.players.get(ap).drawToHand(game.cost2(game.selectedCards.get(0))+4*game.selectedCards.get(0).potions);
        game.displayPlayer(ap);
      }
    }
  }
  class Possession extends DominionCard{
    public Possession(){
      super("possession");
      cost=6;
      potions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(!possessed) possessionSwitch=true;
    }
  }
  //stuff for possession
  void startPossession(int a, int b){
    possessionSwitch=false;
    possessed=true;
    possessor=a;
    possessee=b;
    possesseeInterface=game.server.getController(possessee);
    game.server.changeController(possessee, game.server.getController(possessor));

  }
  void endPossession(){
    possessed=false;
    game.server.changeController(possessee, possesseeInterface);
    game.players.get(possessor).disc.put(possessionCards);
    possessionCards.clear();
    game.players.get(possessee).endTurn();
  }
}
