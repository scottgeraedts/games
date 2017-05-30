import java.util.*;

public class Hinterlands extends Expansion{
  public static boolean crossroadsPlayed=false;
  public static boolean foolsGoldPlayed=false;
  public static int hagglerCounter=0;

  public Hinterlands(Dominion g){
    super(g);
    String [] temp={"crossroads","duchess","foolsgold","develop","oasis","oracle","scheme",
      "jackofalltrades","noblebrigand","nomadcamp","silkroad","spicemerchant","trader",
      "cache","cartographer","embassy","haggler","highway","illgottengains","inn","mandarin",
      "margrave","stables","bordervillage","farmland"};
    cards=temp;
  }
  public class Crossroads extends DominionCard{
    public Crossroads(){
      super("crossroads");
      cost=2;
      isAction=true;
    }
    @Override
    public void work(int ap){
      DominionPlayer player=game.players.get(ap);
      int temp=0;
      for(DominionCard card : player.hand){
        if(card.isVictory) temp++;
      }
      player.drawToHand(temp);
      game.displayPlayer(ap);
      if(!crossroadsPlayed){
        game.actions+=3;
        game.updateSharedFields();
        crossroadsPlayed=true;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      crossroadsPlayed=false;
      return false;
    }
  }
  public class Duchess extends Attack{
    public Duchess(){
      super("duchess");
      cost=2;
      isAttack=false;
      value=2;
    }
    @Override
    public void subWork(int ap){
      maybeDiscard(ap);
    }
    @Override
    public void subStep(int ap, int atk){
      maybeDiscard(ap);
    }
    public void maybeDiscard(int ap){
      String [] options={"Discard","Done"};
      OptionData o=new OptionData(options);
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      try{
        card=player.getCard();
        o.put(card.getImage(),"image");
        String input=game.optionPane(ap,o);
        if(input.equals(options[0])){
          player.disc.put(card);
        }else{
          player.deck.put(card);
        }
        game.displayPlayer(ap);
      }catch(OutOfCardsException ex){}
    }
  }
  public class Foolsgold extends DominionCard{
    public Foolsgold(){
      super("foolsgold");
      cost=2;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      if(foolsGoldPlayed){
        game.money+=3;
        game.updateSharedFields();          
      }
      foolsGoldPlayed=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      foolsGoldPlayed=false;
      return false;
    }
  }
  public class Develop extends RegularCard{
    public Develop(){
      super("develop");
      cost=3;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()>0){
        int cost=game.cost2(game.selectedCards.get(0));
        game.selectedCards.clear();
        game.server.displayComment(ap,"gain a card costing "+(cost+1));
        game.controlledGain(ap,cost+1);
        game.server.displayComment(ap,"gain a card costing "+(cost-1));
        game.controlledGain(ap,cost-1);
      }
    }
  }
  public class Oasis extends RegularCard{
    public Oasis(){
      super("oasis");
      cost=3;
      value=1;
      cards=1;
      actions=1;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",1,1,ap);
    }
  }
  public class Oracle extends Attack{
    public Oracle(){
      super("oracle");
      cost=3;
    }
    @Override
    public void subWork(int ap){
      discardOrKeep(ap,ap);
    }
    @Override
    public void subStep(int ap, int atk){
      discardOrKeep(ap,atk); 
    }
    @Override
    public void cleanup(int ap){
      game.players.get(ap).drawToHand(2);
      game.displayPlayer(ap);
    }
    public void discardOrKeep(int activePlayer, int attacker){
      ArrayList<DominionCard> cards=new ArrayList<>(2);
      String [] options={"Keep","Discard"};
      OptionData o=new OptionData(options);
      for(int i=0;i<2;i++){
        try{
          cards.add(game.players.get(activePlayer).getCard());
        }catch(OutOfCardsException ex){
          break;
        }
      }
      if(cards.size()==0) return;
      
      for(DominionCard card : cards){
        o.put(card.getImage(),"image");
      }
      String result=game.optionPane(attacker,o);
      if(result.equals(options[0])) game.putBack(activePlayer,cards);
      else game.players.get(activePlayer).disc.put(cards);
      game.displayPlayer(activePlayer);
    }    
  }
  public class Scheme extends DominionCard{
    public Scheme(){
      super("scheme");
      cost=3;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      OptionData o=new OptionData();
      for(DominionCard card : game.matcards){
        if(card.isAction) o.put(card.getImage(), "imagebutton");
      }
      o.put("Done", "textbutton");
      String input=game.optionPane(ap,o);
      DominionCard card;
      for(ListIterator<DominionCard> it=game.matcards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(input.equals(card.getImage())){
          player.deck.put(card);
          it.remove();
          break;
        }
      }//loop through cards
      return false;
    }
  }
  public class Jackofalltrades extends RegularCard{
    public Jackofalltrades(){
      super("jackofalltrades");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      game.gainCard("silver",ap);
      DominionPlayer player=game.players.get(ap);
      
      //look at top card
      try{
        String [] options={"Discard", "Put back"};
        OptionData o=new OptionData(options);
        DominionCard card=player.getCard();
        o.put(card.getImage(),"image");
        String input=game.optionPane(ap,o);
        if(input.equals(options[0])){
          player.disc.put(card);
        }else{
          player.deck.put(card);
        }
      }catch(OutOfCardsException ex){}
      
      //draw cards
      player.drawToHand(5-player.hand.size());
      game.displayPlayer(ap);
      
      //trash
      game.mask=makeMask(player.hand);
      game.doWork("trash",0,1,ap);
    }
    @Override 
    public boolean maskCondition(DominionCard card){
      return !card.isMoney;
    }
  }
  public class Noblebrigand extends Attack{
    public Noblebrigand(){
      super("noblebrigand");
      cost=4;
      value=1;
    }
    @Override
    public void onGain(int ap){
      String oldPhase=game.getPhase();
      if(oldPhase.equals("buys")){
        work(ap);
        game.changePhase(oldPhase);      
      }
    }
    @Override
    public void subStep(int victim, int attacker){
      boolean moneyFound=false;
      boolean silverFound=false;
      ArrayList<DominionCard> cards=new ArrayList<>();
      DominionCard card=new DominionCard("copper");
      OptionData o=new OptionData();

      for(int i=0;i<2;i++){
        try{
          card=game.players.get(victim).getCard();
        }catch(OutOfCardsException e){}
        
        cards.add(card);
        if(card.getName().equals("silver") || card.getName().equals("gold")){
          o.put(card.getImage(),"imagebutton");
          silverFound=true;
        }else
          o.put(card.getImage(),"image");
        if(card.isMoney) moneyFound=true;
      }
      if(!silverFound) o.put("Done","textbutton");

      String out=game.optionPane(attacker,o);
      if(out.equals(cards.get(0).getImage())){
        game.players.get(victim).disc.put(cards.get(1));
        game.players.get(attacker).disc.put(cards.get(0));
      }else if(out.equals(cards.get(1).getImage())){
        game.players.get(victim).disc.put(cards.get(0));
        game.players.get(attacker).disc.put(cards.get(1));
      }else{
        game.players.get(victim).disc.put(cards);
      }
      if(!moneyFound){
         game.gainCard("copper", victim, "discard", true);
      }
    }
  }
  public class Nomadcamp extends DominionCard{
    public Nomadcamp(){
      super("nomadcamp");
      cost=4;
      value=2;
      isAction=true;
      buys=1;
    }
    @Override
    public void onGain(int ap){
      DominionPlayer player=game.players.get(ap);
      if(player.disc.remove(this)){
        player.deck.put(this);
      }
    }
  }
  public class Silkroad extends DominionCard{
    public Silkroad(){
      super("silkroad");
      cost=4;
      isVictory=true;
    }
    @Override 
    public int getPoints(Collection<DominionCard> cards){
      int nVic=0;
      for(DominionCard card : cards){
        if(card.isVictory) nVic++;
      }
      return nVic/4;
    }
  }
  public class Spicemerchant extends RegularCard{
    public Spicemerchant(){
      super("spicemerchant");
      cost=4;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("trash",0,1,ap);
      if(game.selectedCards.size()==0) return;
      
      String [] options={"+2 Cards, +1 Action", "+2 Money, +1 Buy"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.players.get(ap).drawToHand(2);
        game.displayPlayer(ap);
        game.actions++;
      }else{
        game.money+=2;
        game.buys++;
      }
      game.updateSharedFields();      
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
  }
  public class Trader extends RegularCard{
    public Trader(){
      super("trader");
      cost=4;
      isReaction2=true;
    }
    @Override
    public void subWork(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()>0){
        for(int i=0;i<game.cost2(game.selectedCards.get(0));i++) game.gainCard("silver",ap);
      }
    } 
  }
  public class Cache extends DominionCard{
    public Cache(){
      super("cache");
      cost=5;
      isMoney=true;
      value=3;
    }
    @Override
    public void onGain(int ap){
      for(int i=0;i<2;i++) game.gainCard("copper",ap,"discard",true);
    }
  }
  public class Cartographer extends DominionCard{
    public Cartographer(){
      super("cartographer");
      cost=5;
      cards=1;
      actions=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      game.server.displayComment(ap,"Choose cards to discard");
      ArrayList<DominionCard> cards=game.players.get(ap).draw(4);
      game.displayPlayer(ap);
      OptionData o=new OptionData();
      String input;
      DominionCard card;
      
      while(true){
        if(cards.size()==0) break;
        for(DominionCard card2 : cards){
          o.put(card2.getImage(),"imagebutton");
        }
        o.put("Done Discarding","textbutton");
        input=game.optionPane(ap, o);
        if(input.equals("Done Discarding")){
          break;
        }else{
          for(ListIterator<DominionCard> it=cards.listIterator(); it.hasNext(); ){
            card=it.next();
            if(card.getImage().equals(input)){
              it.remove();
              game.players.get(ap).disc.put(card);
              game.displayPlayer(ap);
              break;
            }
          }//find the matching card 
        }
        o.clear();
      }
      game.putBack(ap,cards);
    }
  }
  public class Embassy extends RegularCard{
    public Embassy(){
      super("embassy");
      cards=5;
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",3,3,ap);
    }
    @Override
    public void onGain(int ap){
      for(int i=(ap+1)%game.players.size(); i!=ap; i=(i+1)%game.players.size()){
        game.gainCard("silver",i,"discard",true);
      }
    }
  }  
  public class Haggler extends DominionCard{
    public boolean inPlay=false;
    public Haggler(){
      super("haggler");
      cost=5;
      value=2;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(!inPlay){
        hagglerCounter++;
        inPlay=true;
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      hagglerCounter=0;
      inPlay=false;
      return false;
    }
  }
  public class Highway extends DominionCard{
    public boolean inPlay=false;
    public Highway(){
      super("highway");
      cost=5;
      actions=1;
      cards=1;
      isAction=true;
    }
    @Override
    public void work(int ap){
      if(!inPlay){
        inPlay=true;
        game.bridgeCounter++;
        game.displaySupplies();        
      }
    }
    @Override
    public boolean cleanup(int ap, DominionPlayer player){
      inPlay=false;
      return false;
    }
  }
  public class Illgottengains extends DominionCard{
    public Illgottengains(){
      super("illgottengains");
      cost=5;
      value=1;
      isMoney=true;
    }
    @Override
    public void work(int ap){
      String [] options={"Gain Copper","Pass"};
      String input=game.optionPane(ap, new OptionData(options));
      if(input.equals(options[0])){
        game.gainCard("copper", ap, "hand", true);
      }
    }
    @Override
    public void onGain(int ap){
      for(int i=(ap+1)%game.players.size(); i!=ap; i=(i+1)%game.players.size()){
        game.gainCard("curse",i,"discard",true);
      }
    }
  }
  public class Inn extends RegularCard{
    public Inn(){
      super("inn");
      cost=5;
      cards=2;
      actions=2;
    }
    @Override
    public void subWork(int ap){
      game.doWork("discard",2,2,ap);
    }
    @Override
    public void onGain(int ap){
      DominionPlayer player=game.players.get(ap);
      DominionCard card;
      
      OptionData o=new OptionData();
      while(true){
        for(Iterator<DominionCard>it=player.disc.iterator(); it.hasNext(); ){
          card=it.next();
          if(card.isAction) o.put(card.getName(),"imagebutton");
        }
        if(o.size()==0) break;
        o.put("Done","textbutton");
        String input=game.optionPane(ap,o);

        if(input.equals("Done")) break;

        for(Iterator<DominionCard>it=player.disc.iterator();it.hasNext(); ){
          card=it.next();
          if(card.getName().equals(input)){
            player.disc.remove(card);
            player.deck.put(card);
            game.displayPlayer(ap);
            break;
          }
        }
        o.clear();         
      }        
      player.deck.shuffle();
    }
  }
  public class Mandarin extends RegularCard{
    public Mandarin(){
      super("mandarin");
      cost=5;
      value=3;
    }
    @Override
    public void subWork(int ap){
      game.doWork("topdeck",1,1,ap);
    }
    @Override
    public void onGain(int ap){
      DominionCard card;
      for(ListIterator<DominionCard> it=game.matcards.listIterator(); it.hasNext(); ){
        card=it.next();
        if(card.isMoney){
          game.players.get(ap).deck.put(card);
          it.remove();
        }
      }
    }
  }
  public class Margrave extends Attack{
    public Margrave(){
      super("margrave");
      cost=5;
      cards=3;
      buys=1;
    }
    @Override
    public void subStep(int ap, int atk){
      game.server.displayComment(ap,"Discard down to 3 cards");
      game.players.get(ap).drawToHand(1);
      game.displayPlayer(ap);
      int temp=game.players.get(ap).hand.size()-3;
      if(temp>0)
        game.doWork("discard",temp,temp,ap);
    }
  } 
  public class Stables extends RegularCard{
    public Stables(){
      super("stables");
      cost=5;
    }
    @Override
    public void subWork(int ap){
      game.mask=makeMask(game.players.get(ap).hand);
      game.doWork("discard",0,1,ap);
      if(game.selectedCards.size()>0){
        game.players.get(ap).drawToHand(3);
        game.displayPlayer(ap);
        game.actions++;
        game.updateSharedFields();
      }
    }
    @Override
    public boolean maskCondition(DominionCard card){
      return card.isMoney;
    }
  } 
  public class Bordervillage extends DominionCard{
    public Bordervillage(){
      super("bordervillage");
      actions=2;
      cards=1;
      cost=6;
      isAction=true;
    }
    @Override
    public void onGain(int ap){
      game.gainLimit=game.cost2(this)-1;
      game.server.displayComment(ap,"gain a card costing up to "+game.gainLimit);
      game.doWork("gain",1,1,ap);
      game.changePhase("buys");
      game.selectedCards.clear();
      game.server.displayComment(ap,"");
    }
  }
  public class Farmland extends DominionCard{
    public Farmland(){
      super("farmland");
      cost=6;
      isVictory=true;
      vicPoints=2;
    }
    @Override
    public void onGain(int ap){
      game.doWork("trash",1,1,ap);
      if(game.selectedCards.size()==0) return;
      game.gainLimit=game.cost2(game.selectedCards.get(0))+2;
      game.doWork("gain",1,1,ap);      
    }
  }
}
  

