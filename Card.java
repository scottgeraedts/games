import java.util.*;

class Card{

	protected String name;
	protected String imagename;
	
	public Card(String newname){
		name=newname;
	}
	public String getName(){ return name;}
  public String getImage(){return imagename;}
	@Override
	public boolean equals(Object o){ 
	  if(o==null) return false;
	  if(o==this) return true;
	  if(getClass() != o.getClass()) return false;
	  Card c=(Card)o;
	  return name.equals(c.getName()); 
	}
  @Override
  public int hashCode(){
    return name.hashCode();
  }
  @Override
  public String toString(){
    return name;
  }
}
