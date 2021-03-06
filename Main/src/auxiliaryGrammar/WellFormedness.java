package auxiliaryGrammar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ast.Ast;
import ast.ErrorMessage;
import ast.Ast.BlockContent;
import ast.Ast.ConcreteHeader;
import ast.Ast.FieldDec;
import ast.Ast.Mdf;
import ast.Ast.MethodSelector;
import ast.Ast.VarDecXE;
import ast.ExpCore;
import ast.Expression;
import ast.Ast.SignalKind;
import ast.Expression.*;
import sugarVisitors.CheckNoVarDeclaredTwice;
import sugarVisitors.CheckTerminatingBlock;
import sugarVisitors.CheckVarUsedAreInScope;
import sugarVisitors.CloneVisitor;
import tools.Assertions;
import tools.Map;
/*
class Found extends RuntimeException{
  Expression e;Expression ctx;
  Found(Expression e,Expression ctx){this.e=e;this.ctx=ctx;}
}*/
public class WellFormedness {
  public static boolean checkAll(Expression e){
    WellFormedness.operatorPrecedenceCheck(e);
    WellFormedness.returnCheck(e);
    WellFormedness.blockCheck(e);
    WellFormedness.withCheck(e);
    CheckNoVarDeclaredTwice.of(e);
    CheckVarUsedAreInScope.of(e);
    //check all variables used when in scope!
    //looks like not use var after assign?
    //uno e' intorno, l'altro e' dopo...
    WellFormedness.classCheck(e);
    return true;
  }
  //operators at the same level of precedence must be identical, 
  // i.e. a+b+c is ok, but a+b*c is not
  public static void operatorPrecedenceCheck(Expression _e){
    _e.accept(new CloneVisitor(){
      public Expression visit(BinOp s) {
        if(s.getRight() instanceof BinOp){
          Ast.Op op=((BinOp)s.getRight()).getOp();
          if(s.getOp().kind==op.kind){
            throw new AssertionError("parsing strange stuff for "+s);
            }
          }
        if(s.getLeft() instanceof BinOp){
          Ast.Op op=((BinOp)s.getLeft()).getOp();
          if(s.getOp().kind==op.kind && s.getOp()!=op){
            throw new ErrorMessage.NotWellFormed(s,_e, "Here parenthesis are needed to disambiguate operator precedence.");
            }
          }
        return super.visit(s);
        }
      });
    }
  //returns:
  //The \Q@return@ keyword can not be used inside any \Q@if@ or while condition or inside the expression of a $\x$\Q@in@$\e$.
  public static void returnCheck(Expression _e){
    _e.accept(new CloneVisitor(){
      Expression outerDanger=null;
      
      public Expression visit(ClassB s) {
        Expression aux=this.outerDanger;
        this.outerDanger=null;
        try{return super.visit(s);}
        finally{this.outerDanger=aux;}
      }
      public Expression visit(ClassReuse s) {
        Expression aux=this.outerDanger;
        this.outerDanger=null;
        try{return super.visit(s);}
        finally{this.outerDanger=aux;}
      }
      @Override
      public Expression visit(If s) {//I avoided try finally in the following since scoping was dumb here
        Expression aux=this.outerDanger;
        this.outerDanger=s;
        Expression e=lift(s.getCond());
        this.outerDanger=aux;
        return new If(s.getP(),e,lift(s.getThen()),Map.of(this::lift,s.get_else()));
      }
      @Override
      public Expression visit(While s) {
        Expression aux=this.outerDanger;
        this.outerDanger=s;
        Expression e=lift(s.getCond());
        this.outerDanger=aux;
        return new While(s.getP(),e,lift(s.getThen()));
      }
      @Override
      public Expression visit(With s) {
        Expression aux=this.outerDanger;
        this.outerDanger=s;
        List<VarDecXE> is = Map.of(this::liftVarDecXE, s.getIs());
        this.outerDanger=aux;
        return new With(s.getP(),s.getXs(),is,Map.of(this::liftVarDecXE, s.getDecs()),Map.of(this::liftO,s.getOns()),Map.of(this::lift, s.getDefaultE()));
      }
      public Expression visit(Signal s) {
        if(this.outerDanger==null){return super.visit(s);}
        if(s.getKind()!=SignalKind.Return){return super.visit(s);}
        throw new ErrorMessage.NotWellFormed(s, outerDanger, "A return is not allowed here");        
      }
    });
  }
  
  

  public static boolean blockCheck(Expression _e){
    _e.accept(new CloneVisitor(){
      public Expression visit(RoundBlock s) {
        checkBlockContentOk(s,s.getContents());
        return super.visit(s);}
      public Expression visit(CurlyBlock s) {
        if(s.getContents().isEmpty()){
          throw new ErrorMessage.NotWellFormed(s, null, "Blocks should not be empty");
          }
        checkBlockContentOk(s,s.getContents());
        CheckTerminatingBlock.of(s);//this can throw!
        return super.visit(s);}
    });
    return true;
  }
  public static boolean checkBlockContentOk(Expression forErr,List<BlockContent> l) {
    if(l.isEmpty()){return true;}
    for(BlockContent bc:l.subList(0, l.size()-1)){
      if(!bc.get_catch().isPresent()){
        throw Assertions.codeNotReachable("Catch should not be empty on multiple contents");
        }
      if(bc.get_catch().get().getOns().isEmpty()){
        throw Assertions.codeNotReachable("Catch should not be empty on multiple contents");
        }
      if(bc.getDecs().isEmpty()){
        throw new ErrorMessage.NotWellFormed(forErr, null, "empty declarations before catch");
        }
    }
    BlockContent last=l.get(l.size()-1);
    if(last.getDecs().isEmpty()){
      throw Assertions.codeNotReachable("last declarations should not be empty "+forErr);
      }
    return true;
  }

  
  
  static void withVarCheck(List<String>notEqOp,With w){
    w.accept(new CloneVisitor(){
      public Expression visit(ClassB s) {return s;}
      public Expression visit(ClassReuse s) {return s;}
      public Expression visit(BinOp s){
        if (s.getOp().kind!= Ast.OpKind.EqOp){return super.visit(s);}
        assert s.getLeft() instanceof X;
        String x=((X)s.getLeft()).getInner();
        if(!notEqOp.contains(x)){return super.visit(s);}
        throw new ErrorMessage.NotWellFormed(s, w, "Non var iterator variable should not be updated");
        }
    });
    }
  static void withVarCheckNotReadAfterAssign(List<String>notEqOp,Ast.On on){
    //TODO: much much harder than what expected. will call terminating inside? can not be a clone visitor! 
    }
  public static void withCheck(Expression _e){
    _e.accept(new CloneVisitor(){
      public Expression visit(With s) {//it does take care also of the case SquareWith
        //$\withKw\, \emptyset\,\emptyset\, \emptyset\,\_$ is not well formed;
        int size=s.getXs().size()+s.getIs().size()+s.getDecs().size();
        if (size==0){
          throw new ErrorMessage.NotWellFormed(s,null, "With should \"with\" over something, here is empty");
        }
        //$\withKw\,\xs\,\is\,\es\, %\Opt\onStart\,
        //\oRound\Many\onWith\Opt\block\cRound$
        //is well formed if the number of types $\T_\vI\ldots\T_\vn$ in each $\onKw$ is the same of the sum of the cardinalities of $\xs$, $\is$ and $\es$.
        for(Ast.On on:s.getOns()){
          if(!on.getTs().isEmpty() && on.getTs().size()!=size){
            throw new ErrorMessage.NotWellFormed(on.getInner(),s, "on types should be the same number of the with expressions, here on have: "+on.getTs().size()+" while the with have: "+size);
            }
        }
        //In a $\withKw$, variables introduces in the $\is$ can be updated only if they are declared \Q@var@.
        List<String>notEqOp=new ArrayList<String>();
        for(VarDecXE  is:s.getIs()){
          if(!is.isVar()){notEqOp.add(is.getX());}
          }
        withVarCheck(notEqOp,s);
        
        //In a $\onWith$ body, variables whose type have been made more specific can still beeing updated using the more general type, thus 
        //a well formed $\onWith$ body can not read a variable after updating it.
        //TODO: missing, see up!
        return super.visit(s);}      
    });
  }
  static void checkHeader(ClassB cb,ConcreteHeader h) {
    //All fields names in a given header must be unique.
    HashSet<String> fnames=new HashSet<String>();
    for(FieldDec f:h.getFs()){
      fnames.add(f.getName());
      if(f.getName().equals("this")){throw new ErrorMessage.NotWellFormed(cb,null, "\"this\" is not a valid field name");}
      }
    if(fnames.size()!=h.getFs().size()){
      throw new ErrorMessage.NotWellFormed(cb,null, "Some field name is repeated.");
      }
    if(h.getMdf()==Mdf.Type){
      throw new ErrorMessage.NotWellFormed(cb,null, "Constructors can not return types");
      }
    boolean haveVar=false;
    for(FieldDec f:h.getFs()){
      if(f.isVar()){haveVar=true;break;}
    }
    if(haveVar && h.getMdf()==Mdf.Immutable){
      throw new ErrorMessage.NotWellFormed(cb,null, "Immutable classes can not hava variable fields");
      }
  }

  public static void checkMembers(ClassB s) {
    HashSet<String>usedNestedClassNames=new HashSet<>();
    HashSet<MethodSelector>usedSelector=new HashSet<>();
    for(ClassB.Member m:s.getMs()){
      m.match(
        nc->{
          if(usedNestedClassNames.contains(nc.getName())){
            throw new ErrorMessage.NotWellFormed(s,null, "Some nested class name is repeated");
            }
          //All nested class names $\C$ in a class must be unique.
          usedNestedClassNames.add(nc.getName());
          return null;
        },
        mi->{checkMs(s,mi.getS(),usedSelector);return null;},
        mt->{checkMs(s,mt.getMs(),usedSelector);return null;}
      );}
  }
  static void checkMs(ClassB cb ,MethodSelector s, HashSet<MethodSelector> usedSelector) {
    if(usedSelector.contains(s)){
      throw new ErrorMessage.NotWellFormed(cb,null, "Some method selector is repeated");
      }
    usedSelector.add(s);
    //All parameter names declared within a given method header must be unique.
    HashSet<String>ps=new HashSet<String>(s.getNames());
    if(s.getNames().contains("this")){throw new ErrorMessage.NotWellFormed(cb,null, "\"this\" is not a valid parameter name");}
    if(ps.size()!=s.getNames().size()){
      throw new ErrorMessage.NotWellFormed(cb,null, "Some parameter name is repeated");
      }
  }
  public static void classCheck(Expression _e){
    _e.accept(new CloneVisitor(){
      public Expression visit(ClassReuse s) {
        checkMembers(s.getInner());
        return super.visit(s);}
      public Expression visit(ClassB s) {
      checkMembers(s);
      if(s.getH()instanceof Ast.ConcreteHeader){
        checkHeader(s,(Ast.ConcreteHeader)s.getH());
        }
      return super.visit(s);}
      });
    }
  public static boolean checkCoreVariables(ExpCore.ClassB cb) {
    return coreVisitors.CheckNoVarDeclaredTwice.of(cb);

  }
  
 

  //paths:
  //well formedness "post": every used path could be present in the future.
  

  
}
