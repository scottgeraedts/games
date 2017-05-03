import java.util.*;
import java.lang.Exception;
import java.lang.reflect.Array;

class Deck<T extends Card>{
	protected LinkedList<T> cards;
	private Random ran;
	public boolean faceup=false;
	
	//paths for images
	public String backImage;
	public static String standardBack="PlayingCards/PNG-cards-1.3/back.jpg";
	public static String dominionBack="DominionCards/back.png";
	public static String blankBack="DominionCards/empty.png";

	//constructors and stuff
	public Deck(){ 
		ran=new Random();
		cards=new LinkedList<T>(); 
	}
	public Deck(String type){
		this();

		if(type=="standard"){

		}else
			System.out.println("invalid deck type specified");
		
		
	}
	public Deck(Collection<T> newcards){
		this();
		cards=new LinkedList<T>(newcards);
	}
	public Deck(Collection<T> newcards,String deckBackPath){
		this(newcards);
		backImage=deckBackPath;
	}	
	
	public Deck<T> copy(){
		return new Deck<T>(cards, backImage);
	}
	
	public void shuffle(){
		LinkedList<T> newcards=new LinkedList<T>();
			
		while(cards.size()>0){
			newcards.add(randomCard());
		}
		cards=newcards;
	}
	
	public void printDeck(){
		for(int i=0;i<cards.size();i++)
			System.out.println(cards.remove().getName());
	
	}
	
	//get cards from deck
	public ArrayList<T> deal(int n){
		ArrayList<T> out=new ArrayList<T>();
		for(int i=0;i<n;i++){
			if(cards.size()<1) System.out.println("deal ran out of cards!");
			out.add(cards.removeFirst());
		}
		return out;
	}
	public T topCard(){ 
		if(isEmpty()) System.out.println("topCard out of cards!");
		return cards.removeFirst(); 
	}
	public T randomCard(){
		if(isEmpty()) System.out.println("randomCard out of cards!");
		
		Iterator<T> it=cards.iterator();
		int n=ran.nextInt(cards.size());
		T out=it.next();
		for(int i=0;i<n;i++){
			out=it.next();
		}
		it.remove();
		return out;
	}
	public T peekTop(){
	  return cards.peek();
	}
	public ArrayList<T> toArrayList(){
	  return new ArrayList<T>(cards);
	}
	
	//put cards on deck
	public void put(T c){ cards.addFirst(c);	}
	public void put(Collection<T> c){ 
    Iterator<T> it=c.iterator();
    while(it.hasNext()) cards.addFirst( it.next());
//		for(int i=0;i<c.size();i++) cards.addFirst(c.get(i));
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
    String image;
    if(size()==0) image=blankBack;
    else if(faceup) image=cards.get(0).getImage();
    else image=backImage;
    return new Data(cards.size(), image);
  }
  
  public static Deck<StandardCard> makeStandardDeck(){
    ArrayList<StandardCard> newcards=new ArrayList<>();
    
		ArrayList<String> suits=new ArrayList<String>();
		suits.add("S"); suits.add("C"); suits.add("H"); suits.add("D");
		ArrayList<String> numbers=new ArrayList<String>();
		numbers.add("A"); numbers.add("J"); numbers.add("Q"); numbers.add("K");
		for(int i=2;i<11;i++) numbers.add(Integer.toString(i));
	
		for(int s=0;s<suits.size();s++){
			for(int n=0;n<numbers.size();n++){
				newcards.add(new StandardCard(numbers.get(n),suits.get(s)));
			}
		}
		Deck<StandardCard> out=new Deck<>(newcards,standardBack);
		out.shuffle();
		return out;
  }
}
