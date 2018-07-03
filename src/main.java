import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import dao.BaseDao;
import model.ConstraintStru;
import model.QueriesStru;
import model.RandomMarkov;
import model.TableStru;
import utils.ConstraintRewrite2;
import utils.JdbcUtils;

public class Main {
	private JComboBox dbBox;
	private JTextField addrText;
	private JTextField portText;
	private JLabel dbLabel;
	private JLabel addrLabel;
	private JLabel portLabel;
	private JLabel userLabel;
	private JTextField userText;
	private JLabel psdLabel;
	private JTextField psdText;
	private JLabel consLabel;
	private JTextArea consText;
	private JLabel queryLabel;
	private JTextArea queryText;
	private JLabel errorLabel;
	private JTextArea outputText;
	private JPanel outputPanel;
	private JPanel inputPanel;
	private JPanel configPanel;
	private JPanel MainPanel;
	private JButton freshBtn;
	private JButton queryBtn;
	private JScrollPane ouputScroll;
	private JTextField errorText;
	private JLabel confidenceLabel;
	private JTextField confidenceText;
	private JPanel controlPanel;

	private String address = "localhost";
	public String dbName = "cqa";
	public String port = "5432";
	public String usrName = "postgres";
	public String psw = "123";
	private String constraints = "reader(a,b,c,d,e,f),reader'(g,h,c,i,j,k) -: [ false |a=g]";
	private String sqls = "SELECT  *\n" + "FROM  reader\n" + "WHERE rid = '4' and lastname = 'lastname_910_';";

	private float epsilon = 0.1f;
	private float theta = 0.01f;
	private static Connection c;

	private static HashMap<String, ArrayList<String>> tableMap;
	private ConstraintRewrite2 constraintRewrite;
	private static JdbcUtils jdbcUtils = new JdbcUtils();
	private static BaseDao baseDao = new BaseDao();

	public static void main(String[] args) {
		Main main = new Main();
		JFrame frame = new JFrame("Main");
		frame.setContentPane(main.MainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				try {
					c.close();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		main.initGUI(); // init the components
	}

	public Main() {
		Main main = new Main();
		c = baseDao.connectDB(); // init the connection of the database
		tableMap = baseDao.getTableSchema(c); // the schema of the database
		String sql = "SELECT  *\n" + "FROM  reader\n" + "WHERE rid = '4' and lastname = 'lastname_910_';";
		try {
			main.sampleFramework(constraints.trim(), sql, 0);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void initGUI() {
		addrText.setText(address);
		portText.setText(port);
		userText.setText(usrName);
		psdText.setText(psw);
		consText.setText(constraints);
		errorText.setText(String.valueOf(epsilon));
		confidenceText.setText(String.valueOf(theta));
		queryText.setText(sqls);
		ArrayList<String> dbLst = baseDao.getDbName(c);
		for (String name : dbLst) {
			dbBox.addItem(name);
		}

		// get the new user input parameter and create new connection
		freshBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				address = addrText.getText().trim();
				port = portText.getText().trim();
				dbName = dbBox.getSelectedItem().toString().trim();
				usrName = userText.getText().trim();
				psw = psdText.getText().trim();
				c = baseDao.connectDB(address, port, dbName, usrName, psw);
				tableMap = baseDao.getTableSchema(c);
			}
		});
		// execute query button
		queryBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				// invoke the query analysis to get the regarding query tables

				epsilon = Float.valueOf(errorText.getText().trim()); // error bound set by user
				theta = Float.valueOf(confidenceText.getText().trim()); // confidence set by user

				try {
					int sequence = 0; // record which constraint
					for (String constraint : consText.getText().split(";")) {
						String sql = queryText.getText();
						sampleFramework(constraint.trim(), sql, sequence);
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});

	}

	/********
	 *
	 * @param constraint
	 * @param
	 * @return
	 * @throws SQLException
	 *             find the violation tuples and regarding table, and save them in
	 *             the del_ table
	 */
	public ConstraintStru violationCheck(String constraint, int sequence) throws SQLException {

		constraintRewrite = new ConstraintRewrite2();

		/**********
		 * if has two "reader" need to write as "reader","reader'" ex.
		 * reader(firstname,lastname,rid,born,gender,phone),reader'(firstname,lastname,rid,born,gender,phone)
		 * -: [ false |reader.rid = reader'.rid ,reader.firstname = reader'.firtname]
		 * reader(a,b,c,d,e,f), reader'(g,h,c,i,j,k),.... -: [ false |a=g,...]
		 *********/
		// "reader(a,b,c,d,e,f),reader'(g,h,c,i,j,k) -: [ false |a=g]"
		constraintRewrite.parse(constraint);

		/*********
		 * check the constraint format
		 *********/
		if (!constraintRewrite.tbFormatCheck(tableMap)) {
			System.err.println("constraint table schema error");
			return null;
		}

		/*********
		 * constraint rewrite and get violation tuples
		 *********/

		// { ........ }
		// <"att1 att2 att3",ArrayList<tuples>>
		ArrayList<String> consTbLst = new ArrayList<>(); // all the regarding table in a constraint
		for (TableStru tbStru : constraintRewrite.getTableList()) {
			consTbLst.add(tbStru.getTableName());
		}
		// { ........ }
		String[] depSqlArray = constraintRewrite.rewrite(tableMap);
		// String[ sql , attName1,attName2...]
		ArrayList<HashMap> vioTupleMap = constraintRewrite.getVioTuples(depSqlArray, c, tableMap);

		ConstraintStru constraintStru = new ConstraintStru(vioTupleMap, depSqlArray);

		outputText.append(depSqlArray[0] + "\n");

		outputText.repaint();
		outputText.updateUI();
		outputText.validate();

		return constraintStru;
	}

	public void sampleFramework(String constraint, String sql, int sequence) throws SQLException {
		BaseDao basedao = new BaseDao();
		int count = 0;

		Random random = new Random(System.currentTimeMillis());
		int m = (int) ((1 / (2 * epsilon)) * Math.log(2 / theta));
		ArrayList<String> tableNames = basedao.getTableNames(c);

		ConstraintStru constraintStru = violationCheck(constraint, sequence);

		try {
			// Run Row(SQL(theta)) for each constraint

			for (int i = 0; i < 1; i++) {

				ArrayList<TableStru> tableList = constraintRewrite.getTableList();

				RandomMarkov randomMarkov = new RandomMarkov(constraintStru, random, tableList, tableMap);

				// create delete table for each table
				jdbcUtils.createDeleteTbale(c, tableNames);

				// markov chain provide the tuples which to delete next
				while (randomMarkov.hasNext()) {

					HashMap tuple = randomMarkov.next();
					String tableName = (String) tuple.get("tableName");
					// reader_rid,reader_firstname ...reader'_rid
					jdbcUtils.updateTable(tuple, "del_" + tableName, c);

				}

				// create new view for each table which does not contain deleted tuple
				jdbcUtils.CreateDeleteView_NOTEXIST(c, tableNames);

				// get the structure of query
				QueriesStru stru = jdbcUtils.splitQuery(sql);

				// check if the query is satisfied
				boolean flag = jdbcUtils.queryRewrite(stru, c);

				// if yes, count plus 1
				if (flag) {
					count++;
				}

				// drop tables and views created in this turn
				jdbcUtils.DropDView(c, tableNames);
				jdbcUtils.DropDTable(c, tableNames);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		outputText.append(count + "/" + m);

		outputText.repaint();
		outputText.updateUI();
		outputText.validate();

	}

}
