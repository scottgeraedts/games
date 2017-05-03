import java.util.*;

class OutOfCardsException extends Exception{
	public String name;
	public OutOfCardsException(String t){
		name=t;
	}
}
