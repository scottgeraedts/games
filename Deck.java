import java.util.*;
import java.lang.Exception;

class Deck{
	private LinkedList<Card> cards;
	private Random ran;
	
	//paths for images
	public String backImage;
	public static String standardBack="PlayingCards/PNG-cards-1.3/back.jpg";

	//constructors and stuff
	public Deck(){ 
		ran=new Random();
		cards=new LinkedList<Card>(); 
	}
	public Deck(String type){
		this();

		if(type=="standard"){
			ArrayList<String> suits=new ArrayList<String>();
			suits.add("S"); suits.add("C"); suits.add("H"); suits.add("D");
			ArrayList<String> numbers=new ArrayList<String>();
			numbers.add("A"); numbers.add("J"); numbers.add("Q"); numbers.add("K");
			for(int i=2;i<11;i++) numbers.add(Integer.toString(i));
		
			for(int s=0;s<suits.size();s++){
				for(int n=0;n<numbers.size();n++){
					cards.add(new StandardCard(numbers.get(n),suits.get(s)));
				}
			}
			shuffle();
      backImage=standardBack;
		}else
			System.out.println("invalid deck type specified");
		
		
	}
	public Deck(Collection<Card> newcards){
		this();
		cards=new LinkedList<Card>(newcards);
	}
	public Deck(Collection<Card> newcards,String deckBackPath){
		this(newcards);
		backImage=deckBackPath;
	}	
	
	public Deck copy(){
		return new Deck(cards, backImage);
	}
	
	public void shuffle(){
		LinkedList<Card> newcards=new LinkedList<Card>();
			
		while(cards.size()>0){
			newcards.add(randomCard());
		}
		cards=newcards;
	}
	
	public void printDeck(){
		for(int i=0;i<cards.size();i++)
			System.out.println(cards.remove().getName());
	
	}
	public ArrayList<Card> deal(int n){
		ArrayList<Card> out=new ArrayList<Card>();
		for(int i=0;i<n;i++){
			if(cards.size()<1) System.out.println("deal ran out of cards!");
			out.add(cards.removeFirst());
		}
		return out;
	}
	public Card topCard(){ 
		if(isEmpty()) System.out.println("topCard out of cards!");
		return cards.removeFirst(); 
	}
	public Card randomCard(){
		if(isEmpty()) System.out.println("randomCard out of cards!");
		
		Iterator<Card> it=cards.iterator();
		int n=ran.nextInt(cards.size());
		Card out=it.next();
		for(int i=0;i<n;i++){
			out=it.next();
		}
		it.remove();
		return out;
	}
	public void put(Card c){ cards.addFirst(c);	}
	public void put(ArrayList<Card> c){ 
		for(int i=0;i<c.size();i++) cards.addFirst(c.get(i));
	}	
	public boolean isEmpty(){ return cards.isEmpty(); }
	public int size(){ return cards.size(); }
	public void clear(){ cards.clear(); }

  public static class Data{
    public int size;
    public String image;
    public Data(int a, String image){
      size=a;
      this.image=image;
    }
  }
  public Data makeData(){
    return new Data(cards.size(), backImage);
  }
}
