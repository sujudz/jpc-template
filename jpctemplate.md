C, python, Java web template parser<br />
version: 1.0  only supports Java and python<br />
目前支持python和java语言的web模板解析
<br />
<b>创建Template：<br>
<br />载入自定义函数，package包或者jar包文件，jar包要真实物理路径(funcs自定义函数不是必须的，可以没有)</b>
<br />
```
Class<?>[] funcs = Template.loadFuncClass("org.suju.userfunction",
                   request.getServletContext().getRealPath("WEB-INF/lib/funcs.jar"));
```

<b>使用web-inf下目录</b><br />
```
String dir = request.getServletContext().getRealPath("WEB-INF") + "/";

Template template = Template.getTemplate(dir, null, funcs);
//第二个参数是文件编码选项，null值表示使用默认utf-8。
```
<b>使用src下相对目录</b><br />
```
Template template = Template.getTemplate("html", null, funcs);
```
<b>html 模板文件相对目录<br />
name html文件名（如：xx.html） datamap 传入模板的变量（map或Object)</b><br />
```
response.getWriter().write(template.parse(name, datamap));
```
<br /><br />
<span>模板语句使用$开头，会自动过滤html中的javascript代码，和jquery的默认标记$。</span><br />
Template statements use $at the beginning, will automatically filter the javascript code in the HTML, and jquery default tags $.<br />
<br />
<span>模板语句支持</span><br />
Template parser support:<br />
<br />
<strong>statement:</strong><br />
${var}<br />
<strong>Description:</strong><br />
<span>变量值,支持对象的属性和方法。或map键值</span><br />
Variable values, support for object attributes and methods. Or take the map key/value<br />
<br />
<strong>Example:</strong><br />
${id}<br />
${user.name}<br />
${user.say()}<br />
${user.name.toString()}<br />
<br />
<strong>statement:</strong><br />
$if (expression) {<br />
&nbsp; &nbsp; statement<br />
}<br />
$else{<br />
&nbsp; &nbsp; statement<br />
}<br />
<strong>Description:</strong><br />
<span>if语句根据表达式结果执行语句，if可以跟随一个esle语句。esle语句不能独立出现</span><br />
The if statement: according to the result of expression statement execution, if can follow a esle statement. Esle statement cannot be independent<br />
<br />
<strong>Example:</strong><br />
$if(user.say()!=NULL){}<br />
$if(user.id&gt;=0){}<br />
$if(user.id&gt;=0 | name!=NULL)<br />
{<br />
&nbsp; &nbsp; statement<br />
}<br />
$else<br />
{<br />
&nbsp; &nbsp; statement<br />
}<br />
<br />
<strong>statement:</strong><br />
$for(item in list)<br />
{<br />
&nbsp; &nbsp; ${item}<br />
}<br />
<strong>Description:</strong><br />
<span>for语句:迭代list，使用item保存迭代值，并重复执行循环体，直到迭代结束.item变量作用域为for循环内部</span><br />
For statement: iterative list, use the item save iterative value, and repeat the loop body, until the end of the iteration.The item inside the for loop variable scope<br />
<span>list可以为数组或list</span><br />
The list can be an array or a list<br />
<br />
<strong>Example:</strong><br />
$for(item in user.names())<br />
{<br />
&nbsp; &nbsp; ${item}<br />
}<br />
<br />
<strong>statement:</strong><br />
$var{<br />
&nbsp; &nbsp; x=5;<br />
&nbsp; &nbsp; y=6;<br />
}<br />
<strong>Description:</strong><br />
<span>var语句:解析{}中语句，根据=区分键值对，把值赋值给键。并把键存入全局变量域</span><br />
Var statements: parsing {} in the statements, according to the = key/value pair, the value assigned to the key. And put the key in the global variable domain<br />
<br />
<strong>Example:</strong><br />
$var{<br />
&nbsp; &nbsp; name=user.name();<br />
&nbsp; &nbsp; name2=&quot;tom&quot;;<br />
}<br />
<br />
<strong>statement:</strong><br />
$include(filename)<br />
<strong>Description:</strong><br />
<span>包含文件语句，可以解析指定名称的文件，并包含在当前文件$include位置处.文件名称不包含&quot;号</span><br />
Include file statements, can resolve the name of the specified file, and included in the current file $include location.The file name does not contain &quot;number<br />
<br />
<strong>Example:</strong><br />
$include(head.html)<br />
<br />
<span>可以使用对象方法，模板也有内置全局方法.</span><br />
Can use the object method, template method also has a built-in global.<br />
<br />
<strong>Example:</strong><br />
${split(&quot;1,2,3&quot;,&quot;,&quot;)}<br />
<span>分割字符串，返回数组</span><br />
Split a string and returns an array<br />
<br />
${user.say(&quot;hello&quot;)}<br />
<span>调用对象的有参数方法</span><br />
There are parameters of the call object method