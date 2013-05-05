package org.template.core;

import java.util.regex.MatchResult;

public class TemplateTagInfo {
	
	/** end index */
	public int end;
	
	/** start index */
	public int start;
	
	/** find tag */
	public String tag;
	
	/** find matching conditions */
	public String cod;
	
	/** annotation  */
	public String anno;
	
	/** The content of the body  */
	public String expre;
	
	/*
	 * @param   mr 正则匹配结果
	 */
	public TemplateTagInfo(MatchResult mr)
	{
		this.end = mr.end();
		this.start = mr.start();
		this.tag = mr.group(0);
		this.anno = mr.group(1);
		this.cod = mr.group(2);
		this.expre = mr.group(3);
	}
}
