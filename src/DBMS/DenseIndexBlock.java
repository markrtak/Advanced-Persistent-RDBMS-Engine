package DBMS;

import java.io.Serializable;
import java.util.ArrayList;

public class DenseIndexBlock implements Serializable {
	private static final long serialVersionUID = 1L;
	private ArrayList<String> entries;

	public DenseIndexBlock() {
		entries = new ArrayList<>();
	}

	public ArrayList<String> getEntries() {
		return entries;
	}

	public void addEntry(String key, int pageNumber, int recordNumber) {
		entries.add("(" + key + ", r" + recordNumber + "@p" + pageNumber + ")");
	}

	@Override
	public String toString() {
		return entries.toString();
	}
}
