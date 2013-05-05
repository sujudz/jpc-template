package org.template.core;

public class TemplateRes {
	
	/** 数字正则 */
	public static String ISNUM = "\\d+";
	
	/** 变量正则 */
	public static String ISVAR = "\\w[\\w\\d_]+";
	
	/** 字符串正则 */
	public static String STRVAR = "\"([^\"\']*)\"";
	
	/** 内容体正则 */
	public static String EXPRE = "\\((\\w+[^{}|&]*)\\)";
	
	/** 函数正则 */
	public static String FUN = "(\\w+)\\(([^\\n{}]*)\\)";
	
	/** 数组正则 */
	public static String ARR = "([\\w\\d_]+)\\[([\\d]+)\\]";
	
	/** 赋值表达式正则 */
	public static String ASSIGN = "(\\w+)\\x20?=\\x20?([^\n;]+);";
	
	/** 对象取属性、方法，map正则 */
	public static String DICT = "\\w+\\.(?:[^{}\\n]+\\.)*[^{}\\n]+$";
	
	/** for循环正则 */
	public static String FOR = "\\(\\x20?([\\d\\w]+)\\x20in\\x20([^\\n]+)\\x20?\\)";
	
	/** 标签搜索正则 */
	public static String TAG = "(\\\\?)(?:[{}]|\\$(var|if|for|include|else|)([^{}$\n]*))";
	
}
