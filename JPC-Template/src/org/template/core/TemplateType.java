package org.template.core;

public enum TemplateType {
	/** 常用数据类型枚举 */
	SHORT("short", Short.class), INT("int", Integer.class),
	LONG("long", Long.class), BYTE("byte", Byte.class), CHAR("char", Character.class),
	BOOLEAN("boolean", Boolean.class), FLOAT("float", Float.class), DOUBLE("double", Double.class);
	
	/** 枚举名称 */
	String name;
	
	/** 枚举类型 */
	Class<?> type;
	
	TemplateType(String name, Class<?> type)
	{
		this.name = name;
		this.type = type;
	}
	
	/*
	 *@param   name Class类名称
	 *@return  返回基本数据类型对应的对象Class
	 */
	public static Class<?> conver(Class<?> cls)
	{
		for (TemplateType tenum: TemplateType.values()) {
			if (tenum.name.equals(cls.getSimpleName())) {
				return tenum.type;
			}
		}
		return cls;
	}
}
