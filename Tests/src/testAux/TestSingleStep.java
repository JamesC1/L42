package testAux;

import helpers.TestHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import facade.Parser;
import reduction.SmallStep;
import sugarVisitors.Desugar;
import sugarVisitors.InjectionOnCore;
import ast.Ast.Stage;
import ast.ExpCore;
import ast.Expression;
import ast.Ast.Path;
import ast.ExpCore.ClassB;
import auxiliaryGrammar.Norm;
import auxiliaryGrammar.Program;

public class TestSingleStep {

  @RunWith(Parameterized.class)
  public static class Test1 {
    @Parameter(0) public String e1;
    @Parameter(1) public String e2;
    @Parameterized.Parameters
    public static List<Object[]> createData() {
      return Arrays.asList(new Object[][] {
         {"(Any x= Outer0::C.new() void)","(Outer0::C x= Outer0::C.new() void)"
       },{"( Outer0::C xx= Outer0::C.new()"
        + "  mut Outer0::C x= (Outer0::C z=Outer0::C.new() y)"
        + "  Outer0::C zz= Outer0::C.new()"
        + "  xx)",
         "(Outer0::C xx= Outer0::C.new()"
       + " Outer0::C z=Outer0::C.new()"
       + " mut Outer0::C x= y"
       + " Outer0::C zz= Outer0::C.new()"
       + " xx)"

       },{"( mut Outer0::D x= (Outer0::D z=Outer0::D.new(x:zz) Outer0::C zz=Outer0::C.new() y)"
           + "  x)",
           " (Outer0::D z=Outer0::D.new(x:zz)"
          + " Outer0::C zz= Outer0::C.new()"
          + " mut Outer0::D x= y"
          + " x)"
       },{"{ A:{'@plugin\n'L42.is/connected/withAlu\n()}"
        + " C: using A check sumInt32(n1:{} n2:{}) error void}",
          "{ A:{'@plugin\n'L42.is/connected/withAlu\n()}"
        + " C:error {'@stringU\n'InvalidInt32\n}}"

       },{"{ A:{'@plugin\n'L42.is/connected/withAlu\n()}"
           + " C: using A check sumInt32(n1:{'@int32\n'5\n} n2:{'@int32\n'3\n}) error void}",
             "{ A:{'@plugin\n'L42.is/connected/withAlu\n()}"
           + " C:{'@int32\n'8\n}}"


       },{"(Outer0::C x= Outer0::C.new() void)","(void)"
       },{"loop void","(Void void0=void, loop void)"
       },{"(Outer0::C x= Outer0::C.new() Any y= y.m() void)","(Any y= y.m() void)"
       },{"(Any^ x= Outer0::C.new() x)","(Outer0::C x= Outer0::C.new() x)"

       },{"(capsule Any x= Outer0::C.new() x)",
          "(capsule Outer0::C x= Outer0::C.new() x)"

       },{"(capsule Outer0::C x= Outer0::C.new() x)",
          "(Outer0::C.new())"

       },{"( Any x= y x)","(y)"
       },{"( mut Any x= (Outer0::C z=Outer0::C.new() y) x)","( Outer0::C z=Outer0::C.new() mut Any x=  y x)"
       },{"( Any x= error (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::D y) x)","( Any x= error (Outer0::C z=Outer0::C.new() z) x)"
       },{"( Any x= error (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::C y) x)","( Outer0::C y=(Outer0::C z=Outer0::C.new() z) y)"
       },{"( Any x= (Any z=Outer0::C.new() z) catch error y (on Outer0::C y) x)",
          "( Any x= (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::C y) x)"

       },{"( Any x= (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::C y) x)",
          "( Outer0::C x= (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::C y) x)"

       },{"( Outer0::C x= (Outer0::C z=Outer0::C.new() z) catch error y (on Outer0::C y on Outer0::D y) x)",
         "( Outer0::C x= (Outer0::C z=Outer0::C.new() z)  x)"

       },{"Outer0::C.foo(bar:Outer0::C)","(type Outer0::C this0=Outer0::C,type Outer0::C bar=Outer0::C (bar.foo(bar:this0)))"

       },{" (type Outer0::C this0=Outer0::C,type Outer0::C bar=Outer0::C (bar.foo(bar:this0)))",
          " (type Outer0::C bar=Outer0::C ( bar.foo(bar:Outer0::C) ) )"

       },{" (type Outer0::C bar=Outer0::C ( bar.foo(bar:Outer0::C) ) )",
          " ((Outer0::C.foo(bar:Outer0::C)))"


       },{"(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c) (r).x())","(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c) (read Outer0::D d=(r) d.x()))"
       },{"(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c) r.x(that:(c)))","(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c) (Outer0::C c0=(c) r.x(that:c0)))"
       },{"Outer0::C.new()","(Outer0::C c=Outer0::C.new() c)"
       //
       },{"(Outer0::C c=Outer0::C.new() Outer0::D r=(Outer0::D d=Outer0::D.new(x:c) d) r.x())",
          "(Outer0::C c=Outer0::C.new() Outer0::D r=( Outer0::D d=Outer0::D.new(x:c) d ) ( Outer0::C c0=( Outer0::D d0=Outer0::D.new(x:c) d0.x() ) c0 ) )"

       },{"(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c)  r.x())",
          "(Outer0::C c=Outer0::C.new() Outer0::D r=Outer0::D.new(x:c)  c)"

       },{"(Outer0::C c1=Outer0::C.new() Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c1)  r.x(that:c2))",
         "(Outer0::C c1=Outer0::C.new() Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c2)  void)"
       },{"(Outer0::C c1=Outer0::C.new() ( Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c1)  r.x(that:c2)))",
          "(Outer0::C c1=Outer0::C.new() ( Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c2)  void  ))"

       },{"using Any check m(that:(Outer0::C c1=Outer0::C.new() Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c1)  r.x(that:c2))) void",
          "using Any check m(that:(Outer0::C c1=Outer0::C.new() Outer0::C c2=Outer0::C.new() mut Outer0::D r=Outer0::D.new(x:c2)  void)) void"

       },{" (using Any check m(that:(error void)) void catch error x (on Void void) void)",
         "( Void x=( void ) void )"

       },{"(C c1=C.new() ( C c2=C.new() mut D r=D.new(x:c1)  r.x(c2)))",
          "(C c1=C.new() ( C c2=C.new() mut D r=D.new(x:c2)  void  ))"
       },{"(C c1=C.new() mut D r=D.new(x:c1) (C c2=C.new() r.x(c2)))",
          "(C c1=C.new() mut D r=D.new(x:c2) C c2=C.new() (void))"
//not well typed
       },{"( mut D r=D.new(x:r) (mut D r1a=D.new(x:r1b) mut D r1b=D.new(x:r1b) (mut D r2=D.new(x:r1b) Any aa=r.x(r2) r1a)))",
          "( mut D r=D.new(x:r2) mut D r1b=D.new(x:r1b) mut D r2=D.new(x:r1b)  (mut D r1a=D.new(x:r1b) (Any aa= void r1a)))"
       },{"( mut D r=D.new(x:r) (mut D r1a=D.new(x:r1b) mut D r1b=D.new(x:r1b) (mut D r2=D.new(x:r1a) r.x(r2))))",
          "( mut D r=D.new(x:r2) mut D r1a=D.new(x:r1b) mut D r1b=D.new(x:r1b) mut D r2=D.new(x:r1a) ((void)))"
       },{"( mut D r=D.new(x:r) (mut D r1a=D.new(x:r1b) mut D r1b=D.new(x:r1b) (mut D r2a=D.new(x:r2b)mut D r2b=D.new(x:r1a) r.x(r2a))))",
          "( mut D r=D.new(x:r2a)mut D r1a=D.new(x:r1b)mut D r1b=D.new(x:r1b) mut D r2a=D.new(x:r2b)  mut D r2b=D.new(x:r1a) ((void)))"
       },{"{outer() C: {new() type method Library m() {inner()}} D: C.m()}",
            "{outer() C: {new() type method Library m() {inner()}} D:(type C this0=C { inner()} )}"
       },{"{ method Library m() ({outer() C: {new() type method Library m() ({inner()})} D: C.m()})}",
         "{ method Library m() ({ outer()C:{ new()type method Library m() ({ inner()})}D:(type C this0=C ({ inner()} ))})}"


         //can not work without the normalization
       //},{"{ C:( Library result=( ( ( Library result0=(  {'@exitStatus\n'0\n} ) result0 ) ) ) result )}",
         },{"{ C:( Library result=( ( ( Library result0=  {'@exitStatus\n'0\n}  result0 ) ) ) result )}",
           " { C:( Library result=( ( ( {'@exitStatus\n'0\n} ) ) ) result )}"
       },{TestHelper.multiLine("{"
        ,"C:{ k() type method Library ok() ({'@oK\n})"
        ,"        type method Library ko() ({'@kO\n})"
        ,"  }"
        ,"I:{interface }"
        ,"AI:{ k()<:Outer1::I}"
        ,"D:("
        ,"  Any z=error Outer0::AI.k()"
        ,"  catch error x (on Outer0::AI Outer0::C.ok()"
        ,"    )"
        ,"  Outer0::C.ko()"
        ,"  )}"),
        TestHelper.multiLine(
         "{C:{ k()type method Library ok() ("
        ,"  {'@oK"
        ,"}"
        ,"  )type method Library ko() ("
        ,"  {'@kO"
        ,"}"
        ,"  )}I:{interface }AI:{ k()<:Outer1::I}D:("
        ,"  Any z=error ("
        ,"    Outer0::AI aI=Outer0::AI.k()"
        ,"    aI"
        ,"    )"
        ,"  catch error x (on Outer0::AI Outer0::C.ok()"
        ,"    )"
        ,"  Outer0::C.ko()"
        ,"  )}")
       },{
TestHelper.multiLine("{"
,"AI:{ mut k()}"
,"Box:{ lent k(var read Any f)}"
,"D:("
,"  lent Box box=Box.k(f:box)"
,"  Any z1=("
,"    Outer0::AI any=(mut AI aI=AI.k() aI)"
,"    box.f(any))"
,"  Any z2=error box.f()"
,"  {ok()}"
,"  )}"
),TestHelper.multiLine("{"
,"AI:{ mut k()}"
,"Box:{ lent k(var read Any f)}"
,"D:("
,"  Outer0::AI any=(mut AI aI=AI.k() aI)"
,"  lent Box box=Box.k(f:any)"
,"  Any z1=(void)"
,"  Any z2=error box.f()"
,"  {ok()}"
,"  )}")

},{TestHelper.multiLine(" "
,"(Library cond=("
,"    Void unused5=("
,"      Void unused6=error void"
,"      ( Void unused4=return {}  void )"
,"      )"
,"    catch return result0 ( on Library result0 )"
,"    error void"
,"    )"
,"  void)"),
TestHelper.multiLine(" "
,"("
,"  Library cond=("
,"    Void unused5=error ( void )"
,"    error void"
,"    )"
,"  void)")

}});}

    @Test
    public void testStep() {
      TestHelper.configureForTest();
      ExpCore ee1=Desugar.of(Parser.parse(null," "+e1)).accept(new InjectionOnCore());
      ExpCore ee2=Desugar.of(Parser.parse(null," "+e2)).accept(new InjectionOnCore());
      Program p=TestHelper.getProgramCD();
      ExpCore eRed=new SmallStep().step(p, ee1);
      TestHelper.assertEqualExp(eRed,ee2);
    }

    }


}