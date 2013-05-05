package org.template.core;

public class TemplateCache {
	/*
	 * 
	 */
	/** The last modification time */
	public long lastModified;
	
	/** String of HTML files */
	private StringBuffer sb;

	public TemplateCache(long lastModified) {
		this.lastModified = lastModified;
		this.sb = new StringBuffer();
	}
	
	/*
	 * @return  StringBuffer object  
	 */
	public StringBuffer getSBuffer()
	{
		return this.sb;
	}
	
	/*
	 * @return String of HTML files  
	 */
	public String getContent()
	{
		return sb.toString();
	}
}
