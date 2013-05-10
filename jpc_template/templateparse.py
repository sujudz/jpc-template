# -*- coding: utf-8 -*
import re
import templateexpre

# exprepat 表达式正则
# tagpat 语法标签正则 $if,$for ${}等
# forpat for循环正则
# varpat 变量解析正则,变量仅支持纯英文,包含在双引号中""
# dictpat 字典引用，格式:names.name
# arrpat 数组引用 ：names[0]
# assiginpat 赋值正则解析 :x = 1
# funpat 函数格式解析 :split("1,2",",")
# operator 支持的运算符
# stack 表达式栈，cstack 表达式{}中值存放栈 varstack 变量作用域栈

exprepat = re.compile("\((\w+[^{}|&]*)\)", re.DOTALL)
tagpat = re.compile("(\\\\?)(?:[{}]|\$(var|if|for|include|else|)([^{}$\n]*))", re.DOTALL)
forpat = re.compile("\(\x20?([\d\w]+)\x20in\x20([^\n]+)\x20?\)", re.DOTALL)
dictpat = re.compile("\w+\.(?:\w+\.)*\w+$")
strpat = re.compile("\"([^\"\']*)\"", re.DOTALL)
varpat = re.compile("\w[\w\d_]*$", re.DOTALL)
arrpat = re.compile("([\w\d_]+)\[([\d]+)\]", re.DOTALL)
assignpat = re.compile("(\w+)\x20?=\x20?([^\n;]+);", re.DOTALL)
funpat = re.compile("(\w+)\(([^\n{}]*)\)", re.DOTALL)

#templateparse
class templateparse(object):
    lcb = 0
    isanno = False
    TPNAME = 'tobj'
    strstack = ''
    
    def __init__(self, template, joindata):
        self.template = template
        self.ifstack = []
        self.tagstack = []
        self.cstack = []
        self.convertData(templateparse.TPNAME, joindata)
    
    def initglobalvar(self):
        self.data = {
            "True":True,
            "False":False,
            "NULL":None
            }
        
    def convertData(self, tpname, joindata):
        self.initglobalvar()
        for obj in joindata:
            if isinstance(obj, dict):
                self.data.update(obj)
            else:
                self.data[tpname] = obj

    def parse(self, name):
        """parse
        init default variable map: data
        按行解析line并根据utf-8编码进行编码解码
        """
        str = ''
        file = self.template.getreader(name)
        for line in file:
            str += self.translation(unicode(line, 'utf-8'))
            if self.isanno:
                str = str.replace("\\", "")
                self.isanno = False
        return str.encode('utf-8')
    
    # translation
    def translation(self, line):
        """translation
            rline 返回解析后字符串
            start 起始搜索索引
            line 搜索字符串
            
            line = rline if isline else line
            根据isline值，更改搜索字符串line的值
            isline和end同步进行更改

            rline = ''.join((self.strstack, rline))
            self.strstack = ''
            返回rline字符串时，添加strstack内容
        """
        start = 0
        rline = line
        while True:
            result = tagpat.search(line, start)
            if result is None: break
            start, rline, isline = self.search(result, line)
            line = rline if isline else line

        if self.lcb > 0:
            rline ='' if rline.strip() is '' else rline
            self.cstack.append(''.join((self.cstack.pop(), rline)))
            rline = ''

        rline = ''.join((self.strstack, rline))
        self.strstack = ''
        
        # Change the rline coding
        if isinstance(rline, str):
            rline = unicode(rline, encoding='utf-8')
        return rline
    
    #replace
    def replace(self, line, lmark, rmark):
        """replace variable value
        format : ${var} --variable
        return line is replace
        """
        var = line[lmark+1: rmark]
        var_value = unicode(self.getvar(var))
        line = line.replace("${%s}" % (var), var_value)
        return line

    def parameters(self, pmstr):
        """get function variable value"""
        param = [self.getvar(var) for var in pmfromstr(pmstr, ',')]
        return param

    def getvar(self, var, domain=None):
        """variable value in all domain
            dictpat.match(var) -> match fromat: user.name
            var in self.varstack -> local variable
            var.isdigit() -> variable is number:123
            '[' and ']' in var -> variable is array index: nums[1]
            var is "" or '\"' in (var[-1], var[0]) -> variable is string: "name"
            else the variable is function
            data is None when use default data
        """
        if domain is None: domain = self.data
        try:
            if isstr(var):
                var_value = strpat.match(var).group(1)
            elif var.isdigit():
                var_value = int(var)
            elif isdict(var):
                for vitem in pmfromstr(var, '.'):
                    var_value = self.getvar(vitem, domain)
                    domain = var_value
            elif '[' and ']' in var:
                var, index = arrpat.match(var).group(1, 2)
                var_value = self.getvar(var)[int(index)]
            elif isvar(var):
                if isinstance(domain, dict):
                    var_value = domain[var]
                else:
                    var_value = getattr(domain, var)
            else:
                # variable is function
                fun, param = funpat.match(var).group(1, 2)
                if domain != self.data:
                    param = self.parameters(param)
                    var_value = self.execmethod(domain, fun, param)
                else:
                    param = self.parameters(param)
                    var_value = self.template.execfun(fun, param)
        except Exception, e:
            raise Exception(e, var, "Variable is undefined")
        
        if isinstance(var_value, unicode):
            return var_value.encode("unicode-escape").decode("unicode-escape")

        return var_value

    def include(self, line, expre, span):
        start, end = span
        result = exprepat.search(expre)
        if result is not None:
            line = "%s%s%s" % (
                line[:start],
                self.parse(result.group(1)),
                line[start+8+result.end(): end]
                )
        return line
    
    def execmethod(self, data, fun, parameter):
        return getattr(data, fun)(*parameter)

    def search(self, result, line):
        """search tag
        isline default False,
            At the end of the line location continue to search
        三种搜索情况：
            1、tag = $ 进行线性搜索寻找{和}，如果缺一，抛出异常
            2、cod = (var, for, if)中的一种，更改line值为去掉
            line[start:end]字符后，并且(cod, expre, tag)值入
            stack栈
            3、tag = { stack栈顶元组第一项([0])和tag值组合成一个
            2元素元组入栈，保存 { 前部字符串,strstack字符串递加上
            line[:start]字符.并且line值更改为line[end:]
            改变isline,end值，从头开始搜索
            3、tag = }
                cstack进行出栈操作，取出{}中间的内容体，stack出栈
            两次,获取最近一次内容体是那种表达式,(cod, expre, alltag)
            根据cod值进行内容体处理.
            如果包含是包含在for循环体内部的复杂循环体,则保存内容体,最
            后在for循环体方法中执行。
        """
        taginfo = result.group(0, 1, 2, 3)
        tag, anno, cod, expre = taginfo
        start, end = result.span()
        lineobj = (end, line, True)
        if tag == '{':
            if self.tagstack:
                if self.lcb == 0:
                    self.cstack.append('')
                    self.strstack = ''.join((self.strstack ,line[:start]))
                    lineobj = (0, line[end:], True)
                self.lcb += 1
        elif tag == '}':
            if self.tagstack:
                self.lcb -= 1
                if self.lcb == 0:
                    cod, expre, alltag = self.tagstack.pop()
                    func = ''.join(("_cod_", cod))
                    content = ''.join((self.cstack.pop(), line[:end-1]))
                    lineobj = getattr(self, func)(content, expre, line, end)
        else:
            if self.lcb == 0:
                return self.tagswitch(line, taginfo, (start, end))
                    
        return lineobj

    def tagswitch(self, line, taginfo, span):
        start, end = span
        tag, anno, cod, expre = taginfo
        if anno == '\\':
            self.isanno = True
        elif tag == '$':
            lmark = line.find('{', end)
            rmark = line.find('}', end)
            if lmark != -1 and rmark != -1:
                line = self.replace(line, lmark, rmark)
                return (lmark, line, True)
            else:
                raise Exception("not fount '{' or '}'")
        elif cod in ('var', 'for', 'if', 'else'):
            self.tagstack.append((cod, expre, tag))
            line = ''.join((line[:start], line[end:]))
            return (0, line, True)
        elif cod == "include":
            line = self.include(line, expre, span)
        return (end, line, True)
                
    def _cod_if(self, content, expre, line, end):
        """if processing function
            if exp_result re format search,
            result group(lop, op, rop)
            if opeator(lop, op, rop) is true,
            return content,is false then return empty string
            isline is True and end = 0
            From the beginning to search
        """
        expre = expre.replace("\\s", "")[1:-1]
        result = templateexpre.evalexpre(expre, self)
        self.ifstack.append(result)
        if result:
            line = ''.join((content, line[end:]))
        else:
            line = line[end:]
        return (0, line, True)

    def _cod_else(self, content, expre, line, end):
        try:
            if self.ifstack.pop():
                line = ''.join((content, line[end]))
            else:
                line = line[end]
        except:
            raise Exception(e, "not found if")
        return (0, line, True)
    
    def _cod_for(self, content, expre, line, end):
        """for processing function
            Parses the string loop variable body,
            and join in the stack varstack fitem correspond
            to the name of the variable's value
            At the end of the line position to continue to search
            解析循环变量体字符串，并在varstack栈中加入fitem对应名称的变量值
            在line的end位置继续搜索
        """
        for_result = re.search(forpat, expre)
        fitem, flist = for_result.group(1, 2)
        laststr = line[end:]
        line = ''
        for forvar in self.getvar(flist):
            self.data[fitem] = forvar
            line = ''.join((line, self.translation(content)))
            
        self.data.pop(fitem)
        return (end, ''.join((line, laststr)), True)
        
    def _cod_var(self, content, expre, line, end):
        """variable processing function
        For assignment operation, is a global variable
        进行赋值操作，指定的是全局变量
        """
        start = 0
        content = self.translation(content)
        while True:
            result = assignpat.search(content, start)
            if result is None: break
            start = result.end()
            lop, rop = result.group(1, 2)
            self.data[lop] = templateexpre.evalexpre(rop, self)
        line = line[end:]
        return (0, line, True)

    def replace(self, line, lmark, rmark):
        """replace variable value
        format : ${var} --variable
        return line is replace
        """
        var = line[lmark+1: rmark]
        var_value = unicode(self.getvar(var))
        line = line.replace("${%s}" % (var), var_value)
        return line
    
# module function
def isdict(var):
    lb = point = 0
    arr = list(var)
    if arr[0] == '.' or arr[-1] == '.':
        return
    for i in range(len(arr)):
        if arr[i] == '(':
            lb += 1
        elif arr[i] == ')':
            lb -= 1
        elif lb == 0 and arr[i] == '.':
            if arr[i-1] == '.':return False
            point += 1
    if point > 0: return True
    return False

def isstr(var):
    if var[0] =='\"' and var[-1] == '\"':
        return True
    return False

def isvar(var):
    return varpat.match(var)

def pmfromstr(pmstr, sp):
    """
        get function param from string:
        #获取函数参数列表，以英文,号分割#
        example split("tom,man",",")
        pmstack ： "( 符号存放 列表
        result ： 返回列表
        start, end : 函数中,号索引标志值

        for char in ''.join((pmstr,',')):
        #函数参数列表结尾附加一个,用于解析最后一个参数#
    """
    pmstack = []
    start = end = 0
    result = []
    if pmstr == '':return []
    
    for char in ''.join((pmstr, sp)):
        end += 1
        if char == '\"':
            if pmstack and pmstack[-1] == '\"':
                pmstack.pop()
                continue
            else:
                pmstack.append(char)
        elif char == ')':
            if pmstack and pmstack[-1] == '(':
                pmstack.pop()
                continue
            else:
                raise Exception("not found )")
        elif char == '(':
            pmstack.append(char)
        elif pmstack and pmstack[-1] in ('\"', '('):
            continue
        elif char == sp:
            result.append(pmstr[start: end-1])
            start = end
    return result
