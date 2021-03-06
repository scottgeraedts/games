import java.util.*;

public class Seaside extends Expansion{
  public static ArrayList<String> smugglerCards1=new ArrayList<>();
  public static ArrayList<String> smugglerCards2=new ArrayList<>();
  public static boolean outpost;
  public static boolean victoryBought=false; //did we gain a victory card this turn (for treasury)

  public Seaside(Dominion g){
    super(g);
    String [] t={"embargo","haven","lighthouse","nativevillage","pearldiver","ambassador",
        "fishingvillage","lookout","smugglers","warehouse","caravan","cutpurse","island",
        "navigator","pirateship","salvager","seahag","treasuremap","explorer","ghostship",
        "merchantship","outpost","tactician","treasury","wharf"};
    cards=t;
  }
  public class Embargo extends RegularCard{
    public Embargo(){
      super("embargo");
      cost=2;
      value=2;
    }
    @Override
    public void subWork(int activePlayer){
      game.server.displayComment(activePlayer,"Choose a pile for the embargo token");
      game.doWork(Dominion.Phase.SELECT_DECK,0,1,activePlayer);
      game.supplyDecks.get(game.selectedDeck).embargo++;
      game.matcards.remove(this);
      game.trashCard(this, activePlayer);
      game.displaySupply(game.selectedDeck);
      lostTrack=true;
    }
  }
  public class Haven extends RegularCard{
    DominionCard card;
    public Haven(){
      super("haven");
      cost=2;
      actions=1;
      cards=1;
      isDuration=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.SELECT,0,1,ap);
      card=game.selectedCards.get(0);
    }
    @Override
    public void duration(int ap){
      game.players.get(ap).hand.add(card);
      game.displayPlayer(ap);
    }
  }
  public class Lighthouse extends DominionCard{
    public Lighthouse(){
      super("lighthouse");
      cost=2;
      actions=1;
      value=1;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void duration(int ap){
      game.money++;
      game.updateSharedFields();
    }
  }
  public class Nativevillage extends RegularCard{
    String [] options={"Set Aside","Draw Mat Cards"};
    public Nativevillage(){
      super("nativevillage");
      cost=2;
      actions=2;
    }
    @Override
    public void subWork(int ap){
      DominionPlayer player=game.players.get(ap);
      String input=game.optionPane(ap,new OptionData(options));
      if(input.equals(options[0])){
        try{
          player.nativevillage.add(player.getCard());
        }catch(OutOfCardsException ex){}
      }else{
        for(DominionCard card : player.nativevillage){
          player.hand.add(card);
        }
        player.nativevillage.clear();
      }
      game.displayPlayer(ap);
    }
  }
  public class Pearldiver extends DominionCard{
    String [] options={"Put on top","Put on bottom"};
    public Pearldiver(){
      super("pearldiver");
      cost=2;
      actions=1;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      if(player.deck.size()==0) return;
      DominionCard card=player.deck.removeLast();
      OptionData o=new OptionData(options);
      o.add(card.getImage(),"image");
      String input=game.optionPane(ap,o);
      if(input.equals(options[0])){
        player.deck.put(card);
      }else{
        player.deck.addLast(card);
      }            
    }    
  }
  public class Ambassador extends Attack{
    DominionCard card;
    public Ambassador(){
      super("ambassador");
      cost=3;
    }
    @Override 
    public void subWork(int ap){
      game.server.displayComment(ap,"choose the type of card");

      game.doWork(Dominion.Phase.REVEAL,1,1,ap);
      card=game.selectedCards.get(0);
      game.server.displayComment(ap,"Return up to two of those cards");
      game.displayPlayer(ap);
      game.selectedCards.clear();
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork(Dominion.Phase.SELECT,0,2,ap);
      for(DominionCard card : game.selectedCards){
        game.returnToSupply(card, ap);
      }
      game.displaySupply(card.getName());
    }
    @Override
    public void subStep(int victim, int ap){
      game.gainCard(card.getName(),victim);
    }
    @Override
    public boolean maskCondition(DominionCard c){
      return c.equals(card);
    }
  }
  public class Fishingvillage extends DominionCard{
    public Fishingvillage(){
      super("fishingvillage");
      actions=2;
      value=1;
      isAction=true;
      isDuration=true;
      cost=3;
    }
    @Override
    public void duration(int ap){
      game.money+=1;
      game.actions+=1;
      game.updateSharedFields();
    }
  }
  public class Lookout extends DominionCard{
    public Lookout(){
      super("lookout");
      actions=1;
      cost=3;    
      isAction=true;
    }
    @Override
    public void work(int ap){
      LinkedList<DominionCard> options=new LinkedList<>();
      DominionPlayer player=game.players.get(ap);
      for(int i=0;i<3;i++){
        try{
          options.add(player.getCard());
        }catch(OutOfCardsException e){
          break;
        } 
      }
      if(options.size()==0) return;
      OptionData o=new OptionData(new String[0]);
      o.add("Choose a card to trash:","text");
      for(DominionCard card : options){
        o.add(card.getImage(), "imagebutton");
      }
      String input=game.optionPane(ap,o);
      DominionCard card;
      for(ListIterator<DominionCard> it=options.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getImage().equals(input)){
          it.remove();
          game.trashCard(card, ap);
          break;
        }
      }
      o.clear();
      if(options.size()==0) return;
      o.add("Choose a card to discard:","text");
      for(DominionCard card2 : options){
        o.add(card2.getImage(), "imagebutton");
      }
      input=game.optionPane(ap,o);
      for(ListIterator<DominionCard> it=options.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getImage().equals(input)){
          it.remove();
          player.disc.put(card);
          break;
        }
      }
      player.deck.put(options);
    }
  }
  public class Smugglers extends DominionCard{
    public Smugglers(){
      super("smugglers");
      cost=3;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(smugglerCards2.size()==0) return;
      OptionData o=new OptionData(new String[0]);
      for(String cardName : smugglerCards2){
        o.add(cardName,"imagebutton");
      }
      String input=game.optionPane(ap,o);
      game.gainCard(input,ap);
    }
  }
  public class Warehouse extends RegularCard{
    public Warehouse(){
      super("warehouse");
      cost=3;
      cards=3;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.DISCARD,3,3,ap);
    }
  }
  public class Caravan extends DominionCard{
    public Caravan(){
     super("caravan");
      cost=4;
      cards=1;
      actions=1;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void duration(int ap){
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
    }
  }
  public class Cutpurse extends Attack{
    public Cutpurse(){
      super("cutpurse");
      cost=4;
      value=2;
    }
    @Override
    public void subStep(int vic, int ap){
      DominionPlayer player=game.players.get(vic);
      game.mask=makeMask(player.hand);
      int count=Collections.frequency(game.mask,true);
      if(count==1){
        player.disc.add(player.hand.remove(game.mask.indexOf(true)));
      }else if(count>1){
        game.server.displayComment(ap, "discard a copper");
        game.doWork(Dominion.Phase.DISCARD,1,1,vic);
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals("copper");
    }
  }
  public class Island extends RegularCard{
    public Island(){
      super("island");
      cost=4;
      isVictory=true;
      vicPoints=2;
    }
    @Override
    public void subWork(int ap){
      lostTrack=true;
      DominionPlayer player=game.players.get(ap);
      game.doWork(Dominion.Phase.SELECT,1,1,ap);
      player.island.add(game.selectedCards.get(0));
      player.island.add(this);
      game.matcards.remove(this);
      game.displayPlayer(ap);
    }
  }
  public class Navigator extends DominionCard{
    public Navigator(){
      super("navigator");
      cost=4;
      value=2;
      isAction=true;
    }
    @Override 
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      LinkedList<DominionCard> cards=player.draw(5);
      OptionData o=new OptionData();
      for(DominionCard card : cards)
        o.add(card.getImage(),"image");
      o.add("Discard","textbutton");
      o.add("Put Back","textbutton");
      String input=game.optionPane(ap,o);
      if(input.equals("Discard")){
        player.disc.put(cards);
        game.displayPlayer(ap);
      }else{
        game.putBack(ap,cards);
      }
    }
  }
  public class Pirateship extends Attack{
    String input="";
    public Pirateship(){
      super("pirateship");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      int val=game.players.get(ap).pirateship;
      String [] options={"Attack", "Take "+val+" Money"};
      OptionData o=new OptionData(options);
      input=game.optionPane(ap,o);
      if(input.equals(options[1])) game.money+=val;
    }
    @Override
    public void subStep(int vic, int ap){
      if(!input.equals("Attack")) return;
      DominionPlayer victim=game.players.get(vic);
      LinkedList<DominionCard> cards=victim.draw(2);
      if(cards.size()==0) return;

      OptionData o=new OptionData();
      boolean isMoney=false;
      for(DominionCard card : cards){
        if(card.isMoney){
          o.add(card.getImage(),"imagebutton");
          isMoney=true;
        }else{
          o.add(card.getImage(),"image");
        }
      }
      if(!isMoney)
        o.add("Done","textbutton");
      String choice=game.optionPane(ap,o);
      DominionCard card;
      for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.getImage().equals(choice)){
          game.trashCard(card, ap);
          it.remove();
          game.players.get(ap).pirateship++;
          break;
        }
      }
      victim.disc.put(cards);
    }
  }
  public class Salvager extends RegularCard{
    public Salvager(){
      super("salvager");
      cost=4;
      buys=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork(Dominion.Phase.TRASH,1,1,ap);
      game.money+=game.cost2(game.selectedCards.get(0));
      game.updateSharedFields();
    }
  }
  public class Seahag extends Attack{
    public Seahag(){
      super("seahag");
      cost=4;
    }
    @Override
    public void subStep(int vic, int ap){
      try{
        game.players.get(vic).disc.put(game.players.get(vic).getCard());
      }catch(OutOfCardsException ex){}
      
      game.gainCard("curse",vic,Dominion.GainTo.TOP_CARD);
    }
  }
  public class Treasuremap extends RegularCard{
    public Treasuremap(){
      super("treasuremap");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork(Dominion.Phase.TRASH,0,1,ap);
      if(game.selectedCards.size()>0){
        for(int i=0;i<4;i++) game.gainCard("gold",ap,Dominion.GainTo.TOP_CARD);
      }
      game.trashCard(this, ap);
      game.matcards.remove(this);
    }
  }
  public class Explorer extends RegularCard{
    public Explorer(){
      super("explorer");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork(Dominion.Phase.REVEAL,0,1,ap);
      if(game.selectedCards.size()>0) game.gainCard("gold",ap,Dominion.GainTo.HAND);
      else game.gainCard("silver",ap,Dominion.GainTo.HAND);
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals("province");
    }
  }
  public class Ghostship extends Attack{
    public Ghostship(){
      super("ghostship");
      cost=5;
      cards=2;
    }
    @Override
    public void subStep(int vic, int ap){
      game.server.displayComment(vic,"Put cards on your deck until you only have three");
      int diff=game.players.get(vic).hand.size()-3;
      if(diff>=2)
        game.doWork(Dominion.Phase.TOP_DECK,diff,diff,vic);
    }
  }
  public class Merchantship extends DominionCard{
    public Merchantship(){
      super("merchantship");
      cost=5;
      value=2;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void duration(int ap){
      game.money+=2;
      game.updateSharedFields();
    }
  }
  public class Outpost extends DominionCard{
    public Outpost(){
      super("outpost");
      cost=5;
      isAction=true;
      isDuration=true;
    }
  }
  public class Tactician extends DominionCard{
    boolean discarded;
    public Tactician(){
      super("tactician");
      cost=5;
      isAction=true;
      isDuration=true;      
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      if(player.hand.size()>0){
        player.disc.put(player.hand);
        player.hand.clear();
        discarded=true;
        game.displayPlayer(ap);
      }else discarded=false;
    }
    @Override
    public void duration(int ap){
      if(discarded){
        game.actions++;
        game.buys++;
        game.players.get(ap).drawToHand(5);
        game.displayPlayer(ap);
      }
    }      
  }
  public class Treasury extends DominionCard{
    String [] options={"Put on top","Discard"};
    OptionData o;
    public Treasury(){
      super("treasury");
      cost=5;
      cards=1;
      actions=1;
      value=1;
      isAction=true;
      o=new OptionData(options);
      o.add(imagename,"image");
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
    
      if(victoryBought){
        return false;
      }else{
        String input=game.optionPane(ap,o);
        if(input.equals(options[0])){
          player.deck.put(this);
        }else{
          player.disc.put(this);
        }
        return true;  
      }
    }
  }
  public class Wharf extends DominionCard{
    public Wharf(){
      super("wharf");
      cost=5;
      buys=1;
      cards=2;
      isAction=true;
      isDuration=true;
    }
    @Override
    public void duration(int ap){
      game.buys++;
      game.players.get(ap).drawToHand(2);
      game.displayPlayer(ap);
    }
  }
}
