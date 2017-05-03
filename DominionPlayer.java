import java.util.*;

public class DominionPlayer{
  public LinkedList<DominionCard> hand=new LinkedList<DominionCard>();
	public Deck<DominionCard> deck, disc;
	private String name;
	public int money;
	
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
    
    money=0;
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

  public static class Data{
    public Deck.Data deck, disc;
    public Data(Deck.Data a, Deck.Data b){
      deck=a;
      disc=b;
    }
  }
  public Data makeData(){
    return new Data(deck.makeData(), disc.makeData());
  }
  public int victoryPoints(){
    deck.put(disc.toArrayList());
    deck.put(hand);
    
    int out=0;
    ArrayList<DominionCard> cardlist=deck.toArrayList();
    for(int i=0;i<cardlist.size();i++){
      out+=cardlist.get(i).getPoints(cardlist);
    }
    return out;
  }
  public String getName(){ return name;}
}
