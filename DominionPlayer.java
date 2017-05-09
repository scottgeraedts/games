import java.util.*;

public class DominionPlayer{
  public LinkedList<DominionCard> hand=new LinkedList<DominionCard>();
	public Deck<DominionCard> deck, disc;
	private String name;
	
  public DominionPlayer(String newname){
		disc=new Deck<DominionCard>();
		name=newname;
    disc.backImage=Deck.dominionBack;
    disc.faceup=true;
    
    ArrayList<DominionCard> starting=new ArrayList<>();
    for(int i=0;i<3;i++) starting.add( new DominionCard("estate"));
    for(int i=0;i<7;i++) starting.add( new DominionCard("copper"));
    deck=new Deck<DominionCard>(starting,Deck.dominionBack);
    deck.shuffle();    
    drawToHand(5);
    
  }

  public void endTurn(){
    disc.put(hand);
    hand.clear();
    drawToHand(5);
  }

	public int nCards(){ return disc.size()+deck.size(); }

	public DominionCard getCard() throws OutOfCardsException{
		if(!deck.isEmpty()) return deck.topCard(); 			
		else if(!disc.isEmpty()){
			disc.shuffle();
			System.out.println("reshuffling for "+name);
			deck=disc.copy();
			disc.clear();
			return deck.topCard();
		}else{
			throw new OutOfCardsException(name);
		}
		
	}  
  public void drawToHand(int n){
    for(int i=0;i<n;i++){
      try{
        hand.add( getCard()); 
      }catch(OutOfCardsException e){
        break;
      }
    }
  }

  public int victoryPoints(){
    deck.put(disc);
    deck.put(hand);
    
    int out=0;
    ArrayList<DominionCard> cardlist=new ArrayList<>(deck);
    for(int i=0;i<cardlist.size();i++){
      out+=cardlist.get(i).getPoints(cardlist);
    }
    return out;
  }
  public String getName(){ return name;}
  
  //Data stuff
  public static class Data{
    public ArrayList<DominionCard> hand=new ArrayList<>();
    public Deck.Data deck, disc;
    public String name;
    public Data(){}
    public Data (String in){
      String [] parts=in.split("@");
      int handSize=Integer.parseInt(parts[0]);
      hand=new ArrayList<>(handSize);
      for(int i=0;i<handSize;i++){
        hand.add(new DominionCard(parts[i+1],0));
      }
      deck=new Deck.Data(parts[handSize+1]);
      disc=new Deck.Data(parts[handSize+2]);
      name=parts[handSize+3];
    }
    public String toString(){
      String out=""+hand.size();
      for(int i=0;i<hand.size();i++){
        out+="@"+hand.get(i).toString();
      }
      out+="@"+deck.toString()+"@"+disc.toString()+"@"+name;
      return out;
    }
  }
  public Data makeData(){
    Data out=new Data();
    out.deck=deck.makeData();
    out.disc=disc.makeData();
    out.name=name;
    DominionCard card;
    for(Iterator<DominionCard> it=hand.iterator(); it.hasNext(); ){
      card=it.next();
      out.hand.add(new DominionCard(card.isAction, card.isMoney, card.getImage()));
    }
    return out;
  }
}
