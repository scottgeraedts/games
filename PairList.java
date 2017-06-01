import java.util.*;
import java.lang.SuppressWarnings;
//two linked arrays
//this class could be used for other things, but right now its used for storing data for optionpanels
public class PairList<K extends Comparable,V extends Comparable>{
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
  public boolean containsKey(K key){
    return names.contains(key);
  }
  public K getKey(int i){
    return names.get(i);
  }
  public V getValue(int i){
    return types.get(i);
  }
  public String toString(){
    String out=size()+"@";
    for(int i=0;i<size();i++){
      if(i>0) out+="!";
      out+=""+names.get(i);
    }
    out+="@";
    for(int i=0;i<size();i++){
      if(i>0) out+="!";
      out+=""+types.get(i);
    }
    return out;
  }
  //sort the values of the array and return a sorted list
  public void sortByValue(){
    ArrayList<Integer> nums=new ArrayList<Integer>(size());
    for(int i=0;i<size();i++) nums.add(i);
     
    Collections.sort(nums, new Comparator<Integer>(){
      @SuppressWarnings("unchecked")
      public int compare(Integer a, Integer b){
        return PairList.this.types.get(b).compareTo( PairList.this.types.get(a));
      }
    });
    ArrayList<K> newNames=new ArrayList<>(size());
    ArrayList<V> newVals=new ArrayList<>(size());
    for(int i=0;i<size();i++){
      newNames.add(names.get(nums.get(i)));
      newVals.add(types.get(nums.get(i)));
    }
    names=newNames;
    types=newVals;    
  }  
  @SuppressWarnings("unchecked")
  public static void main(String [] args){
//    PairList x=new PairList<String,Integer>();
//    x.put("a",1);
//    x.put("b",3);
//    x.put("c",2);
//    System.out.println(x);
//    x.sortByValue();
//    System.out.println(x);

    HashMap<String,String> map=new HashMap<>();
    map.put("aa","bb");
    System.out.println(map.containsKey("aa"));
    
  }
}  


