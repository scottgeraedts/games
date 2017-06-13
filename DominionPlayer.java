import java.util.*;

public class DominionPlayer{
  public LinkedList<DominionCard> hand=new LinkedList<DominionCard>();
	public Deck<DominionCard> deck, disc;
	private String name;
	public LinkedList<DominionCard> duration=new LinkedList<>();
	public int vicTokens=0;
	public int coinTokens=0;
	public int debt=0;
	
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
  public LinkedList<DominionCard> draw(int n){
    LinkedList<DominionCard> out=new LinkedList<>();
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
  public Pair<Integer, String> victoryPoints(){

    //put all cards from all sources into the deck
    deck.put(disc);
    deck.put(hand);
    deck.put(island);
    deck.put(duration);
    deck.put(nativevillage);
    for(DominionCard card : deck){
      if(card.getName().equals("archive")){
        deck.put( ((Empires.Archive)card).cards);
      }
    }


    HashMap<String,Integer> cards=new HashMap<>();
    
    int total=0;
    String temp;
    for(DominionCard card : deck){
      total+=card.getPoints(deck);
      temp=card.getName();
      if(card.isVictory || card.getName().equals("curse")){
        if(cards.containsKey(temp)) cards.put(temp,cards.get(temp)+1);
        else cards.put(temp,1);
      }
    }
    total+=vicTokens;
    String out="";
    for(Map.Entry<String,Integer> entry : cards.entrySet())
      out+=entry.getKey()+": "+entry.getValue()+", ";
    out+="Victory tokens: "+vicTokens;
    return new Pair<Integer, String>(total,out);
  }
  public String getName(){ return name;}
  
  //Data stuff
  public static class Data{
    public ArrayList<DominionCard> hand=new ArrayList<>();
    public Deck.Data deck, disc;
    public String name;
    int vicTokens;
    int coinTokens;
    ArrayList<String> islandCards=new ArrayList<>();
    ArrayList<String> durationCards=new ArrayList<>();
    ArrayList<String> nativeVillage=new ArrayList<>();
    int pirateship=0;
    int debt;
    
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
      debt=Integer.parseInt(parts[handSize+10]);
    }
    public String toString(){
      String out=""+hand.size();
      for (DominionCard aHand : hand) {
        out += "@" + aHand.toString();
      }
      out+="@"+deck.toString()+"@"+disc.toString()+"@"+name;
      out+="@"+DominionServer.toArray(durationCards);
      out+="@"+DominionServer.toArray(islandCards);
      out+="@"+DominionServer.toArray(nativeVillage);
      out+="@"+pirateship;
      out+="@"+vicTokens;
      out+="@"+coinTokens;
      out+="@"+debt;
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
    out.debt=debt;
    return out;
  }
}
