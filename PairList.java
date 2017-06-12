import java.util.*;
import java.lang.SuppressWarnings;
//two linked arrays
//this class could be used for other things, but right now its used for storing data for optionpanels
public class PairList<K extends Comparable,V extends Comparable>{
  protected ArrayList<K> names=new ArrayList<>();
  protected ArrayList<V> types=new ArrayList<>();
  public PairList(){}

  public PairList(String input, Class<K> clazz1, Class<V> clazz2){
    String [] parts=input.split("@");
    int size=Integer.parseInt(parts[0]);

    if(size==0) return;
    String[] t = parts[1].split("!");
    for (int i = 0; i < size; i++) {
      names.add(generalCast(t[i],clazz1));
    }
    t = parts[2].split("!");
    for (int i = 0; i < size; i++) {
      types.add(generalCast(t[i],clazz2));
    }
  }
  private <T> T generalCast(String t, Class<T> clazz){
    try{
      if(clazz.getName().equals("java.lang.Integer")){
        return clazz.cast(Integer.parseInt(t));
      }
      else return clazz.cast(t);
    }catch(ClassCastException ex){
      System.out.println(t);
      ex.printStackTrace();
    }
    return null;
  }
  public void add(K a, V b) {
    names.add(a);
    types.add(b);
  }
  public void put(K a, V b) {
    if(containsKey(a)){
      types.set(names.indexOf(a), b);
    }else{
      names.add(a);
      types.add(b);
    }
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
  @Override
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
  void sortByValue(){
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

    PairList<String,Integer> x=new PairList<>("3@0!0!0@1!2!3", String.class, Integer.class);
    System.out.println(x);
  }
}  


