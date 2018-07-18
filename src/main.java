import dao.BaseDao;
import model.ConstraintStru;
import model.QueriesStru;
import model.RandomMarkov;
import model.TableStru;
import utils.ConstraintRewrite2;
import utils.JdbcUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
    private JComboBox queryWay;

    private String address = "localhost";
    public String dbName = "cqa";
    public String port = "5432";
    public String usrName = "postgres";
    public String psw = "";
    public String way = "";
    public String sqlPath = "/Users/qq/Documents/GitHub/DPMT/sql/example.sql";
    private static String constraints = "borrow(a,b,c),borrow(d,b,e) -:  a=d,c=e";
    private float epsilon = 0.1f;
    private float theta = 0.01f;
    private static Connection c;
    private HashMap<String, ArrayList> tableMap;
    private ConstraintRewrite2 constraintRewrite;
    private static JdbcUtils jdbcUtils = new JdbcUtils();
    private BaseDao baseDao = new BaseDao();

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
                    // remove the deletion table
                    // if(main.constraintStruLst != null){
                    // for(ConstraintStru2 constraintStru: main.constraintStruLst){
                    // int sequence = constraintStru.getSequence();
                    // String error_sql = "DROP TABLE delTable" + sequence + ";";
                    // main.postgreSQLJDBC.execute(c, error_sql,false);
                    // }
                    // }

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

        c = baseDao.connectDB(address, port, dbName, usrName, psw); // init the connection of the database
        tableMap = baseDao.getTableSchema(c); // the schema of the database

    }

    public void initGUI() {
        addrText.setText(address);
        portText.setText(port);
        userText.setText(usrName);
        psdText.setText(psw);
        consText.setText(constraints);
        errorText.setText(String.valueOf(epsilon));
        confidenceText.setText(String.valueOf(theta));
        ArrayList<String> dbLst = baseDao.getDbName(c);
        for (String name : dbLst) {
            dbBox.addItem(name);
        }

        //init query way
        queryWay.addItem("NOT EXISTS");
        queryWay.addItem("NOT IN");

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
                way = queryWay.getSelectedItem().toString().trim();
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
                    String qText = queryText.getText().trim();
                    System.out.println("query:" + qText);
                    for (String constraint : consText.getText().split(";")) {
                        sampleFramework(constraint.trim(), qText, sequence, way);
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

        ConstraintStru constraintStru = new ConstraintStru(vioTupleMap, depSqlArray);
        // System.out.println(sql);

        //outputText.append(depSqlArray[0] + "\n");

        outputText.repaint();
        outputText.updateUI();
        outputText.validate();

        return constraintStru;
    }

    public void sampleFramework(String constraint, String qText, int sequence, String way) throws SQLException {

        long startTime=System.currentTimeMillis();

        BaseDao basedao = new BaseDao();
        int count = 0;

        Random random = new Random(System.currentTimeMillis());
        int m = (int) ((1 / (2 * epsilon * epsilon)) * Math.log(2 / theta));
        ArrayList<String> tableNames = basedao.getTableNames(c);
        QueriesStru stru = jdbcUtils.splitQuery(qText, c);

        ConstraintStru constraintStru = violationCheck(constraint, sequence);
        HashMap<ArrayList<String>, Integer> tupleList = new HashMap<>();
        try {
            // Run Row(SQL(theta)) for each constraint

            for (int i = 0; i < 5; i++) {
                System.out.println("the " + (i + 1) + " round!");

                ArrayList<TableStru> tableList = constraintRewrite.getTableList();

                RandomMarkov randomMarkov = new RandomMarkov(constraintStru, random, tableList, tableMap);

                // create delete table for each table
                jdbcUtils.createDeleteTbale(c, tableNames);

                // store tuples to delete
                ArrayList<HashMap> dList = new ArrayList<HashMap>();
                // markov chain provide the tuples which to delete next
                while (randomMarkov.hasNext()) {

                    HashMap tuple = randomMarkov.next();
                    String tableName = (String) tuple.get("tableName");
                    tuple.put("tableName", "del_" + tableName);
                    dList.add(tuple);

                    // reader_rid,reader_firstname ...reader'_rid
                    // System.out.println(tuple);
                    // jdbcUtils.updateTable(tuple, "del_" + tableName, c);
                    // System.out.println(tuple);
                }
                jdbcUtils.InsertData(dList, c);
                if (way.equals("NOT IN")) {
                    jdbcUtils.CreateDeleteView_NOTIN(c, tableNames);
                } else {
                    jdbcUtils.CreateDeleteView_NOTEXIST(c, tableNames);
                }

                tupleList = jdbcUtils.queryRewrite(stru, c, tupleList);

                jdbcUtils.DropDView(c, tableNames);
                jdbcUtils.DropDTable(c, tableNames);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime=System.currentTimeMillis(); //获取结束时间

        System.out.println("running time： "+(endTime-startTime)+"ms");

        for (String att : stru.getAtt()) {
            // System.out.println(att);
            outputText.append(att + "\t");

        }

        outputText.append("probablity\n");

        for (Map.Entry<ArrayList<String>, Integer> entry : tupleList.entrySet()) {
            ArrayList<String> tuple = entry.getKey();

            for (String att : tuple) {

                outputText.append(att + "\t");
            }
            float p = (float) entry.getValue() / (float) m;
            outputText.append("probablity: " + p);
            outputText.append("\n");
        }

        outputText.repaint();
        outputText.updateUI();
        outputText.validate();

    }

}
