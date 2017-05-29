import java.util.*;

public class Cornucopia extends Expansion{
  
  public static ArrayList<DominionCard> prizes=new ArrayList<>(5);
  public static String bane;
  
  public Cornucopia(Dominion g){
    super(g);
    String [] temp={"hamlet","fortuneteller","menagerie"};
    cards=temp;
    String [] prizeNames={"diadem","bagofgold","followers","princess","trustysteed"};
    for(String name : prizeNames){
      prizes.add(game.cardFactory(name,"cornucopia"));
    }
  }
  public class Hamlet extends RegularCard{
    public Hamlet(){
      super("hamlet");
      cost=2;
      actions=1;
      cards=1;
    }
    @Override
    public void subWork(int ap){
      game.server.displayComment(ap,"Discard a Card for +1 Action");
      game.doWork("discard",0,1,ap);
      if(game.selectedCards.size()>0){
        game.actions++;
        game.updateSharedFields();
      }
      game.selectedCards.clear();
      game.server.displayComment(ap,"Discard a Card for +1 Buy");
      game.doWork("discard",0,1,ap);
      if(game.selectedCards.size()>0){
        game.buys++;
        game.updateSharedFields();
      }
    }
  }
  public class Fortuneteller extends Attack{
    public Fortuneteller(){
      super("fortuneteller");
      cost=3;
      value=2;
    }
    @Override
    public void subStep(int ap, int atk){
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
       
        cards.add(card);
        if(card.isVictory || card.getName().equals("curse")) break;
      }
      OptionData o=new OptionData();
      for(DominionCard card2 : cards){
        o.put(card2.getImage(),"image");
      }
      o.put("Done","textbutton");
      game.optionPane(ap,o);
      player.deck.put(cards.remove(cards.size()-1));
      player.disc.put(cards);
      game.displayPlayer(ap);
    }
  }
  public class Menagerie extends DominionCard{
    public Menagerie(){
      super("menagerie");
      cost=3;
      actions=1;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      HashSet<DominionCard> handset=new HashSet<>(player.hand);
      System.out.println(handset);
      if(handset.size()==player.hand.size()){
        player.drawToHand(2);
        game.displayPlayer(ap);
      }
    }
  }
  public class Farmingvillage extends DominionCard{
    public Farmingvillage(){
      super("farmingvillage");
      cost=4;
      actions=2;
    }
    @Override
    public void work(int ap){
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
       
        cards.add(card);
        if(card.isAction || card.isMoney) break;
      }
      OptionData o=new OptionData();
      for(DominionCard card2 : cards){
        o.put(card2.getImage(),"image");
      }
      o.put("Done","textbutton");
      game.optionPane(ap,o);
      player.hand.add(cards.remove(cards.size()-1));
      player.disc.put(cards);
      game.displayPlayer(ap);
    }
  }  
  public class Horsetraders extends RegularCard{
    public Horsetraders(){
      super("horsetraders");
      cost=4;
      buys=1;
      value=3;
      isReaction1=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",2,2,ap);
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      player.horseTraders.clear();
      return false;
    }
  }
  public class Remake extends RegularCard{
    public Remake(){
      super("remake");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      for(int i=0;i<2;i++){
        game.doWork("trash",1,1,ap);
        if(game.selectedCards.size()==0) break;
        game.controlledGain(ap, game.cost2(game.selectedCards.get(0)));
        game.selectedCards.clear();
      }
    }
  }
  public class Tournament extends Attack{
    boolean winning=true;
    public Tournament(){
      super("tournament");
      cost=4;
      actions=1;
      isAttack=false;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("discard",0,1,ap);
      if(game.selectedCards.size()==0) return;
      
      OptionData o=new OptionData();
      for(DominionCard card : prizes){
        o.put(card.getImage(), "imagebutton");
      }
      if(game.supplyDecks.get("duchy").size()>0){
        o.put(game.supplyDecks.get("duchy").card.getImage(), "imagebutton");
      }
      String input=game.optionPane(ap, o);
      if(input.equals(game.supplyDecks.get("duchy").card.getImage())){
        game.gainCard("duchy",ap);
      }else{
        DominionCard card2;
        for(ListIterator<DominionCard> it=prizes.listIterator(); it.hasNext(); ){
          card2=it.next();
          if(card2.getImage().equals(input)){
            game.gainCardNoSupply(card2,ap,"discard");
            it.remove();
            break;
          }
        }
      }
    }
    @Override
    public void subStep(int ap, int atk){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("reveal",0,1,ap);
      if(game.selectedCards.size()>0) winning=false;
    }
    @Override
    public void cleanup(int ap){
      if(winning){
        game.players.get(ap).drawToHand(1);
        game.displayPlayer(ap);
        game.money++;
        game.updateSharedFields();
      }
      winning=true;
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals("province");
    }     
  }
  public class Youngwitch extends Attack{
    public Youngwitch(){
      super("youngwitch");
      cards=2;
      cost=4;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",2,2,ap);
    }
    @Override
    public void subStep(int ap, int atk){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("reveal",0,1,ap);
      if(game.selectedCards.size()==0) game.gainCard("curse",ap);
       
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.getName().equals(bane);
    }
  }
  public class Harvest extends DominionCard{
    public Harvest(){
      super("harvest");
      cost=5;
      isAction=true;
    }
    @Override
    public void work(int ap){
      ArrayList<DominionCard> cards=game.players.get(ap).draw(4);
      OptionData o=new OptionData();
      for(DominionCard card : cards){
        o.put(card.getImage(), "image");
      }
      o.put("Done","textbutton");
      game.optionPane(ap,o);

      HashSet<DominionCard> cardSet=new HashSet<>(cards);
      game.money+=cardSet.size();
      game.updateSharedFields();
      game.players.get(ap).disc.put(cards);
      game.displayPlayer(ap);
    }
  }
  public class Hornofplenty extends DominionCard{
    public Hornofplenty(){
      super("hornofplenty");
      cost=5;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      HashSet<DominionCard> cardSet=new HashSet<>(game.matcards);
      game.gainLimit=cardSet.size();
      game.doWork("gain",1,1,ap);
    }
  }
  public class Huntingparty extends DominionCard{
    public Huntingparty(){
      super("huntingparty");
      cost=5;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      HashSet<DominionCard> cardSet=new HashSet<>(player.hand);
      DominionCard card;
      LinkedList<DominionCard> drawnCards=new LinkedList<>();
      while(true){
        try{
          card=player.getCard();
        }catch(OutOfCardsException ex){
          break;
        }
        if(cardSet.contains(card)) drawnCards.add(card);
        else{
          player.hand.add(card);
          break;          
        } 
      }
      player.disc.put(drawnCards);
      game.displayPlayer(ap);
    }
  }
  public class Jester extends Attack{
    public Jester(){
      super("jester");
      cost=5;
      value=2;
    }
    @Override
    public void subStep(int ap, int atk){
      DominionCard card;
      try{
        card=game.players.get(ap).getCard();
        game.players.get(ap).disc.put(card);
        if(card.isVictory) game.gainCard("curse",ap);
        else{
          String [] options={"You gain","They gain"};
          OptionData o=new OptionData(options);
          o.put(card.getImage(),"image");
          String input=game.optionPane(atk,o);
          if(input.equals(options[0])){
            game.gainCard(card.getName(),atk);
          }else{
            game.gainCard(card.getName(),ap);
          }
        }
      }catch(OutOfCardsException ex){
      
      }
    }
  }
  public class Fairgrounds extends Attack{
    public Fairgrounds(){
      super("fairgrounds");
      cost=6;
      isVictory=true;
    }
    @Override
    public int getPoints(Collection<DominionCard> cards){
      HashSet<DominionCard> cardSet=new HashSet<>(cards);
      return 2*(cards.size()/5);
    }
  } 
  /***PRIZES***///
  public class Bagofgold extends DominionCard{
    public Bagofgold(){
      super("bagofgold");
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.gainCard("gold",ap,"topcard",true);
    }
  }
  public class Diadem extends DominionCard{
    public Diadem(){
      super("diadem");
      value=2;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      game.money+=game.actions;
      game.updateSharedFields();
    }
  }
  public class Followers extends Attack{
    public Followers(){
      super("followers");
      cards=2;
    }
    @Override
    public void subWork(int ap){
      game.gainCard("estate",ap);
    }
    @Override
    public void subStep(int ap, int atk){
      game.gainCard("curse",ap);
      int temp=game.players.get(ap).hand.size()-3;
      if(temp>0) game.doWork("discard",temp,temp,ap);
    } 
  }
  public class Princess extends DominionCard{
    private boolean inPlay=false;
    public Princess(){
      super("princess");
      buys=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(!inPlay){
        game.bridgeCounter+=2;
        inPlay=true;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      inPlay=false;
      game.bridgeCounter=0;
      return false;
    }
  }
  public class Trustysteed extends DominionCard{
    public Trustysteed(){
      super("trustysteed");
      isAction=true;        
    }
    @Override
    public void work(int ap){
      ArrayList<String> options=new ArrayList<>(4);
      options.add("+2 Cards");
      options.add("+2 Actions");
      options.add("+2 Money");
      options.add("Gain 4 silvers");
      OptionData o=new OptionData(options.toArray(new String[4]));
      
      String input=game.optionPane(ap,o);
      resolve(input, ap);
      options.remove(input);
      o=new OptionData(options.toArray(new String[3]));
      resolve(game.optionPane(ap,o), ap);
      game.displayPlayer(ap);
      game.updateSharedFields();
    }
    public void resolve(String input,int ap){  
      DominionPlayer player=game.players.get(ap);
      if(input.equals("+2 Cards")) player.drawToHand(2);
      if(input.equals("+2 Actions")) game.actions+=2;
      if(input.equals("+2 Money")) game.money+=2;
      if(input.equals("Gain 4 silvers")){
        for(int i=0; i<4; i++) game.gainCard("silver",ap);
        //put deck in discard pile
        player.disc.put(player.deck.deal(player.deck.size()));
      } 
    }        
  }
}
