import java.util.*;
import java.io.*;
import javax.swing.*;

class War{

	private ArrayList<WarPlayer> player;
	private Board board;
	private ArrayList<Card> stakes=new ArrayList<Card>();
	private ArrayList<Card> stakesViz=new ArrayList<Card>();
	
	public static void main(String [] args) throws IOException{
    new War();
  }
  public War(){
		//set up players
		player=new ArrayList<WarPlayer>();
		int nplayers=2;
		for(int i=0;i<nplayers;i++)
			player.add(new WarPlayer(Integer.toString(i)));
		player.get(0).deck=new Deck("standard");
		player.get(1).deck=new Deck( player.get(0).deck.deal(26), Deck.standardBack);

  }
  public String step(){	
		//play!
		StandardCard card1,card2;

		try{
			card1=player.get(0).getCard();
			card2=player.get(1).getCard();
		}catch(OutOfCardsException e){
			System.out.println("Player"+e.name+"loses!");
			return e.name;
		}

		System.out.println("Cards are: "+card1.getName()+" "+card2.getName());
		stakes.add(card1);
		stakes.add(card2);
		int comp=card1.compare(card2);
		if(comp==0){
			System.out.println("WAR! Cards at stake are: ");
			for(int i=0;i<3;i++){
				try{ card1=player.get(0).getCard();}
				catch(OutOfCardsException e){
					player.get(0).deck.put(card1);
					break;
				}
				try{ card2=player.get(1).getCard();}
				catch(OutOfCardsException e){
					player.get(1).deck.put(card2);
					player.get(0).deck.put(card1);
					break;
				}
				stakes.add(card1);
				stakes.add(card2);
				System.out.println(card1.getName()+" "+card2.getName());
			}
		}else{
			if(comp>0) player.get(0).disc.put(stakes);
			else player.get(1).disc.put(stakes);
		}
		
		stakesViz=new ArrayList<>(stakes);
		if(comp!=0) stakes.clear();
		System.out.println(player.get(0).deck.size()+" "+player.get(0).disc.size()+" "+player.get(1).deck.size()+" "+player.get(1).disc.size());
		return "continue";
	}
	
	public static class WarData{
	  public ArrayList<WarPlayer.Data> players=new ArrayList<WarPlayer.Data>();
	  public ArrayList<String> stakes=new ArrayList<String>();
	  public WarData(ArrayList<WarPlayer.Data> ps, ArrayList<String> stakes){
      players=ps;
      this.stakes=stakes;
	  }
	
	}
	
	public WarData makeData(){
	  ArrayList<WarPlayer.Data> temp=new ArrayList<WarPlayer.Data>();
	  ArrayList<String> tempStakes=new ArrayList<String>();
	  for(int i=0;i<player.size();i++){
	    temp.add(player.get(i).makeData());
	  }
	  for(int i=0;i<stakesViz.size();i++){
	    tempStakes.add(stakesViz.get(i).getImage());
	  }
	  return new WarData(temp,tempStakes);
	}
	public static void initComponents(){
	}
}
