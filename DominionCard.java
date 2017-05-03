import java.util.*;

public class DominionCard extends Card{
	public  boolean isMoney=false;
	public  boolean isVictory=false;
	public boolean isAction=false;
  public boolean isAttack=false;
  //a simple card is one that never goes to a different phase
  //or calls stepselection for any other reason
  //therefore stepselection will need to be called manually
  public boolean simple=true; 
	public int value=0;
	public int cost=0;
	protected int vicPoints=0;
	public int actions=0;
	public int buys=0;
	public int cards=0;

  public DominionCard(String newname){
    super(newname);
 		imagename="DominionCards/"+newname+".png";
		
		if(name=="copper"){
		  isMoney=true;
		  cost=0;
		  value=1;
		}else if(name=="silver"){
		  isMoney=true;
		  cost=3;
		  value=2;
		}else if(name=="gold"){
		  isMoney=true;
		  cost=6;
		  value=3;
		}else if(name=="estate"){
		  isVictory=true;
		  cost=2;
		  vicPoints=1;
		}else if(name=="duchy"){
		  isVictory=true;
		  cost=5;
		  vicPoints=3;
		}else if(name=="province"){
		  isVictory=true;
		  cost=8;
		  vicPoints=6;
		}else if(name=="village"){
		  cost=3;
		  actions=2;
		  cards=1;
		  isAction=true;
		}else if(name=="smithy"){
		  cost=4;
		  isAction=true;
		  cards=3;
		}else if(name=="woodcutter"){
		  cost=3;
		  isAction=true;
		  value=2;
		  buys=1;
		}else if(name=="festival"){
		  cost=5;
		  isAction=true;
		  value=2;
		  buys=1;
		  actions=2;
		}else if(name=="laboratory"){
		  cost=5;
		  isAction=true;
		  cards=2;
		  actions=1;
		}else if(name=="market"){
		  cost=5;
		  isAction=true;
		  cards=1;
		  actions=1;
		  buys=1;
		  value=1;
		}else if(name=="moat"){
		  cost=2;
		  isAction=true;
		  cards=2;
		}else if(name=="curse"){
		  vicPoints=-1;
		}else{
		  simple=false;
		  //if its not one of the above its probably not simple
		}
  }
  public void work(){
  }
  public void step(){}
  public boolean wrapup(){
    return true;
  }
  public boolean [] makeMask(Collection<DominionCard> hand){
      boolean [] mask=new boolean[hand.size()];
      Arrays.fill(mask,false);
      int i=0;
      for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
        mask[i]=maskCondition(it.next());
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
}
