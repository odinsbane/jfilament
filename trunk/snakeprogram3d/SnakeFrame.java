package snakeprogram3d;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

/**
 *  Main control panel, is divided into four regions, the Image/3D view region,
 *  a tabbed panel with controls, and a status region on the bottom.
 *
 *  Image View:  This panel 4 viewing modes, the image panel
 *              which displays a 2D slice of the image.  The host_threed, which
 *              contains the ThreeDSnake class, the host_interact which contains
 *              the interactive viewer.  One mode for each of these views and then
 *              a final mode that shows all 3 of them.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 * */
public class SnakeFrame{
    
    ArrayList<AbstractButton> ALLBUTTONS;
    ArrayList<JTextField> ALLTEXTFIELDS;
    
    ArrayList<JMenu> MENUS;
    
    /**  Text Fields for input of parameters. */
    JTextField PointSpacing, ImageSmoothing, Alpha, Beta, Gamma, Weight, Stretch,
            DeformIterations, ForegroundIntensity, BackgroundIntensity,ZResolution;
    
    /** Used for creating text fields. */
    JTextField TRANSIENT;
    
    /** Status panel values */
    JLabel image_counter_label, total_snakes_label;
    
    JProgressBar deform_progress;
    
    
    /**
     * The main program snake model 
     * */
    SnakeModel snake_model;
    
    /** Button listener for collecting button actions */
    SnakeListener snake_listener;
    
    /** The Main window */
    JFrame FRAME; 
    
    /** this is used to monitor when fields are being editted, it also contros the overall ui */
    FrameAccessListener field_watcher;
    
    /** The Image views */
    JPanel image_panel,host_threed,host_interact, host;
    
    /** determines the number of viewports in display mode */
    
    ButtonGroup view_group;
    
    public SnakeFrame(SnakeModel model){
        ALLBUTTONS = new ArrayList<AbstractButton>();
        ALLTEXTFIELDS = new ArrayList<JTextField>();
        MENUS = new ArrayList<JMenu>();
        snake_model = model;
        snake_listener = new SnakeListener(model,this);
        createLayout();
        createFrameListener();
        

    }
    
    /**
     * When an action is running this prevents any input.
     * */
    public void disableUI(){
        FRAME.getContentPane().requestFocus();
        field_watcher.disableUI();
    }
    
    /**
     * 
     * Creates a FrameAccessListener and associates all of the components
     * that are disable-able
     * 
     * */
    private void createFrameListener(){

        field_watcher = new FrameAccessListener();
        
        AbstractButton[] buttons = new AbstractButton[ALLBUTTONS.size()];
        ALLBUTTONS.toArray(buttons);
        field_watcher.setButtons(buttons);
        
        JMenu[] menu_array = new JMenu[MENUS.size()];
        MENUS.toArray(menu_array);
        field_watcher.setMenus(menu_array);
        
        JTextField[] text_refs = new JTextField[ALLTEXTFIELDS.size()];
        ALLTEXTFIELDS.toArray(text_refs);
        
        field_watcher.setFields(text_refs);
        field_watcher.addSelector(snake_model,image_panel);
        
        FRAME.getContentPane().setFocusable(true);
        field_watcher.addPassiveInterrupt(FRAME.getContentPane());
        disableUI();
        field_watcher.enableOpenImage();
        
    }
    
    
    /** 
     * 
     *   Creates all of the regions.  Starts in the 2x2 view with 
     *   three panels.
     *
     * */
    private void createLayout(){
        FRAME = new JFrame("JFilaments");
        FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //2-d draw panel, the size diplayed is determined by set boutnds
        image_panel = new javax.swing.JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(snake_model.hasImage()){
                    g.drawImage(snake_model.getImage(), 0, 0, snake_model.getDrawWidth(), snake_model.getDrawHeight(), this);
                }
            }
        } ;
        image_panel.setBounds(0,0,400,400);
        image_panel.addMouseListener(snake_listener);
        image_panel.addMouseMotionListener(snake_listener);
        
        //Central widget of the FRAME.
        host = new JPanel();
        host.setLayout(new GridLayout(2,2));
        
        host.setMinimumSize(new Dimension(800,800));
        host.setMaximumSize(new Dimension(800,800));
        host.setPreferredSize(new Dimension(800,800));
		host.add(image_panel);
        
        //3d view
        host_threed = new JPanel();
        host_threed.setLayout(new BorderLayout());
        
        host_threed.setMinimumSize(new Dimension(400,400));
        host_threed.setMaximumSize(new Dimension(400,400));
        host_threed.setPreferredSize(new Dimension(400,400));

        host.add(host_threed);
        
        //3d interactive view
        host_interact = new JPanel();
        host_interact.setLayout(new BorderLayout());
        
        host_interact.setMinimumSize(new Dimension(400,400));
        host_interact.setMaximumSize(new Dimension(400,400));
        host_interact.setPreferredSize(new Dimension(400,400));
        
        host.add(host_interact);
        
        
        
        FRAME.add(host,BorderLayout.CENTER);
        
        
        JPanel parameter_pane = createParameterPane();
        JPanel button_pane = createButtonPane();
        
        JTabbedPane controller_panel = new JTabbedPane(JTabbedPane.VERTICAL);
        
        controller_panel.setFocusable(false);
        
        controller_panel.add("parameters",parameter_pane);
        controller_panel.add("actions", button_pane);
        
        FRAME.add(controller_panel, BorderLayout.EAST);
        
        
        
        
        JPanel bottom_pane = new JPanel();
        
        bottom_pane.setLayout(new BorderLayout(0,10));
        
        bottom_pane.add(createStatusPane(),BorderLayout.NORTH);
        bottom_pane.add(createProgressBarPanel(),BorderLayout.SOUTH);
		bottom_pane.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        bottom_pane.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
        
		
        FRAME.add(bottom_pane,BorderLayout.SOUTH);
        
        JMenuBar menus = createMenuBar();
        
        FRAME.setJMenuBar(menus);
        
        FRAME.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        FRAME.add(Box.createVerticalStrut(10),BorderLayout.NORTH);
        
        FRAME.pack();
        
        
        
    }
    /**
     * Bottom part that shows image number and frame number
     *
     * @return jpanel containing abovemention labels.
     */
    private JPanel createStatusPane(){
        JPanel status = new JPanel();
        BoxLayout bl = new BoxLayout(status, BoxLayout.LINE_AXIS);
        image_counter_label = new JLabel("  Current Image");
        
        
        total_snakes_label = new JLabel("  No snakes yet");
        
        status.setLayout(bl);
        status.add(image_counter_label);
        status.add(Box.createHorizontalGlue());
        status.add(total_snakes_label);
        
        return status;
        
    }

    /**
     * Creates a JPanel with a progress bar in it.
     * @return returns the panel.
     */
    private JPanel createProgressBarPanel(){
        JPanel con = new JPanel();
        deform_progress = new JProgressBar(0,100);
        deform_progress.setStringPainted(true);
        con.add(deform_progress);
	return con;
    }

    /**
     * Creates the menu bar with all of the menus, handles setting action
     * listeners and placing menus in a list for enabling / disabling.
     *
     * @return the jmenu bar.
     */
    private JMenuBar createMenuBar(){
        JMenuBar bar = new JMenuBar();
        JMenu img = new JMenu("image");
        JMenuItem load_img = new JMenuItem("Open Image");
        load_img.setActionCommand(SnakeActions.getandload.name());
        load_img.addActionListener(snake_listener);
        img.add(load_img);
        
        bar.add(img);
        MENUS.add(img);
        
        JMenu snakes = new JMenu("snakes");
        JMenuItem save_snakes = new JMenuItem("Save Snakes");
        save_snakes.setActionCommand(SnakeActions.savesnakes.name());
        save_snakes.addActionListener(snake_listener);
        snakes.add(save_snakes);
        
        
        JMenuItem load_snakes = new JMenuItem("Load Snakes");
        load_snakes.setActionCommand(SnakeActions.loadsnakes.name());
        load_snakes.addActionListener(snake_listener);
        snakes.add(load_snakes);
        
        bar.add(snakes);
        MENUS.add(snakes);
        
        JMenu data = new JMenu("data");
        JMenuItem save_data =new JMenuItem("Save Elongation Data");
        save_data.setActionCommand(SnakeActions.savedata.name());
        save_data.addActionListener(snake_listener);
        
        data.add(save_data);
        
        MENUS.add(data);
        bar.add(data);
        
        JMenu views = new JMenu("view");
       
       
        view_group = new ButtonGroup();
        views.add(addViewItem("3 ports")).setSelected(true);
        views.add(addViewItem("2D image"));
        views.add(addViewItem("3D sections"));
        views.add(addViewItem("3D volume"));

        views.add(new JSeparator());
        
        JMenuItem reduce = new JCheckBoxMenuItem("reduced 3D texture");
        reduce.setActionCommand(SnakeActions.reduce3d.name());
        reduce.addActionListener(snake_listener);
        views.add(reduce);
        
        //JMenuItem start_vtk =new JMenuItem("Launch External VTK viewer ...");
        //start_vtk.setActionCommand(SnakeActions.startvtk.name());
        //start_vtk.addActionListener(snake_listener);

        //views.add(start_vtk);

        MENUS.add(views);
        bar.add(views);

        
        JMenu help = new JMenu("help");
        JMenuItem show_help = new JMenuItem("overview");
        show_help.setActionCommand(SnakeActions.showhelp.name());
        show_help.addActionListener(snake_listener);
        help.add(show_help);

        help.add(new JSeparator());

        JMenuItem about = new JMenuItem("about");
        about.setActionCommand(SnakeActions.showabout.name());
        about.addActionListener(snake_listener);
        help.add(about);
        
        MENUS.add(help);
        bar.add(help);
        
        return bar;
    }
    
    /**
     * Creates view items which determine which view the user is seeing.
     *
     * @param label label of the value.
     * @return returns the menu item
     */
    private JRadioButtonMenuItem addViewItem(String label){
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
        item.setActionCommand("viewselected");
        item.addActionListener(snake_listener);
        view_group.add(item);
        return item;
    }
    
   
   
    
    /** 
     * creates all of the buttons
     *@return a panel containing all of the buttons.
     */
    private JPanel createButtonPane(){
        JPanel bp = new JPanel();
        JPanel half = new JPanel();
        BoxLayout bl = new BoxLayout(half, BoxLayout.PAGE_AXIS);
        half.setLayout(bl);

        GridLayout gl = new GridLayout(12,1);
        gl.setHgap(5);
        gl.setVgap(5);

        bp.setPreferredSize(new Dimension(300, 400));
        bp.setMinimumSize(new Dimension(300, 400));
        bp.setMaximumSize(new Dimension(300, 400));

        bp.setLayout(gl);

        bp.add(createActionButton("Previous Time Frame", SnakeActions.previousframe.name()));
        bp.add(createActionButton("Next Time Frame", SnakeActions.nextframe.name()));

        bp.add(createActionButton("Lower Image Slice",SnakeActions.previousimage.name()));
        bp.add(createActionButton("Raise Image Slice", SnakeActions.nextimage.name()));


        bp.add(createActionButton("Add Snake", SnakeActions.addsnake.name()));
        bp.add(createActionButton("Delete Snake", SnakeActions.deletesnake.name()));
        bp.add(createActionButton("Deform Snake", SnakeActions.deformsnake.name()));
        bp.add(createActionButton("Clear Current",SnakeActions.clearsnake.name()));
        bp.add(createActionButton("Track Snake", SnakeActions.tracksnake.name()));
        bp.add(createActionButton("Track Backwards",SnakeActions.tracksnakeback.name()));
        bp.add(createActionButton("Clear Screen", SnakeActions.clearscreen.name()));
        bp.add(createActionButton("Delete End Fix", SnakeActions.deleteend.name()));
        bp.add(createActionButton("Delete Middle Fix", SnakeActions.deletemiddle.name()));
        bp.add(createActionButton("Stretch Fix", SnakeActions.stretchfix.name()));
        bp.add(createActionButton("Zoom In", SnakeActions.initializezoom.name()));
        bp.add(createActionButton("Zoom Out", SnakeActions.zoomout.name()));
        
        bp.add(createActionButton("+MAX", SnakeActions.pmax.name()));
        bp.add(createActionButton("-MAX", SnakeActions.mmax.name()));
        bp.add(createActionButton("+MIN", SnakeActions.pmin.name()));
        bp.add(createActionButton("-MIN",SnakeActions.mmin.name()));

        half.add(bp);
        half.add(Box.createVerticalGlue());
        return half;
        
    }
    
    
    /**
     * 
     *  creates a button and adds it to the list of buttons
     *  @param label that shows up on the button
     *  @param cmd the String name of a SnakeActions enum constant
     *  @return new jbutton that has an action command and is in the list of buttons.
     **/
    public JButton createActionButton(String label, String cmd){
        JButton butt = new JButton(label);


        butt.setPreferredSize(new Dimension(150, 20));
        butt.setMaximumSize(new Dimension(150, 20));
        butt.setActionCommand(cmd);
        butt.addActionListener(snake_listener);
        ALLBUTTONS.add(butt);
        return butt;

    }
    
    /**
     * 
     * Creates the panel with all of the parameters.  Each parameter is
     * created separatly which is a residual of the previous code.
     *
     * @return the created panel.
     * */
    private JPanel createParameterPane(){
		JPanel border_host = new JPanel();
		border_host.setLayout(new BorderLayout());
		
		int vspace = 5;
		
        JPanel parameter_pane = new JPanel();
        BoxLayout vert = new BoxLayout(parameter_pane, BoxLayout.PAGE_AXIS);
        parameter_pane.setLayout(vert);
        
        parameter_pane.add(createInputPair("Alpha", "15", SnakeActions.setalpha));
        Alpha = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Beta","10", SnakeActions.setbeta));
        Beta = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Gamma", "40", SnakeActions.setgamma));
        Gamma = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Weight","0.5", SnakeActions.setweight));
        Weight = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Stretch Force","100", SnakeActions.setstretch));
        Stretch = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Deform Iterations","100", SnakeActions.setiterations));
        DeformIterations = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Snake Point Separation", "1", SnakeActions.setresolution));
        PointSpacing = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Image Smoothing", "0", SnakeActions.setsigma));
        ImageSmoothing = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Z Spacing","2",SnakeActions.setzresolution));
        ZResolution = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createSeparator("Intensities"));
        
        JPanel row = createInputPair("Foreground","255", SnakeActions.setforeground);
        row.add(createActionButton("Get",SnakeActions.getforeground.name()));
        ForegroundIntensity = TRANSIENT;
        ForegroundIntensity.setMinimumSize(new Dimension(100,25));
        ForegroundIntensity.setMaximumSize(new Dimension(100,25));
        ForegroundIntensity.setPreferredSize(new Dimension(100,25));
        parameter_pane.add(row);
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        
        row = createInputPair("Background","0", SnakeActions.setbackground);
        row.add(createActionButton("Get",SnakeActions.getbackground.name()));
        BackgroundIntensity = TRANSIENT;
        BackgroundIntensity.setMinimumSize(new Dimension(100,25));
        BackgroundIntensity.setMaximumSize(new Dimension(100,25));
        BackgroundIntensity.setPreferredSize(new Dimension(100,25));
        
        parameter_pane.add(row);
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        
        parameter_pane.setMinimumSize(new Dimension(200,512));
        parameter_pane.setMaximumSize(new Dimension(200,512));
        parameter_pane.setPreferredSize(new Dimension(200,512));
        
        parameter_pane.add(Box.createVerticalGlue());
        
        border_host.add(parameter_pane,BorderLayout.CENTER);
        border_host.add(Box.createHorizontalStrut(10),BorderLayout.EAST);
        border_host.add(Box.createHorizontalStrut(10),BorderLayout.WEST);
        
        return border_host;
        
    }
    
    /**
     * Creates a panel that contains a separator with a lablel.  This takes
     * care of some style.
     *
     * @param label the label displayed
     * @return JPanel with separator and label.
     */
    private JPanel createSeparator(String label){
		JPanel row = new JPanel();
        BoxLayout hor = new BoxLayout(row, BoxLayout.LINE_AXIS);
        row.setLayout(hor);
        
        JSeparator s = new JSeparator();
		s.setMinimumSize(new Dimension(200,2));
        s.setMaximumSize(new Dimension(200,2));
        s.setPreferredSize(new Dimension(200,2));
        
        row.setMinimumSize(new Dimension(200,35));
        row.setMaximumSize(new Dimension(200,35));
        row.setPreferredSize(new Dimension(200,35));
        
        JLabel sep_label = new JLabel(label);
        sep_label.setForeground(Color.BLUE);
        
        //sep_label.setVerticalAlignment(JLabel.TOP);
        //sep_label.setVerticalTextPosition(JLabel.BOTTOM);
        
        row.add(sep_label);
        row.add(s);
        
        return row;
	}
    
    /**
     * Overload for create input pair using a SnakeAction instead of a string.
     * Creates the input pair and adds a listener.
     *
     * @param label text that is displayed next to input field.
     * @param value initial value
     * @param act action command.
     * @return a jpanel with a label, and JTextField.
     */
    private JPanel createInputPair(String label, String value, SnakeActions act){
    
        return createInputPair(label,value,act.name());
    
    }
    
    /**
     * Overload for create input pair using a SnakeAction instead of a string.
     * Creates the input pair and adds a listener.
     *
     * @param label text that is displayed next to input field.
     * @param value initial value
     * @param cmd name of action command.
     * @return a jpanel with a label, and JTextField.
     */
    private JPanel createInputPair(String label, String value, String cmd){
        JPanel row = new JPanel();
        BoxLayout hor = new BoxLayout(row, BoxLayout.LINE_AXIS);
        row.setLayout(hor);
        JLabel l = new JLabel(label);
        JTextField f = new JTextField(value);
        f.setMinimumSize(new Dimension(50,25));
        f.setMaximumSize(new Dimension(50,25));
        f.setPreferredSize(new Dimension(50,25));
        f.setHorizontalAlignment(JTextField.RIGHT);
        
        f.setActionCommand(cmd);
        f.addActionListener(snake_listener);
        
        
        ALLTEXTFIELDS.add(f);
        
        
        row.add(l);
        row.add(Box.createHorizontalGlue());
        row.add(f);
        TRANSIENT = f;
        
        return row;
        
    }
    
     /**
     * Trys to set the 'z resolution' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled
     * Called when parameter has been changed.
     */
    void setZResolution(){
        String s = ZResolution.getText();
            try{
                double v = Double.parseDouble(s.trim());
                field_watcher.valueUpdated();
                snake_model.setZResolution(v);
            }  catch (NumberFormatException nfe) {

                System.out.println("NumberFormatException: " + nfe.getMessage());
            
            }
    }
	
     /**
     * Trys to set the 'images smoothing' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled
     * Called when parameter has been changed.
     */
    void setImageSmoothing() {
        
       String s = ImageSmoothing.getText();

       //attempting to parse the text entered by the user into a double
        try{
            double v = Double.parseDouble(s.trim());
            snake_model.setImageSmoothing(v);
            field_watcher.valueUpdated();
        }
        catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }       
        
    }
    
    /**
     * Trys to set the 'point spacing' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    void setResolution() {
        
        String s = PointSpacing.getText();

        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setResolution(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    /**
     * Trys to set the 'deform iterations' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    void setDeformIterations() {
        
        String s = DeformIterations.getText();

        try{

            double v = (int)Double.parseDouble(s.trim());
            snake_model.setDeformIterations((int)v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    /**
     * Trys to set the 'background' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    void setBackgroundIntensity() {
        
        String s = BackgroundIntensity.getText();

        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setBackgroundIntensity(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    /**
     * Trys to set the 'stretch' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    public void setStretch(){
        
        String s = Stretch.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setStretch(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    /**
     * Trys to set the 'foreground' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    public void setForegroundIntensity(){
        
        String s = ForegroundIntensity.getText();

        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setForegroundIntensity(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    /**
     * Trys to set the 'weight' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    public void setWeight(){
        
        String s = Weight.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setWeight(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    /**
     * Trys to set the 'gamma' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    public void setGamma(){
        
        String s = Gamma.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setGamma(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    /**
     * Trys to set the 'beta' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled.
     * Called when parameter has been changed.
     */
    public void setBeta(){
        
        String s = Beta.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setBeta(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    /**
     * Trys to set the 'alpha' value.  First validates input, if it can be a double
     * then the value is set and the ui is enabled
     * Called when parameter has been changed.
     */
    public void setAlpha(){
        
        String s = Alpha.getText();

        try{

            double v = Double.parseDouble(s.trim());

            snake_model.setAlpha(v);
            field_watcher.valueUpdated();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    
    /**
     * enables ui.
     */
    public void enableUI(){
        field_watcher.enableUI();
    }

    /**
     * repaints the JFrame.
     */
    public void repaint(){
        
        FRAME.repaint();
        
    }

    /**
     * repaints the image panel
     */
    public void lightRepaint(){
        image_panel.repaint();
    }

    /**
     * Updates the number of snakes label
     * @param x total number of snakes.
     */
    public void setNumberOfSnakesLabel(int x){
        total_snakes_label.setText(x + " Snakes Stored   ");
    }

    /**
     *
     * Updates the display of the current slice and frame.
     *
     * @param f current frame.
     * @param frames total number of frames.
     * @param s current slice.
     * @param slices total number of slices.
     *
     */
    public void updateStackProgressionLabel(int f, int frames, int s, int slices){
        image_counter_label.setText( "   Time Frame: " + f + "/" + frames + "  Slice: " + s + "/" + slices );
    }


    /**
     * causes values to be updated if the image was loaded correctly.
     *
     * @param t response for the SnakeImages whether it loaded successfully or not.
     */
    public void imageLoaded(boolean t){

        if(t){
            enableUI();
            updateImageTitle();
        }
        
    }

    /**
     * updates the title to show the name of the image.
     */
    public void updateImageTitle(){
		String title = snake_model.getImageTitle();
		FRAME.setTitle("JFilament: " + title);
	}
    
    /*
     * returns the frame used for showing dialogs.
     */
    public JFrame getFrame(){
        return FRAME;
    }

    /**
     * Sets the foreground text after it has been automatically determined.
     * @param s the value
     */
    public void updateForegroundText(String s){
        final String t = s;
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                ForegroundIntensity.setText(t);
                setForegroundIntensity();
            }
        });

    }

    /**
     * Sets the background text after it has been automatically determined.
     * @param s the value
     */
    public void updateBackgroundText(String s){
        final String t = s;
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                BackgroundIntensity.setText(t);
                setBackgroundIntensity();
            }
        });

    }

    /**
     * loads the default values for a HashMap
     *
     * @param values string value
     */
    public void setConstants(HashMap<String,Double> values){
        Alpha.setText(values.get("alpha").toString());
        Beta.setText(values.get("beta").toString());
        BackgroundIntensity.setText(values.get("background").toString());
        Weight.setText(values.get("weight").toString());
        Stretch.setText(values.get("stretch").toString());
        PointSpacing.setText(values.get("spacing").toString());
        ImageSmoothing.setText(values.get("smoothing").toString());
        Gamma.setText(values.get("gamma").toString());
        ForegroundIntensity.setText(values.get("foreground").toString());
        ZResolution.setText(values.get("zresolution").toString());
        resetConstants();
        
    }

    /**
     * Refreshes the constants according to the values in the text fields, which also
     * refreshes the model.
     */
    public void resetConstants(){
        setAlpha();
        setBeta();
        setGamma();
        setWeight();
        setStretch();
        setForegroundIntensity();
        setBackgroundIntensity();
        setResolution();
        setDeformIterations();
        //does a lot of work be careful with this one.
        setZResolution();
    }
    
    /**
     * Sets the value of the progress bar.
     * @param v a number from 0 to 100
     */
    public void updateProgressBar(int v){
        final int u = v;
        EventQueue.invokeLater(new Runnable(){
            public void run(){

                deform_progress.setValue(u);
                deform_progress.repaint();

            }
        });
    }

    /**
     * sets the value to zero.
     */
    public void initializeProgressBar(){
        EventQueue.invokeLater(new Runnable(){
        public void run(){

            deform_progress.setValue(0);
            deform_progress.repaint();

            }
        });
    }
    
    /**
     * Returns the current constants that are set in the parameters pane.
     *
     * @return a HashMap containing String Double mappings. 
     */
    public HashMap<String,Double> getConstants(){
        HashMap<String,Double> ret_value = new HashMap<String,Double>();
        ret_value.put("alpha",Double.parseDouble(Alpha.getText()));
        ret_value.put("beta",Double.parseDouble(Beta.getText()));
        ret_value.put("background",Double.parseDouble(BackgroundIntensity.getText()));
        ret_value.put("weight",Double.parseDouble(Weight.getText()));
        ret_value.put("stretch",Double.parseDouble(Stretch.getText()));
        ret_value.put("spacing",Double.parseDouble(PointSpacing.getText()));
        ret_value.put("smoothing",Double.parseDouble(ImageSmoothing.getText()));
        ret_value.put("gamma",Double.parseDouble(Gamma.getText()));
        ret_value.put("foreground",Double.parseDouble(ForegroundIntensity.getText()));
        ret_value.put("zresolution",Double.parseDouble(ZResolution.getText()));
        return ret_value;
    }

    /**
     * Allows moving the image planes.
     */
    public void enableImageDirections(){
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                field_watcher.enableImageDirections();

            }
        });
    }

    /**
     * The threed view is created afterwards, this set one of the variable
     * panels to be the threed view.
     * @param threedview component that will be used.
     */
    public void set3DPanel(Component threedview){
        final Component c = threedview;
        EventQueue.invokeLater(new Runnable(){
                                public void run(){
                                    host_threed.add(c);
                                }
                            });

        threedview.addKeyListener(field_watcher.getKeyListener());
        
    }

    /**
     * The interactive view is created afterwards, this set one of the variable
     * panels to be the interactive view.
     * @param threedview component that will be used.
     */
    public void setInteractPanel(Component threedview){
        final Component c = threedview;
        EventQueue.invokeLater(new Runnable(){
                                public void run(){
                                    host_interact.add(c);
                                }
                            });
        
        threedview.addKeyListener(field_watcher.getKeyListener());
        
    }

    /**
     * sets the 2x2 layout with all three views.
     */
    public void setGridLayout(){
        host.setLayout(new GridLayout(2,2));
        host.add(image_panel);
        host.add(host_threed);
        host.add(host_interact);
        
        image_panel.setBounds(0,0,400,400);
        snake_model.setMaxDrawingBounds(400,400);
        
    }

    /**
     * places a single component in prominace.
     * @param a the component displayed.
     */
    public void setSingleLayout(Component a){
        host.remove(image_panel);
        host.remove(host_interact);
        host.remove(host_threed);
        host.setLayout(new BorderLayout());
        host.add(a);
        
        image_panel.setBounds(0,0,800,800);
        snake_model.setMaxDrawingBounds(800,800);
        
    }
    
    /**
     *  Changes the display type by incrementing to the next one
     * */
    public void switchCards(){
        int i = getModelIndex() + 1;
        i = i>3?0:i;
        
        int j = 0;
        
        Enumeration<AbstractButton> e = view_group.getElements();
        while(e.hasMoreElements()){
            AbstractButton b = e.nextElement();
            if(j==i){
                b.doClick();
                break;
            }
            j++;
        }
        
    }

    /** decides the componenet to be displayed then revalidates the main Frame*/
    public void setView(){
        switch(getModelIndex()){
            case 0:
                setGridLayout();
                break;
            case 1:
                setSingleLayout(image_panel);
                break;
            case 2:
                setSingleLayout(host_interact);
                break;
            case 3:
                setSingleLayout(host_threed);
                break;
        }
        host.invalidate();
        FRAME.validate();
        FRAME.getContentPane().requestFocus();
        snake_model.updateDisplay();
    }

    /**
     * The index of the selected view in the view selection menu.
     *
     * @return index value as an integer.
     */
    public int getModelIndex(){
        int i = 0;
        Enumeration<AbstractButton> e = view_group.getElements();
        
        while(e.hasMoreElements()){
            AbstractButton b = e.nextElement();
            if(b.isSelected())
                return i;
            i++;
        }
        return 0;
   }
    
}