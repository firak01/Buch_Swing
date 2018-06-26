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
import java.util.*;
import java.io.*;
import java.text.*;
import java.util.Date;
import java.sql.*;
import java.awt.print.*;	// NEW

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class StocksTable extends JFrame
	implements Printable {		// NEW

	protected JTable m_table;
	protected StockTableData m_data;
	protected JLabel m_title;

	protected int m_maxNumPage = 1;	// NEW

	public StocksTable() {
		super("Stocks Table");
		setSize(600, 300);

		UIManager.put("Table.focusCellHighlightBorder",
			new LineBorder(Color.black, 0));

		m_data = new StockTableData();

		m_title = new JLabel(m_data.getTitle(),
			new ImageIcon("money.gif"), SwingConstants.CENTER);
		m_title.setFont(new Font("Helvetica",Font.PLAIN,24));
		getContentPane().add(m_title, BorderLayout.NORTH);

		m_table = new JTable();
		m_table.setAutoCreateColumnsFromModel(false);
		m_table.setModel(m_data);

		for (int k = 0; k < m_data.getColumnCount(); k++) {
			DefaultTableCellRenderer renderer = new
				ColoredTableCellRenderer();
			renderer.setHorizontalAlignment(
				StockTableData.m_columns[k].m_alignment);
			TableColumn column = new TableColumn(k,
				StockTableData.m_columns[k].m_width, renderer, null);
			column.setHeaderRenderer(createDefaultRenderer());
			m_table.addColumn(column);
		}

		JTableHeader header = m_table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(new ColumnListener());
		header.setReorderingAllowed(true);

		m_table.getColumnModel().addColumnModelListener(m_data);

		setJMenuBar(createMenuBar());

		JScrollPane ps = new JScrollPane();
		ps.getViewport().setBackground(m_table.getBackground());
		ps.getViewport().add(m_table);
		getContentPane().add(ps, BorderLayout.CENTER);
	}

	protected TableCellRenderer createDefaultRenderer() {
		DefaultTableCellRenderer label = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value,
							 boolean isSelected, boolean hasFocus, int row, int column) {
				if (table != null) {
					JTableHeader header = table.getTableHeader();
					if (header != null) {
						setForeground(header.getForeground());
						setBackground(header.getBackground());
						setFont(header.getFont());
					}
				}

				setText((value == null) ? "" : value.toString());
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));
				return this;
			}
		};
		label.setHorizontalAlignment(JLabel.CENTER);
		return label;
	}

	protected JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu mFile = new JMenu("File");
		mFile.setMnemonic('f');

		JMenuItem mData = new JMenuItem("Retrieve Data...");
		mData.setMnemonic('r');
		ActionListener lstData = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				retrieveData();
			}
		};
		mData.addActionListener(lstData);
		mFile.add(mData);
		mFile.addSeparator();

		// NEW
		JMenuItem mPrint = new JMenuItem("Print...");
		mPrint.setMnemonic('p');
		ActionListener lstPrint = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread runner = new Thread() {
					public void run() {
						printData();
					}
				};
				runner.start();
			}
		};
		mPrint.addActionListener(lstPrint);
		mFile.add(mPrint);

		JMenuItem mPreviewP = new JMenuItem("Print Preview: PORTRAIT");
		mPreviewP.setMnemonic('o');
		ActionListener lstPreview = new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e)
			{
                            Thread runner = new Thread() {
                              public void run() {
				setCursor(Cursor.getPredefinedCursor(
					Cursor.WAIT_CURSOR));
				new PrintPreview(StocksTable.this, 
					m_title.getText()+" preview",
                                        PageFormat.PORTRAIT);
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
                              }
                            };
                            runner.start();
			}
		};
		mPreviewP.addActionListener(lstPreview);
		mFile.add(mPreviewP);

		JMenuItem mPreviewL = new JMenuItem("Print Preview: LANDSCAPE");
		mPreviewL.setMnemonic('l');
		lstPreview = new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e)
			{
                            Thread runner = new Thread() {
                              public void run() {
				setCursor(Cursor.getPredefinedCursor(
					Cursor.WAIT_CURSOR));
				new PrintPreview(StocksTable.this, 
					m_title.getText()+" preview",
                                        PageFormat.LANDSCAPE);
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
                              }
                            };
                            runner.start();
			}
		};
		mPreviewL.addActionListener(lstPreview);
		mFile.add(mPreviewL);
		mFile.addSeparator();

		JMenuItem mExit = new JMenuItem("Exit");
		mExit.setMnemonic('x');
		ActionListener lstExit = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		mExit.addActionListener(lstExit);
		mFile.add(mExit);
		menuBar.add(mFile);

		JMenu mView = new JMenu("View");
		mView.setMnemonic('v');
		TableColumnModel model = m_table.getColumnModel();
		for (int k = 0; k < m_data.getColumnCount(); k++) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(
				StockTableData.m_columns[k].m_title);
			item.setSelected(true);
			TableColumn column = model.getColumn(k);
			item.addActionListener(new ColumnKeeper(column,
				StockTableData.m_columns[k]));
			mView.add(item);
		}
		menuBar.add(mView);

		return menuBar;
	}

	// NEW
	public void printData() {
		try {
			PrinterJob prnJob = PrinterJob.getPrinterJob();
			prnJob.setPrintable(this);
			if (!prnJob.printDialog())
				return;
			m_maxNumPage = 1;
			prnJob.print();
		}
		catch (PrinterException e) {
			e.printStackTrace();
		}
	}

	// NEW
	public int print(Graphics pg, PageFormat pageFormat,
	 int pageIndex) throws PrinterException {
		if (pageIndex >= m_maxNumPage)
			return NO_SUCH_PAGE;

		pg.translate((int)pageFormat.getImageableX(),
			(int)pageFormat.getImageableY());
		int wPage = 0;
		int hPage = 0;
		if (pageFormat.getOrientation() == pageFormat.PORTRAIT) {
			wPage = (int)pageFormat.getImageableWidth();
			hPage = (int)pageFormat.getImageableHeight();
		}
		else {
			wPage = (int)pageFormat.getImageableWidth();
			wPage += wPage/2;
			hPage = (int)pageFormat.getImageableHeight();
			pg.setClip(0,0,wPage,hPage);
		}

		int y = 0;
		pg.setFont(m_title.getFont());
		pg.setColor(Color.black);
		Font fn = pg.getFont();
		FontMetrics fm = pg.getFontMetrics();
		y += fm.getAscent();
		pg.drawString(m_title.getText(), 0, y);
		y += 20; // space between title and table headers

		Font headerFont = m_table.getFont().deriveFont(Font.BOLD);
		pg.setFont(headerFont);
		fm = pg.getFontMetrics();

		TableColumnModel colModel = m_table.getColumnModel();
		int nColumns = colModel.getColumnCount();
		int x[] = new int[nColumns];
		x[0] = 0;

		int h = fm.getAscent();
		y += h; // add ascent of header font because of baseline
						// positioning (see figure 2.10)

		int nRow, nCol;
		for (nCol=0; nCol<nColumns; nCol++) {
			TableColumn tk = colModel.getColumn(nCol);
			int width = tk.getWidth();
			if (x[nCol] + width > wPage) {
				nColumns = nCol;
				break;
			}
			if (nCol+1<nColumns)
				x[nCol+1] = x[nCol] + width;
			String title = (String)tk.getIdentifier();
			pg.drawString(title, x[nCol], y);
		}

		pg.setFont(m_table.getFont());
		fm = pg.getFontMetrics();

		int header = y;
		h = fm.getHeight();
		int rowH = Math.max((int)(h*1.5), 10);
		int rowPerPage = (hPage-header)/rowH;
		m_maxNumPage = Math.max((int)Math.ceil(m_table.getRowCount()/
			(double)rowPerPage), 1);

		TableModel tblModel = m_table.getModel();
		int iniRow = pageIndex*rowPerPage;
		int endRow = Math.min(m_table.getRowCount(),
			iniRow+rowPerPage);

		for (nRow=iniRow; nRow<endRow; nRow++) {
			y += h;
			for (nCol=0; nCol<nColumns; nCol++) {
				int col = m_table.getColumnModel().getColumn(nCol).getModelIndex();
				Object obj = m_data.getValueAt(nRow, col);
				String str = obj.toString();
				if (obj instanceof ColorData)
					pg.setColor(((ColorData)obj).m_color);
				else
					pg.setColor(Color.black);
					pg.drawString(str, x[nCol], y);
			}
		}

		System.gc();
		return PAGE_EXISTS;
	}

	public void retrieveData() {
		Runnable updater = new Runnable() {
			public void run() {
				SimpleDateFormat frm = new SimpleDateFormat("MM/dd/yyyy");
				String currentDate = frm.format(m_data.m_date);
				String result = (String)JOptionPane.showInputDialog(StocksTable.this,
					"Please enter date in form mm/dd/yyyy:", "Input",
					JOptionPane.INFORMATION_MESSAGE, null, null,
					currentDate);
				if (result==null)
					return;

				java.util.Date date = null;
				try {
					date = frm.parse(result);
				}
				catch (java.text.ParseException ex) {
					date = null;
				}

				if (date == null) {
					JOptionPane.showMessageDialog(StocksTable.this,
						result+" is not a valid date",
						"Warning", JOptionPane.WARNING_MESSAGE);
					return;
				}

				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					m_data.retrieveData(date);
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(StocksTable.this,
						"Error retrieving data:\n"+ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				m_title.setText(m_data.getTitle());
				m_table.tableChanged(new TableModelEvent(m_data));
			}
		};
		SwingUtilities.invokeLater(updater);
	}

	public static void main(String argv[]) {
		StocksTable frame = new StocksTable();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	class ColumnListener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			TableColumnModel colModel = m_table.getColumnModel();
			int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

			if (modelIndex < 0)
				return;
			if (m_data.m_sortCol == modelIndex)
				m_data.m_sortAsc = !m_data.m_sortAsc;
			else
				m_data.m_sortCol = modelIndex;

			for (int i=0; i < m_data.getColumnCount(); i++) {
				TableColumn column = colModel.getColumn(i);
				int index = column.getModelIndex();
				JLabel renderer = (JLabel)column.getHeaderRenderer();
				renderer.setIcon(m_data.getColumnIcon(index));
			}
			m_table.getTableHeader().repaint();

			m_data.sortData();
			m_table.tableChanged(new TableModelEvent(m_data));
			m_table.repaint();
		}
	}

	class ColumnKeeper implements ActionListener {
		protected TableColumn m_column;
		protected ColumnData	m_colData;

		public ColumnKeeper(TableColumn column, ColumnData	colData) {
			m_column = column;
			m_colData = colData;
		}

		public void actionPerformed(ActionEvent e) {
			JLabel renderer = (JLabel)m_column.getHeaderRenderer();
			renderer.setIcon(null);
			JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
			TableColumnModel model = m_table.getColumnModel();
			if (item.isSelected()) {
				model.addColumn(m_column);
			}
			else {
				model.removeColumn(m_column);
			}
			m_table.tableChanged(new TableModelEvent(m_data));
			m_table.repaint();
		}
	}
}

class ColoredTableCellRenderer extends DefaultTableCellRenderer {

	public void setValue(Object value) {
		if (value instanceof ColorData) {
			ColorData cvalue = (ColorData)value;
			setForeground(cvalue.m_color);
			setText(cvalue.m_data.toString());
		}
		else if (value instanceof IconData) {
			IconData ivalue = (IconData)value;
			setIcon(ivalue.m_icon);
			setText(ivalue.m_data.toString());
		}
		else
			super.setValue(value);
	}
}

class ColorData {

	public Color	m_color;
	public Object m_data;
	public static Color GREEN = new Color(0, 128, 0);
	public static Color RED = Color.red;

	public ColorData(Color color, Object data) {
		m_color = color;
		m_data	= data;
	}

	public ColorData(Double data) {
		m_color = data.doubleValue() >= 0 ? GREEN : RED;
		m_data	= data;
	}

	public String toString() {
		return m_data.toString();
	}
}

class IconData {

	public ImageIcon	m_icon;
	public Object m_data;

	public IconData(ImageIcon icon, Object data) {
		m_icon = icon;
		m_data = data;
	}

	public String toString() {
		return m_data.toString();
	}
}

class StockData {

	public static ImageIcon ICON_UP = new ImageIcon("ArrUp.gif");
	public static ImageIcon ICON_DOWN = new ImageIcon("ArrDown.gif");
	public static ImageIcon ICON_BLANK = new ImageIcon("blank.gif");

	public IconData	m_symbol;
	public String		m_name;
	public Double		m_last;
	public Double		m_open;
	public ColorData m_change;
	public ColorData m_changePr;
	public Long			m_volume;

	public StockData(String symbol, String name, double last,
	 double open, double change, double changePr, long volume) {
		m_symbol = new IconData(getIcon(change), symbol);
		m_name = name;
		m_last = new Double(last);
		m_open = new Double(open);
		m_change = new ColorData(new Double(change));
		m_changePr = new ColorData(new Double(changePr));
		m_volume = new Long(volume);
	}

	public static ImageIcon getIcon(double change) {
		return (change>0 ? ICON_UP : (change<0 ? ICON_DOWN : ICON_BLANK));
	}
}

class ColumnData {

	public String	m_title;
	public int		 m_width;
	public int		 m_alignment;

	public ColumnData(String title, int width, int alignment) {
		m_title = title;
		m_width = width;
		m_alignment = alignment;
	}
}

class StockTableData extends AbstractTableModel
	implements TableColumnModelListener{

	static final public ColumnData m_columns[] = {
		new ColumnData( "Symbol", 100, JLabel.LEFT ),
		new ColumnData( "Name", 160, JLabel.LEFT ),
		new ColumnData( "Last", 100, JLabel.RIGHT ),
		new ColumnData( "Open", 100, JLabel.RIGHT ),
		new ColumnData( "Change", 100, JLabel.RIGHT ),
		new ColumnData( "Change %", 100, JLabel.RIGHT ),
		new ColumnData( "Volume", 100, JLabel.RIGHT )
	};

	public static ImageIcon COLUMN_UP = new ImageIcon("SortUp.gif");
	public static ImageIcon COLUMN_DOWN = new ImageIcon("SortDown.gif");

	protected SimpleDateFormat m_frm;
	protected NumberFormat m_volumeFormat;
	protected Vector m_vector;
	protected Date	 m_date;
	protected int m_columnsCount = m_columns.length;

	public int		m_sortCol = 0;
	public boolean m_sortAsc = true;

	public StockTableData() {
		m_frm = new SimpleDateFormat("MM/dd/yyyy");
		m_volumeFormat = NumberFormat.getInstance();
		m_volumeFormat.setGroupingUsed(true);
		m_volumeFormat.setMaximumFractionDigits(0);

		m_vector = new Vector();
		setDefaultData();
	}

	public void setDefaultData() {
		try {
			m_date = m_frm.parse("12/18/2004");
		}
		catch (java.text.ParseException ex) {
			m_date = null;
		}

		m_vector.removeAllElements();
		m_vector.addElement(new StockData("ORCL", "Oracle Corp.",
			23.6875, 25.375, -1.6875, -6.42, 24976600));
		m_vector.addElement(new StockData("EGGS", "Egghead.com",
			17.25, 17.4375, -0.1875, -1.43, 2146400));
		m_vector.addElement(new StockData("T", "AT&T",
			65.1875, 66, -0.8125, -0.10, 554000));
		m_vector.addElement(new StockData("LU", "Lucent Technology",
			64.625, 59.9375, 4.6875, 9.65, 29856300));
		m_vector.addElement(new StockData("FON", "Sprint",
			104.5625, 106.375, -1.8125, -1.82, 1135100));
		m_vector.addElement(new StockData("ENML", "Enamelon Inc.",
			4.875, 5, -0.125, 0, 35900));
		m_vector.addElement(new StockData("CPQ", "Compaq Computers",
			30.875, 31.25, -0.375, -2.18, 11853900));
		m_vector.addElement(new StockData("MSFT", "Microsoft Corp.",
			94.0625, 95.1875, -1.125, -0.92, 19836900));
		m_vector.addElement(new StockData("DELL", "Dell Computers",
			46.1875, 44.5, 1.6875, 6.24, 47310000));
		m_vector.addElement(new StockData("SUNW", "Sun Microsystems",
			140.625, 130.9375, 10, 10.625, 17734600));
		m_vector.addElement(new StockData("IBM", "Intl. Bus. Machines",
			183, 183.125, -0.125, -0.51, 4371400));
		m_vector.addElement(new StockData("HWP", "Hewlett-Packard",
			70, 71.0625, -1.4375, -2.01, 2410700));
		m_vector.addElement(new StockData("UIS", "Unisys Corp.",
			28.25, 29, -0.75, -2.59, 2576200));
		m_vector.addElement(new StockData("SNE", "Sony Corp.",
			96.1875, 95.625, 1.125, 1.18, 330600));
		m_vector.addElement(new StockData("NOVL", "Novell Inc.",
			24.0625, 24.375, -0.3125, -3.02, 6047900));
		m_vector.addElement(new StockData("HIT", "Hitachi, Ltd.",
			78.5, 77.625, 0.875, 1.12, 49400));

		sortData();
	}

	public int getRowCount() {
		return m_vector==null ? 0 : m_vector.size();
	}

	public int getColumnCount() {
		return m_columnsCount;
	}

	public String getColumnName(int column) {
		return m_columns[column].m_title;
	}

	public Icon getColumnIcon(int column) {
		if (column==m_sortCol)
			return m_sortAsc ? COLUMN_UP : COLUMN_DOWN;
		return null;
	}

	public boolean isCellEditable(int nRow, int nCol) {
		return false;
	}

	public Object getValueAt(int nRow, int nCol) {
		if (nRow < 0 || nRow>=getRowCount())
			return "";
		StockData row = (StockData)m_vector.elementAt(nRow);
		switch (nCol) {
			case 0: return row.m_symbol;
			case 1: return row.m_name;
			case 2: return row.m_last;
			case 3: return row.m_open;
			case 4: return row.m_change;
			case 5: return row.m_changePr;
			case 6: return m_volumeFormat.format(row.m_volume);
		}
		return "";
	}

	public String getTitle() {
		if (m_date==null)
			return "Stock Quotes";
		return "Stock Quotes at "+m_frm.format(m_date);
	}

	public void sortData() {
		Collections.sort(m_vector, new
			StockComparator(m_sortCol, m_sortAsc));
	}

	static final String QUERY = "SELECT data.symbol, symbols.name, "+
		"data.last, data.open, data.change, data.changeproc, "+
		"data.volume FROM DATA INNER JOIN SYMBOLS "+
		"ON DATA.symbol = SYMBOLS.symbol WHERE "+
		"month(data.date1)=? AND day(data.date1)=?"+
		" AND year(data.date1)=?";


	public void retrieveData(Date date)
		throws SQLException, ClassNotFoundException {

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		int month = calendar.get(Calendar.MONTH)+1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int year = calendar.get(Calendar.YEAR);

		m_date = date;
		m_vector = new Vector();
		Connection conn = null;
		PreparedStatement pst = null;
		try {
			// Load the JDBC-ODBC bridge driver
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			conn = DriverManager.getConnection(
				"jdbc:odbc:Market", "admin", "");

			pst = conn.prepareStatement(QUERY);
			pst.setInt(1, month);
			pst.setInt(2, day);
			pst.setInt(3, year);
			ResultSet results = pst.executeQuery();

			while (results.next()) {
				String	symbol = results.getString(1);
				String	name = results.getString(2);
				double	last = results.getDouble(3);
				double	open = results.getDouble(4);
				double	change = results.getDouble(5);
				double	changePr = results.getDouble(6);
				long volume = results.getLong(7);
				m_vector.addElement(new StockData(symbol, name, last,
					open, change, changePr, volume));
			}
			Collections.sort(m_vector,
				new StockComparator(m_sortCol, m_sortAsc));
		}
		finally {
			if (pst != null)
				pst.close();
			if (conn != null)
				conn.close();
		}
	}

	// TableColumnModelListener implementation

	public void columnAdded(TableColumnModelEvent e) {
		m_columnsCount++;
	}

	public void columnRemoved(TableColumnModelEvent e) {
		m_columnsCount--;
		if (m_sortCol >= m_columnsCount)
			m_sortCol = -1;
	}

	public void columnMarginChanged(ChangeEvent e) {}
	public void columnMoved(TableColumnModelEvent e) {}
	public void columnSelectionChanged(ListSelectionEvent e) {}
}

class StockComparator implements Comparator {

	protected int		 m_sortCol;
	protected boolean m_sortAsc;

	public StockComparator(int sortCol, boolean sortAsc) {
		m_sortCol = sortCol;
		m_sortAsc = sortAsc;
	}

	public int compare(Object o1, Object o2) {
		if(!(o1 instanceof StockData) || !(o2 instanceof StockData))
			return 0;
		StockData s1 = (StockData)o1;
		StockData s2 = (StockData)o2;
		int result = 0;
		double d1, d2;
		switch (m_sortCol) {
			case 0:		// symbol
				String str1 = (String)s1.m_symbol.m_data;
				String str2 = (String)s2.m_symbol.m_data;
				result = str1.compareTo(str2);
				break;
			case 1:		// name
				result = s1.m_name.compareTo(s2.m_name);
				break;
			case 2:		// last
				d1 = s1.m_last.doubleValue();
				d2 = s2.m_last.doubleValue();
				result = d1<d2 ? -1 : (d1>d2 ? 1 : 0);
				break;
			case 3:		// open
				d1 = s1.m_open.doubleValue();
				d2 = s2.m_open.doubleValue();
				result = d1<d2 ? -1 : (d1>d2 ? 1 : 0);
				break;
			case 4:		// change
				d1 = ((Double)s1.m_change.m_data).doubleValue();
				d2 = ((Double)s2.m_change.m_data).doubleValue();
				result = d1<d2 ? -1 : (d1>d2 ? 1 : 0);
				break;
			case 5:		// change %
				d1 = ((Double)s1.m_changePr.m_data).doubleValue();
				d2 = ((Double)s2.m_changePr.m_data).doubleValue();
				result = d1<d2 ? -1 : (d1>d2 ? 1 : 0);
				break;
			case 6:		// volume
				long l1 = s1.m_volume.longValue();
				long l2 = s2.m_volume.longValue();
				result = l1<l2 ? -1 : (l1>l2 ? 1 : 0);
				break;
		}

		if (!m_sortAsc)
			result = -result;
		return result;
	}

	public boolean equals(Object obj) {
		if (obj instanceof StockComparator) {
			StockComparator compObj = (StockComparator)obj;
			return (compObj.m_sortCol==m_sortCol) &&
				(compObj.m_sortAsc==m_sortAsc);
		}
		return false;
	}
}

