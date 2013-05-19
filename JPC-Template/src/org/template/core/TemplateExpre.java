package org.template.core;

import java.util.Stack;

public class TemplateExpre {

	/*
	 * TemplateExpre
	 * 表达式求值
	 * operas 运算符数组
	 * priority 运算符优先级数组，对应运算符数组
	*/
	/** 表达式运算符数组  */
	private static char[] operas =  {'#', '(', ')', '&', '|',
							'+', '-', '*', '/', '>', '<',
							'>'+'=', '<'+'=', '!'+'=', '='+'='};
	
	/** 表达式优先级 0最小，依次递增 */
	private static int[] priority = {1, 2, 3, 5, 4, 7, 7, 8, 8, 6, 6, 6, 6, 6, 6};
	
	/*
	 * @param   ch  表达式字符
	 * @return  true 运算符数组包含字符，false 不包含
	 */
	public static boolean isPrefix(char ch)
	{
		for (char c: operas) {
			if (ch == c) return true;
		}
		return false;
	}
	
	/*
	 * static boolean eval(Boolean lop, char op, boolean rop)
	 * 计算表达式
	 * @param  op1, op2  运算符
	 * @return true op1优先级高于op2
	 */
	public static boolean comopera(char op1, char op2)
	{
		//To optimize
		int i = 0;
		int level1 = 0, level2 = 0;
		for (char ch: operas) {
			if (op1 == ch) level1 = priority[i];
			if (op2 == ch) level2 = priority[i];
			//op1, op2都查到优先级，break for
			if (level1 != 0 && level2 != 0) break;
			i++;
		}
		return level1 > level2;
	}
	
	/*
	 *@param   rop, lop 表达式计算对象
	 *@param   op 运算符
	 *@return  Object 如果字符串运算，返回String
	 *                如果数值运算 返回BigDecimal
	 *                如果布尔运算 返回boolean
	 */
	public static Object eval(Object rop, char op, Object lop)
	{
		TemplateNumber bdlop = null, bdrop = null;
		//判断是否数值运算
		if (rop instanceof Number) {
			rop = bdrop = new TemplateNumber(rop);
		}
		if (lop instanceof Number) {
			lop = bdlop = new TemplateNumber(lop);
		}
		switch (op) {
			case '&':
				return (Boolean)lop & (Boolean)rop;
			case '|':
				return (Boolean)lop | (Boolean)rop;
			case '+':
				//返回连接后字符串
				if (lop instanceof String) {
					return ((String) lop).concat(rop.toString());
				}
				return bdlop.add(bdrop);
			case '-':
				return bdlop.subtract(bdrop);
			case '*':
				return bdlop.multiply(bdrop);
			case '/':
				return bdlop.divide(bdrop);
			case '!'+'=':
				if (lop == null) return lop != rop;
				return !lop.equals(rop);
			case '='+'=':
				if (lop == null) return lop == rop;
				return lop.equals(rop);
			default:
				//数值比较
				return bdlop.compareToB(op, bdrop);
		}
	}
	
	/*
	 *@param   op 运算符
	 *@param   ops 运算符栈
	 *@param   exs 操作数存放栈
	*/
	public static void expreOpera(char op, Stack<Character> ops, Stack<Object> exs)
	{
		Object ex1, ex2;
		//op运算符优先级小于运算符栈顶，运算符入栈
		if (comopera(op, ops.peek())) {
			ops.push(op);
		} else {
			if (op == '#' &&
					ops.peek() == '#') {
				return ;
			}
			ex1 = exs.pop();					//操作数1
			ex2 = exs.pop();					//操作数2
			exs.push(eval(ex1, ops.pop(), ex2));
			expreOpera(op, ops, exs);			//递归计算是否运算符栈
		}
	}
	
	/*
	 * @param   expre  计算表达式字符串
	 * @param   handler 辅助计算对象
	 *@return  Object 如果字符串运算，返回String
	 *                如果数值运算 返回BigDecimal
	 *                如果布尔运算 返回boolean
	 */
	public static Object evalExpre(String expre, TemplateParse handler)
	{
		char op = 0;        								//索引字符
		int funtag = 0;     								//函数标记
		String exstr = null;    							//操作数
		boolean isstr = false;								//字符串标记
		int start = 0, end = 0;								//操作数起始，结束索引
		Stack<Character> ops = new Stack<Character>();		//运算符栈
		Stack<Object> exs = new Stack<Object>();			//操作数栈
		expre = expre.concat("#");							//添加 结束运算符
		char[] array = expre.toCharArray(); 
		ops.push('#');										//push 起始运算符
		for (int i = 0, len = array.length; i < len; i++) {
			end = i;
			op = array[i];
			//字符串判断
			if (isstr) {
				if (op == '\"') isstr = false;
				continue;
			}
			//函数判断
			if (funtag > 0) {
				if (op == '(') funtag++;	//(函数起始标志
				if (op == ')') funtag--;	//)函数结束标志
				continue;
			}
			switch (op) {
				case '\"':
					isstr = true;
					break;
				case '(':
					//检测(是函数起始标记，还是运算括号
					if (end == 0 || isPrefix(array[end-1])) {
						ops.push(op);
						start = i + 1;
					} else {
						funtag++;
					}
					break;
				case '>':
				case '<':
				case '!':
				case '=':
					//判断是不是==,!=等双字符运算符
					if (array[end+1] == '=') {
						op += array[++i];
					}
				case ')':
				case '#':
				case '+':
				case '-':
				case '*':
				case '/':
				case '&':
				case '|':
					if (ops.peek() != ')') {
						//运算符栈顶非)，handler对象计算操作数值，入栈
						exstr = expre.substring(start, end);
						exs.push(handler.getvar(exstr, null).getValue());
					} else {
						ops.pop();		//运算符   )出栈
						ops.pop();		//运算符   (出栈
					}
					expreOpera(op, ops, exs);
					start = i + 1;
					break;
				default:
					continue;
			}
		}
		return exs.pop();
	}
}
