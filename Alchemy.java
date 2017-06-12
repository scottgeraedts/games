import com.sun.org.apache.xalan.internal.xsltc.DOM;

import java.util.*;
public class Alchemy extends Expansion{
  static String [] potionCost={"transmute", "vineyard", "apothecary", "scrying pool", "university",
          "alchemist", "familiar", "philosphersstone", "golem", "possession"};
  public Alchemy(Dominion g){
    super(g);
    String [] t={"transmute"};
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
      game.doWork("trash", 1, 1, ap);
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
    }
    //this is just scheme with actions changed to money
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
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
      return false;
    }
  }
}
