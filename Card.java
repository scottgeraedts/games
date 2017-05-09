import java.util.*;

class Card{

	protected String name;
	protected String imagename;
	
	public Card(String newname){
		name=newname;
	}
	public String getName(){ return name;}
	public boolean equals(Card c){ return name.equals(c.getName()); }
  public String getImage(){return imagename;}
}
