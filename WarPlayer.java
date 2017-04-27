import java.util.*;

class WarPlayer{

	public Deck deck, disc;
	public boolean playing;
	public String name;
	
	public WarPlayer(String newname){
		playing=true;
		disc=new Deck();
		disc.backImage=Deck.standardBack;
		name=newname;
	}
	public StandardCard getCard() throws OutOfCardsException{
		if(!deck.isEmpty()) return (StandardCard)deck.topCard(); 			
		else if(!disc.isEmpty()){
			disc.shuffle();
			System.out.println("reshuffling for "+name);
			deck=disc.copy();
			disc.clear();
			return (StandardCard) deck.topCard();
		}else{
			playing=false;
			throw new OutOfCardsException(name);
		}
		
	}
	public int nCards(){ return disc.size()+deck.size(); }

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
    
}
