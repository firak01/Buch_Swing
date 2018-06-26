/** 
 *  Copyright 1999-2002 Matthew Robinson and Pavel Vorobiev. 
 *  All Rights Reserved. 
 * 
 *  =================================================== 
 *  This program contains code from the book "Swing" 
 *  2nd Edition by Matthew Robinson and Pavel Vorobiev 
 *  http://www.spindoczine.com/sbe 
 *  =================================================== 
 * 
 *  The above paragraph must be included in full, unmodified 
 *  and completely intact in the beginning of any source code 
 *  file that references, copies or uses (in any way, shape 
 *  or form) code contained in this file. 
 */ 

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;

public class DragDropTest extends JFrame {
  public DragDropTest() {
    super("Drag & Drop Test");

    TransferHandler th = new TransferHandler("text");

    JPanel contentPane = (JPanel) getContentPane();

    JTextField tf1 = new JTextField("DRAG_ME", 10);
    tf1.setTransferHandler(th);
    JTextField tf2 = new JTextField(10);
    tf2.setTransferHandler(th);

    tf1.setDragEnabled(true);

    contentPane.setLayout(new GridLayout(2,2));
    contentPane.add(new JLabel("Text Field (Drag enabled): "));
    contentPane.add(tf1);
    contentPane.add(new JLabel("Text Field (Drag not enabled): "));
    contentPane.add(tf2);

    pack();
  }

  public static void main(String args[]) {
    DragDropTest ddt = new DragDropTest();
    ddt.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    ddt.show();
  }
}

