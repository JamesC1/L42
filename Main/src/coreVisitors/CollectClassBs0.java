package coreVisitors;

import java.util.ArrayList;
import java.util.List;
import tools.Map;
import ast.ExpCore;
import ast.Ast.*;
import ast.ExpCore.ClassB;

public class CollectClassBs0 extends CloneVisitor{
  List<ClassB> cbs=new ArrayList<>();
  public static List<ClassB> of(ExpCore e){
    CollectClassBs0 cp=new CollectClassBs0();
    e.accept(cp);
    return cp.cbs;
  }
  public ExpCore visit(ClassB s) {
    cbs.add(s);
    return s;
    }  
}
