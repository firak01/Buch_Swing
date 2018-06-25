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
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.print.*;
import java.awt.dnd.*;	// NEW
import java.awt.datatransfer.*;	// NEW

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/*
Add files drop gragged from other OS applications
*/

public class BasicTextEditor
	extends JFrame {

	public static final String APP_NAME = "Basic Text Editor";

	public static final String FONTS[] = { "Serif", "SansSerif",
		"Courier" };
	protected Font m_fonts[];

	protected JMenuItem[] m_fontMenus;
	protected JCheckBoxMenuItem m_bold;
	protected JCheckBoxMenuItem m_italic;

	protected JFileChooser m_chooser;

	protected JToolBar	m_toolBar;
	protected JComboBox m_cbFonts;
	protected SmallToggleButton m_bBold;
	protected SmallToggleButton m_bItalic;

	protected ColorMenu m_cmFrg;
	protected ColorMenu m_cmBkg;

	protected JDesktopPane m_desktop;
	protected EditorFrame	m_activeFrame;
	protected JMenu				m_windowMenu;
	protected ButtonGroup	m_windowButtonGroup;

	public static final int INI_WIDTH = 400;
	public static final int INI_HEIGHT = 200;
	protected static int FRAME_COUNTER = 0;

	public BasicTextEditor() {
		super(APP_NAME);
		setSize(600, 400);

		m_fonts = new Font[FONTS.length];
		for (int k=0; k<FONTS.length; k++)
			m_fonts[k] = new Font(FONTS[k], Font.PLAIN, 12);

		m_desktop = new JDesktopPane();
		getContentPane().add(m_desktop, BorderLayout.CENTER);

		// NEW
		new DropTarget(m_desktop, new FileDropper());

		JMenuBar menuBar = createMenuBar();
		setJMenuBar(menuBar);

		m_chooser = new JFileChooser();
		try {
			File dir = (new File(".")).getCanonicalFile();
			m_chooser.setCurrentDirectory(dir);
		} catch (IOException ex) {}

		newDocument();

		WindowListener wndCloser = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (!promptAllToSave())
					return;
				System.exit(0);
			}
		};
		addWindowListener(wndCloser);
	}

	// Get active text editor
	public JTextArea getEditor() {
		if (m_activeFrame == null)
			return null;
		return m_activeFrame.m_editor;
	}

	public void addEditorFrame(File f) {
		EditorFrame frame = new EditorFrame(f);

		frame.setBounds(FRAME_COUNTER*30, FRAME_COUNTER*20,
			INI_WIDTH, INI_HEIGHT);
		FRAME_COUNTER = (FRAME_COUNTER+1) % 10;

		JRadioButtonMenuItem item = frame.m_frameMenuItem;
		m_windowMenu.add(item);
		m_windowButtonGroup.add(item);
		item.setSelected(true);

		frame.addInternalFrameListener(frame.new FrameListener());
		m_desktop.add(frame);
		frame.show();
		activateInternalFrame(frame);
	}

	public void activateInternalFrame(EditorFrame frame) {
		m_activeFrame = frame;
		JRadioButtonMenuItem item = frame.m_frameMenuItem;
		item.setSelected(true);

		JTextArea editor = frame.m_editor;
		Font font = editor.getFont();
		int index = 0;
		for (int k=0; k<FONTS.length; k++) {
			if (font.getName().equals(FONTS[k])) {
				index = k;
				break;
			}
		}
		m_fontMenus[index].setSelected(true);
		m_bold.setSelected(font.isBold());
		m_italic.setSelected(font.isItalic());
		updateEditor();
		m_cmFrg.setColor(editor.getForeground());
		m_cmBkg.setColor(editor.getBackground());
	}

	protected JMenuBar createMenuBar() {
		final JMenuBar menuBar = new JMenuBar();

		JMenu mFile = new JMenu("File");
		mFile.setMnemonic('f');

		ImageIcon iconNew = new ImageIcon("New16.gif");
		Action actionNew = new AbstractAction("New", iconNew) {
			public void actionPerformed(ActionEvent e) {
				newDocument();
			}
		};
		JMenuItem item = new JMenuItem(actionNew);
		item.setMnemonic('n');
		item.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mFile.add(item);

		ImageIcon iconOpen = new ImageIcon("Open16.gif");
		Action actionOpen = new AbstractAction("Open...", iconOpen) {
			public void actionPerformed(ActionEvent e) {
				openDocument();
			}
		};
		item = new JMenuItem(actionOpen);
		item.setMnemonic('o');
		item.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mFile.add(item);

		ImageIcon iconSave = new ImageIcon("Save16.gif");
		Action actionSave = new AbstractAction("Save", iconSave) {
			public void actionPerformed(ActionEvent e) {
				if (m_activeFrame != null)
					m_activeFrame.saveFile(false);
			}
		};
		item = new JMenuItem(actionSave);
		item.setMnemonic('s');
		item.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mFile.add(item);

		ImageIcon iconSaveAs = new ImageIcon("SaveAs16.gif");
		Action actionSaveAs = new AbstractAction("Save As...", iconSaveAs) {
			public void actionPerformed(ActionEvent e) {
				if (m_activeFrame != null)
					m_activeFrame.saveFile(true);
			}
		};
		item = new JMenuItem(actionSaveAs);
		item.setMnemonic('a');
		mFile.add(item);

		mFile.addSeparator();

		Action actionPrint = new AbstractAction("Print...",
			new ImageIcon("Print16.gif")) {
			public void actionPerformed(ActionEvent e) {
				Thread runner = new Thread() {
					public void run() {
						printData();
					}
				};
				runner.start();
			}
		};
		item =	mFile.add(actionPrint);
		item.setMnemonic('p');

		Action actionPrintPreview = new AbstractAction("Print Preview",
			new ImageIcon("PrintPreview16.gif")) {
			public void actionPerformed(ActionEvent e) {
				Thread runner = new Thread() {
					public void run() {
						if (m_activeFrame == null)
							return;
						setCursor(Cursor.getPredefinedCursor(
							Cursor.WAIT_CURSOR));
						PrintPreview preview = new PrintPreview(
							m_activeFrame,
							"Print Preview ["+m_activeFrame.getDocumentName()+"]");
						preview.setVisible(true);
						setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
					}
				};
				runner.start();
			}
		};
		item =	mFile.add(actionPrintPreview);
		item.setMnemonic('v');
		mFile.addSeparator();

		Action actionExit = new AbstractAction("Exit") {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		item = new JMenuItem(actionExit);
		item.setMnemonic('x');
		mFile.add(item);
		menuBar.add(mFile);

		// Create toolbar
		m_toolBar = new JToolBar();
		JButton bNew = new SmallButton(actionNew,
			"New text");
		m_toolBar.add(bNew);
		JButton bOpen = new SmallButton(actionOpen,
			"Open text file");
		m_toolBar.add(bOpen);
		JButton bSave = new SmallButton(actionSave,
			"Save text file");
		m_toolBar.add(bSave);

		JButton bPrint = new SmallButton(actionPrint,
			"Print text file");
		m_toolBar.add(bPrint);

		getContentPane().add(m_toolBar, BorderLayout.NORTH);

		ActionListener fontListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateEditor();
			}
		};

		JMenu mFont = new JMenu("Font");
		mFont.setMnemonic('o');

		ButtonGroup group = new ButtonGroup();
		m_fontMenus = new JMenuItem[FONTS.length];
		for (int k=0; k<FONTS.length; k++) {
			int m = k+1;
			m_fontMenus[k] = new JRadioButtonMenuItem(
				m+" "+FONTS[k]);
			m_fontMenus[k].setSelected(k == 0);
			m_fontMenus[k].setMnemonic('1'+k);
			m_fontMenus[k].setFont(m_fonts[k]);
			m_fontMenus[k].addActionListener(fontListener);
			group.add(m_fontMenus[k]);
			mFont.add(m_fontMenus[k]);
		}

		mFont.addSeparator();

		// Add combobox to tollbar
		m_toolBar.addSeparator();
		m_cbFonts = new JComboBox(FONTS);
		m_cbFonts.setMaximumSize(new Dimension(90, 23));
		m_cbFonts.setToolTipText("Available fonts");
		ActionListener lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = m_cbFonts.getSelectedIndex();
				if (index < 0)
					return;
				m_fontMenus[index].setSelected(true);
				updateEditor();
			}
		};
		m_cbFonts.addActionListener(lst);
		m_toolBar.add(m_cbFonts);

		m_toolBar.addSeparator();
		m_bold = new JCheckBoxMenuItem("Bold");
		m_bold.setMnemonic('b');
		Font fn = m_fonts[1].deriveFont(Font.BOLD);
		m_bold.setFont(fn);
		m_bold.setSelected(false);
		m_bold.addActionListener(fontListener);
		mFont.add(m_bold);

		m_italic = new JCheckBoxMenuItem("Italic");
		m_italic.setMnemonic('i');
		fn = m_fonts[1].deriveFont(Font.ITALIC);
		m_italic.setFont(fn);
		m_italic.setSelected(false);
		m_italic.addActionListener(fontListener);
		mFont.add(m_italic);

		menuBar.add(mFont);

		ImageIcon img1 = new ImageIcon("Bold16.gif");
		m_bBold = new SmallToggleButton(false, img1, img1,
			"Bold font");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_bold.setSelected(m_bBold.isSelected());
				updateEditor();
			}
		};
		m_bBold.addActionListener(lst);
		m_toolBar.add(m_bBold);

		img1 = new ImageIcon("Italic16.gif");
		m_bItalic = new SmallToggleButton(false, img1, img1,
			"Italic font");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_italic.setSelected(m_bItalic.isSelected());
				updateEditor();
			}
		};
		m_bItalic.addActionListener(lst);
		m_toolBar.add(m_bItalic);

		// Add color selection menu
		JMenu mOpt = new JMenu("Options");
		mOpt.setMnemonic('p');


		m_cmFrg = new ColorMenu("Foreground");
		m_cmFrg.setMnemonic('f');
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColorMenu m = (ColorMenu)e.getSource();
				if (getEditor() != null)
					getEditor().setForeground(m.getColor());
			}
		};
		m_cmFrg.addActionListener(lst);
		mOpt.add(m_cmFrg);

		m_cmBkg = new ColorMenu("Background");
		m_cmBkg.setMnemonic('b');
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColorMenu m = (ColorMenu)e.getSource();
				if (getEditor() != null)
					getEditor().setBackground(m.getColor());
			}
		};
		m_cmBkg.addActionListener(lst);
		mOpt.add(m_cmBkg);

		// Chooser menu
		Action actionChooser = new AbstractAction("Color Chooser") {
			public void actionPerformed(ActionEvent e) {
				BasicTextEditor.this.repaint();
				JTextArea editor = getEditor();
				if (editor == null)
					return;

				JColorChooser colorChooser = new JColorChooser();
				PreviewPanel previewPanel = new PreviewPanel(colorChooser);
				colorChooser.setPreviewPanel(previewPanel);

				JDialog colorDialog = JColorChooser.createDialog(
					BasicTextEditor.this,
					"Select Background and Foreground Color",
					true, colorChooser, previewPanel, null);
				previewPanel.setTextForeground(
					editor.getForeground());
				previewPanel.setTextBackground(
					editor.getBackground());
				colorDialog.show();

				if (previewPanel.isSelected()) {
					editor.setBackground(
						previewPanel.getTextBackground());
					m_cmBkg.setColor(previewPanel.getTextBackground());
					editor.setForeground(
						previewPanel.getTextForeground());
					m_cmFrg.setColor(previewPanel.getTextForeground());
				}
			}
		};
		mOpt.addSeparator();
		item =	mOpt.add(actionChooser);
		item.setMnemonic('c');

		menuBar.add(mOpt);

		// Add Window menu
		m_windowMenu = new JMenu("Window");
				m_windowMenu.setMnemonic('w');
				menuBar.add(m_windowMenu);
		m_windowButtonGroup = new ButtonGroup();

		Action actionCascade = new AbstractAction("Cascade") {
			public void actionPerformed(ActionEvent e) {
				cascadeFrames();
			}
		};
		item = new JMenuItem(actionCascade);
		item.setMnemonic('c');
		m_windowMenu.add(item);
		m_windowMenu.addSeparator();

		// Add "About" menu
		JMenu mHelp = new JMenu("Help");
		mHelp.setMnemonic('h');

		Action actionAbout = new AbstractAction("About",
			new ImageIcon("About16.gif")) {
			public void actionPerformed(ActionEvent e) {
				AboutBox dlg = new AboutBox(BasicTextEditor.this);
				dlg.show();
			}
		};
		item =	mHelp.add(actionAbout);
		item.setMnemonic('a');
		menuBar.add(mHelp);

		return menuBar;
	}

	protected void newDocument() {
		addEditorFrame(null);
	}

	protected void openDocument() {
		if (m_chooser.showOpenDialog(BasicTextEditor.this) !=
			JFileChooser.APPROVE_OPTION)
			return;
		File f = m_chooser.getSelectedFile();
		if (f == null || !f.isFile())
			return;

		// Check if empty frame opened by default is still open.
		// If so, close it.
		JInternalFrame[] frames = m_desktop.getAllFrames();
		if (frames.length == 1) {
			EditorFrame frame = (EditorFrame)frames[0];
			if (frame.getFile() == null && !frame.isModified())
				try {
					frame.setClosed(true);
				}
				catch (Exception ex) {}
		}

		addEditorFrame(f);

	}

	protected boolean promptAllToSave() {
		JInternalFrame[] frames = m_desktop.getAllFrames();
		for (int k=0; k<frames.length; k++) {
			EditorFrame frame = (EditorFrame)frames[k];
			if (!frame.promptToSave())
				return false;
		}
		return true;
	}

	// No changes from previous example
	public void cascadeFrames() {
		try {
			JInternalFrame[] frames = m_desktop.getAllFrames();
			JInternalFrame selectedFrame = m_desktop.getSelectedFrame();
			int x = 0;
			int y = 0;
			for (int k=frames.length-1; k>=0; k--) {
				frames[k].setMaximum(false);
				frames[k].setIcon(false);
				frames[k].setBounds(x, y, INI_WIDTH, INI_HEIGHT);
				x += 20;
				y += 20;
			}
			if (selectedFrame != null)
				m_desktop.setSelectedFrame(selectedFrame);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void updateEditor() {
		JTextArea editor = getEditor();
		if (editor == null)
			return;

		int index = -1;
		for (int k=0; k<m_fontMenus.length; k++) {
			if (m_fontMenus[k].isSelected()) {
				index = k;
				break;
			}
		}
		if (index == -1)
			return;

		if (index==2) { // Courier
			m_bold.setSelected(false);
			m_bold.setEnabled(false);
			m_italic.setSelected(false);
			m_italic.setEnabled(false);
			m_bBold.setSelected(false);
			m_bBold.setEnabled(false);
			m_bItalic.setSelected(false);
			m_bItalic.setEnabled(false);
		}
		else {
			m_bold.setEnabled(true);
			m_italic.setEnabled(true);
			m_bBold.setEnabled(true);
			m_bItalic.setEnabled(true);
		}

		// Synchronize toolbar and menu components
		m_cbFonts.setSelectedIndex(index);
		boolean isBold = m_bold.isSelected();
		boolean isItalic = m_italic.isSelected();
		if (m_bBold.isSelected() != isBold)
			m_bBold.setSelected(isBold);
		if (m_bItalic.isSelected() != isItalic)
			m_bItalic.setSelected(isItalic);

		int style = Font.PLAIN;
		if (m_bold.isSelected())
			style |= Font.BOLD;
		if (m_italic.isSelected())
			style |= Font.ITALIC;
		Font fn = m_fonts[index].deriveFont(style);
		editor.setFont(fn);
		editor.repaint();
	}

	public void showError(Exception ex, String message) {
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this,
			message, APP_NAME,
			JOptionPane.WARNING_MESSAGE);
	}

	public void printData() {
		if (m_activeFrame == null)
			return;
		try {
			PrinterJob prnJob = PrinterJob.getPrinterJob();
			prnJob.setPrintable(m_activeFrame);
			if (!prnJob.printDialog())
				return;
			setCursor( Cursor.getPredefinedCursor(
				Cursor.WAIT_CURSOR));
			prnJob.print();
			setCursor( Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));
			JOptionPane.showMessageDialog(this,
				"Printing completed successfully", APP_NAME,
				JOptionPane.INFORMATION_MESSAGE);
		}
		catch (PrinterException ex) {
			showError(ex, "Printing error: "+ex.toString());
		}
	}

	public static void main(String argv[]) {
		BasicTextEditor frame = new BasicTextEditor();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
	}

	// NEW
	class FileDropper extends DropTargetAdapter {
		public void drop(DropTargetDropEvent e) {
			try {
				DropTargetContext context = e.getDropTargetContext();
				e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				Transferable t  = e.getTransferable();
				Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
				if (data instanceof java.util.List) {
					java.util.List list = (java.util.List)data;
					for (int k=0; k<list.size(); k++) {
						Object dataLine = list.get(k);
						if (dataLine instanceof File)
							addEditorFrame((File)dataLine);
					}
				}
				context.dropComplete(true);
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	// Internal frame with editor
	class EditorFrame
		extends JInternalFrame
		implements Printable {

		protected JTextArea m_editor;
		protected File	m_currentFile;
		protected JRadioButtonMenuItem	m_frameMenuItem;

		protected boolean m_textChanged = false;

		public EditorFrame(File f) {
			super("", true, true, true, true);
			m_currentFile = f;
			setTitle(getDocumentName());
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

			m_editor = new CustomTextArea();
			JScrollPane ps = new JScrollPane(m_editor);
			getContentPane().add(ps, BorderLayout.CENTER);

			m_frameMenuItem = new JRadioButtonMenuItem(getTitle());
			m_frameMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if(isSelected())
						return;
					try {
						if (isIcon())
							setIcon(false);
						setSelected(true);
					} catch (java.beans.PropertyVetoException e) { }
			}});

			if (m_currentFile != null) {
				try {
					FileReader in = new FileReader(m_currentFile);
					m_editor.read(in, null);
					in.close();
				}
				catch (IOException ex) {
					showError(ex, "Error reading file "+m_currentFile);
				}
			}
			m_editor.getDocument().addDocumentListener(new UpdateListener());

			m_editor.setDragEnabled(true);
			// We need to add writer method for selectedText property
			TransferHandler tr = new TransferHandler("selectedText");
			m_editor.setTransferHandler(tr);

			MouseListener ml = new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					JComponent c = (JComponent)e.getSource();
					TransferHandler th = c.getTransferHandler();
					th.exportAsDrag(c, e, TransferHandler.MOVE);
				}
			};
			m_editor.addMouseListener(ml);
		}

		public String getDocumentName() {
			return m_currentFile==null ? "Untitled "+(FRAME_COUNTER+1) :
				m_currentFile.getName();
		}

		public File getFile() {
			return m_currentFile;
		}

		public boolean isModified() {
			return m_textChanged;
		}

		public boolean saveFile(boolean saveAs) {
			if (!saveAs && !m_textChanged)
				return true;
			if (saveAs || m_currentFile == null) {
				if (m_chooser.showSaveDialog(BasicTextEditor.this) !=
					JFileChooser.APPROVE_OPTION)
					return false;
				File f = m_chooser.getSelectedFile();
				if (f == null)
					return false;
				m_currentFile = f;
				setTitle(getDocumentName());
				m_frameMenuItem.setText(getDocumentName());
			}

			try {
				FileWriter out = new
					FileWriter(m_currentFile);
				m_editor.write(out);
				out.close();
			}
			catch (IOException ex) {
				showError(ex, "Error saving file "+m_currentFile);
				return false;
			}
			m_textChanged = false;
			return true;
		}

		public boolean promptToSave() {
			if (!m_textChanged)
				return true;
			int result = JOptionPane.showConfirmDialog(
				BasicTextEditor.this,
				"Save changes to "+getTitle()+"?",
				APP_NAME, JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE);
			switch (result) {
			case JOptionPane.YES_OPTION:
				if (!saveFile(false))
					return false;
				return true;
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;
			}
			return true;
		}

		private Vector m_lines;

		public int print(Graphics pg, PageFormat pageFormat,
			int pageIndex) throws PrinterException {
			pg.translate((int)pageFormat.getImageableX(),
				(int)pageFormat.getImageableY());
			int wPage = (int)pageFormat.getImageableWidth();
			int hPage = (int)pageFormat.getImageableHeight();
			pg.setClip(0, 0, wPage, hPage);

			pg.setColor(m_editor.getBackground());
			pg.fillRect(0, 0, wPage, hPage);
			pg.setColor(m_editor.getForeground());

			Font font = m_editor.getFont();
			pg.setFont(font);
			FontMetrics fm = pg.getFontMetrics();
			int hLine = fm.getHeight();

			if (m_lines == null)
				m_lines = getLines(fm, wPage);

			int numLines = m_lines.size();
			int linesPerPage = Math.max(hPage/hLine, 1);
			int numPages = (int)Math.ceil((double)numLines/(double)linesPerPage);
			if (pageIndex >= numPages) {
				m_lines = null;
				return NO_SUCH_PAGE;
			}

			int x = 0;
			int y = fm.getAscent();
			int lineIndex = linesPerPage*pageIndex;
			while (lineIndex < m_lines.size() && y < hPage) {
				String str = (String)m_lines.get(lineIndex);
				pg.drawString(str, x, y);
				y += hLine;
				lineIndex++;
			}

			return PAGE_EXISTS;
		}

		public static final int TAB_SIZE = 4;

		protected Vector getLines(FontMetrics fm, int wPage) {
			Vector v = new Vector();

			String text = m_editor.getText();
			String prevToken = "";
			StringTokenizer st = new StringTokenizer(text, "\n\r", true);
			while (st.hasMoreTokens()) {
				String line = st.nextToken();
				if (line.equals("\r"))
					continue;

				// StringTokenizer will ignore empty lines, so it's a bit tricky to get them...
				if (line.equals("\n") && prevToken.equals("\n"))
					v.add("");
				prevToken = line;
				if (line.equals("\n"))
					continue;

				StringTokenizer st2 = new StringTokenizer(line, " \t", true);
				String line2 = "";
				while (st2.hasMoreTokens()) {
					String token = st2.nextToken();

					if (token.equals("\t")) {
						int numSpaces = TAB_SIZE - line2.length()%TAB_SIZE;
						token = "";
						for (int k=0; k<numSpaces; k++)
							token += " ";
					}

					int lineLength = fm.stringWidth(line2 + token);
					if (lineLength > wPage && line2.length() > 0) {
						v.add(line2);
						line2 = token.trim();
						continue;
					}
					line2 += token;
				}
				v.add(line2);
			}

			return v;
		}

		class UpdateListener implements DocumentListener {

			public void insertUpdate(DocumentEvent e) {
				m_textChanged = true;
			}

			public void removeUpdate(DocumentEvent e) {
				m_textChanged = true;
			}

			public void changedUpdate(DocumentEvent e) {
				m_textChanged = true;
			}
		}

		class FrameListener extends InternalFrameAdapter {

			public void internalFrameClosing(InternalFrameEvent e) {
				if (!promptToSave())
					return;
				m_windowMenu.remove(m_frameMenuItem);
				dispose();
			}

			public void internalFrameActivated(InternalFrameEvent e) {
				m_frameMenuItem.setSelected(true);
				activateInternalFrame(EditorFrame.this);
			}
		}

		// Important: this class must be public, or invocation will fail
		public class CustomTextArea extends JTextArea {
			public void setSelectedText(String value) {
				replaceSelection(value);
			}
		}
	}
}

class SmallButton extends JButton implements MouseListener {
	protected Border m_raised = new SoftBevelBorder(BevelBorder.RAISED);
	protected Border m_lowered = new SoftBevelBorder(BevelBorder.LOWERED);
	protected Border m_inactive = new EmptyBorder(3, 3, 3, 3);
	protected Border m_border = m_inactive;
	protected Insets m_ins = new Insets(4,4,4,4);

	public SmallButton(Action act, String tip) {
		super((Icon)act.getValue(Action.SMALL_ICON));
		setBorder(m_inactive);
		setMargin(m_ins);
		setToolTipText(tip);
		setRequestFocusEnabled(false);
		addActionListener(act);
		addMouseListener(this);
	}

	public float getAlignmentY() {
		return 0.5f;
	}

	// Overridden for 1.4 bug fix
	public Border getBorder() {
		return m_border;
	}

	// Overridden for 1.4 bug fix
	public Insets getInsets() {
		return m_ins;
	}

	public void mousePressed(MouseEvent e) {
		m_border = m_lowered;
		setBorder(m_lowered);
	}

	public void mouseReleased(MouseEvent e) {
		m_border = m_inactive;
		setBorder(m_inactive);
	}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {
		m_border = m_raised;
		setBorder(m_raised);
	}

	public void mouseExited(MouseEvent e) {
		m_border = m_inactive;
		setBorder(m_inactive);
	}
}

class SmallToggleButton extends JToggleButton
	implements ItemListener {

	protected Border m_raised = new SoftBevelBorder(BevelBorder.RAISED);
	protected Border m_lowered = new SoftBevelBorder(BevelBorder.LOWERED);
	protected Insets m_ins = new Insets(4,4,4,4);

	public SmallToggleButton(boolean selected,
		ImageIcon imgUnselected, ImageIcon imgSelected, String tip) {
		super(imgUnselected, selected);
		setHorizontalAlignment(CENTER);
		setBorder(selected ? m_lowered : m_raised);
		setMargin(m_ins);
		setToolTipText(tip);
		setRequestFocusEnabled(false);
		setSelectedIcon(imgSelected);
		addItemListener(this);
	}

	public float getAlignmentY() {
		return 0.5f;
	}

	// Overridden for 1.4 bug fix
	public Insets getInsets() {
		return m_ins;
	}

	public Border getBorder() {
		return (isSelected() ? m_lowered : m_raised);
	}

	public void itemStateChanged(ItemEvent e) {
		setBorder(isSelected() ? m_lowered : m_raised);
	}
}


// Custom menu component
class ColorMenu extends JMenu {

	protected Border m_unselectedBorder;
	protected Border m_selectedBorder;
	protected Border m_activeBorder;

	protected Hashtable m_panes;
	protected ColorPane m_selected;

	public ColorMenu(String name) {
		super(name);
		m_unselectedBorder = new CompoundBorder(
			new MatteBorder(1, 1, 1, 1, getBackground()),
			new BevelBorder(BevelBorder.LOWERED,
			Color.white, Color.gray));
		m_selectedBorder = new CompoundBorder(
			new MatteBorder(2, 2, 2, 2, Color.red),
			new MatteBorder(1, 1, 1, 1, getBackground()));
		m_activeBorder = new CompoundBorder(
			new MatteBorder(2, 2, 2, 2, Color.blue),
			new MatteBorder(1, 1, 1, 1, getBackground()));

		JPanel p = new JPanel();
		p.setBorder(new EmptyBorder(5, 5, 5, 5));
		p.setLayout(new GridLayout(8, 8));
		m_panes = new Hashtable();

		int[] values = new int[] { 0, 128, 192, 255 };
		for (int r=0; r<values.length; r++) {
			for (int g=0; g<values.length; g++) {
				for (int b=0; b<values.length; b++) {
					Color c = new Color(values[r], values[g], values[b]);
					ColorPane pn = new ColorPane(c);
					p.add(pn);
					m_panes.put(c, pn);
				}
			}
		}
		add(p);
	}

	public void setColor(Color c) {
		Object obj = m_panes.get(c);
		if (obj == null)
			return;
		if (m_selected != null)
			m_selected.setSelected(false);
		m_selected = (ColorPane)obj;
		m_selected.setSelected(true);
	}

	public Color getColor() {
		if (m_selected == null)
			return null;
		return m_selected.getColor();
	}

	public void doSelection() {
		fireActionPerformed(new ActionEvent(this,
			ActionEvent.ACTION_PERFORMED, getActionCommand()));
	}

	class ColorPane extends JPanel implements MouseListener {
		protected Color m_c;
		protected boolean m_selected;

		public ColorPane(Color c) {
			m_c = c;
			setBackground(c);
			setBorder(m_unselectedBorder);
			String msg = "R "+c.getRed()+", G "+c.getGreen()+
				", B "+c.getBlue();
			setToolTipText(msg);
			addMouseListener(this);
		}

		public Color getColor() {
			return m_c;
		}

		public Dimension getPreferredSize() {
			return new Dimension(15, 15);
		}

		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		public void setSelected(boolean selected) {
			m_selected = selected;
			if (m_selected)
				setBorder(m_selectedBorder);
			else
				setBorder(m_unselectedBorder);
		}

		public boolean isSelected() {
			return m_selected;
		}

		public void mousePressed(MouseEvent e) {}

		public void mouseClicked(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {
			setColor(m_c);
			MenuSelectionManager.defaultManager().clearSelectedPath();
			doSelection();
		}

		public void mouseEntered(MouseEvent e) {
			setBorder(m_activeBorder);
		}

		public void mouseExited(MouseEvent e) {
			setBorder(m_selected ? m_selectedBorder :
				m_unselectedBorder);
		}
	}
}

class AboutBox extends JDialog {

	public AboutBox(Frame owner) {
		super(owner, "About", true);

		JLabel lbl = new JLabel(new ImageIcon("icon.gif"));
		JPanel p = new JPanel();
		Border b1 = new BevelBorder(BevelBorder.LOWERED);
		Border b2 = new EmptyBorder(5, 5, 5, 5);
		lbl.setBorder(new CompoundBorder(b1, b2));
		p.add(lbl);
		getContentPane().add(p, BorderLayout.WEST);

		String message = "Basic Text Editor sample application\n"+
			"(c) M.Robinson, P.Vorobiev 1998-2001";
		JTextArea txt = new JTextArea(message);
		txt.setBorder(new EmptyBorder(5, 10, 5, 10));
		txt.setFont(new Font("Helvetica", Font.BOLD, 12));
		txt.setEditable(false);
		txt.setBackground(getBackground());
		p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(txt);

		message = "JVM version "+System.getProperty("java.version")+"\n"+
		" by "+System.getProperty("java.vendor");
		txt = new JTextArea(message);
		txt.setBorder(new EmptyBorder(5, 10, 5, 10));
		txt.setFont(new Font("Arial", Font.PLAIN, 12));
		txt.setEditable(false);
		txt.setLineWrap(true);
		txt.setWrapStyleWord(true);
		txt.setBackground(getBackground());
		p.add(txt);

		getContentPane().add(p, BorderLayout.CENTER);

		final JButton btOK = new JButton("OK");
		ActionListener lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		btOK.addActionListener(lst);
		p = new JPanel();
		p.add(btOK);
		getRootPane().setDefaultButton(btOK);
	getRootPane().registerKeyboardAction(lst,
		KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
		JComponent.WHEN_IN_FOCUSED_WINDOW);
		getContentPane().add(p, BorderLayout.SOUTH);

	// That will transer focus from first component upon dialog's show
	WindowListener wl = new WindowAdapter() {
		public void windowOpened(WindowEvent e) {
			btOK.requestFocus();
		}
	};
	addWindowListener(wl);

		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}
}

// Color preview panel
class PreviewPanel extends JPanel
 implements ChangeListener, ActionListener
{
	protected JColorChooser m_chooser;
	protected JLabel m_preview;
	protected JToggleButton m_btBack;
	protected JToggleButton m_btFore;
	protected boolean m_isSelected = false;

	public PreviewPanel(JColorChooser chooser) {
		this(chooser, Color.white, Color.black);
	}

	public PreviewPanel(JColorChooser chooser,
	 Color background, Color foreground) {
		m_chooser = chooser;
		chooser.getSelectionModel().addChangeListener(this);

		setLayout(new BorderLayout());
		JPanel p = new JPanel(new GridLayout(2, 1, 0, 0));
		ButtonGroup group = new ButtonGroup();
		m_btBack = new JToggleButton("Background");
		m_btBack.setSelected(true);
		m_btBack.addActionListener(this);
		group.add(m_btBack);
		p.add(m_btBack);
		m_btFore = new JToggleButton("Foreground");
		m_btFore.addActionListener(this);
		group.add(m_btFore);
		p.add(m_btFore);
		add(p, BorderLayout.WEST);

		p = new JPanel(new BorderLayout());
		Border b1 = new EmptyBorder(5, 10, 5, 10);
		Border b2 = new BevelBorder(BevelBorder.RAISED);
		Border b3 = new EmptyBorder(2, 2, 2, 2);
		Border cb1 = new CompoundBorder(b1, b2);
		Border cb2 = new CompoundBorder(cb1, b3);
		p.setBorder(cb2);

		m_preview = new JLabel("Text colors preview",
			JLabel.CENTER);
		m_preview.setBackground(background);
		m_preview.setForeground(foreground);
		m_preview.setFont(new Font("Arial",Font.BOLD, 24));
		m_preview.setOpaque(true);
		p.add(m_preview, BorderLayout.CENTER);
		add(p, BorderLayout.CENTER);

		m_chooser.setColor(background);
	}

	protected boolean isSelected() {
		return m_isSelected;
	}

	public void setTextBackground(Color c) {
		m_preview.setBackground(c);
	}

	public Color getTextBackground() {
		return m_preview.getBackground();
	}

	public void setTextForeground(Color c) {
		m_preview.setForeground(c);
	}

	public Color getTextForeground() {
		return m_preview.getForeground();
	}

	public void stateChanged(ChangeEvent evt) {
		Color c = m_chooser.getColor();
		if (c != null) {
			if (m_btBack.isSelected())
				m_preview.setBackground(c);
			else
				m_preview.setForeground(c);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == m_btBack)
			m_chooser.setColor(getTextBackground());
		else if (evt.getSource() == m_btFore)
			m_chooser.setColor(getTextForeground());
		else
			m_isSelected = true;
	}
}
