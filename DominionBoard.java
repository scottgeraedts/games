import java.awt.*;       // Using layouts
import java.awt.event.*; // Using AWT event classes and listener interfaces
import javax.swing.*;    // Using Swing components and containers
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class DominionBoard extends JFrame{

  private Dominion game;
  private Container cp;

  //player panel
  private ArrayList<PlayerDisplay> players=new ArrayList<PlayerDisplay>();

  //supply panel
  private LinkedHashMap<String,SupplyDisplay> supplyDecks=new LinkedHashMap<>();
  
  //mat panel
  private JPanel cardPanel;
  private JPanel dataPanel;
  private HashMap<String,JButton> doneButtons=new HashMap<>();
  private JTextField moneyField,buysField,actionsField;
  private DeckDisplay trash;
  
  private boolean mask[];
  private boolean maskSet=false;
  
  public DominionBoard(Dominion tgame){
    game=tgame;
		setTitle("Dominion");

    doneButtons.put("buys",new JButton("Done Buying"));
    doneButtons.put("actions",new JButton("Done Actions"));
    doneButtons.put("topdeck",new JButton("Done Topdecking"));
    doneButtons.put("discard",new JButton("Done Discarding"));
    doneButtons.put("trash",new JButton("Done Trashing"));
    doneButtons.put("gain",new JButton("Done Gaining"));
    
    trash=new DeckDisplay(game.trashData());
    
    //some graphics setup
  	setSize(1500,800);
		cp = getContentPane();
		cp.setLayout(new GridLayout(0,2));  // The content-pane sets its layout
    cp.add(setupPlayerPanel());
    cp.add(setupSharedPanel());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit program if close-window button clicked      
    setVisible(true);
    
  }
  
  private JPanel setupPlayerPanel(){
    
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(game.players.size(),1));
    for(int i=0;i<game.players.size();i++){
      players.add( new PlayerDisplay(game.players.get(i)) );
      panel.add(players.get(i).getPanel());
    }
    players.get(0).active=true;
    players.get(0).display();
    return panel;
  }  
  
  private JPanel setupSharedPanel(){
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(2,0));
    
    JPanel supplyPanel=new JPanel();
    SupplyDisplay tempSupply;
    Set<String> supplyNames=game.getSupplyNames();
    String name;
    for(Iterator<String> it=supplyNames.iterator(); it.hasNext(); ){
      name=it.next();
      tempSupply=new SupplyDisplay(game.supplyData(name),name);
      supplyDecks.put(name,tempSupply);
      supplyPanel.add(tempSupply.getPanel()); 
    }  
    panel.add(supplyPanel);

    //starting mat panel    
    JPanel matPanel=new JPanel();
    matPanel.setLayout(new GridLayout(2,0));
    
    cardPanel=new JPanel();
    cardPanel.setLayout(new FlowLayout());
    
    dataPanel=new JPanel();
    dataPanel.setLayout(new FlowLayout());
    
    matPanel.add(dataPanel);
    matPanel.add(cardPanel);
    panel.add(matPanel);
    
    //action listener for the done buying button
    doneButtons.get("buys").addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(ActionEvent evt) {
         game.endTurn();    
         refreshCardPanel();
         changePhase("buys","actions");
       }
    });

    doneButtons.get("actions").addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(ActionEvent evt) {
        game.setPhase("buys");
        changePhase("actions","buys");
       }
    });
    
    EndSelection listener=new EndSelection();
    doneButtons.get("topdeck").addActionListener(listener);
    doneButtons.get("discard").addActionListener(listener);
    doneButtons.get("trash").addActionListener(listener);
    doneButtons.get("gain").addActionListener(listener);
    
    JLabel actionsLabel=new JLabel("Actions: ");
    dataPanel.add(actionsLabel);
    actionsField=new JTextField(2);
    actionsField.setEditable(false);
    dataPanel.add(actionsField);
    
    JLabel moneyLabel=new JLabel("Money: ");
    dataPanel.add(moneyLabel);
    moneyField=new JTextField(2);
    moneyField.setEditable(false);
    dataPanel.add(moneyField);

    JLabel buysLabel=new JLabel("Buys: ");
    dataPanel.add(buysLabel);
    buysField=new JTextField(2);
    buysField.setEditable(false);
    dataPanel.add(buysField);

    dataPanel.add(trash.getPanel());
    dataPanel.add(doneButtons.get("actions"));
    
    refreshSharedFields();
    
    return panel;
  }
  public class EndSelection implements ActionListener{
    public EndSelection(){}
    public void actionPerformed(ActionEvent evt){
      game.stepSelection(false);
      players.get(game.getActivePlayer()).display();
    }
  }
  public void changePlayer(int oldPlayer, int newPlayer){
    maskSet=false;
    players.get(oldPlayer).active=false;
    players.get(oldPlayer).display();
    players.get(newPlayer).active=true;
    players.get(newPlayer).display();  
  }  
  public void refreshCardPanel(){
    cardPanel.removeAll();
    
    for(int i=0;i<game.matcards.size();i++){
      cardPanel.add(new JLabel(new ImageIcon(getImage(game.matcards.get(i).getImage()))));
    }
    refreshSharedFields();
    
    repaint();
    revalidate();
  }
  public void changePhase(String oldPhase, String newPhase){
    try{
      dataPanel.remove(doneButtons.get(oldPhase)); 
    }catch(NullPointerException e){
    }
   
    try{
      dataPanel.add(doneButtons.get(newPhase)); 
    }catch(NullPointerException e){
    }
 
    players.get(game.getActivePlayer()).display();
     
    repaint();
    revalidate();
  }
  private void refreshSharedFields(){
    moneyField.setText(game.getMoney()+"");
    buysField.setText(game.getBuys()+"");
    actionsField.setText(game.getActions()+"");  
  }
  public void showScores(Map<String,Integer> points){
    String out="";
    Iterator <Map.Entry<String,Integer>> it=points.entrySet().iterator();
    while(it.hasNext()){
      Map.Entry<String,Integer> entry=it.next();
      out+=entry.getKey()+": "+entry.getValue()+"\n";
    }
    JOptionPane.showMessageDialog(this,out);
  }
  public void pressButton(JButton button){
    for(ActionListener a: button.getActionListeners()) {
      a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
    }          
  }   
  public void setMask(boolean [] t){
    mask=t;
    maskSet=true;
    players.get(game.getActivePlayer()).display();
  }
  public String optionPane(Dominion.OptionData o){
    OptionPane pane=new OptionPane(this,o);
    return pane.getValue();
  }
  
  //***ACCESS FUNCTIONS***///
  public void displaySupply(String supplyName, Deck.Data data){ 
    supplyDecks.get(supplyName).display(data); 
  }
  public void displayPlayer(int i){ 
    players.get(i).display(); 
  }
  public void displayTrash(Deck.Data data){
    trash.display(data);
  }

////////////************CLASSES THAT HANDLE LARGE COMPONENTS*********///
  //hold a JPanel and some associated info associated with a player
  protected class PlayerDisplay implements ActionListener{
    private JPanel panel=new JPanel();
    private JPanel handPanel=new JPanel();
    private JPanel infoPanel=new JPanel();
    private DominionPlayer player;
    public boolean active=false;
    private DeckDisplay deck,disc;
    private JButton treasuresButton=new JButton("Play All Treasures");
    
    public PlayerDisplay(DominionPlayer tplayer){
      player=tplayer;

      panel.setLayout(new GridLayout(2,0));
      panel.setBorder(BorderFactory.createLineBorder(Color.black));
      infoPanel.setLayout(new FlowLayout());
      handPanel.setLayout(new FlowLayout());

      infoPanel.add(new JLabel(player.getName()));
      deck=new DeckDisplay(player.deck.makeData());
      infoPanel.add(deck.getPanel());
      disc=new DeckDisplay(player.disc.makeData());
      infoPanel.add(disc.getPanel());

      infoPanel.add(treasuresButton);
     //action listener for the done buying button
      treasuresButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
          if(game.getPhase()=="actions"){
            pressButton(doneButtons.get("actions"));
          } 

          if(game.getPhase()!="buys") return;
          Iterator it=player.hand.iterator();
          int i=0;
          DominionCard card;
          while(it.hasNext()){
            card=(DominionCard)it.next();
            ActionEvent temp=new ActionEvent(this,0,Integer.toString(i));
            if(card.isMoney){
              PlayerDisplay.this.actionPerformed(temp);
              it=player.hand.iterator();
              i=0;
            }
            else i++;
          }
         }
      });
         
      panel.add(infoPanel);
      panel.add(handPanel);
      
      display();
    }
    public void display(){
      handPanel.removeAll();
      
      Iterator<DominionCard> it=player.hand.iterator();
      int i=0;
//      if (maskSet) 
//        for(int k=0;k<mask.length;k++) System.out.println(mask[k]);
      while(it.hasNext()){
        DominionCard card=it.next();
        JButton button=new JButton();
        if(active){
          button.setIcon(new ImageIcon(getImage(card.getImage())));          
          if((game.getPhase()=="actions" && (card.isMoney || card.isAction)) || (game.getPhase()=="buys" && card.isMoney) 
            || ( (game.getPhase()=="topdeck" || game.getPhase()=="discard" || game.getPhase()=="trash" || game.getPhase()=="throneroom") && (!maskSet || mask[i])  ) ) {
            button.setEnabled(true);   
          }else button.setEnabled(false);
        }else{
          button.setIcon(new ImageIcon(getImage(Deck.dominionBack)));
          button.setEnabled(false);
        }
        button.addActionListener(this);
        button.setActionCommand(Integer.toString(i));
        handPanel.add(button);
        i++;
      }
      deck.display(player.deck.makeData());
      disc.display(player.disc.makeData());
      repaint();
      revalidate();
    }
    public JPanel getPanel(){
      return panel;
    }
    public void actionPerformed(ActionEvent e){
      int cardnum=Integer.parseInt(e.getActionCommand());
      if(game.getPhase()=="actions" && player.hand.get(cardnum).isMoney){
        pressButton(doneButtons.get("actions"));
      }
      game.playCard(cardnum);
      players.get(game.getActivePlayer()).display();
      refreshCardPanel();
      
    }
    
  }
 
  //holds a JPanel and some associated info associated with a deck of cards
  public class DeckDisplay{
    protected JTextField n;
    private JLabel picLabel=new JLabel();
    protected JPanel panel=new JPanel();
    protected BufferedImage image;
    protected DeckDisplay(){
      
    }
    public DeckDisplay(Deck.Data data){
      image=getImage(data.image);
      picLabel = new JLabel(new ImageIcon(image));
      n=new JTextField(2);
      n.setEditable(false);
      panel.add(n);
      panel.add(picLabel);    
      n.setText(data.size+"");  
    }
    public void display(Deck.Data data){
      image=getImage(data.image);
      picLabel = new JLabel(new ImageIcon(image));
      n.setText(data.size+"");
      panel.removeAll();
      panel.add(n);
      panel.add(picLabel);
    }
    public JPanel getPanel(){
      return panel;
    }
  }
  //an extension of DeckDisplay for supply piles, JLabel is replaced by a button
  public class SupplyDisplay extends DeckDisplay{
    private JButton button=new JButton();
    private JTextField cost;
    private String name;
    public SupplyDisplay(Deck.Data data, String tname){
      name=tname;
      image=getImage(data.image);
      n=new JTextField(2);
      n.setEditable(false);
      panel.add(n);      
      n.setText(data.size+"");  
      
      cost=new JTextField(1);
      cost.setEditable(false);
      panel.add(cost);
      cost.setText(game.supplyCost(name)+"");
      
      button.setIcon(new ImageIcon(image));
      panel.add(button);

      //action listener
      button.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            if(game.getPhase()=="buys" || game.getPhase()=="gain"){
              boolean success=game.gainCard(name,game.getPhase()=="buys",false);
              if(success){
                Deck.Data newdata=game.supplyData(name);
                players.get(game.getActivePlayer()).display();
                display(newdata);
                moneyField.setText(game.getMoney()+"");
                buysField.setText(game.getBuys()+"");
                if(game.getBuys()==0){
                  pressButton(doneButtons.get("buys"));
                }else if(game.getPhase()=="gain") pressButton(doneButtons.get("gain"));
              }
            }
         }
      });

    }

    public void display(Deck.Data data){
      image=getImage(data.image);
      button.setIcon(new ImageIcon(image));
      n.setText(data.size+"");
      cost.setText(game.supplyCost(name)+"");
      panel.removeAll();
      panel.add(n);
      panel.add(cost);
      panel.add(button);
    }

  }
  public static class OptionPane extends JDialog implements ActionListener{
    private String out="";
    public OptionPane(JFrame parent, Dominion.OptionData o){
      super(parent, "", true);
      if (parent != null) {
        Dimension parentSize = parent.getSize(); 
        Point p = parent.getLocation(); 
        setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
      }
      
      getContentPane().setLayout(new FlowLayout());
      for(int i=0; i<o.size(); i++){
        if(o.getValue(i)=="image"){
          getContentPane().add(new JLabel(new ImageIcon(getImage(o.getKey(i))))); 
        }else if(o.getValue(i)=="text"){
          getContentPane().add(new JLabel(o.getKey(i))); 
        }else if(o.getValue(i).contains("button")){
          JButton b=new JButton();
          b.addActionListener(this);
          b.setActionCommand(o.getKey(i));
          if(o.getValue(i)=="imagebutton")
            b.setIcon(new ImageIcon(getImage(o.getKey(i))));
          else if(o.getValue(i)=="textbutton"){
            b.setText(o.getKey(i));
          }
          getContentPane().add(b);
        }
      }
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      pack();
      setVisible(true); 
    }
    public void actionPerformed(ActionEvent e){
      out=e.getActionCommand();
      setVisible(false);
      dispose();
    }
    public String getValue(){
      return out;
    }
  }

/////**********STATIC METHODS********////
  public static BufferedImage getImage(String imagename){
    BufferedImage img=new BufferedImage(1,1,1);
    try{
      img = ImageIO.read(new File(imagename));     
      img=resize(img,80,110);
      return img;
    }catch(IOException e){
      System.out.println("failed to load image"+imagename);
    }
    return img;
  }
  public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g2d = dimg.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();

    return dimg;
  } 
   // The entry main() method
   public static void main(String[] args) {
      // Run the GUI construction in the Event-Dispatching thread for thread-safety
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            String [] playernames={"Scott","Khoi"};
            Dominion engine=new Dominion(playernames);
            DominionBoard board=new DominionBoard(engine); // Let the constructor do the job
            engine.setBoard(board);
         }
      });
   }

}
