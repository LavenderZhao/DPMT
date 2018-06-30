import java.sql.Connection;

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
		BaseDao dao = new BaseDao();
		Connection conn = dao.connectDB();
		/*
		 * stru = jUtils.splitQuery(); rewrite.createOquery(conn); String sql =
		 * rewrite.rewrite(stru, conn);
		 */
		String data = "'99889874', 'firstname_488_48', 'lastname_58', '2017-02-16', 't', 'phone_545_54'";
		dao.insertdata(conn, "reader", data);

	}

}
