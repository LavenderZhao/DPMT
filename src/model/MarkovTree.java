package model;

import java.util.HashMap;
import java.util.Iterator;

interface MarkovTree extends Iterator {
	ConstraintStru constraintStru = null;

	public boolean hasNext();

	public HashMap next();
}
