package org.template.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
	/*
	 *
	*/
	/** htmlfile File storage directory */
	private String base;
	
	/** character encoding */
	private String coding;
	
	/** Methods the array Including custom */
	private Method[] funcs;
	
	/** The file cache map */
	private Map<String, TemplateCache> filecache;
	
	/** The template cache map */
	private static Map<Integer, Template> tmap;
	
	/** Global methods array */
	private static Class<?>[] PUBFUNC = {TemplateFunc.class};
	
	/** default coding */
	private static String defcod = "UTF-8";
	
	//init tmap
	static {
		//初始化一个静态map用于缓存已使用的Tempalte
		tmap = new HashMap<Integer, Template>();
	}
	/*
	 * @param    base  htmlfile dir
	 * @param    coding  character encoding 
	 * @param    fclss   Array of class function method
	 */
	public Template(String base, String coding, Class<?>...fclss)
	{
		File dir = new File(base);
		this.coding = coding != null ? coding: defcod;
		if (dir.isDirectory()) {
			this.base = base != null ? base: "";
		} else {
			//classpath Relative paths
			this.base = Template.class.getClassLoader().getResource(base).getFile();
		}
		loadFunc(PUBFUNC, fclss);
		filecache = new HashMap<String, TemplateCache>();
	}
	
	/*
	 * load public class method
	 * @param class[] 2d array
	 */
	public void loadFunc(Class<?>[]...fclss)
	{
		ArrayList<Method> list = new ArrayList<Method>();
		for (Class<?>[] clss: fclss) {
			for (Class<?> cls: clss) {
				Method[] methods = cls.getDeclaredMethods();
				list.addAll(Arrays.asList(methods));
			}
		}
		//init methods array
		funcs = new Method[list.size()];
		funcs = list.toArray(funcs);
	}
	
	/*
	 * @param    name  html filename
	 * @param    joindata   Template variables
	 * @return   After parsing a string
	 */
	public String parse(String name, Object... joindata)
	{
		TemplateParse tparse = new TemplateParse(this, joindata);
		return tparse.parse(name);
	}

	/*
	 * @param    name filename
	 * @return   a BufferedReader if filecache include file then
	 *           return StringReader in filecache
	 */
	public BufferedReader getReader(String name) throws Exception
	{
		BufferedReader reader = null;
		String fname = base.concat(name);
		File input = new File(fname);
		TemplateCache tcache = filecache.get(fname);
		
		//filecache contains input file and Equals the lastModified
		if (filecache.containsKey(fname) 
				&& input.lastModified() == tcache.lastModified) {
			reader = new BufferedReader(new StringReader(tcache.getContent()));
		} else {
			//packing io stream
			tcache = new TemplateCache(input.lastModified());
			filecache.put(name, tcache);
			reader = new TemplateReader(
					tcache
					,new InputStreamReader(new FileInputStream(fname) ,coding));
		}
		return reader;
	}
	
	/*
	 * @param    name method`s name
	 * @param    objs method paramters
	 * @return   methods the results
	 */
	public Object execfun(String name, Object... objs)
	{
		try {
			//funcs An array of cached method
			for (Method method: funcs) {
				if (name.equals(method.getName())) {
					Object obj = method.invoke(null, objs);
					return obj;
				}
			}
		} catch (Exception e) {
			throw new Error(e);
		}
		return null;
	}
	
	/*
	 *@param   base  html文件目录名
	 *@param   coding 文件默认编码
	 *@param   fclss  更多方法库class
	 *@return  返回已缓存的指定base的Template,或new一个新Template
	 */
	public static Template getTemplate(String base, String coding, Class<?>...fclss)
	{
		//缓存Template:仅根据base和coding的值
		int hashcode = getHashCode(base, coding);
		Template template = tmap.get(hashcode);
		if (template == null) {
			template = new Template(base, coding, fclss);
			tmap.put(hashcode, template);
		}
		return template;
	}
	
	/*
	 *@param    Object对象数组
	 *@return   返回数组中对象异或后的hashcode
	 */
	private static int getHashCode(Object...args)
	{
		int hashcode = 0;	//0^a == a
		for (Object obj: args) {
			if (obj == null) continue;
			hashcode ^= obj.hashCode();
		}
		return hashcode;
	}
	//test
	public static void main(String[] args) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, Object> suju = new HashMap<String, Object>();
		suju.put("time", new Timestamp(System.currentTimeMillis()));
		suju.put("array", new int[]{5, 4});
		data.put("suju",suju);
		data.put("links", "baidu,www.baidu.com;sina,www.sina.com");
		data.put("nums", new int[]{5, 4});
		Template t1 = null;
		for (int i=0; i < 1; i++) {
			t1 =Template.getTemplate("view/", null);
			String result = t1.parse("demo.html", data);
			System.out.println("re:"+result);
		}
	}
}
