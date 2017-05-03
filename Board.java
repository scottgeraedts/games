import java.awt.*;       // Using layouts
import java.awt.event.*; // Using AWT event classes and listener interfaces
import javax.swing.*;    // Using Swing components and containers
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
 
// A Swing GUI application inherits the top-level container javax.swing.JFrame
public class Board extends JFrame {
  public Container cp;
	protected JPanel mat;
 	protected int nPlayers=2;
 	
   public Board() {
   
		setSize(800,800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit program if close-window button clicked      
   }
  public class DeckDisplay{
    private JTextField n;
    private JLabel picLabel=new JLabel();
    public DeckDisplay(Deck.Data data, JPanel playerPanel){
      try{
        BufferedImage myPicture = ImageIO.read(new File(data.image));
        picLabel = new JLabel(new ImageIcon(myPicture));
      }catch(IOException e){
        System.out.println("couldn't find image "+data.image);
      }
      n=new JTextField(2);
      n.setEditable(false);
      playerPanel.add(n);
      playerPanel.add(picLabel);      
    }
    public void display(Deck.Data data){
      n.setText(data.size+"");
    }
  }   
  
  public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
      Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
      BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

      Graphics2D g2d = dimg.createGraphics();
      g2d.drawImage(tmp, 0, 0, null);
      g2d.dispose();

      return dimg;
  }   
}
