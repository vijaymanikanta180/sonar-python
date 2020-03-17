import zope
from argumentNumberImported import fn
fn(1, 2) # OK, no project level information

def functions():

    def foo(p1,p2): pass

    foo(42) # Noncompliant {{Add 1 missing arguments; 'foo' expects 2 positional arguments.}}
#   ^^^
    foo(p1=42) # Noncompliant {{Add 1 missing arguments; 'foo' expects 2 positional arguments.}}

    foo(1,2,3) # Noncompliant {{Remove 1 unexpected arguments; 'foo' expects 2 positional arguments.}}
#   ^^^
    foo(1, x = 42) # Noncompliant {{Remove this unexpected named argument 'x'.}}
#          ^^^^^^


    foo(1, 2) # OK
    foo(1, p2 = 2) # OK
    args = [1,2]
    foo(*args) # OK


    def foo_with_default_value(p1, p2 = 42): pass

    foo_with_default_value() # Noncompliant {{Add 1 missing arguments; 'foo_with_default_value' expects 1 positional arguments.}}
#   ^^^^^^^^^^^^^^^^^^^^^^
    foo_with_default_value(42) # OK
    foo_with_default_value(1,2,3) # Noncompliant {{Remove 1 unexpected arguments; 'foo_with_default_value' expects 1 positional arguments.}}

    if True:
        def foo_with_multiple_binding(p1, p2): pass
    else:
        def foo_with_multiple_binding(p1): pass

    foo_with_multiple_binding(1) # OK

    def foo_with_keyword_only(p1, *, p2): pass

    foo_with_keyword_only() # Noncompliant {{Add 1 missing arguments; 'foo_with_keyword_only' expects 1 positional arguments.}} {{Add the missing keyword arguments: 'p2'}}
#   ^^^^^^^^^^^^^^^^^^^^^
    foo_with_keyword_only(42) # Noncompliant {{Add the missing keyword arguments: 'p2'}}
#   ^^^^^^^^^^^^^^^^^^^^^
    foo_with_keyword_only(1, 2, p2 = 3) # Noncompliant {{Remove 1 unexpected arguments; 'foo_with_keyword_only' expects 1 positional arguments.}}

    def foo_with_variadics(**kwargs): pass

    foo_with_variadics(1, 2, 3) # OK

    def foo_with_optional_keyword_only(p1, *, p2 = 42): pass
    foo_with_optional_keyword_only(42) # OK
    from mod import dec
    @dec
    def foo_with_decorator(): pass

    foo_with_decorator(1, 2, 3) # OK

def methods():
    def meth(p1, p2): pass
    class A:
      def __new__(cls, a, b):
          cls.__new__(A, 1, 2)
      def meth(self, p1, p2):
        meth(1, 2) # OK
        meth(1, 2, 3) # Noncompliant
      @classmethod
      def class_meth(cls, p1, p2): pass
      @staticmethod
      def static_meth(p1, p2): pass
      def foo(p1): pass
      foo(42)

    A.class_meth(42) # FN {{'class_meth' expects 2 positional arguments, but 1 was provided}}

    A.class_meth(1, 2) # OK
    A.static_meth(42) # FN {{'static_meth' expects 2 positional arguments, but 1 was provided}}

    A.static_meth(1, 2) # OK
    a = A()
    a.meth(42, 43)
    a.meth(42) # Noncompliant {{Add 1 missing arguments; 'meth' expects 2 positional arguments.}}
    a.meth(42, 43, 44) # Noncompliant {{Remove 1 unexpected arguments; 'meth' expects 2 positional arguments.}}

    A.foo() # FN
    A.foo(42)

    m = a.meth
    m(42, 43) # OK
    m(42) # FN

    class MyInterface(zope.interface.Interface):
        def foo(): pass
    x = MyInterface()
    x.foo()


    # Coverage: loop in inheritance
    class A1(A2):
      def fn(self): pass

    class A2(A3):
      pass

    class A3(A1):
      pass

    a1 = A1()
    a1.fn(42) # Noncompliant


    class B1:
      def __reduce__(self, p1, p2):
        pass

    class B2(B1):
      def foo(self):
        super().__reduce__(1, 2) # OK, __reduce__ is not 'object.__reduce__' but B1.__reduce__
