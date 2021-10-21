/*
 * Displays the help message and aboutMessage.
 */
package snakeprogram;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/**
 *    This class manages turning buttons and text fields on/off when a text field
 *    is being modified or has finished being modified.
 *
 *  Displays brief help files.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */

public class HelpMessages implements HyperlinkListener {

    
    /**
     * Creats a JFrame containing a JEditor pane displaying 'parameters.html'
     * which contains a list of the parameters used and a list of the button
     * functions
     *
     */
    static public void showHelp(){
        HelpMessages hm = new HelpMessages();

        String about_url = hm.getClass().getResource("parameters.html").toString();


        try{
            JEditorPane helper = new JEditorPane(about_url);
            helper.setEditable(false);
            final JFrame shower = new JFrame("JFilament3D Commands");

            /** setup image display */
            JScrollPane display = new JScrollPane(
                                                            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED ,
                                                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                                        );

            JViewport jvp = new JViewport();
            jvp.setView(helper);

            display.setViewport(jvp);

            shower.add(display);
            shower.setSize(400,400);

            shower.setVisible(true);
        } catch(Exception e){
            //ruined
        }
        
        
    }

    /**
     * Shows 'about.html' which has some credits and
     */
    static public void showAbout(){
        
        
        InputStream about_stream = HelpMessages.class.getResourceAsStream("about.html");

        boolean checking=true;
        try{
            
            BufferedReader br = new BufferedReader(new InputStreamReader(about_stream));
            StringBuilder s = new StringBuilder();
            String line = br.readLine();
            while(line!=null){
                if(checking&&line.contains("SnakeApplication.VERSION")){
                    line = line.replace("SnakeApplication.VERSION", SnakeApplication.VERSION);
                    checking=false;
                }
                s.append(line);
                //s.append("\n");
                line = br.readLine();
            }
            br.close();
            final JFrame shower = new JFrame("JFilament About");
            JEditorPane helper = new JEditorPane("text/html",s.toString());
            shower.setSize(400,400);
            helper.setEditable(false);

            helper.addHyperlinkListener(new HelpMessages());
            shower.add(helper);

            shower.setVisible(true);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Opens a browser if the one hyper link in the 'about.html' is clicked
     *
     * @param e
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
             if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                 System.out.println(e.getURL());

                if (Desktop.isDesktopSupported()) {
                      Desktop desktop = Desktop.getDesktop();
                      try{
                          desktop.browse(new URI(e.getURL().toExternalForm()));
                      } catch(Exception ex){
                          //this is my own mistake
                      }
                }
            }
    }
}
