package org.template.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


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
		if (base == null) base = "";
		File dir = new File(base);
		this.coding = coding != null ? coding: defcod;
		//物理路径
		if (dir.isDirectory()) {
			this.base =base;
		} else {
			//classpath Relative paths
			this.base = Thread.currentThread().
						getContextClassLoader().getResource(base).getFile();
		}
		loadFunc(PUBFUNC, fclss);
		filecache = new HashMap<String, TemplateCache>();
	}
	
	/*
	 *@param  paths       class路径或class对象
	 *				      The class path or the class object
	 *@return Class<?>[]  存放全局方法的class数组
	 *					  Storage class array of global method
	 */
	public static Class<?>[] loadFuncClass(Object... paths)
	{
		ArrayList<Class<?>> clss = new ArrayList<Class<?>>();
		for (Object path: paths) {
			if (path instanceof Class) {
				//load class
				clss.add((Class<?>)path);
			} else if (path instanceof String) {
				String name = path.toString();
				if (name.endsWith(".jar")) {
					//load class to jar
					clss.addAll(loadClassToJar(name));
				} else {
					//load class to package
					clss.addAll(loadPackage(name));
				}
			}
		}
		return clss.toArray(new Class<?>[clss.size()]);
	}
	
	/*
	 *@param  jarfile         jar file
	 *@return list<Class<?>>  Collection of the class
	 */
	private static List<Class<?>> loadClassToJar(String jarfile)
	{
		ArrayList<Class<?>> clss = new ArrayList<Class<?>>();
		try {
			JarFile jar = new JarFile(jarfile);
			//jar包中的枚举集合
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.endsWith(".class")) {
					//去掉末尾的.class
					name = name.substring(0, name.lastIndexOf('.'));
					//org/suju/func/userfunc转换org.suju.func.userfunc形式
					clss.add(Class.forName(name.replace('/', '.')));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clss;
	}
	
	/*
	 *@param  pack            package名
	 *@return List<Class<?>>  返回包中的所有类的集合
	 */
	private static List<Class<?>> loadPackage(String pack)
	{
		ArrayList<Class<?>> clss = new ArrayList<Class<?>>();
		//使用当前线程类的线程创建者的ClassLoader,载入包实际路径
		URL filter=Thread.currentThread().
					getContextClassLoader().getResource(pack.replace('.', '/'));
		try {
			String [] mappings=new File(filter.toURI()).list();
			for (String mapping: mappings){
				//去掉后缀.class
				String name = mapping.substring(0, mapping.lastIndexOf('.'));
				//pack包和name组合成class名称
				Class<?> mapclazz=Class.forName(pack.concat(".").concat(name));
				clss.add(mapclazz);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return clss;
	}
	
	/*
	 * load public class method
	 * @param class[] 2d array
	 */
	private void loadFunc(Class<?>[]...fclss)
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
}
