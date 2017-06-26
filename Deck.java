import java.util.*;
import java.lang.Exception;
import java.lang.reflect.Array;

class Deck<T extends Card> extends LinkedList<T>{
//	protected LinkedList<T> cards;
	private Random ran;
	boolean faceup=false;
	
	//paths for images
	String backImage;
	static String standardBack="PlayingCards/PNG-cards-1.3/back.jpg";
	static String dominionBack="back";
	static String blankBack="empty";

	//constructors and stuff
	public Deck(){ 
		ran=new Random();
		//cards=new LinkedList<T>(); 
	}
	public Deck(Collection<T> newcards){
		super(newcards);
		ran=new Random();
	}
	public Deck(Collection<T> newcards,String deckBackPath){
		this(newcards);
		backImage=deckBackPath;
	}	
	
	public Deck<T> copy(){
		return new Deck<T>(this, backImage);
	}
	
	public void shuffle(){
		ArrayList<T> newcards=new ArrayList<T>();
	  Deck<T> oldCards=copy();
	  
	  clear();
		while(oldCards.size()>0){
			add(oldCards.randomCard());
		}
	}
	
	public void printDeck(){
		for(int i=0;i<size();i++)
			System.out.println(get(i).getName());
	
	}
	
	//get cards from deck
	public ArrayList<T> deal(int n){
		ArrayList<T> out=new ArrayList<T>();
		for(int i=0;i<n;i++){
			if(size()<1) System.out.println("deal ran out of cards!");
			out.add(removeFirst());
		}
		return out;
	}
	public T topCard(){ 
		if(isEmpty()) System.out.println("topCard out of cards!");
		return removeFirst(); 
	}
	public T randomCard(){
		if(isEmpty()) System.out.println("randomCard out of cards!");
		return remove(ran.nextInt(size()));
	}
	//add cards on deck
	public void put(T c){ addFirst(c);	}
	public void put(Collection<T> c){ 
    addAll(0,c);
	}	

  public static class Data{
    public int size;
    public String image;
    public Data(int a, String image){
      size=a;
      this.image=image;
    }
    public Data(){}
    public Data(String in){
      String [] parts=in.split("&");
      size=Integer.parseInt(parts[0]);
      image=parts[1];
    }
    public String toString(){
      return size+"&"+image;
    }
  }
  public static class SupplyData extends Data{
    int cost;
    String name;
    boolean landmark;
    boolean event;
    PairList<String, String> icons;
    public SupplyData(int a, String image, int tcost, String name, boolean event, boolean landmark, PairList<String, String> icons){
      super(a,image);
      size=a;
      this.image=image;
      cost=tcost;
      this.name=name;
      this.event=event;
      this.landmark=landmark;
			this.icons=icons;
    }
    public SupplyData(String in){
      super(in);
      String [] parts=in.split("&");
      cost=Integer.parseInt(parts[2]);
      name=parts[3];      
			event=Boolean.parseBoolean(parts[4]);
			landmark=Boolean.parseBoolean(parts[5]);
			icons=new PairList<>(parts[6],String.class, String.class);

    }
    public SupplyData(){}
    @Override
    public String toString(){
      return super.toString()+"&"+cost+"&"+name+"&"+event+"&"+landmark+"&"+icons.toString();
    }
  }
  public Data makeData(){
    String image;
    if(size()==0) image=blankBack;
    else if(faceup) image=get(0).getImage();
    else image=backImage;
    return new Data(size(), image);
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
