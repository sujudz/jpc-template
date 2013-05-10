operas = ['(', ')', '#', '&', '|',
               '+', '-', '*', '/', '>', '<',
               '>'+'=', '<'+'=', '!'+'=', '='+'='];
OPDOMAINALL = operas[1:10]
OPDOMAIN = ('<', '>', '!', '=')
priority = [2, 3, 1, 5, 4, 7, 7, 8, 8, 6, 6, 6, 6, 6, 6];

# operator
def operator(rop, op, lop):
    return eval("lop %s rop" % (op))

def comopera(op1, op2):
    i = 0
    level1 = level2 = 0
    for ch in operas:
        if op1 == ch: level1 = priority[i]
        if op2 == ch: level2 = priority[i]
        if level1 != 0 and level2 != 0:
            break
        i += 1
    return level1 > level2

def expreopera(op, ops, exs):
    if comopera(op, ops[-1]):
        ops.append(op)
    else:
        if op == '#' and ops[-1] == '#':
            return
        ex1 = exs.pop()
        ex2 = exs.pop()
        exs.append(operator(ex1, ops.pop(), ex2))
        expreopera(op, ops, exs)
        
def evalexpre(expre, handler):
    ops = []
    exs = []
    isstr = False
    funtag = 0
    start = 0
    expre = ''.join((expre, '#'))
    ops.append('#')
    iteror = iter(range(len(expre)))
    for i in iteror:
        end = i
        op = expre[i]
        if isstr:
            if op == '\"': isstr = False
            continue
        if funtag > 0:
            if op == '(': funtag += 1
            elif op == ')': funtag -= 1
            continue

        if op == '\"': isstr = True
        elif op == '(':
            if end == 0 or expre[end-1] in operas:
                ops.append(op)
                start = i + 1
            else:
                funtag += 1
        elif op in OPDOMAINALL:
            if op in OPDOMAIN:
                if expre[end+1] == '=':
                    i += 1
                    iteror.next()
                    op += expre[i]
                    
            if ops[-1] != ')':
                exstr = expre[start: end]
                exs.append(handler.getvar(exstr))
            else:
                ops.pop()
                ops.pop()
            expreopera(op, ops, exs)
            start = i + 1
        else:
            continue
    return exs.pop()
