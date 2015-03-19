<b>读取一个数组</b>
<br />
```
<!-- for循环数组，集合set或者list，name保存每次迭代的对象 -->

$for(name in names){
	${name} 
}
```
<br />
<b>读取一个map</b>
<br />
```
<!-- 
	for循环一个map book值为map迭代的Map.ntry对象
	取值对应字段属性或方法，book.key，先搜索key字段值，
	如果没有key字段，就使用getKey()方法获取值
 -->
$for(book in books)
{
	名称:${book.key}<span> 价格:${book.value}</span>
}
```
<br /><br />
<b>全局方法调用，格式化显示时间</b>
<br />
```
<!-- formatTime是全局方法 -->
现在时间是：${formatTime(now)}
```
<br /><br />
<b>for循环嵌套if使用</b>
<br />
```
<!-- ==号运算符比较字符串，在java中会自动转换为equals对比值 -->
$for(name in names){
	$if(name == "ibm"){
		<u>只显示${name}</u>
	}
}
```
<br /><br />
<b>语句表达式使用</b>
<br />
<b>比较价格大于等于22.5的书</b><br />
```
<!-- map的迭代对象，Map.Entry 也可以直接使用其方法getValue()获取值 -->
$for(book in books)
{
	$if(book.getValue() >= 22.5) {
		${book.getKey()} 价格大于等于22.5
	}
	$else {
		${book.getKey()} 价格小于22.5
	}
	<br/>
}
```
<br />
<b>对象方法调用</b>
<br />
```
<!-- 可以直接调用对象的方法，如果对象有这个方法 -->
$for(name in names){
	${name}名称的长度是:${name.length()}<br/>
}
```
<br />
<b>赋值语句和数组使用</b><br />
```
<!-- 
	数组取单个值，使用array[0] 这种格式 
	var语句可以进行赋值，值存放在全局变量域中
-->
$for(str in url.split(";")){
	$var{name=str.split(":");}
	名称:${name[0]} 网址:${name[1]}<br/>
}
```
<br />
<b>自定义方法</b>
<br />
```
${helloWorld("hello world")}
<br/>
${sayHello("hello world")}
```