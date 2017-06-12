import java.awt.*;       // Using layouts
import java.awt.event.*; // Using AWT event classes and listener interfaces
import javax.swing.*;    // Using Swing components and containers
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class DominionBoard extends JFrame{

  private String output="";
  boolean lock=true;

  //player panel
  private ArrayList<PlayerDisplay> players=new ArrayList<PlayerDisplay>();

  //supply panel
  private LinkedHashMap<String,SupplyDisplay> supplyDecks=new LinkedHashMap<>();
  private HashMap<String, ImageIcon> tokens=new HashMap<>();
  
  //mat panel
  private JPanel cardPanel;
  private JPanel dataPanel;
  private JPanel supplyPanel;
  private JPanel fieldsPanel; //global so it can be reset
  private HashMap<String,JButton> doneButtons=new HashMap<>();
  private JButton coinTokenButton=new JButton("Play Coin Token");
  private JButton debtButton=new JButton("Pay Off Debt");
  private HashMap<String, JTextField> specialFields;
  private HashMap<String, JLabel> specialLabels;
  private JTextField helpField;
  private DeckDisplay trash;
  
  private String phase="actions";
  private int money, buys;
  private ArrayList<String> playedDuration=new ArrayList<>();
  private int activePlayer=0;
  
  private HashMap<String,ImageIcon> imageTable=new HashMap<>();
  private HashMap<String,ImageIcon> giantImageTable=new HashMap<>();
  
  public DominionBoard(ArrayList<DominionPlayer.Data> playersData, 
      ArrayList<Deck.SupplyData> supplyData, ArrayList<Integer> controlled, int startingPlayer,
                       ArrayList<String> gameOptions, ArrayList<String> playerOptions){

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

    //images that will be used later to mark supply piles
    String [] s={"embargo", "contraband","obelisk", "bane"};
    ImageIcon tempIcon=null;
    for (String value : s) {
      try {
        tempIcon = new ImageIcon(this.getClass().getResource("DominionCards/" + value + "token.jpg"));
      } catch (NullPointerException ex) {
        try {
          tempIcon = new ImageIcon(this.getClass().getResource("DominionCards/" + value + "token.png"));
        } catch (NullPointerException ex2) {
          System.out.println("failed to load " + value);
        }
      }
      tokens.put(value, new ImageIcon(resize(tempIcon.getImage(), 35, 25)));
    }

    //some graphics setup
  	setSize(1500,800);
  	Container cp;
		cp = getContentPane();
		cp.setLayout(new GridLayout(0,2));  // The content-pane sets its layout
    cp.add(setupPlayerPanel(playersData, controlled, startingPlayer, playerOptions));
    cp.add(setupSharedPanel(supplyData, gameOptions));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit program if close-window button clicked      
    setVisible(true);
  }
  public void reset(ArrayList<DominionPlayer.Data> playersData, 
      ArrayList<Deck.SupplyData> supplyData, int startingPlayer, ArrayList<String> gameOptions,
                    ArrayList<String> playerOptions){

    //reset supplies
    supplyPanel.removeAll();
    SupplyDisplay tempSupply;
    for(int i=0; i<supplyData.size(); i++){
      tempSupply=new SupplyDisplay(supplyData.get(i), nameList(supplyData));
      supplyDecks.put(supplyData.get(i).name,tempSupply);
      supplyPanel.add(tempSupply.getPanel()); 
    }
    
    //reset players
    for(int i=0;i<playersData.size();i++){
      players.get(i).setupOptional(playerOptions);
      displayPlayer(i,playersData.get(i),new ArrayList<Boolean>());
    }
    changePlayer(activePlayer,playersData.get(activePlayer),startingPlayer,playersData.get(startingPlayer),new ArrayList<Boolean>());
    activePlayer=startingPlayer;
    changePhase(phase,"actions",new ArrayList<Boolean>());
    displayTrash(new Deck.Data(0,Deck.blankBack));
    resetSharedPanel(gameOptions);
    repaint();
    revalidate();
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
  private JPanel setupPlayerPanel(ArrayList<DominionPlayer.Data> playersData, ArrayList<Integer> controlled,
                                  int startingPlayer, ArrayList<String> playerOptions){
    
    players.clear();
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(playersData.size(),1));
    for(int i=0;i<playersData.size();i++){
      players.add( new PlayerDisplay(playersData.get(i), playerOptions) );
      panel.add(players.get(i).getPanel());
    }
    for(Integer i : controlled) players.get(i).controlled=true;

    activePlayer=startingPlayer;
    players.get(startingPlayer).active=true;
    players.get(startingPlayer).display(playersData.get(startingPlayer),new ArrayList<Boolean>());
    return panel;
  }  
  
  private JPanel setupSharedPanel(ArrayList<Deck.SupplyData> supplyData, ArrayList<String> gameOptions){
    JPanel panel=new JPanel();
    panel.setLayout(new GridLayout(2,0));
    
    supplyPanel=new JPanel();
    supplyPanel.setPreferredSize(new Dimension(500,1000));
    SupplyDisplay tempSupply;
    for(int i=0; i<supplyData.size(); i++){
      tempSupply=new SupplyDisplay(supplyData.get(i), nameList(supplyData));
      supplyDecks.put(supplyData.get(i).name,tempSupply);
      supplyPanel.add(tempSupply.getPanel()); 
    }  
//    scrollPane.add(supplyPanel);    
    JScrollPane scrollPane=new JScrollPane(supplyPanel);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);    
    panel.add(scrollPane);

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
    coinTokenButton.addActionListener(listener);
    coinTokenButton.setActionCommand("Bcoin");

    debtButton.addActionListener(listener);
    debtButton.setActionCommand("Bdebt");

    fieldsPanel=new JPanel();
    fieldsPanel.setLayout(new GridLayout(5,2));

    resetSharedPanel(gameOptions);
    dataPanel.add(fieldsPanel);

    dataPanel.add(trash.getPanel());
    dataPanel.add(doneButtons.get("actions"));

    refreshSharedFields();
 
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
        if(!lock) output=evt.getActionCommand();
    }
  }
  //optional additions to the fields panel
  private void resetSharedPanel(ArrayList<String> gameOptions){
    //try to remove options
    fieldsPanel.removeAll();
    specialFields=new HashMap<>();
    specialLabels=new HashMap<>();
    for(String option : gameOptions){
      specialFields.put(option, new JTextField(2));
      specialFields.get(option).setEditable(false);
      specialFields.get(option).setText("0");
      specialLabels.put(option, new JLabel(option+": "));
      fieldsPanel.add(specialLabels.get(option));
      fieldsPanel.add(specialFields.get(option));
    }

  }
  public void changePlayer(int oldPlayer, DominionPlayer.Data oldData, int newPlayer, DominionPlayer.Data newData, ArrayList<Boolean> mask){
    players.get(oldPlayer).active=false;
    players.get(oldPlayer).display(oldData,new ArrayList<Boolean>());
    activePlayer=newPlayer; 
    players.get(newPlayer).active=true;
    players.get(newPlayer).display(newData,mask); 
    refreshSharedFields();
    cardPanel.removeAll();
    playedDuration=new ArrayList<>(players.get(newPlayer).durationCards);
    players.get(newPlayer).durationCards.clear();
    refreshCardPanel(new ArrayList<DominionCard>());
//    repaint();
//    revalidate();
  }  
  public void refreshCardPanel(ArrayList<DominionCard> matcards){
    cardPanel.removeAll();
    for(String card : playedDuration){
      cardPanel.add(new JLabel(getImage(card)));
    }
    
    for(int i=0;i<matcards.size();i++){
      cardPanel.add(new JLabel(getImage(matcards.get(i).getImage())));
    }
//    refreshSharedFields();
    
    repaint();
    revalidate();
  }
  public void changePhase(String oldPhase, String newPhase, ArrayList<Boolean> mask){

    phase=newPhase;
    try{
      //remove this every time, only sometimes readd it
      //if the button isn't there nothing bad will happen
      dataPanel.remove(coinTokenButton);
      dataPanel.remove(doneButtons.get(oldPhase));
    }catch(NullPointerException e){
    }
   
    try{
      dataPanel.add(doneButtons.get(newPhase)); 
      if(newPhase.equals("buys") && players.get(activePlayer).coinTokens>0) dataPanel.add(coinTokenButton);
    }catch(NullPointerException e){
    }
 
    displayPlayer(activePlayer,players.get(activePlayer).player,mask);
    repaint();
    revalidate();
  }
  private void refreshSharedFields(){
    for(Map.Entry<String, JTextField> e : specialFields.entrySet()){
      if(e.getKey().equals("Actions") || e.getKey().equals("Buys"))
        e.getValue().setText("1");
      else
        e.getValue().setText("0");
    }
    buys=1;

  }
  void refreshSharedFields(PairList<String, Integer> fields){
    for(int i=0; i<fields.size(); i++){
      specialFields.get(fields.getKey(i)).setText(fields.getValue(i)+"");
    }
    money=fields.get("Money");
    buys=fields.get("Buys");
  }
  void showScores(OptionData points){
    String out="";
    for(int i=0;i<points.size();i++){
      out+=points.getValue(i)+"\n";
    }
    JOptionPane.showMessageDialog(this,out);
  }
  void pressButton(JButton button){
    for(ActionListener a: button.getActionListeners()) {
      a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
    }          
  }   
  public ImageIcon getImage(String imagename){
    return getImage(imagename,false, false);
  }
  public ImageIcon getImage(String imagename, boolean giant, boolean event){
    HashMap<String,ImageIcon> dummy;
    if(giant) dummy=giantImageTable;
    else dummy=imageTable;
    
    if(dummy.containsKey(imagename)){
      return dummy.get(imagename);
    }else{
      ImageIcon img=new ImageIcon();
      //System.out.println("loading image "+imagename);
      try{
        img = new ImageIcon(this.getClass().getResource("DominionCards/"+imagename+".jpg"));
      }catch(NullPointerException ex){
        try{
          img = new ImageIcon(this.getClass().getResource("DominionCards/"+imagename+".png"));
        }catch(NullPointerException ex2) {
          System.out.println("failed to load " + imagename);
        }
      }
      if(giant){
        if(event) img=new ImageIcon(resize(img.getImage(),600,250));
        else img=new ImageIcon(resize(img.getImage(),400,550));
      }
      else{
        if(event) img=new ImageIcon(resize(img.getImage(),120,80));
        else img=new ImageIcon(resize(img.getImage(),80,110));
      }
      dummy.put(imagename,img);
      return img;
    }
  }
  

  public void optionPane(OptionData o){
    OptionPane pane=new OptionPane(this,o);
    if(!lock) output=pane.getValue();
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
  public void displayPlayer(int i, DominionPlayer.Data data, ArrayList<Boolean> mask){ 
    players.get(i).display(data,mask);
    //do we need to add in a debt button
    dataPanel.remove(debtButton);
    if(phase.equals("buys") && players.get(activePlayer).debt>0){
      dataPanel.add(debtButton);
      repaint();
      revalidate();
    }
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
    boolean active=false;
    boolean controlled=false;
    public DominionPlayer.Data player;
    private DeckDisplay deck,disc;
    private JButton treasuresButton=new JButton("Play All Treasures");
    
    //stuff for special cards
    private JPanel optionPanel=new JPanel();
    private JTextField pirateship=new JTextField();
    private JButton islandButton=new JButton("Island");
    private ArrayList<String> islandCards;
    private JButton durationButton=new JButton("Duration");
    public ArrayList<String> durationCards;
    private JButton nativevillageButton=new JButton("Native Village");
    private ArrayList<String> nativeVillageCards;
    public int coinTokens=0;
    public int debt=0;
    private JTextField vicfield=new JTextField();    
    private JTextField coinfield=new JTextField();
    private JTextField debtfield=new JTextField();

  public PlayerDisplay(DominionPlayer.Data tplayer, ArrayList<String> playerOptions){

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
            if(!lock) output="Btreasures";
          }
         }
      });
      
      setupOptional(playerOptions);
      
      infoPanel.add(optionPanel);   
      panel.add(infoPanel);
      panel.add(handPanel);
      
      display(player,new ArrayList<Boolean>());
    }
    public void setupOptional(ArrayList<String> playerOptions){

      //stuff that isn't always displayed
      islandCards=player.islandCards;
      durationCards=player.durationCards;
      nativeVillageCards=player.nativeVillage;
      coinTokens=player.coinTokens;
      debt=player.debt;
      optionPanel.removeAll();
      optionPanel.setLayout(new GridLayout(playerOptions.size(),1));
      for(String s : playerOptions){
        if(s.equals("pirateship")){
          JPanel piratepanel=new JPanel();
          piratepanel.setLayout(new FlowLayout());
          piratepanel.add(new JLabel("Pirate Money"));
          pirateship.setEditable(false);
          pirateship.setText(player.pirateship+"");
          piratepanel.add(pirateship);
          optionPanel.add(piratepanel);
        }
        if(s.equals("victorytokens")){
          JPanel vicpanel=new JPanel();
          vicpanel.setLayout(new FlowLayout());
          vicpanel.add(new JLabel("Victory Tokens"));
          vicfield.setEditable(false);
          vicfield.setText(player.vicTokens+"");
          vicpanel.add(vicfield);
          optionPanel.add(vicpanel);
        }
        if(s.equals("cointokens")){
          JPanel coinpanel=new JPanel();
          coinpanel.setLayout(new FlowLayout());
          coinpanel.add(new JLabel("Coin Tokens"));
          coinfield.setEditable(false);
          coinfield.setText(player.coinTokens+"");
          coinpanel.add(coinfield);
          optionPanel.add(coinpanel);
        }
        if(s.equals("debt")){
          JPanel debtpanel=new JPanel();
          debtpanel.setLayout(new FlowLayout());
          debtpanel.add(new JLabel("Debt"));
          debtfield.setEditable(false);
          debtfield.setText(player.debt+"");
          debtpanel.add(debtfield);
          optionPanel.add(debtpanel);
        }
        if(s.equals("island")){
          islandButton.addMouseListener(new PlayerMouseAdapter(DominionBoard.this,islandCards));        
          optionPanel.add(islandButton);
        }
        if(s.equals("duration")){
          durationButton.addMouseListener(new PlayerMouseAdapter(DominionBoard.this,durationCards));  
          optionPanel.add(durationButton);                
        }
        if(s.equals("nativevillage")){
          nativevillageButton.addMouseListener(new PlayerMouseAdapter(DominionBoard.this,nativeVillageCards));  
          optionPanel.add(nativevillageButton);                
        }
      }
    }
    //use last times data to display
    public void display(DominionPlayer.Data tplayer, ArrayList<Boolean> mask){
      player=tplayer;
      handPanel.removeAll();
      coinTokens=player.coinTokens;
      debt=player.debt;
      Iterator<DominionCard> it=player.hand.iterator();
      int i=0;

      while(it.hasNext()){
        DominionCard card=it.next();
        JButton button=new JButton();
        //if its your turn and its your player you can see the cards and click on some of them
        if(active && controlled){
          button.setIcon(getImage(card.getImage()));      
          if((phase.equals("actions") && (card.isMoney || card.isAction)) || (phase.equals("buys") && card.isMoney) ){
            button.setEnabled(true);
          }else if ((mask.size()!=player.hand.size() || mask.get(i)) && 
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

      //option stuff
      islandCards.clear();
      islandCards.addAll(player.islandCards);
      durationCards.clear();
      durationCards.addAll(player.durationCards);
      nativeVillageCards.clear();
      nativeVillageCards.addAll(player.nativeVillage);
      pirateship.setText(player.pirateship+"");
      vicfield.setText(player.vicTokens+"");
      coinfield.setText(player.coinTokens+"");
      debtfield.setText(player.debt+"");

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
      if(!lock) output=Integer.toString(cardnum);
      
    }
    
  }
  public class PlayerMouseAdapter extends MouseAdapter{
    ArrayList<String> cards;
    JFrame parent;
    Dimension parentSize;
    Point p;
    JDialog popup;
    public PlayerMouseAdapter(JFrame parent, ArrayList<String> names){
      cards=names;
      this.parent=parent;
      parentSize = parent.getSize(); 
      p = parent.getLocation(); 
    }
    @Override
    public void mousePressed(MouseEvent e){
      if(SwingUtilities.isRightMouseButton(e) || e.isControlDown()){
        popup=new JDialog();
        popup.setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        popup.getContentPane().setLayout(new FlowLayout());
        popup.setDefaultCloseOperation(HIDE_ON_CLOSE);
        for(String name : cards){
          System.out.println("native card "+name);
          popup.getContentPane().add(new JLabel(getImage(name)));
        }
        popup.pack();
        popup.setVisible(true);
      }
    }
    @Override
    public void mouseReleased(MouseEvent e){
      if(SwingUtilities.isRightMouseButton(e)){
        popup.setVisible(false);
        popup.dispose();
      }
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
    private JLabel button=new JLabel();
    private JTextField cost;
    public String name;
    public Deck.SupplyData data;
    private SupplyMouseAdapter adapter;
    private HashMap<String, JPanel> iconPanels=new HashMap<>();
    private JTextField taxField=new JTextField(1);
    private JPanel dataPanel;
    
    public SupplyDisplay(Deck.SupplyData tdata, Collection<String> supplies){
      data=tdata;
      name=data.name;
//      image=getImage(data.image, false, data.landmark || data.event);
      
      if(data.landmark || data.event) panel.setPreferredSize(new Dimension(120,80));
      else panel.setPreferredSize(new Dimension(100,130));
      panel.setLayout(new OverlayLayout(panel));
      panel.setBorder(BorderFactory.createLineBorder(Color.black));     
      
      dataPanel=new JPanel();
      dataPanel.setLayout(new GridLayout(2,2));

      //add optional panels
      //need to add 4 total piles for this to look right, so count how many panels you've added
      //and add empty panels if necessary
      int addedPanels=2;
      if(supplies.contains("embargo")){
        addPanel("embargo");
        addedPanels++;
      }
      if(supplies.contains("obelisk")){
        addPanel("obelisk");
        addedPanels++;
      }
      if(supplies.contains("youngwitch")){
        addPanel("bane");
        addedPanels++;
      }
      if(supplies.contains("contraband") || supplies.contains("grandmarket")) {
        addPanel("contraband");
        addedPanels++;
      }
      if(supplies.contains("tax")) {
        addPanel("tax");
        taxField.setEditable(false);
        taxField.setBackground(Color.RED);
        taxField.setFont(new Font("Arial", Font.BOLD, 20));
        addedPanels++;
      }
      for( ; addedPanels<4; addedPanels++){
        addPanel(Integer.toString(addedPanels));
      }

      dataPanel.setAlignmentX(0.5f);
      dataPanel.setAlignmentY(0.f);
      
      n=new JTextField(2);
      n.setEditable(false);
      n.setBackground(Color.WHITE);
      n.setFont(new Font("Arial", Font.BOLD, 20));
//      n.setText(data.size+"");
      
      if (!data.landmark && !data.event) dataPanel.add(n);
      
      cost=new JTextField(1);
      cost.setBackground(Color.WHITE);
      cost.setEditable(false);
      cost.setFont(new Font("Arial", Font.BOLD, 20));
//      cost.setText(data.cost+"");
      if(!data.landmark) dataPanel.add(cost);


      dataPanel.setOpaque(false);
      panel.add(dataPanel);
//      button=new JLabel(image);
      button.setOpaque(false);
      button.setAlignmentX(0.5f);
      button.setAlignmentY(0.5f);
      panel.add(button);
      
      //display card on right click
      adapter=new SupplyMouseAdapter(DominionBoard.this,data.image,this);
      panel.addMouseListener(adapter);

      display(data);

    }
    private void addPanel(String s){
      iconPanels.put(s, new JPanel());
      iconPanels.get(s).setOpaque(false);
      dataPanel.add(iconPanels.get(s));
    }

    void display(Deck.SupplyData tdata){
      data=tdata;
      image=getImage(data.image, false, data.landmark || data.event);
      button.setIcon(image);
      n.setText(data.size+"");
      cost.setText(data.cost+"");

      //turn off all the icons
      for(Map.Entry<String, JPanel> e : iconPanels.entrySet()){
        e.getValue().removeAll();
        e.getValue().setOpaque(false);
      }
      //turn some of them back on
      for(int i=0; i<data.icons.size(); i++){
        if(data.icons.getKey(i).equals("tax")){
          taxField.setText(data.icons.getValue(i));
          iconPanels.get("tax").add(taxField);
        }else{
          String s=data.icons.getKey(i);
          iconPanels.get(s).add(new JLabel(tokens.get(s)));
        }
        iconPanels.get(data.icons.getKey(i)).setOpaque(true);
      }
      adapter.refreshPopup(data.image);
      repaint();
      revalidate();
    }
  }
  public class SupplyMouseAdapter extends MouseAdapter{
    JDialog popup=new JDialog();
    JLabel image;
    SupplyDisplay supply;
    String oldName;
    SupplyMouseAdapter(JFrame parent, String name, SupplyDisplay tsupply){
      oldName=name;
      supply=tsupply;
      Dimension parentSize = parent.getSize(); 
      Point p = parent.getLocation(); 
      popup.setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
      image=new JLabel(getImage(name,true, supply.data.landmark || supply.data.event));
      popup.add(image);
      popup.pack();
      popup.setDefaultCloseOperation(HIDE_ON_CLOSE);
    }
    //refresh the big image if needed
    void refreshPopup(String name){
      if(!name.equals(oldName)){
        popup.remove(image);
        image=new JLabel(getImage(name, true, supply.data.landmark || supply.data.event));
        popup.add(image);
        popup.pack();
        oldName=name;
      }
    }
    @Override
    public void mousePressed(MouseEvent e){

      //if we can buy the card
      boolean buysCheck=phase.equals("buys") && money>=supply.data.cost && buys>0;
      //can only gain events in the event phase, can never gain landmarks
      boolean eventCheck=(phase.equals("buys") || !supply.data.event) && !supply.data.landmark;
      if(SwingUtilities.isRightMouseButton(e) || e.isControlDown()){
        popup.setVisible(true);
      }else{
        if( (phase.equals("selectDeck2") ||
                (phase.equals("selectDeck") || phase.equals("gain") || buysCheck
                        && supply.data.size>0 && !supply.data.icons.containsKey("contraband"))) && eventCheck){
          if(!lock) output="G"+supply.name;
        }
      }
    }
    @Override
    public void mouseReleased(MouseEvent e){
      if(SwingUtilities.isRightMouseButton(e)){
        popup.setVisible(false);
      }
    }
  }

  //a little function that turns a list of supplydeck.data into a list of strings
  private Collection<String> nameList(ArrayList<Deck.SupplyData> in){
    HashSet<String> out=new HashSet<>(in.size());
    for(Deck.SupplyData deck : in){
      out.add(deck.name);
    }
    return out;
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
