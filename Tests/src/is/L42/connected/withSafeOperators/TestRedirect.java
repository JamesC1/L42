package is.L42.connected.withSafeOperators;

import static helpers.TestHelper.getClassB;
import static org.junit.Assert.fail;
import helpers.TestHelper;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import platformSpecific.javaTranslation.Resources;
import ast.Ast.MethodSelector;
import ast.Ast.Path;
import ast.ExpCore.ClassB;
import auxiliaryGrammar.Program;

public class TestRedirect{
@RunWith(Parameterized.class)
public static class TestRedirect1 {//add more test for error cases
  @Parameter(0) public String[] _p;
  @Parameter(1) public String _cb1;
  @Parameter(2) public String _path1;
  @Parameter(3) public String _path2;
  @Parameter(4) public String _expected;
  @Parameter(5) public boolean isError;
  @Parameterized.Parameters
  public static List<Object[]> createData() {
    return Arrays.asList(new Object[][] {
    {new String[]{"{A:{}}"},
      "{InnerA:{} B:{ method InnerA m(InnerA a) a}}","Outer0::InnerA","Outer1::A","{B:{ method Outer2::A m(Outer2::A a) a}}",false
    },{new String[]{"{A:{}}"},
        "{InnerA:{}  method InnerA m(InnerA a) a}","Outer0::InnerA","Outer1::A","{ method Outer1::A m(Outer1::A a) a}",false

}});}
//},{"Outer2::D::C","Outer1::C",new String[]{"{A:{}}","{C:{}}","{D:##walkBy}"}
@Test  public void test() {
  TestHelper.configureForTest();
  Program p=TestHelper.getProgram(_p);
  ClassB cb1=getClassB(_cb1);
  Path path1=Path.parse(_path1);
  Path path2=Path.parse(_path2);
  ClassB expected=getClassB(_expected);
  if(!isError){
    ClassB res=Redirect.redirect(p, cb1,path1,path2);
    TestHelper.assertEqualExp(expected,res);
    }
  else{
    try{Redirect.redirect(p, cb1,path1,path2);fail("error expected");}
    catch(Resources.Error err){
      ClassB res=(ClassB)err.unbox;
      TestHelper.assertEqualExp(expected,res);
    }
  }
}
}


}