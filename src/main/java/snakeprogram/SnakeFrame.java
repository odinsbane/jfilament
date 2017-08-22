package snakeprogram;


import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Main user interface.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class SnakeFrame{
    /** BUTTONS */
    //JButton PreviousImage, NextImage, AddSnake, DeleteSnake, DeformSnake, ClearScreen, 
    //      ZoomIn, ZoomOut, StretchFix, DeleteEndFix, DeleteMiddleFix, GetForeGround, GetBackground;

    final ArrayList<AbstractButton> ALLBUTTONS;
    final ArrayList<JTextField> ALLTEXTFIELDS;
    
    final ArrayList<JMenu> MENUS;
    
    /** Text Fields */
    JTextField PointSpacing, ImageSmoothing, Alpha, Beta, Gamma, Weight, Stretch, DeformIterations,
               ForegroundIntensity, BackgroundIntensity;
    
    JTextField TRANSIENT;
    JLabel image_counter_label, total_snakes_label;
    JProgressBar deform_progress;
    
    
    JRadioButton curve;
    ButtonGroup energy_group;
    /** interface to the original snake model */
    final SnakeModel snake_model;
    final SnakeListener snake_listener;
    JSlider frameSlider;
    JFrame FRAME; 

    FrameAccessListener field_watcher;
    
    JPanel image_panel;
    
    public SnakeFrame(SnakeModel model){
        ALLBUTTONS = new ArrayList<AbstractButton>();
        ALLTEXTFIELDS = new ArrayList<JTextField>();
        MENUS = new ArrayList<JMenu>();
        snake_model = model;
        snake_listener = new SnakeListener(model,this);
        createLayout();
        createFrameListener();
        

    }
    
    public void disableUI(){
        frameSlider.setEnabled(false);
        field_watcher.disableUI();
        FRAME.getContentPane().requestFocus();
    }
    
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
    private void createLayout(){
        FRAME = new JFrame("JFilaments");
        FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        image_panel = new javax.swing.JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(snake_model.hasImage()){
                    g.drawImage(snake_model.getImage(), 0, 0, snake_model.getDrawWidth(), snake_model.getDrawHeight(), this);
                }
            }
        } ;
        image_panel.setBounds(0,0,672,512);
        
        JPanel host = new JPanel();
        BoxLayout hor = new BoxLayout(host,BoxLayout.LINE_AXIS);
        host.setLayout(hor);
        
        host.setMinimumSize(new Dimension(672,512));
        host.setMaximumSize(new Dimension(672,512));
        host.setPreferredSize(new Dimension(672,512));
		host.add(image_panel);

        FRAME.add(host,BorderLayout.CENTER);
        
        
        JPanel parameter_pane = createParameterPane();
        
        FRAME.add(parameter_pane, BorderLayout.EAST);
        
        
        
        
        JPanel bottom_pane = new JPanel();
        //BoxLayout bottom_layout = new BoxLayout(bottom_pane,BoxLayout.PAGE_AXIS);
        //bottom_pane.setLayout(bottom_layout);
        bottom_pane.setLayout(new BorderLayout(0,10));
        
        bottom_pane.add(createStatusPane(),BorderLayout.NORTH);
        bottom_pane.add(createButtonPane(),BorderLayout.CENTER);
        bottom_pane.add(createProgressBarPanel(),BorderLayout.SOUTH);
		bottom_pane.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        bottom_pane.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
        
		
        FRAME.add(bottom_pane,BorderLayout.SOUTH);
        
        JMenuBar menus = createMenuBar();
        
        FRAME.setJMenuBar(menus);
        
        FRAME.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        FRAME.add(Box.createVerticalStrut(10),BorderLayout.NORTH);
        //JPanel status_pane = createStatusPane();
        
        //FRAME.add(status_pane, BorderLayout.NORTH);
        FRAME.pack();
        
        
        
    }
    
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
    
    private JPanel createProgressBarPanel(){
		JPanel con = new JPanel();
		deform_progress = new JProgressBar(0,100);
        deform_progress.setStringPainted(true);
        con.add(deform_progress);
		return con;
	}
    
    private JMenuBar createMenuBar(){
        JMenuBar bar = new JMenuBar();
        JMenu img = new JMenu("image");
        JMenuItem load_img = new JMenuItem("Open Image");
        load_img.setActionCommand(SnakeActions.getAndLoad.name());
        load_img.addActionListener(snake_listener);
        img.add(load_img);
        
        bar.add(img);
        MENUS.add(img);
        
        JMenu snakes = new JMenu("snakes");
        JMenuItem save_snakes = new JMenuItem("Save Snakes");
        save_snakes.setActionCommand(SnakeActions.saveSnakes.name());
        save_snakes.addActionListener(snake_listener);
        snakes.add(save_snakes);
        
        
        JMenuItem load_snakes = new JMenuItem("Load Snakes");
        load_snakes.setActionCommand(SnakeActions.loadSnakes.name());
        load_snakes.addActionListener(snake_listener);
        snakes.add(load_snakes);

        JMenuItem transform_snake = new JMenuItem("Transform Selected");
        transform_snake.setActionCommand(SnakeActions.moveAndRotate.name());
        transform_snake.addActionListener(snake_listener);
        transform_snake.setAccelerator(KeyStroke.getKeyStroke('m'));
        snakes.add(transform_snake);

        bar.add(snakes);
        MENUS.add(snakes);
        
        JMenu data = new JMenu("data");
        JMenuItem save_data =new JMenuItem("Save Elongation Data");
        save_data.setActionCommand(SnakeActions.saveData.name());
        save_data.addActionListener(snake_listener);
        
        data.add(save_data);

        JMenuItem set_max_length = new JMenuItem("Set Max Length");
        set_max_length.setActionCommand(SnakeActions.setMaxLength.name());
        set_max_length.addActionListener(snake_listener);

        data.add(set_max_length);

        JMenuItem set_line_width = new JMenuItem("Set Line Width");
        set_line_width.setActionCommand(SnakeActions.setLineWidth.name());
        set_line_width.addActionListener(snake_listener);

        data.add(set_line_width);

        MENUS.add(data);
        bar.add(data);

        
        energy_group = new ButtonGroup();
        JMenu energies = new JMenu("energies");
        JRadioButtonMenuItem intensity =  new JRadioButtonMenuItem("Intensity( ridges )");
        JRadioButtonMenuItem gradient =  new JRadioButtonMenuItem("Gradient( edges )");
        JRadioButtonMenuItem balloon = new JRadioButtonMenuItem("Balloon + Gradient");
        energy_group.add(intensity);
        energy_group.add(gradient);
        energy_group.add(balloon);

        energies.add(intensity);
        energies.add(gradient);
        energies.add(balloon);

        intensity.setSelected(true);

        energies.addSeparator();
        JMenuItem external = new JMenuItem("external forces");
        external.setActionCommand(SnakeActions.showAnnotations.name());
        external.addActionListener(snake_listener);

        energies.add(external);
        MENUS.add(energies);
        bar.add(energies);

        JMenu help = new JMenu("help");
        JMenuItem about = new JMenuItem("about");
        about.setActionCommand(SnakeActions.showVersion.name());
        about.addActionListener(snake_listener);

        help.add(about);
        MENUS.add(help);
        bar.add(help);

        return bar;
    }
    
    
    private JPanel createButtonPane(){
        JPanel bp = new JPanel();
        GridLayout gl = new GridLayout(4,4);
        gl.setHgap(5);
        gl.setVgap(5);
        
        bp.setLayout(gl);
        
        bp.add(createActionButton("Previous Image",SnakeActions.previousImage.name()));
        bp.add(createActionButton("Next Image", SnakeActions.nextImage.name()));
        bp.add(createActionButton("New Snake", SnakeActions.addSnake.name()));
        bp.add(createActionButton("Delete Snake", SnakeActions.deleteSnake.name()));
        bp.add(createActionButton("Deform Snake", SnakeActions.deformSnake.name()));
        bp.add(createActionButton("Track Snake", SnakeActions.trackSnake.name()));
        bp.add(createActionButton("Deform Fix", SnakeActions.deformFix.name()));
        bp.add(createActionButton("Delete End Fix", SnakeActions.deleteEnd.name()));
        bp.add(createActionButton("Delete Middle Fix", SnakeActions.deleteMiddle.name()));
        bp.add(createActionButton("Stretch Fix", SnakeActions.stretchFix.name()));
        bp.add(createActionButton("Zoom In", SnakeActions.initializeZoom.name()));
        bp.add(createActionButton("Zoom Out", SnakeActions.zoomOut.name()));
        bp.add(createActionButton("Track All Frames", SnakeActions.trackAllFrames.name(),
                "Track from current frame,+SHIFT track backwards from current frame, +SHIFT+CTRL all frames"));
        bp.add(createActionButton("Track Backwards", SnakeActions.trackBackwards.name()));
        bp.add(createActionButton("Deform All Frames", SnakeActions.deformAllFrames.name(),
                "All frames, +CTRL only frames after current"));
        bp.add(createActionButton("Move End Points", SnakeActions.repositionEnd.name(),
                "Move the location of the beginning and end points for closed contour."));
        return bp;
        
    }

    public JButton createActionButton(String label, String cmd, String tooltip){

        JButton butt = createActionButton(label, cmd);
        butt.setToolTipText(tooltip);
        return butt;
    }

    /**
     *  Creates an actions button that will be enabled/disabled with the
     *
     * @param label
     * @param cmd
     * @return
     */
    public JButton createActionButton(String label, String cmd){
        JButton butt = new JButton(label);
        butt.setActionCommand(cmd);
        butt.setFocusable(false);
        butt.addActionListener(snake_listener);
        ALLBUTTONS.add(butt);
        return butt;

    }
    
    private JPanel createParameterPane(){
		JPanel border_host = new JPanel();
		border_host.setLayout(new BorderLayout());
		
		int vspace = 5;
		
        JPanel parameter_pane = new JPanel();
        BoxLayout vert = new BoxLayout(parameter_pane, BoxLayout.PAGE_AXIS);
        parameter_pane.setLayout(vert);
        
        parameter_pane.add(createInputPair("Alpha", "15", SnakeActions.setAlpha));
        Alpha = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Beta","10", SnakeActions.setBeta));
        Beta = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Gamma", "400", SnakeActions.setGamma));
        Gamma = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Weight","0.5", SnakeActions.setWeight));
        Weight = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Stretch Force","100", SnakeActions.setStretch));
        Stretch = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Deform Iterations","100", SnakeActions.setIterations));
        DeformIterations = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Point Spacing", "1", SnakeActions.setResolution));
        PointSpacing = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createInputPair("Image Smoothing", "1.01", SnakeActions.setSigma));
        ImageSmoothing = TRANSIENT;
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        parameter_pane.add(createSeparator("Intensities"));
        
        JPanel row = createInputPair("Foreground","255", SnakeActions.setForeground);
        row.add(createActionButton("Get",SnakeActions.getForeground.name()));
        ForegroundIntensity = TRANSIENT;
        parameter_pane.add(row);
        parameter_pane.add(Box.createVerticalStrut(vspace));
        
        
        row = createInputPair("Background","0", SnakeActions.setBackground);
        row.add(createActionButton("Get",SnakeActions.getBackground.name()));
        BackgroundIntensity = TRANSIENT;
        parameter_pane.add(row);
        parameter_pane.add(createActionButton("Guess Intensities", SnakeActions.guessForeBackground.name()));
        parameter_pane.add(Box.createVerticalStrut(vspace));


        
		parameter_pane.add(createSeparator("Curve Type"));
        
        ButtonGroup curve_contour = new ButtonGroup();
        
        curve = new JRadioButton("curve");
        curve.setFocusable(false);
        curve_contour.add(curve);
        ALLBUTTONS.add(curve);
        
        curve.setSelected(true);
        
        parameter_pane.add(curve);
        
        JRadioButton contour = new JRadioButton("contour");
        contour.setFocusable(false);
        curve_contour.add(contour);
        ALLBUTTONS.add(contour);
        parameter_pane.add(contour);
        
        frameSlider = new JSlider();
        frameSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                if(frameSlider.getValue()!=snake_model.getCurrentFrame() && frameSlider.isEnabled()){
                    snake_model.setImageFrame(frameSlider.getValue());
                }
            }
        });


        frameSlider.setEnabled(false);

        parameter_pane.add(frameSlider);

        parameter_pane.setMinimumSize(new Dimension(200,512));
        parameter_pane.setMaximumSize(new Dimension(200,512));
        parameter_pane.setPreferredSize(new Dimension(200,512));
        
        parameter_pane.add(Box.createVerticalGlue());
        
        border_host.add(parameter_pane,BorderLayout.CENTER);
        border_host.add(Box.createHorizontalStrut(10),BorderLayout.EAST);
        border_host.add(Box.createHorizontalStrut(10),BorderLayout.WEST);
        
        return border_host;
        
    }
    
    
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
    private JPanel createInputPair(String label, String value, SnakeActions act){
    
        return createInputPair(label,value,act.name());
    
    }
    
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
    
 
    
    void setImageSmoothing() {
        
       String s = ImageSmoothing.getText();

       //attempting to parse the text entered by the user into a double
        try{
            double v = Double.parseDouble(s.trim());
            snake_model.setImageSmoothing(v);
            field_watcher.valueUpdated();
            FRAME.validate();

        }
        catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }       
       
    }

    void setResolution() {
        
        String s = PointSpacing.getText();

        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setResolution(v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    void setDeformIterations() {
        
        String s = DeformIterations.getText();

        try{

            double v = (int)Double.parseDouble(s.trim());
            snake_model.setDeformIterations((int)v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    void setBackgroundIntensity() {
        
        String s = BackgroundIntensity.getText();

        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setBackgroundIntensity(v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }

    public void setStretch(){
        
        String s = Stretch.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setStretch(v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    public void setForegroundIntensity(){
        
        String s = ForegroundIntensity.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setForegroundIntensity(v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    public void setWeight(){
        
        String s = Weight.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setWeight(v);
            field_watcher.valueUpdated();
            FRAME.validate();


        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    public void setGamma(){
        
        String s = Gamma.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setGamma(v);
            field_watcher.valueUpdated();
            FRAME.validate();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    public void setBeta(){
        
        String s = Beta.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setBeta(v);
            field_watcher.valueUpdated();
            FRAME.validate();

        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    public void setAlpha(){
        
        String s = Alpha.getText();
            
        try{

            double v = Double.parseDouble(s.trim());
            snake_model.setAlpha(v);
            field_watcher.valueUpdated();
            FRAME.validate();
        }
        catch (NumberFormatException nfe) {

            System.out.println("NumberFormatException: " + nfe.getMessage());

        }       
    }
    
    
    public void enableUI(){
        frameSlider.setEnabled(true);
        field_watcher.enableUI();
        FRAME.validate();
    }

    public void repaint(){
        FRAME.repaint();
    }
    
    public void setNumberOfSnakesLabel(int x){
        total_snakes_label.setText(x + " Snakes Stored   ");
    }
    
    public void updateStackProgressionLabel(int x, int total){
        image_counter_label.setText( "   Image Counter: " + x + "/" + total );

        frameSlider.setValue(x);

        frameSlider.setMinimum(1);
        frameSlider.setMaximum(total);

    }
    
    public void imageLoaded(boolean t){

        if(t){
            enableUI();
            updateImageTitle();
		}
        
    }
    
    public void updateImageTitle(){
		String title = snake_model.getImageTitle();
		FRAME.setTitle("JFilament: " + title);
	}
    
    public JFrame getFrame(){
        return FRAME;
    }
    
    /**
     *  returns the check value of the intensity energy
     * */
    public int getEnergyType(){
        Enumeration<AbstractButton> buttons = energy_group.getElements();
        int i = 0;
        while(buttons.hasMoreElements()){
            AbstractButton b = buttons.nextElement();
            if(energy_group.isSelected(b.getModel())){
                return i;
            }
            i++;
        }

        return i;
    }

    public void registerButton(AbstractButton b){
        field_watcher.registerButton(b);
    }
    
    public void updateForegroundText(String s){
        ForegroundIntensity.setText(s);
        setForegroundIntensity();
    }
    
    public void updateBackgroundText(String s){
        BackgroundIntensity.setText(s);
        setBackgroundIntensity();
    }
    
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
        resetConstants();
        
    }
    
    public void resetConstants(){
        setAlpha();
        setBeta();
        setGamma();
        setWeight();
        setStretch();
        setForegroundIntensity();
        setBackgroundIntensity();
        setResolution();
        
        
    }
    public int getSnakeType(){
        
        if(curve.isSelected())
                return Snake.OPEN_SNAKE;
        else
            return Snake.CLOSED_SNAKE;
    }
    
    public void updateProgressBar(int v){
        deform_progress.setValue(v);
        deform_progress.repaint();
    }
    
    public void initializeProgressBar(){
            deform_progress.setValue(0);
            deform_progress.repaint();
    }
    
    
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
        return ret_value;
    }
}