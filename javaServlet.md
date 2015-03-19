
```
package org.suju.demo;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.template.core.Template;

/**
 * Servlet implementation class demo
 */
@WebServlet("/demo")
public class Demo extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public Demo() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/*
		 * 使用web-inf下目录
		 * String dir = request.getServletContext().getRealPath("WEB-INF") + "/";
		 * Template template = Template.getTemplate(dir, null, funcs);
		 */
		
		//载入自定义函数，package包或者jar包文件
		Class<?>[] funcs = Template.loadFuncClass(
				"org.suju.userfunction",
				request.getServletContext().getRealPath("WEB-INF/lib/funcs.jar"));
		
		//使用src下相对目录
		Template template = Template.getTemplate("html", null, funcs);
		
		//存入一个变量
		request.setAttribute("url", "baidu:www.baidu.com;sina:www.sina.com");
		//本地函数使用
		request.setAttribute("now", new Timestamp(System.currentTimeMillis()));
		//存入一个数组或集合
		request.setAttribute("names", new String[]{"ibm", "sun", "apple"});
		//存入一个map
		Map<String, Float> booksmap = new HashMap<String, Float>();
		booksmap.put("十万个为什么", 12.5f);
		booksmap.put("时间简史", 22.5f);
		booksmap.put("计算机导论", 45.6f);
		request.setAttribute("books", booksmap);
		
		//解析相对目录下的文件
		String content = template.parse("demo2.html", requestToMap(request));
		response.getWriter().write(content);
	}

	//request Attribute属性转map
	public Map<String, Object> requestToMap(HttpServletRequest request)
	{
		Map<String, Object> requestMap = new HashMap<String, Object>();
		Enumeration<String> e = request.getAttributeNames();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			requestMap.put(key, request.getAttribute(key));
		}
		return requestMap;
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}

```