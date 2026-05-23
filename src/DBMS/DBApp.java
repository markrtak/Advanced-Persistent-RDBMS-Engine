package DBMS;

import java.io.IOException;
import java.util.ArrayList;

public class DBApp
{
	static int dataPageSize = 2;
	static int indexPageSize = 5;

	private static String repeat(String str, int count)
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < count; i++)
		{
			sb.append(str);
		}
		return sb.toString();
	}

	public static void createTable(String tableName, String[] columnsNames)
	{
		Table t = new Table(tableName, columnsNames);
		FileManager.storeTable(tableName, t);
	}

	public static void insert(String tableName, String[] record)
	{
		Table t = FileManager.loadTable(tableName);
		if (t == null)
		{
			return;
		}
		t.insert(record);
		FileManager.storeTable(tableName, t);
	}

	public static ArrayList<String []> select(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select();
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, int pageNumber, int recordNumber)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(pageNumber, recordNumber);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static ArrayList<String []> select(String tableName, String[] cols, String[] vals)
	{
		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = t.select(cols, vals);
		FileManager.storeTable(tableName, t);
		return res;
	}

	public static String getFullTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getFullTrace();
		return res;
	}

	public static String getLastTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		String res = t.getLastTrace();
		return res;
	}

	public static ArrayList<String[]> validateRecords(String tableName)
	{
		Table tbl = FileManager.loadTable(tableName);
		ArrayList<String[]> missingRecs = new ArrayList<String[]>();
		if(tbl != null)
		{
			missingRecs = tbl.validateRecords();
			FileManager.storeTable(tableName, tbl);
		}
		return missingRecs;
	}

	public static void recoverRecords(String tableName, ArrayList<String[]> missing)
	{
		Table tbl = FileManager.loadTable(tableName);
		if(tbl != null)
		{
			tbl.recoverRecords(missing);
			FileManager.storeTable(tableName, tbl);
		}
	}

	public static void createBitMapIndex(String tableName, String colName)
	{
		Table tbl = FileManager.loadTable(tableName);
		if(tbl != null)
		{
			tbl.createBitMapIndex(colName);
			FileManager.storeTable(tableName, tbl);
		}
	}

	public static String getValueBits(String tableName, String colName, String value)
	{
		Table tbl = FileManager.loadTable(tableName);
		if(tbl == null)
		{
			return "";
		}
		String bitString = tbl.getValueBits(colName, value);
		return bitString;
	}

	public static ArrayList<String[]> selectIndex(String tableName, String[] cols, String[] vals)
	{
		Table tbl = FileManager.loadTable(tableName);
		if(tbl == null)
		{
			return new ArrayList<String[]>();
		}
		ArrayList<String[]> output = tbl.selectIndex(cols, vals);
		FileManager.storeTable(tableName, tbl);
		return output;
	}

	public static void createDenseIndex(String tableName, String colName)
	{
		long startTime = System.currentTimeMillis();
		Table t = FileManager.loadTable(tableName);
		if (t == null)
		{
			return;
		}

		String[] colNames = t.getColumnsNames();
		int colIndex = -1;
		for (int i = 0; i < colNames.length; i++)
		{
			if (colNames[i].equals(colName))
			{
				colIndex = i;
				break;
			}
		}
		if (colIndex == -1)
		{
			return;
		}

        ArrayList<String> allEntries = new ArrayList<>();
        int pageCount = t.getPageCount();
        for (int pNum = 0; pNum < pageCount; pNum++)
        {
            Page p = FileManager.loadTablePage(tableName, pNum);
            if (p != null)
            {
                ArrayList<String[]> records = p.select();
                for (int rNum = 0; rNum < records.size(); rNum++)
                {
                    String[] record = records.get(rNum);
                    String val = record[colIndex];
                    allEntries.add(val + "|" + pNum + "|" + rNum);
                }
            }
        }

		java.util.Collections.sort(allEntries, new java.util.Comparator<String>()
		{
			@Override
			public int compare(String s1, String s2)
			{
				int p2 = s1.lastIndexOf('|');
				int p1 = s1.lastIndexOf('|', p2 - 1);
				String k1 = s1.substring(0, p1);
				int pg1 = Integer.parseInt(s1.substring(p1 + 1, p2));
				int r1 = Integer.parseInt(s1.substring(p2 + 1));

				int q2 = s2.lastIndexOf('|');
				int q1 = s2.lastIndexOf('|', q2 - 1);
				String k2 = s2.substring(0, q1);
				int pg2 = Integer.parseInt(s2.substring(q1 + 1, q2));
				int r2 = Integer.parseInt(s2.substring(q2 + 1));

				int cmp = k1.compareTo(k2);
				if (cmp != 0)
				{
					return cmp;
				}
				cmp = Integer.compare(pg1, pg2);
				if (cmp != 0)
				{
					return cmp;
				}
				return Integer.compare(r1, r2);
			}
		});

		int size = allEntries.size();
		int blocksCount = (int) Math.ceil((double) size / indexPageSize);

		for (int bNum = 0; bNum < blocksCount; bNum++)
		{
			DenseIndexBlock block = new DenseIndexBlock();
			int start = bNum * indexPageSize;
			int end = Math.min(start + indexPageSize, size);
			for (int i = start; i < end; i++)
			{
				String entry = allEntries.get(i);
				int p2 = entry.lastIndexOf('|');
				int p1 = entry.lastIndexOf('|', p2 - 1);
				String val = entry.substring(0, p1);
				int pNum = Integer.parseInt(entry.substring(p1 + 1, p2));
				int rNum = Integer.parseInt(entry.substring(p2 + 1));
				block.addEntry(val, pNum, rNum);
			}
			FileManager.storeIndexBlock(tableName, colName, bNum, block);
		}

		long stopTime = System.currentTimeMillis();
		t.getTrace().add("Dense Index created on column: " + colName + ", execution time (mil): " + (stopTime - startTime));
		FileManager.storeTable(tableName, t);
	}

	public static String getIndexRepresentation(String tableName, String colName)
	{
		ArrayList<DenseIndexBlock> blocks = new ArrayList<DenseIndexBlock>();
		int bNum = 0;
		while (true)
		{
			DenseIndexBlock block = FileManager.loadIndexBlock(tableName, colName, bNum);
			if (block == null)
			{
				break;
			}
			blocks.add(block);
			bNum++;
		}
		return blocks.toString();
	}
	
	public static void main(String []args) throws IOException
	{
		System.out.println(repeat("=", 80));
		System.out.println("COMPREHENSIVE DATABASE TEST - MS1 (Bitmap) + MS3 (Dense Index)");
		System.out.println(repeat("=", 80));
		
		FileManager.reset();
		String[] cols = {"id","name","major","semester","gpa"};
		createTable("student", cols);
		
		System.out.println("\n[TEST 1] BASIC INSERT AND SELECT OPERATIONS");
		System.out.println(repeat("-", 80));
		String[] r1 = {"1", "stud1", "CS", "5", "0.9"};
		insert("student", r1);
		String[] r2 = {"2", "stud2", "BI", "7", "1.2"};
		insert("student", r2);
		String[] r3 = {"3", "stud3", "CS", "2", "2.4"};
		insert("student", r3);
		String[] r4 = {"4", "stud4", "DMET", "9", "1.2"};
		insert("student", r4);
		String[] r5 = {"5", "stud5", "BI", "4", "3.5"};
		insert("student", r5);
		
		System.out.println("Output of selecting the whole table content:");
		ArrayList<String[]> result1 = select("student");
		for (String[] array : result1) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		
		System.out.println("\n[TEST 2] SELECT BY POSITION");
		System.out.println(repeat("-", 80));
		System.out.println("Output of selecting by position (page 1, record 1):");
		ArrayList<String[]> result2 = select("student", 1, 1);
		for (String[] array : result2) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		
		System.out.println("\n[TEST 3] SELECT BY COLUMN CONDITION");
		System.out.println(repeat("-", 80));
		System.out.println("Output of selecting by column condition (gpa = 1.2):");
		ArrayList<String[]> result3 = select("student", new String[]{"gpa"}, new String[]{"1.2"});
		for (String[] array : result3) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		
		System.out.println("\n[TEST 4] BITMAP INDEX CREATION AND TESTING");
		System.out.println(repeat("-", 80));
		createBitMapIndex("student", "gpa");
		createBitMapIndex("student", "major");
		
		System.out.println("Bitmap of the value 'CS' from the major index: " + getValueBits("student", "major", "CS"));
		System.out.println("Bitmap of the value '1.2' from the gpa index: " + getValueBits("student", "gpa", "1.2"));
		
		System.out.println("\nInserting more records after index creation...");
		String[] r6 = {"6", "stud6", "CS", "8", "1.2"};
		insert("student", r6);
		String[] r7 = {"7", "stud7", "BI", "3", "0.9"};
		insert("student", r7);
		
		System.out.println("\nAfter new insertions:");
		System.out.println("Bitmap of the value 'CS' from the major index: " + getValueBits("student", "major", "CS"));
		System.out.println("Bitmap of the value '1.2' from the gpa index: " + getValueBits("student", "gpa", "1.2"));
		
		System.out.println("\n[TEST 5] SELECT WITH INDEX - ALL COLUMNS INDEXED");
		System.out.println(repeat("-", 80));
		System.out.println("Selection using index when all columns are indexed (major='CS' AND gpa='1.2'):");
		ArrayList<String[]> result4 = selectIndex("student", new String[]{"major", "gpa"}, new String[]{"CS", "1.2"});
		for (String[] array : result4) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		System.out.println("Last trace: " + getLastTrace("student"));
		
		System.out.println("\n[TEST 6] SELECT WITH INDEX - PARTIAL INDEXING");
		System.out.println(repeat("-", 80));
		System.out.println("Selection using index when only one column is indexed (major='CS' AND semester='5'):");
		ArrayList<String[]> result5 = selectIndex("student", new String[]{"major", "semester"}, new String[]{"CS", "5"});
		for (String[] array : result5) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		System.out.println("Last trace: " + getLastTrace("student"));
		
		System.out.println("\n[TEST 7] SELECT WITH INDEX - SOME COLUMNS INDEXED");
		System.out.println(repeat("-", 80));
		System.out.println("Selection using index when some columns are indexed (major='CS' AND semester='5' AND gpa='0.9'):");
		ArrayList<String[]> result6 = selectIndex("student", new String[]{"major", "semester", "gpa"}, new String[]{"CS", "5", "0.9"});
		for (String[] array : result6) {
			for (String str : array) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		System.out.println("Last trace: " + getLastTrace("student"));
		
		System.out.println("\n[TEST 8] DENSE INDEX CREATION");
		System.out.println(repeat("-", 80));
		createDenseIndex("student", "id");
		System.out.println("Dense Index created on column 'id'");
		System.out.println("Index Representation: " + getIndexRepresentation("student", "id"));
		
		createDenseIndex("student", "name");
		System.out.println("\nDense Index created on column 'name'");
		System.out.println("Index Representation: " + getIndexRepresentation("student", "name"));
		
		System.out.println("\n[TEST 9] FULL TRACE");
		System.out.println(repeat("-", 80));
		System.out.println("Full Trace of the table:");
		System.out.println(getFullTrace("student"));
		
		System.out.println("\n[TEST 10] FILE SYSTEM TRACE");
		System.out.println(repeat("-", 80));
		System.out.println("The trace of the Tables Folder:");
		System.out.println(FileManager.trace());
		
		System.out.println("\n[TEST 11] VALIDATE AND RECOVER RECORDS");
		System.out.println(repeat("-", 80));
		ArrayList<String[]> missing = validateRecords("student");
		System.out.println("Missing records count: " + missing.size());
		if(missing.size() > 0)
		{
			System.out.println("Recovering missing records...");
			recoverRecords("student", missing);
			System.out.println("Recovery complete!");
		}
		else
		{
			System.out.println("No missing records found - all data intact!");
		}
		
		System.out.println("\n[TEST 12] RESET DATABASE");
		System.out.println(repeat("-", 80));
		FileManager.reset();
		System.out.println("Database reset completed.");
		System.out.println("The trace of the Tables Folder after resetting:");
		System.out.println(FileManager.trace());
		
		System.out.println("\n" + repeat("=", 80));
		System.out.println("ALL TESTS COMPLETED SUCCESSFULLY!");
		System.out.println(repeat("=", 80));
	}

}
