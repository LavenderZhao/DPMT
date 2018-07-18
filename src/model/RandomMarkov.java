package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class RandomMarkov implements MarkovTree {
	private ArrayList<HashMap> vioTupleLst;
	private Random random;
	private ArrayList<TableStru> tableList;
	private HashMap<String, ArrayList> tableMap;
	private Boolean[] bitmap;
	private int remainNum;
	private int tupleNumber;

	public RandomMarkov(ConstraintStru constraintStru, Random random, ArrayList<TableStru> tableList,
			HashMap<String, ArrayList> tableMap) {
		this.vioTupleLst = (ArrayList<HashMap>) constraintStru.getVioTupleMap().clone();
		this.random = random;
		this.tableList = tableList;
		this.tableMap = tableMap;
		this.bitmap = new Boolean[vioTupleLst.size()];
		this.remainNum = vioTupleLst.size();
		this.tupleNumber = vioTupleLst.size();
		for (int i = 0; i < vioTupleLst.size(); i++) {
			bitmap[i] = true;
		}
	}

	@Override
	public boolean hasNext() {
		return remainNum > 0;
	}

	@Override
	public HashMap next() {
		// choose one combined violation tuple, ex reader_rid,reader_firstname
		// ...reader'fistname
		int rand = random.nextInt();
		int bitmapPos = Math.abs(rand) % remainNum;
		int realPos = 0;
		while (bitmapPos >= 0) {
			if (bitmap[realPos]) {
				bitmapPos--;
			}
			realPos++;
		}
		realPos--;
		// find the valid tuple's real position

		HashMap<String, HashMap> vioTuple = vioTupleLst.get(realPos);
		// choose a tuple of one table from combined violation tuple, ex.
		// [TB0(x,y,z...),TB1(x1,y1,z1,),...]

		int pos2 = Math.abs(random.nextInt()) % tableList.size();
		TableStru tbStru = tableList.get(pos2);
		// choose one single part ex.TB0(x,y,z...)
		String tbName = tbStru.getTableName(); // reader
		String nickName = tbStru.getNickName(); // TB0

		ArrayList<String> repeatTbLst = new ArrayList();
		for (TableStru tableStru : tableList) {
			if (tableStru.getTableName().equals(tbName)) {
				repeatTbLst.add(tableStru.getNickName()); // add the nickname to the repeat table
			}
		}

		HashMap tuple = new HashMap();
		tuple.put("tableName", tbName);
		for (Object attName : tableMap.get(tbName)) {
			tuple.put(attName, vioTuple.get(nickName).get(attName));
		}

		// delete all the combined violation tuple contained this tuple
		bitmap[realPos] = false;
		remainNum--;
		for (int i = 0; i < tupleNumber; i++) {
			if (!bitmap[i])
				continue; // false
			for (String repeatTbName : repeatTbLst) {
				Boolean bool = true;
				for (Object attName : tableMap.get(tbName)) {
					HashMap<String, HashMap> remainTuple = vioTupleLst.get(i);
					if ((remainTuple.get(repeatTbName).get(attName) == null && tuple.get(attName) == null)) {
						continue;
					}
					if (remainTuple.get(repeatTbName).get(attName) == null || tuple.get(attName) == null
							|| !remainTuple.get(repeatTbName).get(attName).equals(tuple.get(attName))) {
						bool = false;
						break;
					}
				}
				if (bool) {
					bitmap[i] = false;
					remainNum--;
					break;
				}
			}

		}
		return tuple;
	}

}
