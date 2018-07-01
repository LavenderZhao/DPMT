import java.sql.Connection;
import java.util.ArrayList;

import control.JdbcUtils;
import control.VIEW_Rewrite;
import dao.BaseDao;
import model.QueriesStru;

public class main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// NOTEXIST_Rewrite rewrite = new NOTEXIST_Rewrite();
		// NOTIN_Rewrite rewrite = new NOTIN_Rewrite();
		VIEW_Rewrite rewrite = new VIEW_Rewrite();
		JdbcUtils jUtils = new JdbcUtils();
		QueriesStru stru = new QueriesStru();
		BaseDao basedao = new BaseDao();
		Connection c = basedao.connectDB();
		/*
		 * stru = jUtils.splitQuery(); rewrite.createOquery(conn); String sql =
		 * rewrite.rewrite(stru, conn);
		 */
		ArrayList<String> tableNames = basedao.getTableNames(c);
		jUtils.DropDView(c, tableNames);
		jUtils.DropDTable(c, tableNames);
	}

}
