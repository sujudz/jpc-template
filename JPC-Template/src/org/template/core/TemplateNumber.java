package org.template.core;

import java.math.BigDecimal;

public class TemplateNumber extends BigDecimal{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1001L;

	/*
	 *@param   val 转换对象，可以是Number对象或者数值字符串
	 */
	public TemplateNumber(Object val) {
		super(val.toString());
	}
	
	/*
	 * @param   op  运算符
	 * @param   bdrop   对比数值对象
	 */
	public boolean compareToB(char op, TemplateNumber bdrop) {
		//返回父类对比int类型结果
		int result = super.compareTo(bdrop);
		switch (op) {
			case '>':
				return result > 0 ? true : false;
			case '<':
				return result < 0 ? true : false;
			case '>'+'=':
				return result >= 0 ? true : false;
			case '<'+'=':
				return result <= 0 ? true : false;
			default:
				throw new Error("Operator is not supported");
		}
	}
	
}
