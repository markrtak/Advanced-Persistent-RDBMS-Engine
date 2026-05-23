package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class BitmapIndex implements Serializable
{
	private HashMap<String, String> bitMaps;

	public BitmapIndex()
	{
		bitMaps = new HashMap<String, String>();
	}

	public String getBits(String value)
	{
		String bits = bitMaps.get(value);
		if(bits == null)
		{
			return "";
		}
		return bits;
	}

	public Set<String> getValues()
	{
		return bitMaps.keySet();
	}

	public void put(String value, String bits)
	{
		bitMaps.put(value, bits);
	}

	public int getLength()
	{
		for(String bits : bitMaps.values())
		{
			return bits.length();
		}
		return 0;
	}

	public static BitmapIndex buildForColumn(ArrayList<String[]> records, int colIndex)
	{
		BitmapIndex idx = new BitmapIndex();
		int rowNum = 0;
		while(rowNum < records.size())
		{
			String currVal = records.get(rowNum)[colIndex];
			ArrayList<String> existingVals = new ArrayList<String>(idx.getValues());
			for(String ev : existingVals)
			{
				String existingBits = idx.getBits(ev);
				char bitToAdd = ev.equals(currVal) ? '1' : '0';
				idx.put(ev, existingBits + bitToAdd);
			}
			boolean valExists = idx.getValues().contains(currVal);
			if(!valExists)
			{
				StringBuilder newBits = new StringBuilder();
				int zeros = 0;
				while(zeros < rowNum)
				{
					newBits.append('0');
					zeros++;
				}
				newBits.append('1');
				idx.put(currVal, newBits.toString());
			}
			rowNum++;
		}
		return idx;
	}

	public void appendRecord(String value)
	{
		ArrayList<String> allKeys = new ArrayList<String>(getValues());
		int keyIdx = 0;
		while(keyIdx < allKeys.size())
		{
			String currentKey = allKeys.get(keyIdx);
			String oldBits = getBits(currentKey);
			boolean match = currentKey.equals(value);
			put(currentKey, oldBits + (match ? '1' : '0'));
			keyIdx++;
		}
		boolean valueAlreadyExists = getValues().contains(value);
		if(!valueAlreadyExists)
		{
			int currentLen = getLength();
			StringBuilder zeros = new StringBuilder();
			int zerosNeeded = currentLen - 1;
			for(int z = 0; z < zerosNeeded; z++)
			{
				zeros.append('0');
			}
			zeros.append('1');
			put(value, zeros.toString());
		}
	}

	public static String andBitmaps(String b1, String b2)
	{
		int minLen = b1.length() < b2.length() ? b1.length() : b2.length();
		StringBuilder result = new StringBuilder();
		int pos = 0;
		while(pos < minLen)
		{
			char bit1 = b1.charAt(pos);
			char bit2 = b2.charAt(pos);
			char andResult = (bit1 == '1' && bit2 == '1') ? '1' : '0';
			result.append(andResult);
			pos++;
		}
		return result.toString();
	}
}
