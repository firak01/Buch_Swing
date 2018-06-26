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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

/*

This example shows custom drag-and-drop implementation.
XML node can be dragged from one parent to another.

UI features:
- cursor is changed, indicationt that drop is available or is not available
- target component changes indicating that drop is available
- when dragging cursor is hovering over the edge of the scroll pane, it scrolls

Compare to standard DnD implementation in the next chapter

*/

public class XmlViewer
	extends JFrame {

	public static final String APP_NAME = "XML Viewer";

	protected Document m_doc;

	protected JTree  m_tree;
	protected JScrollPane m_treeScrollPane;	// NEW
	protected DefaultTreeModel m_model;
	protected DefaultTreeCellEditor m_treeEditor;
	protected Node m_editingNode = null;

	protected JTable m_table;
	protected AttrTableModel m_tableModel;

	protected JFileChooser m_chooser;
	protected File  m_currentFile;

	protected boolean m_xmlChanged = false;

	protected JButton m_addNodeBtn;
	protected JButton m_editNodeBtn;
	protected JButton m_delNodeBtn;
	protected JButton m_addAttrBtn;
	protected JButton m_editAttrBtn;
	protected JButton m_delAttrBtn;

	// NEW
	protected Cursor m_dragCursor;
	protected Cursor m_nodropCursor;
	protected XmlViewerNode m_draggingTreeNode;
	protected XmlViewerNode m_draggingOverNode;

	public XmlViewer() {
		super(APP_NAME);
		setSize(800, 400);
		getContentPane().setLayout(new BorderLayout());

		JToolBar tb = createToolbar();
		getContentPane().add(tb, BorderLayout.NORTH);

		DefaultMutableTreeNode top = new DefaultMutableTreeNode(
			"No XML loaded");
		m_model = new DefaultTreeModel(top);
		m_tree = new JTree(m_model);

		m_tree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_tree.setShowsRootHandles(true);
		m_tree.setEditable(false);

		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			// NEW
			Color m_draggingBackground = new Color(0, 0, 128);
			Color m_draggingForeground = Color.white;
			Color m_standardBackground = getBackgroundNonSelectionColor();
			Color m_standardForeground = getTextNonSelectionColor();

			public Component getTreeCellRendererComponent(JTree tree,
				Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
				// NEW
				if (value.equals(m_draggingOverNode)) {
					setBackgroundNonSelectionColor(m_draggingBackground);
					setTextNonSelectionColor(m_draggingForeground);
					sel = false;
				}
				else {
					setBackgroundNonSelectionColor(m_standardBackground);
					setTextNonSelectionColor(m_standardForeground);
				}

				Component res = super.getTreeCellRendererComponent(tree,
					value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof XmlViewerNode) {
					Node node = ((XmlViewerNode)value).getXmlNode();
					if (node instanceof Element)
						setIcon(expanded ? openIcon : closedIcon);
					else
						setIcon(leafIcon);
				}
				return res;
			}
		};
		m_tree.setCellRenderer(renderer);

		m_treeEditor = new DefaultTreeCellEditor(m_tree, renderer) {
			public boolean isCellEditable(EventObject event) {
				Node node = getSelectedNode();
				if (node != null && node.getNodeType() == Node.TEXT_NODE)
					return super.isCellEditable(event);
				else
					return false;
			}

			public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean isSelected, boolean expanded, boolean leaf, int row) {
				if (value instanceof XmlViewerNode)
					m_editingNode = ((XmlViewerNode)value).getXmlNode();
				return super.getTreeCellEditorComponent(tree,
					value, isSelected, expanded, leaf, row);
			}
		};
		m_treeEditor.addCellEditorListener(new XmlEditorListener());
		m_tree.setCellEditor(m_treeEditor);
		m_tree.setEditable(true);
		m_tree.setInvokesStopCellEditing(true);

		m_tableModel = new AttrTableModel();
		m_table = new JTable(m_tableModel);

		m_treeScrollPane = new JScrollPane(m_tree);	// NEW
		JScrollPane s2 = new JScrollPane(m_table);
		s2.getViewport().setBackground(m_table.getBackground());
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			m_treeScrollPane, s2);	// NEW
		sp.setDividerLocation(400);
		sp.setDividerSize(5);
		getContentPane().add(sp, BorderLayout.CENTER);

		TreeSelectionListener lSel = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				Node node = getSelectedNode();
				setNodeToTable(node);	// null is OK
				enableNodeButtons();
				enableAttrButtons();
			}
		};
		m_tree.addTreeSelectionListener(lSel);

		ListSelectionListener lTbl = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				enableAttrButtons();
			}
		};
		m_table.getSelectionModel().addListSelectionListener(lTbl);

		enableNodeButtons();
		enableAttrButtons();

		// NEW
		// Load drag-and-drop cursors.
		try {
			ImageIcon icon = new ImageIcon("DragCursor.gif");
			m_dragCursor = Toolkit.getDefaultToolkit().
				createCustomCursor(icon.getImage(),
				new Point(5, 5), "D&D Cursor");
			icon = new ImageIcon("NodropCursor.gif");
			m_nodropCursor = Toolkit.getDefaultToolkit().
				createCustomCursor(icon.getImage(),
				new Point(15, 15), "NoDrop Cursor");
		} catch (Exception ex) {
			System.out.println("Loading cursor: "+ex);
			m_dragCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
			m_nodropCursor = m_dragCursor;
		}

		TreeMouseListener dnd = new TreeMouseListener();
		m_tree.addMouseListener(dnd);
		m_tree.addMouseMotionListener(dnd);

		WindowListener wndCloser = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (!promptToSave())
					return;
				System.exit(0);
			}
		};
		addWindowListener(wndCloser);

		m_chooser = new JFileChooser();
		m_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		m_chooser.setFileFilter(new SimpleFilter("xml",
			"XML Files"));
		try {
			File dir = (new File(".")).getCanonicalFile();
			m_chooser.setCurrentDirectory(dir);
		} catch (IOException ex) {}
	}

	protected JToolBar createToolbar() {
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		JButton bt = new JButton(new ImageIcon("New24.gif"));
		bt.setToolTipText("New XML document");
		ActionListener lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!promptToSave())
					return;
				newDocument();
 			}
		};
		bt.addActionListener(lst);
		tb.add(bt);

		bt = new JButton(new ImageIcon("Open24.gif"));
		bt.setToolTipText("Open XML file");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!promptToSave())
					return;
				openDocument();
 			}
		};
		bt.addActionListener(lst);
		tb.add(bt);

		bt = new JButton(new ImageIcon("Save24.gif"));
		bt.setToolTipText("Save changes to current file");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveFile(false);
 			}
		};
		bt.addActionListener(lst);
		tb.add(bt);

		bt = new JButton(new ImageIcon("SaveAs24.gif"));
		bt.setToolTipText("Save changes to another file");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveFile(true);
 			}
		};
		bt.addActionListener(lst);
		tb.add(bt);

		tb.addSeparator();
		tb.add(new JLabel("Node:"));
		m_addNodeBtn = new JButton(new ImageIcon("Add24.gif"));
		m_addNodeBtn.setToolTipText("Add new XML element");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addNewNode();
 			}
		};
		m_addNodeBtn.addActionListener(lst);
		tb.add(m_addNodeBtn);

		m_editNodeBtn = new JButton(new ImageIcon("Edit24.gif"));
		m_editNodeBtn.setToolTipText("Edit XML node");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editNode();
 			}
		};
		m_editNodeBtn.addActionListener(lst);
		tb.add(m_editNodeBtn);

		m_delNodeBtn = new JButton(new ImageIcon("Delete24.gif"));
		m_delNodeBtn.setToolTipText("Delete XML node");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteNode();
 			}
		};
		m_delNodeBtn.addActionListener(lst);
		tb.add(m_delNodeBtn);

		tb.addSeparator();
		tb.add(new JLabel("Attr:"));
		m_addAttrBtn = new JButton(new ImageIcon("Add24.gif"));
		m_addAttrBtn.setToolTipText("Add new attribute");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addNewAttribute();
 			}
		};
		m_addAttrBtn.addActionListener(lst);
		tb.add(m_addAttrBtn);

		m_editAttrBtn = new JButton(new ImageIcon("Edit24.gif"));
		m_editAttrBtn.setToolTipText("Edit attribute");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editAttribute();
 			}
		};
		m_editAttrBtn.addActionListener(lst);
		tb.add(m_editAttrBtn);

		m_delAttrBtn = new JButton(new ImageIcon("Delete24.gif"));
		m_delAttrBtn.setToolTipText("Delete attribute");
		lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteAttribute();
 			}
		};
		m_delAttrBtn.addActionListener(lst);
		tb.add(m_delAttrBtn);

		return tb;
	}

	public String getDocumentName() {
		return m_currentFile==null ? "Untitled" :
			m_currentFile.getName();
	}

	public void newDocument() {
		String input = (String)JOptionPane.showInputDialog(this,
			"Please enter root node name of the new XML document",
			APP_NAME, JOptionPane.PLAIN_MESSAGE,
			null, null, "");
		if (!isLegalXmlName(input))
			return;

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
		try {
			DocumentBuilderFactory docBuilderFactory =
				DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.
				newDocumentBuilder();

			m_doc = docBuilder.newDocument();

			Element root = m_doc.createElement(input);
			root.normalize();
			m_doc.appendChild(root);

			DefaultMutableTreeNode top = createTreeNode(root);

			m_model.setRoot(top);
			m_tree.treeDidChange();
			expandTree(m_tree);
			setNodeToTable(null);
			m_currentFile = null;
			setTitle(APP_NAME+" ["+getDocumentName()+"]");
			m_xmlChanged = true;	// Will prompt to save
		}
		catch (Exception ex) {
			showError(ex, "Error creating new XML document");
		}
		finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	protected void openDocument() {
		Thread runner = new Thread() {
			public void run() {
				if (m_chooser.showOpenDialog(XmlViewer.this) !=
					JFileChooser.APPROVE_OPTION)
					return;
				File f = m_chooser.getSelectedFile();
				if (f == null || !f.isFile())
					return;

				setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
				try {
					DocumentBuilderFactory docBuilderFactory =
						DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docBuilderFactory.
						newDocumentBuilder();

					m_doc = docBuilder.parse(f);

					Element root = m_doc.getDocumentElement();
					root.normalize();

					DefaultMutableTreeNode top = createTreeNode(root);

					m_model.setRoot(top);
					m_tree.treeDidChange();
					expandTree(m_tree);
					setNodeToTable(null);
					m_currentFile = f;
					setTitle(APP_NAME+" ["+getDocumentName()+"]");
					m_xmlChanged = false;
				}
				catch (Exception ex) {
					showError(ex, "Error reading or parsing XML file");
				}
				finally {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		};
		runner.start();
	}

	protected boolean saveFile(boolean saveAs) {
		if (m_doc == null)
			return false;
		if (saveAs || m_currentFile == null) {
			if (m_chooser.showSaveDialog(XmlViewer.this) !=
				JFileChooser.APPROVE_OPTION)
				return false;
			File f = m_chooser.getSelectedFile();
			if (f == null)
				return false;
			m_currentFile = f;
			setTitle(APP_NAME+" ["+getDocumentName()+"]");
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
		try {
			FileWriter out = new FileWriter(m_currentFile);
			XMLRoutines.write(m_doc, out);
			out.close();
		}
		catch (Exception ex) {
			showError(ex, "Error saving XML file");
 		}
 		finally {
 			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		m_xmlChanged = false;
		return true;
	}

	protected boolean promptToSave() {
		if (!m_xmlChanged)
			return true;
		int result = JOptionPane.showConfirmDialog(this,
			"Save changes to "+getDocumentName()+"?",
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

	protected void enableNodeButtons() {
		boolean b1 = (getSelectedNode() instanceof Element);
		boolean b2 = (getSelectedNode() != null);
		m_addNodeBtn.setEnabled(b1);
		m_editNodeBtn.setEnabled(b2);
		m_delNodeBtn.setEnabled(b2);
	}

	protected void enableAttrButtons() {
		boolean b1 = (m_tableModel.getNode() instanceof Element);
		boolean b2 = (m_table.getSelectedRowCount() > 0);
		m_addAttrBtn.setEnabled(b1);
		m_editAttrBtn.setEnabled(b2);
		m_delAttrBtn.setEnabled(b2);
	}

	protected void addNewNode() {
		if (m_doc == null)
			return;
		XmlViewerNode treeNode = getSelectedTreeNode();
		if (treeNode == null)
			return;
		Node parent = treeNode.getXmlNode();
		if (parent == null)
			return;

		String input = (String)JOptionPane.showInputDialog(this,
			"Please enter name of the new XML node",
			APP_NAME, JOptionPane.PLAIN_MESSAGE,
			null, null, "");
		if (!isLegalXmlName(input))
			return;

		try {
			Element newElement = m_doc.createElement(input);
			XmlViewerNode nodeElement = new XmlViewerNode(newElement);
			treeNode.addXmlNode(nodeElement);

			m_model.nodeStructureChanged(treeNode);	// Necessary to display your new node
			TreePath path = m_tree.getSelectionPath();
			if (path != null) {
				path = path.pathByAddingChild(nodeElement);
				m_tree.setSelectionPath(path);
				m_tree.scrollPathToVisible(path);
			}
			m_xmlChanged = true;
		}
		catch (Exception ex) {
			showError(ex, "Error adding new node");
		}
	}

	protected void addNewAttribute() {
		Node node = m_tableModel.getNode();
		if (!(node instanceof Element))
			return;

		String input = (String)JOptionPane.showInputDialog(this,
			"Please enter new attribute name",
			APP_NAME, JOptionPane.PLAIN_MESSAGE,
			null, null, "");
		if (!isLegalXmlName(input))
			return;

		try {
			((Element)node).setAttribute(input, "");
			setNodeToTable(node);
			for (int k=0; k<m_tableModel.getRowCount(); k++)
				if (m_tableModel.getValueAt(k, AttrTableModel.NAME_COLUMN).equals(input)) {
					m_table.editCellAt(k, AttrTableModel.VALUE_COLUMN);
					break;
				}
			m_xmlChanged = true;
		}
		catch (Exception ex) {
			showError(ex, "Error adding attribute");
		}
	}

	protected void editNode() {
		TreePath path = m_tree.getSelectionPath();
		XmlViewerNode treeNode = getSelectedTreeNode();
		if (treeNode == null)
			return;
		Node node = treeNode.getXmlNode();
		if (node == null)
			return;
		try {
			switch (node.getNodeType()) {
			case Node.ELEMENT_NODE:
				// Find child text node
				for (int k=0; k<treeNode.getChildCount(); k++) {
					XmlViewerNode childNode = (XmlViewerNode)
						treeNode.getChildAt(k);
					Node nd = childNode.getXmlNode();
					if (nd instanceof Text) {
						path = path.pathByAddingChild(childNode);
						m_tree.setSelectionPath(path);
						m_tree.scrollPathToVisible(path);
						m_tree.startEditingAtPath(path);
						return;
					}
				}
				// Not found, so add a new text node
				Text text = m_doc.createTextNode("");
				XmlViewerNode nodeText = new XmlViewerNode(text);
				treeNode.addXmlNode(nodeText);
				m_model.nodeStructureChanged(treeNode);
				path = path.pathByAddingChild(nodeText);
				m_tree.setSelectionPath(path);
				m_tree.scrollPathToVisible(path);
				m_tree.startEditingAtPath(path);
				return;
			case Node.TEXT_NODE:
				m_tree.startEditingAtPath(path);
				return;
			}
		}
		catch (Exception ex) {
			showError(ex, "Error editing node");
		}
	}

	protected void editAttribute() {
		int row = m_table.getSelectedRow();
		if (row >= 0)
			m_table.editCellAt(row, AttrTableModel.VALUE_COLUMN);
	}

	protected void deleteNode() {
		TreePath path = m_tree.getSelectionPath();
		XmlViewerNode treeNode = getSelectedTreeNode();
		if (treeNode == null)
			return;
		Node node = treeNode.getXmlNode();
		if (node == null)
			return;
		int result = JOptionPane.showConfirmDialog(
			XmlViewer.this, "Delete node "+node.getNodeName()+" ?",
			APP_NAME, JOptionPane.YES_NO_OPTION);
		if (result != JOptionPane.YES_OPTION)
			return;

		try {
			TreeNode treeParent = treeNode.getParent();
			treeNode.remove();
			m_model.nodeStructureChanged(treeParent);
			m_xmlChanged = true;
		}
		catch (Exception ex) {
			showError(ex, "Error deletinging node");
		}
	}

	protected void deleteAttribute() {
		int row = m_table.getSelectedRow();
		if (row < 0)
			return;
		Node node = getSelectedNode();
		if (!(node instanceof Element))
			return;

		String name = (String)m_tableModel.getValueAt(row,
			AttrTableModel.NAME_COLUMN);
		int result = JOptionPane.showConfirmDialog(
			XmlViewer.this, "Delete attribute "+name+" ?",
			APP_NAME, JOptionPane.YES_NO_OPTION);
		if (result != JOptionPane.YES_OPTION)
			return;

		try {
			((Element)node).removeAttribute(name);
			setNodeToTable(node);
			m_xmlChanged = true;
		}
		catch (Exception ex) {
			showError(ex, "Error deletinging attribute");
		}
	}


	protected DefaultMutableTreeNode createTreeNode(Node root) {
		if (!canDisplayNode(root))
			return null;
		XmlViewerNode treeNode = new XmlViewerNode(root);
		NodeList list = root.getChildNodes();
		for (int k=0; k<list.getLength(); k++) {
			Node nd = list.item(k);
			DefaultMutableTreeNode child = createTreeNode(nd);
			if (child != null)
				treeNode.add(child);
		}
		return treeNode;
	}

	protected boolean canDisplayNode(Node node) {
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			return true;
		case Node.TEXT_NODE:
			String text = node.getNodeValue().trim();
			return !(text.equals("") || text.equals("\n") || text.equals("\r\n"));
		}
		return false;
	}

	// NEW
	protected boolean dragNodeOverTree(int screenX, int screenY) {
		Point pt = m_treeScrollPane.getLocationOnScreen();
		int x = screenX - pt.x;
		int y = screenY - pt.y;
		if (!m_treeScrollPane.contains(x, y)) {
			JViewport viewPort = m_treeScrollPane.getViewport();
			int maxHeight = viewPort.getView().getHeight()-viewPort.getHeight();
			if (x > 0 && x < m_treeScrollPane.getWidth() && y < 0) {
				pt = viewPort.getViewPosition();
				pt.y -= 3;
				pt.y = Math.max(0, pt.y);
				pt.y = Math.min(maxHeight, pt.y);
				viewPort.setViewPosition(pt);
			}
			if (x > 0 && x < m_treeScrollPane.getWidth() && y > m_treeScrollPane.getHeight()) {
				pt = viewPort.getViewPosition();
				pt.y += 3;
				pt.y = Math.max(0, pt.y);
				pt.y = Math.min(maxHeight, pt.y);
				viewPort.setViewPosition(pt);
			}
			m_draggingOverNode = null;
			m_tree.repaint();
			return false;
		}

		pt = m_tree.getLocationOnScreen();
		x = screenX - pt.x;
		y = screenY - pt.y;
		TreePath path = m_tree.getPathForLocation(x, y);
		if (path == null) {
			m_draggingOverNode = null;
			m_tree.repaint();
			return false;
		}

		Object obj = path.getLastPathComponent();
		if (obj instanceof XmlViewerNode &&
				((XmlViewerNode)obj).getXmlNode() instanceof Element) {
			m_draggingOverNode = (XmlViewerNode)obj;
			m_tree.scrollPathToVisible(path);
			m_tree.repaint();
			return true;
		}
		else {
			m_draggingOverNode = null;
			m_tree.repaint();
			return false;
		}
	}

	// NEW
	protected void moveNode(XmlViewerNode source, XmlViewerNode target) {
		if (source == null || target == null)
			return;
		if (isChildNode(source, target)) {
			JOptionPane.showMessageDialog(this,
				"Cannot move node to it's child node", APP_NAME,
				JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			// Remove node from old parent
			TreeNode srcParent = source.getParent();
			source.remove();
			m_model.nodeStructureChanged(srcParent);

			// Add node to new parent
			target.addXmlNode(source);
			m_model.nodeStructureChanged(target);

			TreePath path = getTreePathForNode(source);
			m_tree.setSelectionPath(path);
			m_tree.scrollPathToVisible(path);
			m_xmlChanged = true;
		}
		catch (Exception ex) {
			showError(ex, "Error moving node");
		}
	}

	public XmlViewerNode getSelectedTreeNode() {
		TreePath path = m_tree.getSelectionPath();
		if (path == null)
			return null;
		Object obj = path.getLastPathComponent();
		if (!(obj instanceof XmlViewerNode))
			return null;
		return (XmlViewerNode)obj;
	}

	public Node getSelectedNode() {
		XmlViewerNode treeNode = getSelectedTreeNode();
		if (treeNode == null)
			return null;
		return treeNode.getXmlNode();
	}

	public void setNodeToTable(Node node) {
		m_tableModel.setNode(node);
		m_table.tableChanged(new TableModelEvent(m_tableModel));
	}

	public boolean isLegalXmlName(String input) {
		if (input==null || input.length()==0)
			return false;
		if (!(XMLRoutines.isLegalXmlName(input))) {
			JOptionPane.showMessageDialog(this,
				"Invalid XML name", APP_NAME,
				JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

	public void showError(Exception ex, String message) {
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this,
			message, APP_NAME,
			JOptionPane.WARNING_MESSAGE);
	}

    public static void expandTree(JTree tree) {
	    TreeNode root = (TreeNode)tree.getModel().getRoot();
	    TreePath path = new TreePath(root);
	    for (int k = 0; k<root.getChildCount(); k++) {
			TreeNode child = (TreeNode)root.getChildAt(k);
			expandTree(tree, path, child);
		}
	}

    public static void expandTree(JTree tree, TreePath path, TreeNode node) {
		if (path==null || node==null)
			return;
		tree.expandPath(path);
		TreePath newPath = path.pathByAddingChild(node);
	    for (int k = 0; k<node.getChildCount(); k++) {
			TreeNode child = (TreeNode)node.getChildAt(k);
			if (child != null) {
				expandTree(tree, newPath, child);
			}
		}
	}

	// NEW
    public static TreePath getTreePathForNode(TreeNode node) {
		Vector v = new Vector();
		while (node != null) {
			v.insertElementAt(node, 0);
			node = node.getParent();
		}
		return new TreePath(v.toArray());
    }

	// NEW
	public static boolean isChildNode(TreeNode parent, TreeNode node) {
		if (parent == null || node == null)
			return false;
		if (parent.equals(node))
			return true;
		for (int k=0; k<parent.getChildCount(); k++) {
			TreeNode child = parent.getChildAt(k);
			if (isChildNode(child, node))
				return true;
		}
		return false;
	}

	public static void main(String argv[]) {
		XmlViewer frame = new XmlViewer();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
	}

	class XmlEditorListener implements CellEditorListener {
		public void editingStopped(ChangeEvent e) {
			String value = m_treeEditor.getCellEditorValue().toString();
			if (m_editingNode != null)
				m_editingNode.setNodeValue(value);
			TreePath path = m_tree.getSelectionPath();
			if (path != null) {
				DefaultMutableTreeNode treeNode =
					(DefaultMutableTreeNode)path.getLastPathComponent();
				treeNode.setUserObject(m_editingNode);
				m_model.nodeStructureChanged(treeNode);
			}
			m_xmlChanged = true;
			m_editingNode = null;
		}

		public void editingCanceled(ChangeEvent e) {
			m_editingNode = null;
		}
	}

	class AttrTableModel extends AbstractTableModel {
		public static final int NAME_COLUMN = 0;
		public static final int VALUE_COLUMN = 1;

		protected Node m_node;
		protected NamedNodeMap m_attrs;

		public void setNode(Node node) {
			m_node = node;
			m_attrs = node==null ? null : node.getAttributes();
		}

		public Node getNode() {
			return m_node;
		}

		public int getRowCount() {
			if (m_attrs == null)
				return 0;
			return m_attrs.getLength();
		}

		public int getColumnCount() {
			return 2;
		}

		public String getColumnName(int nCol) {
			return nCol==NAME_COLUMN ? "Attribute" : "Value";
		}

		public Object getValueAt(int nRow, int nCol) {
			if (m_attrs == null || nRow < 0 || nRow>=getRowCount())
				return "";
			Attr attr = (Attr)m_attrs.item(nRow);
			if (attr == null)
				return "";
			switch (nCol) {
			case NAME_COLUMN:
				return attr.getName();
			case VALUE_COLUMN:
				return attr.getValue();
			}
			return "";
		}

		public boolean isCellEditable(int nRow, int nCol) {
			return (nCol==VALUE_COLUMN);
		}

		public void setValueAt(Object value, int nRow, int nCol) {
			if (nRow < 0 || nRow>=getRowCount())
				return;
			if (!(m_node instanceof Element))
				return;
			String name = getValueAt(nRow, NAME_COLUMN).toString();
			((Element)m_node).setAttribute(name, value.toString());
			m_xmlChanged = true;
		}
	}

	// NEW
	class TreeMouseListener extends MouseInputAdapter {

		private boolean m_isDragging = false;

		public void mousePressed(MouseEvent evt){
			XmlViewerNode treeNode = getSelectedTreeNode();
			if (treeNode != null && treeNode.getXmlNode() instanceof Element)
				m_draggingTreeNode = treeNode;
			m_draggingOverNode = null;
		}

		public void mouseReleased(MouseEvent evt){
			if (m_draggingTreeNode == null)
				return;
			m_tree.setCursor(Cursor.getDefaultCursor());

			moveNode(m_draggingTreeNode, m_draggingOverNode);

			m_isDragging = false;
			m_draggingTreeNode = null;
			m_draggingOverNode = null;
			m_tree.repaint();
		}

		public void mouseDragged(MouseEvent evt) {
			if (m_draggingTreeNode == null)
				return;
			if (!m_isDragging) {	// Update cursor only on move, not on click
				m_isDragging = true;
				m_tree.setCursor(m_dragCursor);
			}
			Component src = (Component)evt.getSource();
			Point p1 = src.getLocationOnScreen();
			int x = p1.x + evt.getX();
			int y = p1.y + evt.getY();
			if (dragNodeOverTree(x, y))
				m_tree.setCursor(m_dragCursor);
			else
				m_tree.setCursor(m_nodropCursor);
		}
	}
}

class XmlViewerNode extends DefaultMutableTreeNode {
	public XmlViewerNode(Node node) {
		super(node);
	}
	public Node getXmlNode() {
		Object obj = getUserObject();
		if (obj instanceof Node)
			return (Node)obj;
		return null;
	}

	public void addXmlNode(XmlViewerNode child)
		throws Exception {
		Node node = getXmlNode();
		if (node == null)
			throw new Exception(
				"Corrupted XML node");
		node.appendChild(child.getXmlNode());
		add(child);
	}

	public void remove() throws Exception {
		Node node = getXmlNode();
		if (node == null)
			throw new Exception(
				"Corrupted XML node");
		Node parent = node.getParentNode();
		if (parent == null)
			throw new Exception(
				"Cannot remove root node");
		TreeNode treeParent = getParent();
		if (!(treeParent instanceof DefaultMutableTreeNode))
			throw new Exception(
				"Cannot remove tree node");
		parent.removeChild(node);
		((DefaultMutableTreeNode)treeParent).remove(this);
	}

	public String toString () {
		Node node = getXmlNode();
		if (node == null)
			return getUserObject().toString();
		StringBuffer sb = new StringBuffer();
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			sb.append('<');
			sb.append(node.getNodeName());
			sb.append('>');
			break;
		case Node.TEXT_NODE:
			sb.append(node.getNodeValue());
			break;
		}
		return sb.toString();
	}
}

class SimpleFilter
	extends javax.swing.filechooser.FileFilter {

	private String m_description = null;
	private String m_extension = null;

	public SimpleFilter(String extension, String description) {
		m_description = description;
		m_extension = "."+extension.toLowerCase();
	}

		public String getDescription() {
		return m_description;
	}

	public boolean accept(File f) {
		if (f == null)
			return false;
		if (f.isDirectory())
			return true;
		return f.getName().toLowerCase().endsWith(m_extension);
	}
}

class XMLRoutines {

	public static void write(Document doc, Writer out) throws Exception {
		write(doc.getDocumentElement(), out);
	}

	public static void write(Node node, Writer out) throws Exception {
		if (node==null || out==null)
			return;

		int type = node.getNodeType();
		switch (type) {
		case Node.DOCUMENT_NODE:
			write(((Document)node).getDocumentElement(), out);
			out.flush();
			break;

		case Node.ELEMENT_NODE:
			out.write('<');
			out.write(node.getNodeName());
			NamedNodeMap attrs = node.getAttributes();
			for (int k = 0; k< attrs.getLength(); k++ ) {
				Node attr = attrs.item(k);
				out.write(' ');
				out.write(attr.getNodeName());
				out.write("=\"");
				out.write(attr.getNodeValue());
				out.write('"');
			}
			out.write('>');
			break;

		case Node.ENTITY_REFERENCE_NODE:
			out.write('&');
			out.write(node.getNodeName());
			out.write(';');
			break;

		// print cdata sections
		case Node.CDATA_SECTION_NODE:
			out.write("<![CDATA[");
			out.write(node.getNodeValue());
			out.write("]]>");
			break;

		// print text
		case Node.TEXT_NODE:
			out.write(node.getNodeValue());
			break;

		// print processing instruction
		case Node.PROCESSING_INSTRUCTION_NODE:
			out.write("<?");
			out.write(node.getNodeName());
			String data = node.getNodeValue();
			if ( data != null && data.length() > 0 ) {
				out.write(' ');
				out.write(data);
			}
			out.write("?>");
			break;

		default:
			out.write("<TYPE="+type);
			out.write(node.getNodeName());
			out.write("?>");
			break;
		}

		NodeList children = node.getChildNodes();
		if ( children != null ) {
	 		for ( int k = 0; k<children.getLength(); k++ ) {
				write(children.item(k), out);
			}
		}

		if (node.getNodeType() == Node.ELEMENT_NODE ) {
			out.write("</");
			out.write(node.getNodeName());
			out.write('>');
		}
		out.flush();
	}

	public static boolean isLegalXmlName(String input) {
		if (input == null || input.length() == 0)
			return false;
		for (int k=0; k<input.length(); k++) {
			char ch = input.charAt(k);
			if (
				Character.isLetter(ch) ||
				(ch == '_') || (ch == ':') ||
				(k>0 &&
					(Character.isDigit(ch) ||
					(ch == '.') || (ch == '-')
					)
				)
			)
				continue;
			return false;
		}
		return true;
	}
}

