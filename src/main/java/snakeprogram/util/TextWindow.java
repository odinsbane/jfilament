/*-
 * #%L
 * JFilament 2D active contours.
 * %%
 * Copyright (C) 2010 - 2023 University College London
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the UCL LMCB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package snakeprogram.util;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class TextWindow extends JFrame implements Runnable, ActionListener {
    String DATA;
    public TextWindow(String title, String data){

        super(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea jta = new JTextArea(data);
        jta.setEditable(false);

        JScrollPane jsp = new JScrollPane(jta);
        add(jsp);
        setSize(600,800);

        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("file");
        JMenuItem save = new JMenuItem("save");
        save.setActionCommand("save");
        save.addActionListener(this);
        file.add(save);
        menubar.add(file);
        setJMenuBar(menubar);

        DATA = data;
    }
    /**
     * The only action - save.
     * @param event event passed from the man.
     */
    public void actionPerformed(ActionEvent event) {
        FileDialog fd = new FileDialog(this,"Save CSV File",FileDialog.SAVE);
        fd.setFile("data.csv");
        fd.setVisible(true);
        String fname = fd.getFile();
        if(fname==null)
            return;
        String dirname = fd.getDirectory();
        File output = new File(dirname,fname);
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(output));
            bw.write(DATA);
            bw.close();
        } catch(Exception except){
            except.printStackTrace();
            //whoops
        }

    }
    /** for showing on event queue*/
    public void run() {
        setVisible(true);
    }

    public void display(){

        java.awt.EventQueue.invokeLater(this);

    }

}
