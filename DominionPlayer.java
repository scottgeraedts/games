import java.util.*;

public class DominionPlayer{
  public LinkedList<DominionCard> hand=new LinkedList<DominionCard>();
	public Deck<DominionCard> deck, disc;
	private String name;
	public LinkedList<DominionCard> duration=new LinkedList<>();
	public int vicTokens=0;
	public int coinTokens=0;
	
	//****specific card related stuff***///
	//nativevillage
	public ArrayList<DominionCard> nativevillage=new ArrayList<>();
	//island
	public ArrayList<DominionCard> island=new ArrayList<>();
	//pirateship
	public int pirateship=0;
	//horsetraders
	public ArrayList<DominionCard> horseTraders=new ArrayList<>();
	
  public DominionPlayer(String newname){
		disc=new Deck<DominionCard>();
		name=newname;
    disc.backImage=Deck.dominionBack;
    disc.faceup=true;
    
    ArrayList<DominionCard> starting=new ArrayList<>();
//    for(int i=0;i<3;i++) starting.add( new DominionCard("estate"));
    for(int i=0;i<7;i++) starting.add( new DominionCard("copper"));
    deck=new Deck<DominionCard>(starting,Deck.dominionBack);

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
  public ArrayList<DominionCard> draw(int n){
    ArrayList<DominionCard> out=new ArrayList<>();
    for(int i=0;i<n;i++){
      try{
        out.add(getCard());
      }catch(OutOfCardsException e){
        break;
      }
    }
    return out;
  }

  //returns the breakdown of victory points for this player
  public String victoryPoints(){
    deck.put(disc);
    deck.put(hand);
    deck.put(island);
    deck.put(duration);
    deck.put(nativevillage);
    HashMap<String,Integer> cards=new HashMap<>();
    
    int total=0;
    String temp;
    ArrayList<DominionCard> cardlist=new ArrayList<>(deck);
    for(DominionCard card : cardlist){
      total+=card.getPoints(cardlist);
      temp=card.getName();
      if(card.isVictory || card.getName().equals("curse")){
        if(cards.containsKey(temp)) cards.put(temp,cards.get(temp)+1);
        else cards.put(temp,1);
      }
    }
    total+=vicTokens;
    String out="";
    if(total<10) out+=" ";
    out+=total+"  (";
    for(Map.Entry<String,Integer> entry : cards.entrySet())
      out+=entry.getKey()+": "+entry.getValue()+", ";
    out+="Victory tokens: "+vicTokens;
    return out+")";
  }
  public String getName(){ return name;}
  
  //Data stuff
  public static class Data{
    public ArrayList<DominionCard> hand=new ArrayList<>();
    public Deck.Data deck, disc;
    public String name;
    public int vicTokens;
    public int coinTokens;
    public ArrayList<String> islandCards=new ArrayList<>();
    public ArrayList<String> durationCards=new ArrayList<>();
    public ArrayList<String> nativeVillage=new ArrayList<>();
    public int pirateship=0;
    
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
      durationCards=DominionClient.readArray(parts[handSize+4],"");
      islandCards=DominionClient.readArray(parts[handSize+5],"");
      nativeVillage=DominionClient.readArray(parts[handSize+6],"");
      pirateship=Integer.parseInt(parts[handSize+7]);
      vicTokens=Integer.parseInt(parts[handSize+8]);
      coinTokens=Integer.parseInt(parts[handSize+9]);
    }
    public String toString(){
      String out=""+hand.size();
      for(int i=0;i<hand.size();i++){
        out+="@"+hand.get(i).toString();
      }
      out+="@"+deck.toString()+"@"+disc.toString()+"@"+name;
      out+="@"+DominionServer.toArray(durationCards);
      out+="@"+DominionServer.toArray(islandCards);
      out+="@"+DominionServer.toArray(nativeVillage);
      out+="@"+pirateship;
      out+="@"+vicTokens;
      out+="@"+coinTokens;
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
    for(DominionCard card2 : duration){
      out.durationCards.add(card2.getImage());
    }
    for(DominionCard card2 : island){
      out.islandCards.add(card2.getImage());
    }
    for(DominionCard card2 : nativevillage){
      out.nativeVillage.add(card2.getImage());
    }
    out.pirateship=pirateship;
    out.vicTokens=vicTokens;
    out.coinTokens=coinTokens;
    return out;
  }
}
