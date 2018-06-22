import java.sql.Connection;

import control.NOTIN_Rewrite;
import control.SplitQueries;
import dao.BaseDao;
import model.QueriesStru;

public class main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// NOTEXIST_Rewrite rewrite = new NOTEXIST_Rewrite();
		NOTIN_Rewrite rewrite = new NOTIN_Rewrite();
		// VIEW_Rewrite rewrite = new VIEW_Rewrite();
		SplitQueries splitQueries = new SplitQueries();
		QueriesStru stru = new QueriesStru();
		BaseDao dao = new BaseDao();
		Connection conn = dao.connectDB();
		stru = splitQueries.splitQuery();
		System.out.println(stru.getWhere());
		rewrite.rewrite(stru, conn);

	}

}
