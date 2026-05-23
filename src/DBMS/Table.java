package DBMS;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Table implements Serializable
{
	private String name;
	private String[] columnsNames;
	private int pageCount;
	private int recordsCount;
	private ArrayList<String> trace;
	private ArrayList<String[]> allRecords;
	private ArrayList<String> indexedColumns;
	
	public Table(String name, String[] columnsNames) 
	{
		super();
		this.name = name;
		this.columnsNames = columnsNames;
		this.trace = new ArrayList<String>();
		this.allRecords = new ArrayList<String[]>();
		this.indexedColumns = new ArrayList<String>();
		this.trace.add("Table created name:" + name + ", columnsNames:"
				+ Arrays.toString(columnsNames));
	}


	@Override
	public String toString() 
	{
		return "Table [name=" + name + ", columnsNames="
				+ Arrays.toString(columnsNames) + ", pageCount=" + pageCount
				+ ", recordsCount=" + recordsCount + "]";
	}

	private int getColumnIndex(String colName)
	{
		for(int i = 0; i < columnsNames.length; i++)
		{
			if(columnsNames[i].equals(colName))
			{
				return i;
			}
		}
		return -1;
	}

	private boolean isIndexed(String colName)
	{
		return indexedColumns.contains(colName);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		if(allRecords == null)
		{
			allRecords = new ArrayList<String[]>();
		}
		if(indexedColumns == null)
		{
			indexedColumns = new ArrayList<String>();
		}
		if(trace == null)
		{
			trace = new ArrayList<String>();
		}
	}

	private int getExpectedPageCount()
	{
		if(recordsCount == 0)
		{
			return 0;
		}
		return (int) Math.ceil(recordsCount / (double) DBApp.dataPageSize);
	}

	private File getPageFile(int pageNumber)
	{
		return new File(new File(FileManager.directory, name), pageNumber + ".db");
	}

	private boolean isPageMissing(int pageNumber)
	{
		File pageFile = getPageFile(pageNumber);
		return !pageFile.exists();
	}
	
	public void insert(String []record)
	{
		long startTime = System.currentTimeMillis();
		Page current;
		if(pageCount == 0)
		{
			current = new Page();
			current.insert(record);
			pageCount = 1;
		}
		else
		{
			current = FileManager.loadTablePage(this.name, pageCount - 1);
			if(current == null || !current.insert(record))
			{
				current = new Page();
				current.insert(record);
				pageCount++;
			}
		}
		FileManager.storeTablePage(this.name, pageCount - 1, current);
		allRecords.add(Arrays.copyOf(record, record.length));
		updateIndexesOnInsert(record);
		recordsCount++;
		long stopTime = System.currentTimeMillis();
		this.trace.add("Inserted:"+ Arrays.toString(record)+", at page number:"+(pageCount-1)
				+", execution time (mil):"+(stopTime - startTime));
	}

	private void updateIndexesOnInsert(String[] record)
	{
		int numIndexed = indexedColumns.size();
		for(int idx = 0; idx < numIndexed; idx++)
		{
			String colName = indexedColumns.get(idx);
			int colPosition = getColumnIndex(colName);
			BitmapIndex bitmapIdx = FileManager.loadTableIndex(name, colName);
			if(bitmapIdx != null)
			{
				String recordValue = record[colPosition];
				bitmapIdx.appendRecord(recordValue);
				FileManager.storeTableIndex(name, colName, bitmapIdx);
			}
		}
	}
	
	public String[] fixCond(String[] cols, String[] vals)
	{
		String[] res = new String[columnsNames.length];
		for(int i=0;i<res.length;i++)
		{
			for(int j=0;j<cols.length;j++)
			{
				if(columnsNames[i].equals(cols[j]))
				{
					res[i]=vals[j];
				}
			}
		}
		return res;
	}
	
	public ArrayList<String []> select(String[] cols, String[] vals)
	{
		String[] cond = fixCond(cols, vals);
		String tracer ="Select condition:"+Arrays.toString(cols)+"->"+Arrays.toString(vals);
		ArrayList<ArrayList<Integer>> pagesResCount = new ArrayList<ArrayList<Integer>>();
		ArrayList<String []> res = new ArrayList<String []>();
		long startTime = System.currentTimeMillis();
		for(int i=0;i<pageCount;i++)
		{
			Page p = FileManager.loadTablePage(this.name, i);
			if(p == null)
			{
				continue;
			}
			ArrayList<String []> pRes = p.select(cond);
			if(pRes.size()>0)
			{
				ArrayList<Integer> pr = new ArrayList<Integer>();
				pr.add(i);
				pr.add(pRes.size());
				pagesResCount.add(pr);
				res.addAll(pRes);
			}
		}
		long stopTime = System.currentTimeMillis();
		tracer +=", Records per page:" + pagesResCount+", records:"+res.size()
				+", execution time (mil):"+(stopTime - startTime);
		this.trace.add(tracer);
		return res;
	}

	public ArrayList<String[]> select(String[] cond)
	{
		ArrayList<String[]> res = new ArrayList<String[]>();
		for(int i = 0; i < allRecords.size(); i++)
		{
			if(matchesCondition(allRecords.get(i), cond))
			{
				res.add(allRecords.get(i));
			}
		}
		return res;
	}

	private boolean matchesCondition(String[] record, String[] cond)
	{
		for(int j = 0; j < cond.length; j++)
		{
			if(cond[j] != null)
			{
				if(!cond[j].equals(record[j]))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public ArrayList<String []> select(int pageNumber, int recordNumber)
	{
		String tracer ="Select pointer page:"+pageNumber+", record:"+recordNumber;
		ArrayList<String []> res = new ArrayList<String []>();
		long startTime = System.currentTimeMillis();
		Page p = FileManager.loadTablePage(this.name, pageNumber);
		ArrayList<String []> pRes = p.select(recordNumber);
		if(pRes.size()>0)
		{
			res.addAll(pRes);
		}
		long stopTime = System.currentTimeMillis();
		tracer+=", total output count:"+res.size()
				+", execution time (mil):"+(stopTime - startTime);
		this.trace.add(tracer);
		return res;
	}
	
	
	public ArrayList<String []> select()
	{
		ArrayList<String []> res = new ArrayList<String []>();
		long startTime = System.currentTimeMillis();
		for(int i=0;i<pageCount;i++)
		{
			Page p = FileManager.loadTablePage(this.name, i);
			if(p != null)
			{
				res.addAll(p.select());
			}
		}
		long stopTime = System.currentTimeMillis();
		this.trace.add("Select all pages:" + pageCount+", records:"+recordsCount
				+", execution time (mil):"+(stopTime - startTime));
		return res;
	}

	public void createBitMapIndex(String colName)
	{
		long timeStart = System.currentTimeMillis();
		int columnIdx = getColumnIndex(colName);
		boolean invalid = columnIdx < 0 || isIndexed(colName);
		if(invalid)
		{
			return;
		}
		BitmapIndex bitmapIdx = BitmapIndex.buildForColumn(allRecords, columnIdx);
		indexedColumns.add(colName);
		Collections.sort(indexedColumns);
		FileManager.storeTableIndex(name, colName, bitmapIdx);
		long timeEnd = System.currentTimeMillis();
		long duration = timeEnd - timeStart;
		this.trace.add("Index created for column: " + colName + ", execution time (mil):" + duration);
	}

	public String getValueBits(String colName, String value)
	{
		BitmapIndex bitmapIdx = FileManager.loadTableIndex(name, colName);
		if(bitmapIdx == null)
		{
			return zeros(recordsCount);
		}
		String valueBits = bitmapIdx.getBits(value);
		int bitsLen = valueBits.length();
		if(bitsLen < recordsCount)
		{
			int padding = recordsCount - bitsLen;
			return valueBits + zeros(padding);
		}
		return valueBits;
	}

	private String zeros(int count)
	{
		StringBuilder zeroBits = new StringBuilder();
		int added = 0;
		while(added < count)
		{
			zeroBits.append('0');
			added++;
		}
		return zeroBits.toString();
	}

	public ArrayList<String[]> selectIndex(String[] cols, String[] vals)
	{
		long timeBegin = System.currentTimeMillis();
		ArrayList<String> colsWithIndex = new ArrayList<String>();
		ArrayList<String> colsWithoutIndex = new ArrayList<String>();

		int colCount = 0;
		while(colCount < cols.length)
		{
			String colName = cols[colCount];
			if(isIndexed(colName))
			{
				colsWithIndex.add(colName);
			}
			else
			{
				colsWithoutIndex.add(colName);
			}
			colCount++;
		}
		ArrayList<String[]> output = new ArrayList<String[]>();
		int indexedMatchCount = 0;

		boolean noIndexes = colsWithIndex.isEmpty();
		if(noIndexes)
		{
			String[] fullCond = fixCond(cols, vals);
			output = select(fullCond);
			indexedMatchCount = output.size();
		}
		else
		{
			String mergedBits = null;
			int idxColNum = 0;
			while(idxColNum < colsWithIndex.size())
			{
				String idxCol = colsWithIndex.get(idxColNum);
				String idxVal = getIndexedVal(cols, idxCol, vals);
				String colBits = getValueBits(idxCol, idxVal);
				if(mergedBits == null)
				{
					mergedBits = colBits;
				}
				else
				{
					mergedBits = BitmapIndex.andBitmaps(mergedBits, colBits);
				}
				idxColNum++;
			}
			if(mergedBits == null)
			{
				mergedBits = "";
			}
			int bitPos = 0;
			while(bitPos < mergedBits.length())
			{
				char bitVal = mergedBits.charAt(bitPos);
				if(bitVal == '1')
				{
					indexedMatchCount++;
				}
				bitPos++;
			}

			String[] fullCond = fixCond(cols, vals);
			int recIdx = 0;
			int maxIdx = mergedBits.length();
			if(allRecords.size() < maxIdx)
			{
				maxIdx = allRecords.size();
			}
			while(recIdx < maxIdx)
			{
				char bitAtPos = mergedBits.charAt(recIdx);
				if(bitAtPos == '1')
				{
					String[] rec = allRecords.get(recIdx);
					boolean matches = matchesCondition(rec, fullCond);
					if(matches)
					{
						output.add(rec);
					}
				}
				recIdx++;
			}
		}

		long timeEnd = System.currentTimeMillis();
		long elapsed = timeEnd - timeBegin;
		String traceStr = buildSelectIndexTrace(cols, vals, colsWithIndex, colsWithoutIndex,
				indexedMatchCount, output.size(), elapsed);
		this.trace.add(traceStr);
		return output;
	}

	private String getIndexedVal(String[] cols, String col, String[] vals)
	{
		int pos = 0;
		while(pos < cols.length)
		{
			if(cols[pos].equals(col))
			{
				return vals[pos];
			}
			pos++;
		}
		return "";
	}

	private String buildSelectIndexTrace(String[] cols, String[] vals,
			ArrayList<String> indexedCols, ArrayList<String> nonIndexedCols,
			int indexedSelectionCount, int finalCount, long execTime)
	{
		StringBuilder traceBuilder = new StringBuilder();
		traceBuilder.append("Select index condition: ").append(Arrays.toString(cols))
				.append("->").append(Arrays.toString(vals));

		ArrayList<String> alphabeticalIndexed = new ArrayList<String>(indexedCols);
		Collections.sort(alphabeticalIndexed);

		boolean hasIndexedCols = !alphabeticalIndexed.isEmpty();
		if(hasIndexedCols)
		{
			traceBuilder.append(", Indexed columns: ").append(alphabeticalIndexed.toString());
			traceBuilder.append(", Indexed selection count: ").append(indexedSelectionCount);
		}

		boolean fullyIndexed = nonIndexedCols.isEmpty() && !indexedCols.isEmpty();
		if(!fullyIndexed)
		{
			ArrayList<String> alphabeticalNonIndexed = new ArrayList<String>(nonIndexedCols);
			Collections.sort(alphabeticalNonIndexed);
			traceBuilder.append(", Non Indexed: ").append(alphabeticalNonIndexed.toString());
		}

		traceBuilder.append(", Final count: ").append(finalCount)
				.append(", execution time (mil):").append(execTime);
		return traceBuilder.toString();
	}

	private void syncAllRecordsFromPages()
	{
		allRecords = new ArrayList<String[]>();
		int totalPages = getExpectedPageCount();
		int currentPage = 0;
		while(currentPage < totalPages)
		{
			Page pg = FileManager.loadTablePage(name, currentPage);
			if(pg != null)
			{
				ArrayList<String[]> pageData = pg.select();
				int recIdx = 0;
				while(recIdx < pageData.size())
				{
					String[] originalRec = pageData.get(recIdx);
					String[] copiedRec = Arrays.copyOf(originalRec, originalRec.length);
					allRecords.add(copiedRec);
					recIdx++;
				}
			}
			currentPage++;
		}
		recordsCount = allRecords.size();
		pageCount = getExpectedPageCount();
	}

	public ArrayList<String[]> validateRecords()
	{
		ArrayList<String[]> missingRecords = new ArrayList<String[]>();
		int expectedPages = getExpectedPageCount();
		int pageNum = 0;
		while(pageNum < expectedPages)
		{
			boolean pageLost = isPageMissing(pageNum);
			if(pageLost)
			{
				int firstRec = pageNum * DBApp.dataPageSize;
				int lastRec = firstRec + DBApp.dataPageSize;
				if(lastRec > recordsCount)
				{
					lastRec = recordsCount;
				}
				int recNum = firstRec;
				while(recNum < lastRec && recNum < allRecords.size())
				{
					missingRecords.add(allRecords.get(recNum));
					recNum++;
				}
			}
			pageNum++;
		}
		String traceMsg = "Validating records: " + missingRecords.size() + " records missing.";
		this.trace.add(traceMsg);
		return missingRecords;
	}

	public void recoverRecords(ArrayList<String[]> missing)
	{
		ArrayList<Integer> affectedPages = new ArrayList<Integer>();
		int missingIdx = 0;
		while(missingIdx < missing.size())
		{
			String[] missingRec = missing.get(missingIdx);
			int recPosition = findRecordIndex(missingRec);
			if(recPosition >= 0)
			{
				int pageIdx = recPosition / DBApp.dataPageSize;
				boolean alreadyAdded = affectedPages.contains(pageIdx);
				if(!alreadyAdded)
				{
					affectedPages.add(pageIdx);
				}
			}
			missingIdx++;
		}
		Collections.sort(affectedPages);

		int pageIdx = 0;
		while(pageIdx < affectedPages.size())
		{
			int targetPage = affectedPages.get(pageIdx);
			Page newPage = new Page();
			int firstRecInPage = targetPage * DBApp.dataPageSize;
			int lastRecInPage = firstRecInPage + DBApp.dataPageSize;
			if(lastRecInPage > recordsCount)
			{
				lastRecInPage = recordsCount;
			}
			int recIdx = firstRecInPage;
			while(recIdx < lastRecInPage)
			{
				String[] recData = allRecords.get(recIdx);
				newPage.insert(recData);
				recIdx++;
			}
			FileManager.storeTablePage(name, targetPage, newPage);
			pageIdx++;
		}

		String recoveryTrace = "Recovering " + missing.size() + " records in pages: " + affectedPages + ".";
		this.trace.add(recoveryTrace);
	}

	private int findRecordIndex(String[] record)
	{
		int idx = 0;
		while(idx < allRecords.size())
		{
			String[] storedRec = allRecords.get(idx);
			boolean same = Arrays.equals(storedRec, record);
			if(same)
			{
				return idx;
			}
			idx++;
		}
		return -1;
	}
	
	
	
	
	
	
	public String getFullTrace() 
	{
		StringBuilder res = new StringBuilder();
		for(int i=0;i<this.trace.size();i++)
		{
			if(i > 0)
			{
				res.append('\n');
			}
			res.append(this.trace.get(i));
		}
		ArrayList<String> sortedIndexed = new ArrayList<String>(indexedColumns);
		Collections.sort(sortedIndexed);
		if(this.trace.size() > 0)
		{
			res.append('\n');
		}
		res.append("Pages Count: ").append(pageCount).append(", Records Count: ")
				.append(recordsCount).append(", Indexed Columns: ").append(sortedIndexed);
		return res.toString();
	}
	
	public String getLastTrace() 
	{
		return this.trace.get(this.trace.size()-1);
	}

	public String getName() 
	{
		return name;
	}

	public String[] getColumnsNames() 
	{
		return columnsNames;
	}

	public int getPageCount() 
	{
		return pageCount;
	}

	public ArrayList<String> getTrace() 
	{
		return trace;
	}

}
