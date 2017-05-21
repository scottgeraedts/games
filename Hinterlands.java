import java.util.*;

public class Hinterlands extends Expansion{

  public Hinterlands(Dominion g){
    super(g);
    String [] temp={"crossroads"};
    cards=temp;
  }
  public class Crossroads extends DominionCard{
    public Crossroads(){
      super("crossroads");
      cost=2;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      int temp=0;
      for(DominionCard card : player.hand){
        if(card.isVictory) temp++;
      }
      player.drawToHand(temp);
      game.displayPlayer(ap);
      if(!game.crossroadsPlayed){
        game.actions++;
        game.updateSharedFields();
        game.crossroadsPlayed=true;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      game.crossroadsPlayed=false;
      return false;
    }
  }
  public class Duchess extends Attack{
    public Duchess(){
      super("duchess");
      cost=2;
      isAttack=false;
      value=2;
    }
    @Override
    public void subWork(int ap){
      maybeDiscard(ap);
    }
    @Override
    public void subStep(int ap, int atk){
      maybeDiscard(ap);
    }
    public void maybeDiscard(int ap){
      String [] options={"Discard","Done"};
      OptionData o=new OptionData(options);
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      try{
        card=player.getCard();
        o.put(card.getImage(),"image");
        String input=game.optionPane(ap,o);
        if(input.equals(options[0])){
          player.disc.put(card);
        }else{
          player.deck.put(card);
        }
        game.displayPlayer(ap);
      }catch(OutOfCardsException ex){}
    }
  }
  public class Foolsgold extends DominionCard{
    public Foolsgold(){
      super("foolsgold");
      cost=2;
      value=1;
    }
    @Override
    public void work(int ap){
      if(game.foolsGoldPlayed){
        game.money+=3;
        game.updateSharedFields();          
      }
      game.foolsGoldPlayed=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      game.foolsGoldPlayed=false;
      return false;
    }
  }




}
  

