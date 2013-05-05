package org.template.core;

public class LineObj {
	/*
	 * 
	 */
	
	/** The ending index */
	public int end;
	
	/** Boolean value if true Replace the search characters */
	public boolean isline;
	
	/** Parsed characters */
	public String line;
	
	public LineObj(int end, boolean isline, String line)
	{
		this.end = end;
		this.isline = isline;
		this.line = line;
	}
}
