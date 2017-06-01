import java.util.*;

public class Guilds extends Expansion{

  public Guilds(Dominion g){
    super(g);
    String [] t={"candlestickmaker"};
    cards=t;
  }
  public static int overpay(Dominion g, int ap){
    String [] options={"Overpay","Done"};
    OptionData o=new OptionData(options);
    String input;
    int out=0;
    while(g.money>0){
      input=g.optionPane(ap,o);
      if(input.equals(options[0])){
        out++;
        g.money--;
        g.updateSharedFields();
      }else{
        break;
      }
    }
    return out;
  }  
  public class Candlestickmaker extends DominionCard{
    public Candlestickmaker(){
      super("candlestickmaker");
      cost=2;
      actions=1;
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.players.get(ap).coinTokens++;
      game.displayPlayer(ap);
    }
  }
  public class Stonemason extends RegularCard{
    public Stonemason(){
      super("stonemason");
      cost=2;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()==0) return;
      game.gainLimit=Math.max(game.cost2(game.selectedCards.get(0))-1,0);
      game.selectedCards.clear();
      game.doWork("gain",2,2,ap);
    }
    @Override
    public void onGain(int ap){
      int over=overpay(game,ap);
      int counter=0;

      boolean canGain=false;

      while(true){

        if(!game.isValidSupply(ap,over, (DominionCard c) -> c.isAction)) break;
        
        game.doWork("selectDeck",1,1,ap);
        
        if(game.supplyDecks.get(game.selectedDeck).card.isAction && 
            game.supplyDecks.get(game.selectedDeck).getCost()==over){
          
          counter++;
          game.gainCard(game.selectedDeck,ap);
        }
        if(counter==2) break; 
      }
    }      
  }
}
