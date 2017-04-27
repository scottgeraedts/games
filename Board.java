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
  private ArrayList<PlayerDisplay> players=new ArrayList<PlayerDisplay>();
	public Container cp;
 	
   // Constructor to setup the GUI components and event handlers
   public Board(War.WarData data) {
		// Retrieve the content-pane of the top-level container JFrame
		// All operations done on the content-pane
		cp = getContentPane();
		cp.setLayout(new GridLayout(3,1));  // The content-pane sets its layout
		setTitle("WAR!");
		setSize(800,800);
		
    for(int i=0;i<data.players.size();i++){
      players.add(new PlayerDisplay(data.players.get(i)));
    }
		
		display(data);

		setVisible(true);

//      cp.add(new JLabel("Enter an Integer: "));
//      tfInput = new JTextField(10);
//      cp.add(tfInput);
//      cp.add(new JLabel("The Accumulated Sum is: "));
//      tfOutput = new JTextField(10);
//      tfOutput.setEditable(false);  // read-only
//      cp.add(tfOutput);
// 
//      // Allocate an anonymous instance of an anonymous inner class that
//      //  implements ActionListener as ActionEvent listener
//      tfInput.addActionListener(new ActionListener() {
//         @Override
//         public void actionPerformed(ActionEvent evt) {
//            // Get the String entered into the input TextField, convert to int
//            int numberIn = Integer.parseInt(tfInput.getText());
//            sum += numberIn;      // accumulate numbers entered into sum
//            tfInput.setText("");  // clear input TextField
//            tfOutput.setText(sum + ""); // display sum on the output TextField
//         }
//      });
 
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit program if close-window button clicked

      //setVisible(true);   // "super" Frame shows
      
   }
   public void display(War.WarData data){
    for(int i=0;i<players.size();i++){
      players.get(i).display(data.players.get(i));
    }
      
   }


   public class PlayerDisplay{
    public DeckDisplay deck,disc;
    public PlayerDisplay(WarPlayer.Data data){
      deck=new DeckDisplay(data.deck);
      disc=new DeckDisplay(data.disc);
    }
    public void display(WarPlayer.Data data){
      deck.display(data.deck);
      disc.display(data.disc);
    }
   }

  public class DeckDisplay{
    public JTextField n;
    public DeckDisplay(Deck.Data data){
      try{
        BufferedImage myPicture = ImageIO.read(new File(data.image));
        JLabel picLabel = new JLabel(new ImageIcon(myPicture));
        cp.add(picLabel);      
      }catch(IOException e){
        System.out.println("couldn't find image "+data.image);
      }
      n=new JTextField(2);
      n.setEditable(false);
      cp.add(n);
    }
    public void display(Deck.Data data){
      n.setText(data.size+"");
    }
  }   
   // The entry main() method
//   public static void main(String[] args) {
//      // Run the GUI construction in the Event-Dispatching thread for thread-safety
//      SwingUtilities.invokeLater(new Runnable() {
//         @Override
//         public void run() {
//            new Board(); // Let the constructor do the job
//         }
//      });
//   }
}
