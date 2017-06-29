import java.util.*;

public class DominionPlayer{
  public LinkedList<DominionCard> hand=new LinkedList<DominionCard>();
	public Deck<DominionCard> deck, disc;
	private String name;
	LinkedList<DominionCard> duration=new LinkedList<>();
	int vicTokens=0;
	int coinTokens=0;
	int debt=0;
  //adventures stuff
	LinkedList<DominionCard> tavern=new LinkedList<>();
	boolean journey=false;
  boolean minusMoneyToken=false;
  boolean minusCardToken=false;

	//****specific card related stuff***///
	//nativevillage
	ArrayList<DominionCard> nativevillage=new ArrayList<>();
	//island
	ArrayList<DominionCard> island=new ArrayList<>();
	//pirateship
	int pirateship=0;
	//horsetraders
	ArrayList<DominionCard> horseTraders=new ArrayList<>();
	//champions
  int champions=0;
  //adventures tokens which are activate on play and are set by teacher (card, money, buy, action)
  HashMap<String, String> adventureTokens=new HashMap<>();
  //miser
  int miser=0;
  //hautned woods
  boolean hauntedWoods=false;
  //swamp hag
  boolean swampHag=false;
  //save
  DominionCard saveCard=null;
  //expidition
  boolean expedition=false;

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

  void endTurn(){
    disc.put(hand);
    hand.clear();
    drawToHand(5);
    if(saveCard != null){
      hand.add(saveCard);
      saveCard=null;
    }
    if(expedition){
      drawToHand(2);
      expedition=false;
    }
  }

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
    if(minusCardToken){
      n--;
      minusCardToken=false;
    }
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
  Pair<Integer, String> victoryPoints(){

    //put all cards from all sources into the deck
    deck.put(disc);
    deck.put(hand);
    deck.put(island);
    deck.put(duration);
    deck.put(nativevillage);
    LinkedList<DominionCard> archiveCards=new LinkedList<>();
    for(DominionCard card : deck){
      if(card.getName().equals("archive")){
        archiveCards.addAll( ((Empires.Archive)card).cards);
      }
    }
    deck.put(archiveCards);
    deck.put(tavern);

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
    //distant lands
    for(DominionCard card : tavern){
      if(card.getName().equals("distantlands")) total+=4;
    }
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
    int pirateship;
    int debt;
    int miser;
    boolean journey;
    boolean minusMoneyToken;
    boolean minusCardToken;
    ArrayList<String> tavern=new ArrayList<>();
    
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
      miser=Integer.parseInt(parts[handSize+11]);
      journey=Boolean.parseBoolean(parts[handSize+12]);
      minusMoneyToken=Boolean.parseBoolean(parts[handSize+13]);
      minusCardToken=Boolean.parseBoolean(parts[handSize+14]);
      tavern=DominionClient.readArray(parts[handSize+15], "");
    }
    public String toString(){
      StringBuilder out=new StringBuilder();
      out.append(hand.size());
      for (DominionCard aHand : hand) {
        out.append("@").append(aHand.toString());
      }
      out.append("@").append(deck.toString()).append("@").append(disc.toString()).append("@").append(name);
      out.append("@").append(DominionServer.toArray(durationCards));
      out.append("@").append(DominionServer.toArray(islandCards));
      out.append("@").append(DominionServer.toArray(nativeVillage));
      out.append("@").append(pirateship);
      out.append("@").append(vicTokens);
      out.append("@").append(coinTokens);
      out.append("@").append(debt);
      out.append("@").append(miser);
      out.append("@").append(journey);
      out.append("@").append(minusMoneyToken);
      out.append("@").append(minusCardToken);
      out.append("@").append(DominionServer.toArray(tavern));
      return out.toString();
    }
  }
  Data makeData(){
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
    out.miser=miser;
    out.journey=journey;
    out.minusMoneyToken=minusMoneyToken;
    out.minusCardToken=minusCardToken;
    for(DominionCard card2 : tavern){
      out.tavern.add(card2.getImage());
    }

    return out;
  }
}
