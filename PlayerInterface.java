//must be implemented by any kind of player, whether human or robot
import java.util.*;


public interface PlayerInterface{
  public void initialize(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData,
                         int startingPlayer, ArrayList<Integer> controlled,
                         LinkedHashSet<String> o, HashSet<String> o2);
  public void reset(ArrayList<Deck.SupplyData> supplyData, ArrayList<DominionPlayer.Data> playerData, int startingPlayer,
                    LinkedHashSet<String> o, HashSet<String> o2);
  public void displayMatCards(ArrayList<DominionCard> matcards);
  public void changePlayer(int oldPlayerNum, DominionPlayer.Data oldPlayer, int newPlayerNum, DominionPlayer.Data newPlayer, ArrayList<Boolean> mask);
  public void changePhase(Dominion.Phase oldPhase, Dominion.Phase newPhase, ArrayList<Boolean> mask);
  public void showScores(PairList<?,?> scores);
  public void displayPlayer(int playerNum, DominionPlayer.Data player, Collection<Boolean> mask);
  public void optionPane(OptionData o);
  public void displayTrash(Deck.Data dat);
  public void displaySupply(Deck.SupplyData dat);
  public void displayComment(String text);
  public void updateSharedFields(PairList<String, Integer> fields);
  public boolean playAgain();
  public void terminate();
  public String getUserInput(DominionCard card);
  void changeController(int player, boolean control);
  
}
