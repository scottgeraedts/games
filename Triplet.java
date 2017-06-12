import java.util.*;

public class Triplet <X extends Comparable, Y, Z> implements Comparable{
  private X a;
  private Y b;
  private Z c;
  public Triplet(X a, Y b, Z c){
    this.a=a;
    this.b=b;
    this.c=c;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Triplet<?, ?, ?> triplet = (Triplet<?, ?, ?>) o;

    if (!a.equals(triplet.a)) return false;
    if (!b.equals(triplet.b)) return false;
    return c.equals(triplet.c);
  }

  @Override
  public int hashCode() {
    int result = a.hashCode();
    result = 31 * result + b.hashCode();
    result = 31 * result + c.hashCode();
    return result;
  }

  public X getA() {
    return a;
  }

  public void setA(X a) {
    this.a = a;
  }

  public Y getB() {
    return b;
  }

  public void setB(Y b) {
    this.b = b;
  }

  public Z getC() {
    return c;
  }

  public void setC(Z c) {
    this.c = c;
  }

  @Override
  public int compareTo(Object o) {
    Triplet<?, ?, ?> triplet=(Triplet<?, ?, ?>) o;
    return a.compareTo(triplet.getA());
  }
}
