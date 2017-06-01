import java.util.*;
import java.util.function.Predicate;

public class DominionCard extends Card{
	public  boolean isMoney=false;
	public  boolean isVictory=false;
	public boolean isAction=false;
  public boolean isAttack=false;
  public boolean isDuration=false;
  
  public boolean isReaction1=false;
  public boolean isReaction2=false;
  public boolean isReaction3=false;
  public boolean isReactionX=false;
  
	public int value=0;
	public int cost=0;
	protected int vicPoints=0;
	public int actions=0;
	public int buys=0;
	public int cards=0;
	
	//a switch to tell duration cards activate their duration power twice 
	//or three times if kings courted
	public int throneroomed=1;

  public DominionCard(String newname){
    super(newname);
 		imagename=newname;
		
		if(name.equals("copper")){
		  isMoney=true;
		  cost=0;
		  value=1;
		}else if(name.equals("silver")){
		  isMoney=true;
		  cost=3;
		  value=2;
		}else if(name.equals("gold")){
		  isMoney=true;
		  cost=6;
		  value=3;
		}else if(name.equals("platinum")){
		  isMoney=true;
		  cost=9;
		  value=5;		
		}else if(name.equals("estate")){
		  isVictory=true;
		  cost=2;
		  vicPoints=1;
		}else if(name.equals("duchy")){
		  isVictory=true;
		  cost=5;
		  vicPoints=3;
		}else if(name.equals("province")){
		  isVictory=true;
		  cost=8;
		  vicPoints=6;
		}else if(name.equals("colony")){
		  isVictory=true;
		  cost=11;
		  vicPoints=10;
		}else if(name.equals("village")){
		  cost=3;
		  actions=2;
		  cards=1;
		  isAction=true;
		}else if(name.equals("smithy")){
		  cost=4;
		  isAction=true;
		  cards=3;
		}else if(name.equals("woodcutter")){
		  cost=3;
		  isAction=true;
		  value=2;
		  buys=1;
		}else if(name.equals("festival")){
		  cost=5;
		  isAction=true;
		  value=2;
		  buys=1;
		  actions=2;
		}else if(name.equals("laboratory")){
		  cost=5;
		  isAction=true;
		  cards=2;
		  actions=1;
		}else if(name.equals("market")){
		  cost=5;
		  isAction=true;
		  cards=1;
		  actions=1;
		  buys=1;
		  value=1;
		}else if(name.equals("moat")){
		  cost=2;
		  isAction=true;
		  cards=2;
		  isReaction1=true;
		}else if(name.equals("curse")){
 		  vicPoints=-1;
    }else if(name.equals("harem")){
      vicPoints=2;
      cost=6;
      value=2;
      isMoney=true;
      isVictory=true;
    }else if(name.equals("greathall")){
      cost=3;
      vicPoints=1;
      actions=1;
      cards=1;
      isVictory=true;
      isAction=true;
    }else if(name.equals("bazaar")){
      cost=5;
      isAction=true;
      value=1;
      actions=2;
      cards=1;  
    }else if(name.equals("workersvillage")){
      cost=4;
      actions=2;
      cards=1;
      buys=1;
      isAction=true;
    }else if(name.equals("grandmarket")){
      cost=6;
      actions=1;
      cards=1;
      buys=1;
      value=2;
      isAction=true;
    }else if(name.equals("peddler")){
      cost=8;
      actions=1;
      cards=1;
      value=1;
      isAction=true;
    }else if(name.equals("tunnel")){
      cost=3;
      isVictory=true;
      isReactionX=true;
      vicPoints=2;
		}else{
		}
  }
  public DominionCard(boolean a, boolean b, String c){
    super(c);
    imagename=c;
    isAction=a;
    isMoney=b;
  }
  public DominionCard(String in, int t){
    super(in);
    String [] parts=in.split("!");
    isAction=Boolean.parseBoolean(parts[0]);
    isMoney=Boolean.parseBoolean(parts[1]);
    imagename=parts[2];
  }

  public void work(int x){}
  public void duration(int x){}
  //takes care of what to do with this card at the end of the turn
  //if it returns true, the card will not be added to the discard pile
  //it gets the player object directly since we can't modify the player here in dominioncard
  //it also gets the index of the player so its subclasses can do stuff
  public boolean cleanup(int x, DominionPlayer player){
    if(isDuration){
      player.duration.add(this);
      return true;
    }else{
      return false;
    }
  }
  public void onGain(int x){}
  
  public ArrayList<Boolean> makeMask(Collection<DominionCard> hand){
      ArrayList<Boolean> mask=new ArrayList<Boolean>(hand.size());
      int i=0;
      for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        mask.add(maskCondition(it.next()));
        i++;
      }
      return mask;
  }
  public ArrayList<Boolean> makeMask(Collection<DominionCard> hand, Predicate<DominionCard> tester){
    ArrayList<Boolean> mask=new ArrayList<Boolean>(hand.size());
    int i=0;
    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        mask.add(tester.test(it.next()));
        i++;
    }
    return mask;
  }
  public boolean maskCondition(DominionCard card){
    return true;
  }
  public int getPoints(Collection<DominionCard> cards){
    return vicPoints;
  }
  public boolean isReaction(){
    return isReaction1 || isReaction2 || isReactionX;
  }
  //what the AI should do if this card makes it trash/discard/etc
  //if this returns default there is some default behavior
  //but individual cards can override this
  public String AIResponse(){
    return "default";
  }
  @Override
  public String toString(){
    return isAction+"!"+isMoney+"!"+imagename;
  }
  
}
