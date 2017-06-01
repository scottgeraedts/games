import java.util.*;

public class DarkAges extends Expansion{
  public static String [] shelterNames={"hovel", "necropolis" , "overgrownestate"};
  public DarkAges(Dominion g){
    super(g);
    cards=darkAgesCards;
  }
  class Hovel extends DominionCard{
    public Hovel(){
      super("hovel");
      cost=1;
      isVictory=true;
      isReactionX=true;
      isShelter=true;
    }
  }
  class Necropolis extends DominionCard{
    public Necropolis(){
      super("necropolis");
      cost=1;
      isShelter=true;
      actions=2;
      isAction=true;
    }
  }
  class Overgrownestate extends DominionCard{
    public Overgrownestate(){
      super("overgrownestate");
      cost=1;
      isVictory=true;
      isShelter=true;
    }
    @Override
    public void onTrash(int ap){
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
    }
  }
  class Poorhouse extends DominionCard{
    public Poorhouse(){
      super("poorhouse");
      cost=1;
      isAction=true;
      value=4;
    }
    @Override
    public void work(int ap){
      int total=0;
      for(DominionCard card : game.players.get(ap).hand){
        if(card.isMoney) total++;
      }
      game.money-=Math.min(total, 4);
      game.updateSharedFields();
    }
  }

}
