package org.template.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateParse {
	/*
	 * 
	 */
	/** 模块对象  */
	private Template template;
	
	/** if的else节点判断栈 */
	private Stack<Boolean> ifstack;
	
	/** 内容体字符串栈  */
	private Stack<StringBuilder> cs;
	
	/** 标签前置字符串 */
	private StringBuilder strstack;
	
	/** 标签信息栈  */
	private Stack<TemplateTagInfo> tagstack;
	
	/** 全局变量存放域  */
	public Map<String, Object> data;
	
	/** 是否注释  */
	private boolean isanno;
	
	/** 左大括号  */
	private int lcb = 0;
	
	/** if for标签执行方法参数class数组 */
	private static Class<?>[] MECLS = 
			new Class[]{String.class, String.class, TemplateTagInfo.class, int.class};
	
	/** 默认全局对象名称  */
	private static String TPNAME = "tobj";
	
	/** 静态正则编译对象 */
	private static Pattern forpat, strpat, arrpat, assignpat, funpat,
						   exprepat, tagpat, isvar;
	
	/*
	 *Initialize the static canonical variables
	 */
	static {
		exprepat = Pattern.compile(TemplateRes.EXPRE, Pattern.DOTALL);
		tagpat = Pattern.compile(TemplateRes.TAG, Pattern.DOTALL);
		forpat = Pattern.compile(TemplateRes.FOR, Pattern.DOTALL);
		strpat = Pattern.compile(TemplateRes.STRVAR, Pattern.DOTALL);
		arrpat = Pattern.compile(TemplateRes.ARR, Pattern.DOTALL);
		assignpat = Pattern.compile(TemplateRes.ASSIGN, Pattern.DOTALL);
		funpat = Pattern.compile(TemplateRes.FUN, Pattern.DOTALL);
		isvar = Pattern.compile(TemplateRes.ISVAR, Pattern.DOTALL);
	}
	
	/**
	 * init stack, varstack
	 * @param    template  模板对象
	 * @param    joindata  引用对象数组
	 */
	public TemplateParse(Template template, Object... joindata)
	{
		this.isanno = false;
		this.template = template;
		this.ifstack = new Stack<Boolean>();
		this.strstack = new StringBuilder("");
		this.tagstack = new Stack<TemplateTagInfo>();
		this.cs = new Stack<StringBuilder>();
		//初始化全局变量
		convertData(TPNAME, joindata);
	}
	
	/*
	 *@return    全局静态变量
	 */
	public Map<String, Object> initglobalvar()
	{
		data = new HashMap<String, Object>();
		data.put("True", true);
		data.put("False", false);
		data.put("NULL", null);
		return data;
	}
	
	/*
	 *@param    tpname  全局变量名称 
	 *@param    joindata  引入变量数组
	 */
	@SuppressWarnings("unchecked")
	public void convertData(String tpname, Object... joindata)
	{
		initglobalvar();
		for (Object obj: joindata) {
			if (obj instanceof Map) {
				//push map
				data.putAll((Map<? extends String, ? extends Object>)obj);
			} else {
				data.put(tpname, obj);    //非map对象使用默认变量名称
			}
		}
	}

	/*
	 *@param   name  解析html文件名称
	 *@return  返回解析后的String
	 */
	public String parse(String name)
	{
		String line = "";
		StringBuffer sb = new StringBuffer("");
		BufferedReader reader = null;
		try {
			reader = template.getReader(name);
			while ( (line = reader.readLine()) != null) {
				line = translation(line.concat("\n"));	//添加\n换行
				if (isanno) {
					line =line.replace("\\", "");		//替换注释	
					isanno = false;
				}
				sb.append(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();						//捕捉异常
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return sb.toString();
	}
	
	/*
	 *@param   var 变量名称
	 *@param   domain 变量作用域
	 *@return  返回包装后的变量值
	 */
	public TemplateObj<?> getvar(String var, Object domain)
	{
		Matcher mat = null;
		Object var_value = null;
		if (domain == null) domain = this.data;
		
		try {
			if (isStr(var)) {				//字符串;if else按照方法执行速度排列
				mat = strpat.matcher(var);
				if (mat.find()) {
					var_value = mat.group(1);
				}
			} else if (isNum(var)) {		//数值
				var_value = new TemplateNumber(var);
			} else if (isDict(var)) {		//对象或map引用
				for (Object vitem: pmfromstr(var, '.')){
					//递归调用getvar逐个获取变量值,并替换作用域
					var_value = getvar((String)vitem, domain).getValue();
					domain = var_value;
				}
			} else if (isVar(var)) {		//变量
				if (domain instanceof Map) {
					var_value = ((Map<?, ?>)domain).get(var);
				} else {
					//非map,获取对象属性
					var_value = getattr(domain, var);
				}
			} else if (isArray(var)) {		//数组
				mat = arrpat.matcher(var);
				if (mat.find()) {
					var = mat.group(1);
					int index = Integer.parseInt(mat.group(2));
					var_value = getvar(var, domain).getObj(index);
				}
			} else {						//默认函数
				//variable is function
				mat = funpat.matcher(var);
				if (mat.find()) {
					String fun = mat.group(1), param = mat.group(2);
					if (domain != data) {
						//执行对象方法
						var_value = execMethod(domain, fun, parameters(param));
					} else {
						//执行默认template方法
						var_value = template.execfun(fun, parameters(param));
					}		
				}
			}
		} catch (Exception e) {
			throw new Error(e);
		}
		return TemplateObj.convert(var_value);
	}
	
	/*
	 *@param   line  替换的字符串行
	 *@param   lmark 替换起始位置
	 *@param   rmark 替换结束位置
	 *@return  返回替换后的字符串
	 */
	public String replace(String line, int lmark, int rmark)
	{
		String var = line.substring(lmark+1, rmark);
		TemplateObj<?> var_value = getvar(var, null); 
		line = line.replace("${".concat(var).concat("}"),
							var_value.toString());
		return line;
	}
	
	/*
	 *@param    mr    匹配结果
	 *@param    line  搜索匹配的字符行
	 *@return   返回匹配搜索匹配后的结果行对象 
	 */
	public LineObj search(MatchResult mr, String line) throws Exception
	{
		int start = mr.start(), end = mr.end();
		TemplateTagInfo tf = new TemplateTagInfo(mr);
		TagEnum te = TagEnum.conver(tf);
		
		switch (te) {
			case LCB:		//左大括号,标签栈不为空
				if (!tagstack.empty() && lcb++ == 0) {
					cs.push(new StringBuilder(""));
					strstack.append(line.substring(0, start));
					return new LineObj(0, true, line.substring(end));
				}
				break;
			case RCB:		//右大括号,lcb索引递减
				if (!tagstack.empty() && --lcb == 0){
					TemplateTagInfo tsinfo = tagstack.pop();
					Method method =getClass().getMethod("cod_".concat(tsinfo.cod), MECLS);
					String content = cs.pop().append(line.substring(0, end-1)).toString();
					return (LineObj)method.invoke(this, content, line, tsinfo, end);
				}
				break;
			default:
				//lcb为0继续搜索匹配line
				if(lcb == 0) {
					return tagSwitch(te, line, tf);
				}
		}
		return new LineObj(end, false, line);
	}
	
	/*
	 *@param   te    标签枚举
	 *@param   line  搜索字符行
	 *@parma   tf    标签信息对象
	 *@return  返回匹配后字符行对象
	 */
	public LineObj tagSwitch(TagEnum te, String line, TemplateTagInfo tf)
	{
		switch (te)
		{
			case ANNO:		//注释
				this.isanno = true;
				break;
			case DOLL:		//$标签
				int lmark = line.indexOf("{", tf.end);
				int rmark = line.indexOf("}", tf.end);
				if (lmark != -1 & rmark != -1) {
					//替换标签
					line = replace(line, lmark, rmark);
					return new LineObj(lmark, true, line);
				} else {
					throw new Error("not fount '{' or '}'");
				}
			case ELSE:
			case IF:
			case VAR:
			case FOR:		//为任意esle,if,var,for标签
				tagstack.push(tf);
				line = line.substring(0, tf.start).concat(line.substring(tf.end));
				return new LineObj(0, true, line);
			case INCLUDE:	//include包含标签
				line = include(line, tf);
				break;
			default:
				//...nothing
		}
		return new LineObj(tf.end, false, line);
	}
	
	/*
	 *@param   line  字符行
	 *@return  解析后的字符行 
	 */
	public String translation(String line)
	{
		/** 返回匹配后的字符串行 */
		String rline = line;
		
		/** 搜索起始索引 */
		int start = 0;
		Matcher tagmat = tagpat.matcher(line);
		while (tagmat.find(start)){
			try {
				LineObj lobj = search(tagmat.toMatchResult(), line);
				start = lobj.end;
				rline = lobj.line;
				//isline=true 重置并替换搜索行line
				if (lobj.isline) {
					line = rline;
					tagmat.reset(line);
				}
			} catch (Exception e) {
				throw new Error(e);
			}	
		}
		//lcb大于0 当前行字符串包含在上一个标签体{}内
		if (lcb > 0) {
			cs.peek().append("".equals(rline.trim()) ? "": rline);
			rline = "";
		}
		//添加标签体前置字符串
		rline = strstack.append(rline).toString();
		strstack.delete(0, strstack.length());
		return rline;
	}
	
	/* 
	 *@param    content  执行内容体
	 *@param    line     搜索字符行
	 *@param    tsinfo   标签信息类
	 *@param    end      搜索结束索引
	 *@return   返回匹配替换后的行对象 
	*/
	public  LineObj cod_if(String content, String line,
						   TemplateTagInfo tsinfo, int end)
	{
		//替换expre中的空格和两边()号
		String expre = tsinfo.expre.replaceAll("\\s", "");
		expre = expre.substring(1, expre.length()-1);
		boolean result = (Boolean) TemplateExpre.evalExpre(expre, this);
		//存储if表达式执行结果,判断下一个esle是否执行
		ifstack.push(result);
		if (result) {
			line = content.concat(line.substring(end));
		} else {
			line = line.substring(end);
		}
		return new LineObj(0, true, line);
	}
	
	/* 
	 *@param    content  执行内容体
	 *@param    line     搜索字符行
	 *@param    tsinfo   标签信息类
	 *@param    end      搜索结束索引
	 *@return   返回匹配替换后的行对象 
	*/
	public LineObj cod_else(String content, String line,
							TemplateTagInfo tsinfo, int end)
	{
		try {
			//上一个if为false
			if (!ifstack.pop()) {
				line = content.concat(line.substring(end));
			} else {
				line = line.substring(end);
			}
		} catch (Exception e) {
			throw new Error("not found if");
		}
		return new LineObj(0, true, line);
	}
	
	/* 
	 *@param    content  执行内容体
	 *@param    line     搜索字符行
	 *@param    tsinfo   标签信息类
	 *@param    end      搜索结束索引
	 *@return   返回匹配替换后的行对象 
	*/
	public LineObj cod_var(String content, String line,
						   TemplateTagInfo tsinfo, int end)
	{
		//content内容体先解析
		Matcher assmat = assignpat.matcher(translation(content));
		while (assmat.find()) {
			String lop = assmat.group(1), rop = assmat.group(2);
			//解析变量值后存放全局data域
			data.put(lop, TemplateExpre.evalExpre(rop, this));
		}
		line = line.substring(end);
		return new LineObj(0, true, line);
	}
	
	/* 
	 *@param    content  执行内容体
	 *@param    line     搜索字符行
	 *@param    tsinfo   标签信息类
	 *@param    end      搜索结束索引
	 *@return   返回匹配替换后的行对象 
	*/
	public LineObj cod_for(String content, String line,
						   TemplateTagInfo tsinfo, int end)
	{
		Matcher format = forpat.matcher(tsinfo.expre);
		String laststr = line.substring(end);
		if (format.find()) {
			line = "";
			String fitem = format.group(1), flist = format.group(2);
			//获取flist对应的集合，遍历解析content
			for (Object forvar : getvar(flist, null).getArray()) {
				data.put(fitem, forvar);	//forvar集合迭代值put入全局data域
				line = line.concat(translation(content));
			}
			data.remove(fitem);
		}
		return new LineObj(0, true, line.concat(laststr));
	}
	
	/*
	 *@param    pmstr  带解析方法字符串形式
	 *@return   返回以,分割的方法调用参数数组
	 */
	public Object[] parameters(String pmstr)
	{
		Object[] pmstrs = pmfromstr(pmstr, ',');
		Object[] objs = 
				pmstrs.length != 0 ? new Object[pmstrs.length]: null;
		for (int i = 0; i < pmstrs.length; i++) {
			//逐个获取参数对应值
			objs[i] = getvar(pmstrs[i].toString(), null).getValue();
		}
		return objs;
	}
	
	/*
	 *@param    line     字符串行
	 *@param    taginfo  标签信息类
	 *@return   返回包含方法解析的字符串
	 */
	public String include(String line, TemplateTagInfo taginfo)
	{
		//int 8:$include length
		Matcher mat  = exprepat.matcher(taginfo.expre);
		if (mat.find()) {
			//格式化字符串，参数为：start索引前字符，解析后的标签体，标签体end索引后字符
			line = String.format("%s%s%s",
					line.substring(0, taginfo.start),
					parse(mat.group(1)),
					line.substring(taginfo.start+8+mat.end(), taginfo.end));
		}
		return line;
	}
	
	/*
	 *@param    data       方法对应对象
	 *@param    fun        方法名
	 *@parma    parameter  参数数组
	 *@return   返回方法执行结果
	 */
	public static Object execMethod(Object data, String fun, Object... parameter)
	{
		Method method = null;
		try {
			//toClassArray(parameter)获取参数class数组
			method = data.getClass().getMethod(fun, toClassArray(parameter));
			return method.invoke(data, parameter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 *@param    data   属性归属对象
	 *@param    var    变量名称
	 *@return   返回变量名对应值
	 */
	public static Object getattr(Object data, String var)
	{
		Field field = null;
		try {
			field = data.getClass().getField(var);
			return field.get(data);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	/*
	 *@param    parameter   参数数组
	 *@return   返回参数class的数组
	 */
	public static Class<?>[] toClassArray(Object[] parameter)
	{
		Class<?>[] cls = null;
		if (parameter != null ) {
			int length = parameter.length;
			cls = new Class<?>[length];
			for (int i = 0; i < length; i++) {
				//对象的class
				cls[i] = parameter[i].getClass();
			}
		}
		return cls;
	}
	
	/*
	 *@param    pmstr  方法字符串形式
	 *@param    sp     以此字符分割
	 *@return   返回分割后的字符串数组
	 */
	public Object[] pmfromstr(String pmstr, char sp)
	{
		/** start搜索起始索引，end 结束索引 */
		int start = 0, end =0;
		/** 字符栈 */
		Stack<Character> pmstack = new Stack<Character>();
		/** 结果字符串栈 */
		ArrayList<String> result = new ArrayList<String>();
		pmstr = pmstr.concat(Character.toString(sp));
		for (char ch: pmstr.toCharArray()) {
			end += 1;
			switch (ch) {
				case '\"':						//匹配字符串，执行continue
					if (!pmstack.empty() 
							&& pmstack.peek() == '\"') {
						pmstack.pop();
						continue;
					} else {
						pmstack.push(ch);
					}
					break;
				case ')':						//匹配)括号
					if (!pmstack.empty() 
							&& pmstack.peek() == '(') {
						pmstack.pop();
						continue;
					} else {
						throw new RuntimeException("not found )");
					}
					//break; //not need break
				case '(':						//匹配(括号
						pmstack.push(ch);
						break;
				default:
					if (!pmstack.empty() 
							&& (pmstack.peek() == '\"' 
							|| pmstack.peek() == '(')) {
						continue;
					} else if (ch == sp) {		//匹配分割字符
						if (start != (end - 1)) {
							//分割后字符串入栈
							result.add(pmstr.substring(start, end-1));
							start = end;
						}
					}
			}
		}
		return result.toArray();
	}
	
	/*
	 *@param   var  变量名称
	 *@return  true为数值
	 */
	public boolean isNum(String var)
	{
		int dollpos = 0;
		char[] arr = var.toCharArray();
		char lastc = arr[arr.length-1];
		//起始不能为.
		if (arr[0] == '.') return false;
		
		//末尾匹配float和double标志
		if (lastc != 'f' 
				&& lastc !='b' 
				&& !Character.isDigit(lastc)) {
			return false;
		}
		for (int i=0; i< arr.length -1; i++) {
			//判断数值中是否只有一个.
			if (dollpos > 1) return false;
			if (arr[i] == '.') {
				dollpos++;
				continue;
			}
			//57='9' 48='0' 数值区间
			if (arr[i] < 58 & arr[i] > 47) continue;
			return false;
		}
		return true;
	}
	
	/*
	 *@param   var  变量名称
	 *@return  true表示为数组格式
	 */
	public boolean isArray(String var)
	{
		return arrpat.matcher(var).matches();
	}
	
	/*
	 *@param   var  变量名称
	 *@return  true表示为字符串格式
	 */
	public boolean isStr(String var)
	{
		//faster than strpat.matcher(var).matches();
		int length = var.length() - 1;
		//判断起始和结束位置字符
		if (var.charAt(0) == '\"' 
				&& var.charAt(length) == '\"') {
			return true;
		}
		return false;
	}
	
	/*
	 *@param   var  变量名称
	 *@return  true表示为对象.属性/方法或者map取值
	 */
	public boolean isDict(String var)
	{
		//fast //return dictpat.matcher(var).matches();
		int lb = 0, point = 0;
		char[] arr = var.toCharArray();
		//判断首字符和末字符不为.
		if (arr[0] == '.' 
				|| arr[arr.length -1] == '.') return false;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '(') {					//判断字符是否是(
				lb++;
			}else if (arr[i] == ')') {				//判断字符)
				lb--;
			}else if (lb == 0 && arr[i] == '.') {	//判断不在()中的.字符
				if (arr[i-1] == '.') return false;
				point++;
			}
		}
		if (point > 0) return true;
		return false;
	}
	
	/*
	 *@param   var  变量名称
	 *@return  true表示为变量格式
	 */
	public boolean isVar(String var)
	{
		return isvar.matcher(var).matches();
	}	
}
