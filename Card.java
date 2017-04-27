import java.util.*;
import java.awt.image.BufferedImage;   
import javax.swing.*;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

class Card{

	protected String name;
	protected JLabel picLabel;
	protected boolean displaySetup;
	protected String imagename;
	
	public Card(String newname){
		name=newname;
		displaySetup=false;
	
	}
	public String getName(){ return name;}
	public boolean equals(Card c){ return name==c.getName(); }
	public JLabel display(){
		if(!displaySetup){
			try{
				BufferedImage deckBack = ImageIO.read(new File(imagename));
				picLabel = new JLabel(new ImageIcon(deckBack));		
				displaySetup=true;
			}catch(Exception e){
				System.out.println("can't find card image "+imagename);
				displaySetup=false;
			}
		}
		return picLabel;
	}

}
