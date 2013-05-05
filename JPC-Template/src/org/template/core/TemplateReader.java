package org.template.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class TemplateReader extends BufferedReader{

	/** 字符缓存区 */
	private StringBuffer sbstr;
	
	/*
	 * @param    tcache 模板缓存类
	 * @param    in  Reader父类
	 */
	public TemplateReader(TemplateCache tcache, Reader in) {
		super(in);
		sbstr = tcache.getSBuffer();
	}
	
	/*
	 * @see java.io.BufferedReader#readLine()
	 */
	@Override
	public String readLine() throws IOException {
		String line = super.readLine();
		if (line != null) {
			//模板缓存系统缓存输入字符行
			sbstr.append(line).append('\n');
		}
		return line;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.io.BufferedReader#close()
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}
}
