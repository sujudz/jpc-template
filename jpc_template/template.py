# -*- coding: utf-8 -*
import sys
from StringIO import StringIO
import os.path
from templateparse import templateparse

class treader(StringIO):
    def __init__(self, name):
        with open(name, 'r') as f:
            StringIO.__init__(self, f.read())
            
class template(object):
    coding = "UTF-8"
    funs = {}
    tmap = {}
    def __init__(self, base, coding=None, models=None):
        if coding is not None:self.coding = coding
        self.base = base
        self.filecache = {}
        self.loadfunc(models)

    def loadfunc(self, models):
        for model in models:
            self.funs.update(
                dict([(fun, getattr(model, fun)) for fun in model.__all__])
                )

    def execfun(self, fun, objs):
        if fun in self.funs:
            return self.funs[fun](*objs)
        else:
            raise Exception(e, "no function")
        
    @staticmethod
    def gethashcode(*args):
        hashcode = 0
        for obj in args:
            if obj is None: continue
            hashcode ^= hash(obj)
        return hashcode
    
    @staticmethod
    def gettemplate(base, coding=None, *models):
        hashcode = template.gethashcode(base, coding)
        if hashcode in template.tmap:
            tp = template.tmap[hashcode]
        else:
            tp = template(base, coding, models)
            template.tmap[hashcode] = tp
        return tp
    
    def parse(self, name, *joindata):
        tparse = templateparse(self, joindata)
        return tparse.parse(name)

    def getreader(self, name):
        fname = self.base + name
        #filecache {[lastModified, string],}
        lastModified = os.path.getmtime(fname)
        if fname in self.filecache:
            tcache = self.filecache[fname]
            if lastModified == tcache[0]:
                return StringIO(tcache[1])
        strio = treader(fname)
        self.filecache[fname] = [lastModified, strio.getvalue()]
        return strio
