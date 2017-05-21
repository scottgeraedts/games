import java.util.*;

public class StandardCard extends Card{

	private String suit,number;
	private int val;

	public StandardCard(String tnumber, String tsuit){
		super(tnumber+tsuit);

		String filesuit,filenum;

		number=tnumber;
		suit=tsuit;
		if(tnumber=="J"){
			val=11;
			filenum="jack";
		}else if(tnumber=="Q"){
			val=12;
			filenum="queen";
		}else if(tnumber=="K"){
			val=13;
			filenum="king";
		}else if(tnumber=="A"){
			val=14;
			filenum="ace";
		}else{
			val=Integer.parseInt(tnumber);
			filenum=tnumber;
		}
		
		filesuit="";
		if(suit=="S") filesuit="spades";
		else if(suit=="C") filesuit="clubs";
		else if(suit=="D") filesuit="diamonds";
		else if(suit=="H") filesuit="hearts";
		else System.out.println("unrecognized suit");
			
		imagename="PlayingCards/PNG-cards-1.3/"+filenum+"_of_"+filesuit+".png";
	}
	public int getVal(){ return val; }
	public int compare (StandardCard c){ return val-c.getVal(); }

}
