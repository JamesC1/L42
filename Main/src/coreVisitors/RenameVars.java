package coreVisitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import tools.Assertions;
import tools.Map;
import ast.ExpCore;
import ast.ExpCore.Block;
import ast.ExpCore.Block.Catch;
import ast.ExpCore.WalkBy;
import ast.ExpCore.X;

public class RenameVars extends CloneVisitor{
  HashMap<String,String> toRename;
  RenameVars(HashMap<String,String> toRename){this.toRename=toRename;}
  
  public ExpCore visit(WalkBy s) {throw Assertions.codeNotReachable();}
  public ExpCore visit(ExpCore.ClassB s) {return s;}
  public static ExpCore of(ExpCore e,HashMap<String,String> toRename){
    return e.accept(new RenameVars(toRename));
  }
  public ExpCore visit(X s) {
    String alt=toRename.get(s.getInner());
    if(alt==null){return s;}
    return new X(alt);
    }
  public ExpCore visit(Block s) {
    Optional<Catch> k = Map.of(this::liftK,s.get_catch());
    if(k.isPresent()){
      String altK=toRename.get(k.get().getX());
      if(altK!=null){k=Optional.of(k.get().withX(altK));}
    }
    List<Block.Dec> decs = new ArrayList<>();
    for(Block.Dec dec:s.getDecs()){
      String altxi=toRename.get(dec.getX());
      if(altxi==null){decs.add(dec);}
      else {decs.add(dec.withX(altxi));}
    }
    
    return new Block(s.getSource(),s.getDoc(),Map.of(this::liftDec,decs),lift(s.getInner()),k);
  }
  

  
}
