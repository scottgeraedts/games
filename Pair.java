import java.util.*;

public class Pair <K extends Comparable, V> implements Comparable{
  private K a;
  private V b;
  public Pair(K a, V b){
    this.a=a;
    this.b=b;
  }

  public K getA() {
    return a;
  }

  public void setA(K a) {
    this.a = a;
  }

  public V getB() {
    return b;
  }

  public void setB(V b) {
    this.b = b;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair<?, ?> pair = (Pair<?, ?>) o;

    if (!a.equals(pair.a)) return false;
    return b.equals(pair.b);
  }

  @Override
  public int hashCode() {
    int result = a.hashCode();
    result = 31 * result + b.hashCode();
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compareTo(Object o) {
    if(o.getClass()!= getClass()) return 0;
    Pair<?, ?> p=(Pair <?,?>)o;
    return a.compareTo(p.getA());
  }
}
