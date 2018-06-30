import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import control.ConstraintRewrite2;
import control.PostgreSQLJDBC6;
import model.ConstraintStru2;
import model.RandomMarkov;
import model.TableStru;

public class MainTest {

	private static String address = "localhost";
	public static String dbName = "cqa";
	public static String port = "5432";
	public static String usrName = "postgres";
	public static String psw = "";
	public String sqlPath = "/Users/qq/Documents/GitHub/DPMT/sql/example.sql";
	private static String constraints = "reader(a,b,c,d,e,f),reader'(g,h,c,i,j,k) -: [ false |a=g]";
	private float epsilon = 0.1f;
	private float theta = 0.01f;
	private static Connection c;
	private static PostgreSQLJDBC6 postgreSQLJDBC;
	private static HashMap<String, ArrayList> tableMap;
	private ConstraintRewrite2 constraintRewrite;

	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub

		postgreSQLJDBC = new PostgreSQLJDBC6();
		MainTest test = new MainTest();
		c = postgreSQLJDBC.connectDB(address, port, dbName, usrName, psw); // init the connection of the database
		tableMap = postgreSQLJDBC.getTableSchema(c); // the schema of the database
		test.sampleFramework(constraints.trim(), 0);
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
	public ConstraintStru2 violationCheck(String constraint, int sequence) throws SQLException {

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
		HashMap<String, ArrayList<HashMap>> vioTupleMap = constraintRewrite.getVioTuples(depSqlArray, c, tableMap);

		// create deletion table with same structure and store them in the deletion
		// table

		/*
		 * String createDelTable = "CREATE TABLE del_" + depSqlArray[0] + sequence +
		 * " AS SELECT * FROM " + depSqlArray[0] + " WHERE 1=2;"; String createDelSql =
		 * constraintRewrite.createDeletionTableSql(depSqlArray[0],c,
		 * tableMap.get(depSqlArray[0]),vioTuples, sequence);
		 * postgreSQLJDBC.execute(c,createDelTable,false);
		 * postgreSQLJDBC.execute(c,createDelSql,false);
		 */

		ConstraintStru2 constraintStru = new ConstraintStru2(vioTupleMap, depSqlArray);
		// System.out.println(sql);

		return constraintStru;
	}

	public void sampleFramework(String constraint, int sequence) throws SQLException {
		int count = 0;
		Random random = new Random();
		int m = (int) ((1 / (2 * epsilon)) * Math.log(2 / theta));

		ConstraintStru2 constraintStru = violationCheck(constraint, sequence);

		System.out.println("m: " + m);

		try {
			// Run Row(SQL(theta)) for each constraint
			if (constraintStru.getDepSqlLst().length > 1) {
				// has equal attribute for different table
				for (int i = 0; i <= m; i++) {
					RandomMarkov randomMarkov = new RandomMarkov(constraintStru, random);
					// markov chain provide the tuples which to delete next
					ArrayList<TableStru> tableList = constraintRewrite.getTableList();

					while (randomMarkov.hasNext()) {

						HashMap vioTuple = randomMarkov.next();
						// reader_rid,reader_firstname ...reader'_rid
						int pos = Math.abs(random.nextInt()) % tableList.size();
						System.out.println(pos);
						TableStru tbStru = tableList.get(pos);
						String tbName = tbStru.getTableName();
						HashMap tuple = new HashMap();
						for (Object attName : tableMap.get(tbName.replaceAll("'", ""))) {
							tuple.put(attName, vioTuple.get(tbName + "_" + attName));
							// reader_rid,reader_firstname ...reader_phone
						}

						System.out.println(vioTuple);
						System.out.println(tuple);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(count + "/" + m);
	}

}
