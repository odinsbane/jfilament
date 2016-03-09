package snakeprogram;

import ij.process.ImageProcessor;
import snakeprogram.interactions.SnakeInteraction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * For adding user defined points that influence the snake by applying a force.
 *
 * Created by msmith on 2/12/14.
 */
public class AnnotationEnergies implements ImageCounterListener{
    private JFrame frame;
    private SnakeModel model;
    private SnakeImages images;

    HashSet<AnnotationForce> forces = new HashSet<AnnotationForce>();
    SnakeInteraction si;

    JButton add_annotation;
    JButton remove_annotation;
    JButton select_annotation;

    JButton edit_fields;
    JButton refresh_fields;

    ArrayList<JTextField> fields = new ArrayList<JTextField>();
    JTextField first_field, last_field, radius_field, force_field;
    AnnotationForce selected;
    public AnnotationEnergies(SnakeModel snakeModel, SnakeImages images) {
        frame = new JFrame("JFilament: Annotation Energies");
        model = snakeModel;
        this.images = images;
        JPanel content = new JPanel();
        BoxLayout content_layout = new BoxLayout(content, BoxLayout.LINE_AXIS );
        content.setLayout(content_layout);

        JPanel buttons = new JPanel();
        BoxLayout button_layout = new BoxLayout(buttons, BoxLayout.PAGE_AXIS);
        buttons.setLayout(button_layout);

        content.add(buttons);

        JPanel display = new JPanel();
        BoxLayout display_layout = new BoxLayout(display, BoxLayout.PAGE_AXIS);
        display.setLayout(display_layout);

        content.add(display);

        JLabel first_label = new JLabel("first: ");
        first_field = new JTextField();
        display.add(first_label);
        display.add(first_field);
        fields.add(first_field);
        JLabel last_label = new JLabel("second: ");
        last_field = new JTextField();
        fields.add(last_field);
        display.add(last_label);
        display.add(last_field);
        JLabel radius_label = new JLabel("radius: ");
        radius_field = new JTextField();
        fields.add(radius_field);
        display.add(radius_label);
        display.add(radius_field);
        JLabel force_label = new JLabel("force: ");
        force_field = new JTextField();
        display.add(force_label);
        display.add(force_field);
        fields.add(force_field);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        edit_fields = new JButton("edit");
        edit_fields.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editFields();
            }
        });
        row.add(edit_fields);
        refresh_fields = new JButton("set");
        refresh_fields.setEnabled(false);

        refresh_fields.addActionListener(
                new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshFields();
                    }
                }
        );
        row.add(refresh_fields);

        display.add(row);
        frame.setContentPane(content);

        add_annotation = new JButton("add new");
        final AnnotationEnergies ref = this;
        add_annotation.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(si==null){
                    si = new AnnotationAdder(ref);
                    model.registerSnakeInteractor(si);
                }
            }
        });

        buttons.add(add_annotation);

        remove_annotation = new JButton("remove");
        remove_annotation.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                removeSelected();
            }
        });
        buttons.add(remove_annotation);

        select_annotation = new JButton("select");
        buttons.add(select_annotation);
        select_annotation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(si==null){
                    si = new AnnotationModifier(ref);
                    model.registerSnakeInteractor(si);
                }
            }
        });


        for(JTextField field: fields){
            field.setEnabled(false);
        }

        frame.pack();


    }

    void unregister(){
        if(si!=null){
            model.unRegisterSnakeInteractor(si);
            si = null;
        }
    }

    public void setVisible(boolean v){
        frame.setVisible(v);
    }

    public void registerButtons(SnakeFrame snake_panel) {
        snake_panel.registerButton(add_annotation);
        snake_panel.registerButton(remove_annotation);
        snake_panel.registerButton(select_annotation);
    }

    public void createForceCenter(int x, int y) {
        double rx = images.fromZoomX(x);
        double ry = images.fromZoomY(y);
        AnnotationForce f = new AnnotationForce(new double[]{rx, ry}, images.getCounter());
        images.addDrawable(f);
        forces.add(f);
        model.updateImagePanel();
    }

    public void selectForce(AnnotationForce force){
        if(selected!=null){
            selected.c = Color.GREEN;
        }
        selected = force;
        first_field.setText(String.format("%d",force.first));
        last_field.setText(String.format("%d", force.last));
        radius_field.setText(String.format("%4.2f", force.radius));
        force_field.setText(String.format("%4.2f", force.force));
        force.c = Color.WHITE;
        model.updateImagePanel();
    }

    public void findForce(int x, int y){
        double ix = images.fromZoomX(x);
        double iy = images.fromZoomY(y);

        for(AnnotationForce force: forces){
            if(force.position!=null){
                double dx = force.position[0] - ix;
                double dy = force.position[1] - iy;
                if(dx*dx + dy*dy < force.radius*force.radius){
                    //found
                    selectForce(force);
                    return;
                }
            }
        }
    }

    @Override
    public void setFrame(int i) {
        for(AnnotationForce force: forces){
            force.setFrame(i);
        }
    }

    public Set<ExternalEnergy> getActiveForces(){

        HashSet<ExternalEnergy> active = new HashSet<ExternalEnergy>();
        for(AnnotationForce f: forces){

            if(f.position!=null){
                active.add(f);
            }

        }

        return active;
    }

    public void editFields(){
        for(JTextField c: fields){
            c.setEnabled(true);
        }
        edit_fields.setEnabled(false);
        refresh_fields.setEnabled(true);


    }

    public void refreshFields(){
        for(JTextField c: fields){
            c.setEnabled(false);
        }
        edit_fields.setEnabled(true);
        refresh_fields.setEnabled(false);

        if(selected==null) return;

        selected.first = Integer.parseInt(first_field.getText());
        selected.last = Integer.parseInt(last_field.getText());
        if(selected.last<=selected.first){
            selected.last=selected.first;
        }
        selected.radius = Double.parseDouble(radius_field.getText());
        selected.force = Double.parseDouble(force_field.getText());
        selected.updateFrames();
        model.updateImagePanel();

    }

    public AnnotationForce getForceAt(int x, int y) {
        double ix = images.fromZoomX(x);
        double iy = images.fromZoomY(y);

        for(AnnotationForce force: forces){
            if(force.position!=null){
                double dx = force.position[0] - ix;
                double dy = force.position[1] - iy;
                if(dx*dx + dy*dy < force.radius*force.radius){
                    //found
                    return force;
                }
            }
        }
        return null;
    }

    public void displaceForce(int dx, int dy) {
        if(selected!=null){
            double[] image_xy = images.toZoom(selected.position);

            image_xy[0] = images.fromZoomX(image_xy[0] + dx);
            image_xy[1] = images.fromZoomY(image_xy[1] + dy);

            selected.setPosition(image_xy);
            model.updateImagePanel();
        }
    }

    public void nextFrame(){
        model.nextImage();
    }

    public void previousFrame(){
        model.previousImage();
    }

    public void removeSelected(){
        if(selected!=null){
            forces.remove(selected);
            images.removeDrawable(selected);
            selected = null;
            model.updateImagePanel();
        }
    }
}

class DummyInteractor implements SnakeInteraction{
    final AnnotationEnergies parent;
    public DummyInteractor(AnnotationEnergies parent){
        this.parent = parent;
    }
    @Override
    public void cancelActions() {
        parent.unregister();
    }

    @Override
    public void mouseClicked(MouseEvent e) {    }

    @Override
    public void mousePressed(MouseEvent e) {    }

    @Override
    public void mouseReleased(MouseEvent e) {    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}
}

class AnnotationModifier extends DummyInteractor{
    int[] last_grab = new int[2];
    boolean grabbing = false;
    public AnnotationModifier(AnnotationEnergies parent){
        super(parent);
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.isShiftDown()){
            parent.nextFrame();
        } else if(e.isControlDown()){
            parent.previousFrame();
        } else {
            parent.findForce(e.getX(), e.getY());
        }

    }
    @Override
    public void mousePressed(MouseEvent e){
        if(parent.selected!=null&&parent.selected==parent.getForceAt(e.getX(), e.getY())){
            grabbing=true;
            last_grab[0] = e.getX();
            last_grab[1] = e.getY();
        };
    }

    @Override
    public void mouseReleased(MouseEvent evt){
        grabbing=false;
    }
    @Override
    public void mouseDragged(MouseEvent e){
        if(grabbing){

            parent.displaceForce(e.getX()-last_grab[0], e.getY() - last_grab[1]);
            last_grab[0] = e.getX();
            last_grab[1] = e.getY();
        }
    }

}

class AnnotationAdder extends DummyInteractor{

    public AnnotationAdder(AnnotationEnergies energies){
        super(energies);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON1){
            parent.createForceCenter(e.getX(), e.getY());
        } else if(e.getButton()==MouseEvent.BUTTON3){
            cancelActions();
        }
    }

}

class AnnotationForce implements ProcDrawable, ExternalEnergy{
    Map<Integer, double[]> positions = new TreeMap<Integer, double[]>();
    double[] position;
    double radius;
    int current;
    int first, last;
    double force;
    Color c = Color.GREEN;
    double[] xy_force = new double[2];
    final static double[] no_force = new double[2];
    public AnnotationForce(double[] p, int frame){
        current = frame;
        position = p;
        radius = 10;
        force = 10;
        positions.put(frame, p);
        first = frame;
        last = frame;
        no_force[0] = 0;
        no_force[1] = 0;
    }

    public void setFrame(int i){
        current = i;
        if(positions.keySet().contains(i)){
            position = positions.get(i);
        } else{
            position = null;
        }
    }

    @Override
    public void draw(ImageProcessor proc, Transform transform) {
        if(position==null) return;

        proc.setColor(c);
        double[] a = transform.transform(new double[]{position[0] - radius, position[1] - radius});
        double[] b = transform.transform(new double[]{position[0] + radius, position[1] + radius});
        proc.drawOval((int)a[0], (int)a[1], (int)(b[0]-a[0]), (int)(b[1]-a[1]));
    }

    public double[] getForce(double x, double y){
        if(position==null){
            return no_force;
        }


        double dx = x - position[0];
        double dy = y - position[1];
        double mag = dx*dx + dy*dy;
        if(mag>0&&mag<radius*radius){
            mag = Math.sqrt(mag);

            xy_force[0] = dx/mag*force;
            xy_force[1] = dy/mag*force;
            return xy_force;
        }

        return no_force;

    }

    public void updateFrames(){
        //at least will be either the first valid element, or the first non-valid element.
        //if there are no elements in the range.

        double[] atleast = null;
        boolean valid_found = false;
        Iterator<Integer> iter = positions.keySet().iterator();

        while(iter.hasNext()){
            Integer i = iter.next();
            if(i<first || i>last){
                if(atleast==null){
                    //just in case.
                    atleast = positions.get(i);

                }

                iter.remove();

            } else if(!valid_found){
                atleast = positions.get(i);
                valid_found=true;
            }
        }

        //this guy is broken!
        if(atleast==null) return;

        for(int i = first; i<=last; i++){
            if(positions.keySet().contains(i)){
                //skip it.
                atleast = positions.get(i);
            } else{
                //put the last valid.
                positions.put(i, new double[]{atleast[0], atleast[1]});
            }
        }


    }
    /**
     * Checks if this force has any valid points, such that it can be selected modified
     * or removed.
     *
     */
    public boolean isValid(){

        return !positions.isEmpty();

    }


    public void setPosition(double[] image_xy) {
        double[] pt = positions.get(current);
        pt[0] = image_xy[0];
        pt[1] = image_xy[1];
    }
}

interface ExternalEnergy{
    public double[] getForce(double x, double y);
}