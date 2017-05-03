import java.util.*;
import java.io.*;
import java.net.*;

public class DominionClient{
  public static void main(String [] args) throws IOException{
    Socket socket=new Socket("localhost",4444);
    BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter output=new PrintWriter(socket.getOutputStream(),true);
    BufferedReader stdin=new BufferedReader(new InputStreamReader(System.in));

    String inputLine,name;    
    while(true){
      inputLine=input.readLine();
      System.out.println(inputLine);
      if(inputLine.equals("break")){
        break;
      }
      if(inputLine.charAt(0)=='!'){
        name=stdin.readLine();
        output.println(name);
      }
    }
  }
}
