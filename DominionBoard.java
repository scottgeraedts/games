import java.awt.*;       // Using layouts
import java.awt.event.*; // Using AWT event classes and listener interfaces
import javax.swing.*;    // Using Swing components and containers
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class DominionBoard extends JFrame{

  private Container cp;
  private String output="";

  //player panel
  private ArrayList<PlayerDisplay> players=new ArrayList<PlayerDisplay>();

  //supply panel
  private LinkedHashMap<String,SupplyDisplay> supplyDecks=new LinkedHashMap<>();
  
  //mat panel
  private JPanel cardPanel;
  private JPanel dataPanel;
  private JPanel supplyPanel;
  private HashMap<String,JButton> doneButtons=new HashMap<>();
  private JTextField moneyField,buysField,actionsField;
  private JTextField helpField;
  private DeckDisplay trash;
  
  private boolean mask[];
  private boolean maskSet=false;

  private String phase="actions";
  private int actions, money, buys;
  private int activePlayer=0;
  
  private HashMap<String,ImageIcon> imageTable=new HashMap<>();
  private HashMap<String,ImageIcon> giantImageTable=new HashMap<>();
  
  public DominionBoard(ArrayList<DominionPlayer.Data> playersData, ArrayList<Deck.SupplyData> supplyData, int playerNum, int startingPlayer){
		setTitle("Dominion");

    doneButtons.put("buys",new JButton("Done Buying"));
    doneButtons.put("actions",new JButton("Done Actions"));
    doneButtons.put("topdeck",new JButton("Done Topdecking"));
    doneButtons.put("discard",new JButton("Done Discarding"));
    doneButtons.put("trash",new JButton("Done Trashing"));
    doneButtons.put("gain",new JButton("Done Gaining"));
    doneButtons.put("select",new JButton("Done Selecting"));
    doneButtons.put("reveal",new JButton("Done Revealing"));
    
    trash=new DeckDisplay(new Deck.Data(0,Deck.blankBack));
    
    //some graphics setup
  	setSize(1500,800);
		cp = getContentPane();
		cp.setLayout(new GridLayout(0,2));  // The content-pane sets its layout
    cp.add(setupPlayerPanel(playersData,playerNum,startingPlayer));
    cp.add(setupSharedPanel(supplyData));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit program if close-window button clicked      
    setVisible(true);
  }
  public void reset(ArrayList<DominionPlayer.Data> playersData, ArrayList<Deck.SupplyData> supplyData, int startingPlayer){
    //reset supplies
    supplyPanel.removeAll();
    SupplyDisplay tempSupply;
    for(int i=0; i<supplyData.size(); i++){
      tempSupply=new SupplyDisplay(supplyData.get(i));
      supplyDecks.put(supplyData.get(i).name,tempSupply);
      supplyPanel.add(tempSupply.getPanel()); 
    }
    
    //reset players
    for(int i=0;i<playersData.size();i++){
      displayPlayer(i,playersData.get(i));
    }
    changePlayer(activePlayer,playersData.get(activePlayer),startingPlayer,playersData.get(startingPlayer));
    activePlayer=startingPlayer;
    changePhase(phase,"actions");
  }
  public void playAgain(){
    String [] options={"Play again","Quit"};
    OptionData o=new OptionData(options);
    optionPane(o);
  }
  public void kill(){
    setVisible(false);
    dispose();
  }
  private JPanel setupPlayerPanel(ArrayList<DominionPlayer.Data> playersData, int playerNum, int startingPlayer){
    
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(playersData.size(),1));
    for(int i=0;i<playersData.size();i++){
      players.add( new PlayerDisplay(playersData.get(i)) );
      panel.add(players.get(i).getPanel());
      //players.get(i).controlled=true;
    }
    players.get(playerNum).controlled=true;
    System.out.println("this client controls player "+playerNum);
    activePlayer=startingPlayer;
    players.get(startingPlayer).active=true;
    players.get(startingPlayer).display(playersData.get(startingPlayer));
    return panel;
  }  
  
  private JPanel setupSharedPanel(ArrayList<Deck.SupplyData> supplyData){
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(2,0));
    
    supplyPanel=new JPanel();
    SupplyDisplay tempSupply;
    for(int i=0; i<supplyData.size(); i++){
      tempSupply=new SupplyDisplay(supplyData.get(i));
      supplyDecks.put(supplyData.get(i).name,tempSupply);
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
    
    JPanel infoPanel=new JPanel();
    infoPanel.setLayout(new GridLayout(1,2));
    
    
    //action listener for the done buying button
    EndSelection listener=new EndSelection();
    String [] phases={"actions","buys","topdeck","discard","trash","gain","select","reveal"};
    for(int i=0;i<phases.length;i++){
      doneButtons.get(phases[i]).addActionListener(listener);
      doneButtons.get(phases[i]).setActionCommand("B"+phases[i]);
    }
    
    JPanel fieldsPanel=new JPanel();
    fieldsPanel.setLayout(new GridLayout(3,2));
    JLabel actionsLabel=new JLabel("Actions: ");
    fieldsPanel.add(actionsLabel);
    actionsField=new JTextField(2);
    actionsField.setEditable(false);
    fieldsPanel.add(actionsField);
    
    JLabel moneyLabel=new JLabel("Money: ");
    fieldsPanel.add(moneyLabel);
    moneyField=new JTextField(2);
    moneyField.setEditable(false);
    fieldsPanel.add(moneyField);

    JLabel buysLabel=new JLabel("Buys: ");
    fieldsPanel.add(buysLabel);
    buysField=new JTextField(2);
    buysField.setEditable(false);
    fieldsPanel.add(buysField);
    dataPanel.add(fieldsPanel);

    dataPanel.add(trash.getPanel());
    dataPanel.add(doneButtons.get("actions"));
    
    refreshSharedFields(1,0,1);
 
    helpField=new JTextField("");
    infoPanel.add(helpField);
    infoPanel.add(dataPanel);
 
    matPanel.add(infoPanel);
    matPanel.add(cardPanel);
    panel.add(matPanel);
   
    return panel;
  }
  public class EndSelection implements ActionListener{
    public EndSelection(){}
    public void actionPerformed(ActionEvent evt){
      if(players.get(activePlayer).controlled)
        output=evt.getActionCommand();
    }
  }
  public void changePlayer(int oldPlayer, DominionPlayer.Data oldData, int newPlayer, DominionPlayer.Data newData){
    maskSet=false;
    players.get(oldPlayer).active=false;
    players.get(oldPlayer).display(oldData);
    activePlayer=newPlayer; 
    players.get(newPlayer).active=true;
    players.get(newPlayer).display(newData); 
    refreshSharedFields(1,0,1);
    cardPanel.removeAll();
    repaint();
    revalidate();
  }  
  public void refreshCardPanel(ArrayList<DominionCard> matcards){
    cardPanel.removeAll();
    
    for(int i=0;i<matcards.size();i++){
      cardPanel.add(new JLabel(getImage(matcards.get(i).getImage())));
    }
//    refreshSharedFields();
    
    repaint();
    revalidate();
  }
  public void changePhase(String oldPhase, String newPhase){
    maskSet=false;

    phase=newPhase;
    try{
      dataPanel.remove(doneButtons.get(oldPhase)); 
    }catch(NullPointerException e){
    }
   
    try{
      dataPanel.add(doneButtons.get(newPhase)); 
    }catch(NullPointerException e){
    }
 
    players.get(activePlayer).display();
     
    repaint();
    revalidate();
  }
  public void refreshSharedFields(int actions, int money, int buys){
    this.actions=actions;
    this.money=money;
    this.buys=buys;
    moneyField.setText(money+"");
    buysField.setText(buys+"");
    actionsField.setText(actions+"");  
  }
  public void showScores(OptionData points){
    String out="";
    for(int i=0;i<points.size();i++){
      out+=points.getKey(i)+": "+points.getValue(i)+"\n";
    }
    JOptionPane.showMessageDialog(this,out);
  }
  public void pressButton(JButton button){
    for(ActionListener a: button.getActionListeners()) {
      a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
    }          
  }   
  public ImageIcon getImage(String imagename){
    return getImage(imagename,false);
  }
  public ImageIcon getImage(String imagename, boolean giant){
    HashMap<String,ImageIcon> dummy;
    if(giant) dummy=giantImageTable;
    else dummy=imageTable;
    
    if(dummy.containsKey(imagename)){
      return dummy.get(imagename);
    }else{
      ImageIcon img=new ImageIcon();
      //System.out.println("loading image "+imagename);
      img = new ImageIcon(this.getClass().getResource("DominionCards/"+imagename+".png"));     
      if(giant) img=new ImageIcon(resize(img.getImage(),400,550));
      else img=new ImageIcon(resize(img.getImage(),80,110));
      dummy.put(imagename,img);
      return img;
    }
  }
  

  public void setMask(boolean [] t){
    mask=t;
    maskSet=true;
    players.get(activePlayer).display();
  }
  public void optionPane(OptionData o){
    OptionPane pane=new OptionPane(this,o);
    output=pane.getValue();
  }
  
//  //***ACCESS FUNCTIONS***///
    public String getOutput(){
      String temp=output;
      output="";
      return temp;
    }
  public void displaySupply(Deck.SupplyData data){ 
    supplyDecks.get(data.name).display(data); 
  }
  public void displayPlayer(int i, DominionPlayer.Data data){ 
    players.get(i).display(data); 
  }
  public void displayTrash(Deck.Data data){
    trash.display(data);
  }
  public void displayComment(String comment){
    helpField.setText(comment);
  }

//////////////************CLASSES THAT HANDLE LARGE COMPONENTS*********///
  //hold a JPanel and some associated info associated with a player
  protected class PlayerDisplay implements ActionListener{
    private JPanel panel=new JPanel();
    private JPanel handPanel=new JPanel();
    private JPanel infoPanel=new JPanel();
    public boolean active=false;
    public boolean controlled=false;
    public DominionPlayer.Data player;
    private DeckDisplay deck,disc;
    private JButton treasuresButton=new JButton("Play All Treasures");
    
    public PlayerDisplay(DominionPlayer.Data tplayer){

      player=tplayer;
      panel.setLayout(new GridLayout(2,0));
      panel.setBorder(BorderFactory.createLineBorder(Color.black));
      infoPanel.setLayout(new FlowLayout());
      handPanel.setLayout(new FlowLayout());

      JLabel nameLabel=new JLabel(player.name);
      nameLabel.setFont(new Font("Arial", Font.BOLD, 30));
      infoPanel.add(nameLabel);
      deck=new DeckDisplay(player.deck);
      infoPanel.add(deck.getPanel());
      disc=new DeckDisplay(player.disc);
      infoPanel.add(disc.getPanel());

      infoPanel.add(treasuresButton);
     //action listener for the done buying button
      treasuresButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
          if(active && (phase.equals("actions") || phase.equals("buys"))){
            output="Btreasures";
          }
         }
      });
         
      panel.add(infoPanel);
      panel.add(handPanel);
      
      display(player);
    }
    //use last times data to display
    public void display(){
      display(player);
    }
    public void display(DominionPlayer.Data tplayer){
      player=tplayer;
      handPanel.removeAll();
      
      Iterator<DominionCard> it=player.hand.iterator();
      int i=0;
//      if (maskSet) 
//        for(int k=0;k<mask.length;k++) System.out.println(mask[k]);
      while(it.hasNext()){
        DominionCard card=it.next();
        JButton button=new JButton();
        //if its your turn and its your player you can see the cards and click on some of them
        if(active && controlled){
          button.setIcon(getImage(card.getImage()));          
          if((phase.equals("actions") && (card.isMoney || card.isAction)) || (phase.equals("buys") && card.isMoney) ){
            button.setEnabled(true);
          }else if ((!maskSet || mask[i]) && 
              (phase.equals("trash") || phase.equals("discard") || phase.equals("topdeck") 
              || phase.equals("select") || phase.equals("reveal"))){

            button.setEnabled(true); 
            button.setOpaque(true);  
            if(phase.equals("trash")) button.setBackground(Color.RED);
            else if(phase.equals("discard")) button.setBackground(Color.YELLOW);
            else if(phase.equals("topdeck")) button.setBackground(Color.GREEN);
            else if(phase.equals("select") || phase.equals("reveal")) button.setBackground(Color.ORANGE);           
                  
          }else button.setEnabled(false);
          
        //if its not your player you cant see anything
        }else if(!controlled){
          button.setIcon(getImage(Deck.dominionBack));
          button.setEnabled(false);
        //if you control this player but its not your turn, you can see the cards but not click them
        }else{
          button.setIcon(getImage(card.getImage()));
          button.setEnabled(false);
        }
        button.addActionListener(this);
        button.setActionCommand(Integer.toString(i));
        handPanel.add(button);
        i++;
      }
      deck.display(player.deck);
      disc.display(player.disc);
      repaint();
      revalidate();
    }
    public JPanel getPanel(){
      return panel;
    }
    public void actionPerformed(ActionEvent e){
      int cardnum=Integer.parseInt(e.getActionCommand());
      if(phase.equals("actions") && player.hand.get(cardnum).isMoney){
        pressButton(doneButtons.get("actions"));
      }
      output=Integer.toString(cardnum);
      
    }
    
  }
 
  //holds a JPanel and some associated info associated with a deck of cards
  public class DeckDisplay{
    protected JTextField n;
    private JLabel picLabel=new JLabel();
    protected JPanel panel=new JPanel();
    protected JLayeredPane layer=new JLayeredPane();
    protected ImageIcon image;
    protected DeckDisplay(){
      
    }
    public DeckDisplay(Deck.Data data){

      panel.setLayout(new OverlayLayout(panel));

      image=getImage(data.image);
      picLabel = new JLabel(image);
      picLabel.setOpaque(false);
      panel.add(picLabel);  
      
      n=new JTextField(2);
      n.setBackground(Color.WHITE);
      n.setEditable(false);
      n.setAlignmentY(0.f);
      n.setAlignmentX(0.f);
      n.setFont(new Font("Arial", Font.BOLD, 20));
      n.setText(data.size+"");  
      n.setMaximumSize(new Dimension(40,25));
      panel.add(n);

      panel.add(layer);  

      
      
    }
    public void display(Deck.Data data){
      image=getImage(data.image);
      picLabel = new JLabel(image);
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
    private Deck.SupplyData data;
    public SupplyDisplay(Deck.SupplyData tdata){
      data=tdata;
      name=data.name;
      image=getImage(data.image);
      
      panel.setLayout(new OverlayLayout(panel));
      
      n=new JTextField(2);
      n.setEditable(false);
      n.setBackground(Color.WHITE);
      n.setAlignmentY(0.f);
      n.setAlignmentX(0.f);
      n.setFont(new Font("Arial", Font.BOLD, 20));
      n.setMaximumSize(new Dimension(30,25));
      n.setText(data.size+"");  
      
      panel.add(n);      
      
      cost=new JTextField(1);
      cost.setBackground(Color.WHITE);
      cost.setEditable(false);
      cost.setAlignmentY(1.f);
      cost.setAlignmentX(0.f);
      cost.setFont(new Font("Arial", Font.BOLD, 20));
      cost.setMaximumSize(new Dimension(30,25));
      cost.setText(data.cost+"");
      panel.add(cost);
      
      button.setIcon(image);
      button.setOpaque(false);
      panel.add(button);

      //action listener
      button.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
          if( (phase.equals("selectDeck") || phase.equals("gain") || (phase.equals("buys") && money>=data.cost && buys>0) ) && data.size>0){
            output="G"+name;
          }
        }
      });
      
      //display card on right click
      button.addMouseListener(new SupplyMouseAdapter(DominionBoard.this,name));
//      button.addMouseListener(new MouseAdapter(){
//        JDialog popup=new JDialog();
//        
//        @Override
//        public void mouseClicked(MouseEvent e){
//          if(SwingUtilities.isRightMouseButton(e)){
//            System.out.println("Right clicked!");
//          }
//        }
//        @Override
//        public void mouseClicked(MouseEvent e){
//          if(SwingUtilities.isRightMouseButton(e)){
//            System.out.println("Right clicked!");
//            JDialog popup
//          }
//        }
//      });

    }

    public void display(Deck.SupplyData tdata){
      data=tdata;
      image=getImage(data.image);
      button.setIcon(image);
      n.setText(data.size+"");
      cost.setText(data.cost+"");
      panel.removeAll();
      panel.add(n);
      panel.add(cost);
      panel.add(button);
    }

  }
  public class SupplyMouseAdapter extends MouseAdapter{
    JDialog popup=new JDialog();
    JLabel image;
    public SupplyMouseAdapter(JFrame parent, String name){
      Dimension parentSize = parent.getSize(); 
      Point p = parent.getLocation(); 
      popup.setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
      image=new JLabel(getImage(name,true));
      popup.add(image);
      popup.pack();
      popup.setDefaultCloseOperation(HIDE_ON_CLOSE);
    }
    @Override
    public void mousePressed(MouseEvent e){
      if(SwingUtilities.isRightMouseButton(e)){
        popup.setVisible(true);
      }
    }
    @Override
    public void mouseReleased(MouseEvent e){
      if(SwingUtilities.isRightMouseButton(e)){
        popup.setVisible(false);
      }
    }
  }
  
  //displayers a popup of options for the user to click, closes when one is selected
  //can't be static because it uses the image hashtable
  public class OptionPane extends JDialog implements ActionListener{
    private String out="";
    public OptionPane(JFrame parent, OptionData o){
      super(parent, "", true);
      if (parent != null) {
        Dimension parentSize = parent.getSize(); 
        Point p = parent.getLocation(); 
        setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
      }
      getContentPane().setLayout(new FlowLayout());
      for(int i=0; i<o.size(); i++){
        if(o.getValue(i).equals("image")){
          getContentPane().add(new JLabel(getImage(o.getKey(i)))); 
        }else if(o.getValue(i).equals("text")){
          getContentPane().add(new JLabel(o.getKey(i))); 
        }else if(o.getValue(i).contains("button")){
          JButton b=new JButton();
          b.addActionListener(this);
          b.setActionCommand(o.getKey(i));
          if(o.getValue(i).equals("imagebutton"))
            b.setIcon(getImage(o.getKey(i)));
          else if(o.getValue(i).equals("textbutton")){
            b.setText(o.getKey(i));
          }
          getContentPane().add(b);
        }else{
          System.out.println("invalid option display type");
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
//  public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
//    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

//    Graphics2D g2d = dimg.createGraphics();
//    g2d.drawImage(tmp, 0, 0, null);
//    g2d.dispose();

//    return dimg;
//  }
  private static Image resize(Image srcImg, int w, int h){
    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = resizedImg.createGraphics();

    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(srcImg, 0, 0, w, h, null);
    g2.dispose();

    return resizedImg;
  }   
//   // The entry main() method
//   public static void main(String[] args) {
//      // Run the GUI construction in the Event-Dispatching thread for thread-safety
//      SwingUtilities.invokeLater(new Runnable() {
//         @Override
//         public void run() {
//            String [] playernames={"Scott","Khoi"};
//            Dominion engine=new Dominion(playernames);
//            DominionBoard board=new DominionBoard(engine); // Let the constructor do the job
//            engine.setBoard(board);
//         }
//      });
//   }

}
