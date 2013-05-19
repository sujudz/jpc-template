package org.template.core;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateObj<T> {
	
	/** 包装对象 */
	private T obj;
	
	public TemplateObj(T obj)
	{
		this.obj = obj;
	}
	
	/*
	 * @param    obj  泛型被包装对象
	 * @return   返回TemplateObj对象，包装obj泛型
	 */
	public static <K> TemplateObj<K> convert(K obj)
	{
		return new TemplateObj<K>(obj);
	}
	
	/*
	 * @param    index  数组列表索引
	 * @return   返回数组列表索引项
	 */
	public Object getObj(int index)
	{
		Object result = null;
		try {
			if (obj instanceof List) {
				//返回list索引项
				result = ((List<?>) obj).get(index);
			} else {
				result = Array.get(this.obj, index);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/*
	 * @return   obj包装对象转数组
	 */
	public Object[] getArray()
	{
		if (obj instanceof Collection) {
			return ((Collection<?>)obj).toArray();
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>)obj).entrySet().toArray();
		}
		int length = Array.getLength(obj);		//数组length
		Class<?> type = obj.getClass().getComponentType();
		//使用TemplateType枚举转换基本数据类型type类型
		type = TemplateType.conver(type);
		
		Object[] result = (Object[])Array.newInstance(type, length);
		for (int i = 0; i < result.length; i++) {
			result[i] = getObj(i);
		}
		return result;
	}
	
	/*
	 * @return   返回包装obj
	 */
	public T getValue()
	{
		return this.obj;
	}
	
	/*
	 * @return   返回强制转换K后的包装obj
	 */
	@SuppressWarnings("unchecked")
	public <K> K getValue(Class<K> type)
	{
		return (K)this.obj;
	}

	/*
	 * @return  返回包装对象字符串形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (obj == null) return "null";
		return this.obj.toString();
	}
}
