import java.util.*;

public class OptionData extends PairList<String,String>{
  public OptionData(String input){
    String [] parts=input.split("@");
    int size=Integer.parseInt(parts[0]);
    String [] t=parts[1].split("!");
    for(int i=0; i<size; i++){
      names.add(t[i]);
    }
    t=parts[2].split("!");
    for(int i=0; i<size; i++){
      types.add(t[i]);
    }
  }
  public OptionData(){
    this(new String[0]);
  }
  public OptionData(String [] options){
    for(int i=0;i<options.length;i++){
      names.add(options[i]);
      types.add("textbutton");
    }
  }
}
