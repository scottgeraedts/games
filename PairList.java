import java.util.*;
//two linked arrays
//this class could be used for other things, but right now its used for storing data for optionpanels
public class PairList<K,V>{
  protected ArrayList<K> names=new ArrayList<>();
  protected ArrayList<V> types=new ArrayList<>();
  public PairList(){}
  public void put(K a,V b){
    names.add(a);
    types.add(b);
  }
  public V get(K a){
    return types.get(names.indexOf(a));
  }
  public int size(){
    return names.size();
  }
  public void remove(K a){
    int n=names.indexOf(a);
    types.remove(n);
    names.remove(n);
  }
  public void clear(){
    types.clear();
    names.clear();
  }
  public K getKey(int i){
    return names.get(i);
  }
  public V getValue(int i){
    return types.get(i);
  }
}
