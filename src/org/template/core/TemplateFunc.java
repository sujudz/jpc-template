package org.template.core;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class TemplateFunc {
	
	/*
	 *@param  number 数值字符串形式
	 *@return integer
	*/
	public static int num(String number)
	{
		return Integer.parseInt(number);
	}
	
	/*
	 * @param   str 待分割字符串
	 * @param   regex 拆分字符串或正则表达式
	 * @return  拆分后字符串数组
	 */
	public static String[] split(String str, String regex)
	{
		return str.split(regex);
	}
	
	/*
	 * @param    num  生成int数组长度
	 * @return   返回生成的int数组
	 */
	public static int[] range(int num)
	{
		int[] array = new int[num];
		while (num > 0) {
			array[--num] = num;
		}
		return array;
	}
	
	/*
	 * @param    time  默认时间格式
	 * @return   格式化后系统默认显示格式
	 */
	public static String formatTime(Timestamp time)
	{
		return DateFormat.getInstance().format(time);
	}
	
	/*
	 * @param    str 待格式化字符串
	 * @return   返回utf-8编码后的字符串
	 */
	public static String formatStr(String str)
	{
		try {
			str = URLDecoder.decode(str,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}
		return str;
	}
}
