package facade;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import sugarVisitors.CollapsePositions;
import sugarVisitors.ToFormattedText;
import tools.Assertions;
import tools.StringBuilders;
import ast.Ast;
import ast.Ast.Doc;
import ast.Ast.Path;
import ast.Ast.Stage;
import ast.ErrorMessage;
import ast.ErrorMessage.UserLevelError.Kind;
import ast.ExpCore;
import ast.ExpCore.ClassB;
import ast.ExpCore.ClassB.Member;
import ast.ExpCore.ClassB.NestedClass;
import ast.Expression;
import ast.Ast.Position;
import ast.ExpCore.ClassB.MethodWithType;
import auxiliaryGrammar.Program;
import coreVisitors.InjectionOnSugar;
import coreVisitors.IsCompiled;
import coreVisitors.RecoverStoredSugar;
import facade.L42.ExecutionStage;

public class ErrorFormatter {
  public interface Reporter{ String toReport(ArrayList<Ast.Position>ps);}
  public static ErrorMessage.UserLevelError formatError(ErrorMessage msg) {
    Class<?> c=msg.getClass();
    try {return formatError(msg, c);}
    catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {throw new Error(e);}
  }
  public static ErrorMessage.UserLevelError formatError(ErrorMessage msg, Class<?> c) throws IllegalAccessException, NoSuchFieldException, SecurityException {
    String errorStart="\n\n\n------------------------------------\n";
    ArrayList<Ast.Position> ps=new ArrayList<Ast.Position>();
    String errorTxt="";
    errorTxt+= infoFields(msg, c,ps);
    try{
      errorTxt+="Surrounding context:\n";
      errorTxt+= envs(msg, c,ps);
      ArrayList<Position> ps2 = ps;
      if(!ps.isEmpty()){ps2=new ArrayList<>();}
      try{errorTxt+= ctxP(msg, c,ps2);}catch(NoSuchFieldException ignored){}
    }
    catch(NoSuchFieldException ignored){}
    Position p=positionsFilter(ps);
    if(c==ErrorMessage.DotDotDotCanNotBeResolved.class){
        ErrorMessage.DotDotDotCanNotBeResolved ddd=(ErrorMessage.DotDotDotCanNotBeResolved)msg;

    }
    errorTxt="Error kind: "+c.getSimpleName()+"\nPosition:"+
    ((p==null)?"unknown":p.toString())+"\n"+errorTxt;
    //ps+"\n"+errorTxt;
    ErrorMessage.UserLevelError.Kind kind=findKind(msg);
    switch (kind){
      case TypeError:
        errorTxt=errorStart+"runStatus: "+kind.name()+"\n"+errorTxt;
        break;
      case MetaError:
        errorTxt=errorStart+"runStatus: "+kind.name()+"\n"+
        "Error in generating the following class: "+reportPlaceOfMetaError(msg).replace("::\n","\n")+errorTxt;
      default:
        break;
      }

    Throwable cause=null;
    if(msg.getCause()!=null){
      if(msg.getCause() instanceof ErrorMessage){
        ErrorMessage.UserLevelError uleCause=formatError((ErrorMessage)msg.getCause());
        cause=uleCause;
        errorTxt+="\n-------- caused by -----\n"+uleCause.getErrorTxt();
        }
      else {cause=msg.getCause();}
      }
    ErrorMessage.UserLevelError result= new ErrorMessage.UserLevelError(kind,p,msg,errorTxt);
    result.initCause(cause);
    return result;

  }

  private static String reportPlaceOfMetaError(ErrorMessage msg) {
    ErrorMessage.MalformedFinalResult _msg=(ErrorMessage.MalformedFinalResult)msg;
    ClassB cb=_msg.getFinalRes();
    String path=_msg.getReason();
    //String path=reportPlaceOfMetaError(cb);
    return path+"\n";
  }
  public static String reportPlaceOfMetaError(ClassB cb) {
    for(Member m:cb.getMs()){
      if(IsCompiled.of(m)){continue;}
      return m.match(
        nc->nc.getName()+"::"+isItErrorPlace(nc.getInner()),
        mi->formatSelectorCompact(mi.getS())+"::"+isItErrorPlace(mi.getInner()),
        mt->formatSelectorCompact(mt.getMs())+"::"+isItErrorPlace(mt.getInner().get())
        );
      }
    for(Member m:cb.getMs()){
      String err=m.match(nc->{
        ClassB ncb=(ClassB)nc.getInner();
        if(ncb.getStage()!=Stage.Star){
          return nc.getName()+"::"+isItErrorPlace(nc.getInner());
          }
        return null;
        }, mi->null, mt->null);
      if(err!=null){
        return "NotStarOf "+err;
        }
    }
    return "it should be a nested somewhere";//TODO: improve//
    //throw Assertions.codeNotReachable();
  }
  private static String isItErrorPlace(ExpCore inner) {
    if(inner instanceof ClassB){return reportPlaceOfMetaError((ClassB)inner);}
    return "\n    "+reportMetaError(inner);
  }
  public static String reportMetaError(ExpCore inner) {
    String str1=ToFormattedText.of(inner).replace("\n"," ");
    String str2=ToFormattedText.of(inner.accept(new RecoverStoredSugar())).replace("\n"," ");
    String str =str1+" \nwas:\n "+str2;
    return str;
  }
  private static Kind findKind(ErrorMessage msg) {
    if (L42.getStage()==ExecutionStage.CheckingWellFormedness){
      return Kind.WellFormedness;}
    if(msg instanceof ErrorMessage.TypeError){return Kind.TypeError;}
    if(msg instanceof ErrorMessage.MalformedFinalResult){return Kind.MetaError;}
    return Kind.Unclassified;

  }
  private static Position positionsFilter(ArrayList<Position> ps) {
    if(ps.isEmpty()){return null;}
    Position p=ps.get(0);
    for(Position pi:ps){
      p=CollapsePositions.accumulatePos(p, pi);
    }
    return p;
  }
  public static String envs(ErrorMessage msg, Class<?> c,ArrayList<Ast.Position> ps)
      throws NoSuchFieldException, IllegalAccessException {
    String errorTxt="";
    Field f=c.getField("envs");
    f.setAccessible(true);
    Collection<?> envs=(Collection<?>)f.get(msg);
    for(Object o:envs){
      errorTxt+=errorFormat(o,ps)+"\n";
    }
    return errorTxt;
  }
  public static String ctxP(ErrorMessage msg, Class<?> c,ArrayList<Ast.Position>ps)
      throws NoSuchFieldException, IllegalAccessException {
    String errorTxt="";
    Field f=c.getDeclaredField("p");
    f.setAccessible(true);
    Collection<?> envs=(Collection<?>)f.get(msg);
    for(Object o:envs){
      errorTxt+=errorFormat(o,ps)+"\n";
    }
    return errorTxt;
  }

  public static String infoFields(ErrorMessage msg, Class<?> c,ArrayList<Ast.Position>ps)
      throws IllegalAccessException {
    String errorTxt="";
    for(Field f:c.getDeclaredFields()){
      f.setAccessible(true);
      if(f.getName().equals("p")){continue;}
      Object obj=f.get(msg);
      if(obj==null){continue;}
      errorTxt+=f.getName();
      errorTxt+=": ";
      errorTxt+=errorFormat(obj,ps);
      errorTxt+="\n";
    }
    return errorTxt;
  }
  public static String errorFormat(Object obj,ArrayList<Ast.Position>ps) {
    if(obj instanceof String){return (String)obj;}
    if(obj instanceof Integer){return obj.toString();}
    if(obj instanceof Expression){
      Ast.Position p=CollapsePositions.of((Expression)obj);
      ps.add(p);
      return ToFormattedText.ofCompact((Expression)obj);
      }
    if(obj instanceof ExpCore){//thanks to Path, this have to be after Expression
      ExpCore exp=(ExpCore)obj;
      //Expression expression=exp.accept(new RecoverStoredSugar());
      Expression expression=exp.accept(new InjectionOnSugar());
      return errorFormat(expression,ps);
      }
    if(obj instanceof Ast.MethodSelector){
      return formatSelectorCompact((Ast.MethodSelector)obj);
          }
    if(obj instanceof ExpCore.ClassB.MethodWithType){
      return ToFormattedText.of((MethodWithType)obj).trim().replace("\n", " ");
      }
  //  if(obj instanceof Ast.MethodType){
  //    }
    if(obj instanceof Reporter){
      return ((Reporter)obj).toReport(ps);
    }
    if(obj instanceof Collection<?>){
      Collection<?> c=(Collection<?>)obj;
      if(c.size()==0){return "[]";}
      if(c.size()==1){return "["+errorFormat(c.iterator().next(),ps)+"]";}
      String res="[\n";
      for(Object o:c){
        res+="  "+errorFormat(o,ps)+"\n";
      }
      return res+"  ]\n";
    }
    if(obj instanceof Ast.Type){return ToFormattedText.of((Ast.Type)obj);}

    if(obj instanceof ClassB.Member){return ToFormattedText.of((ClassB.Member)obj);}
    if(obj instanceof Expression){return ToFormattedText.of((Expression)obj);}
    if(obj instanceof Expression.ClassB.Member){return ToFormattedText.of((Expression.ClassB.Member)obj);}
    if(obj instanceof java.nio.file.Path){return obj.toString();}
    return "unknown kind "+obj.getClass().getCanonicalName();
  }
  public static String formatSelectorCompact(Ast.MethodSelector ms) {
    StringBuilder sb=new StringBuilder();
    sb.append(ms.getName()+"(");
    StringBuilders.formatSequence(sb,ms.getNames().iterator(),", ",n->sb.append(n));
    return sb.toString()+")";
  }

  public static void printType(Program p) {
    //for(int i=0;i<50;i++){System.out.print("\n");}
    //System.out.print("\n*************************\n");
    //printType(0,p);
    }
  private static void printType(int i, Program p) {
    if(p.isEmpty()){return;}
    printType(i,"",p.top());
    printType(i+1,p.pop());

  }
  private static void printType(int i,String prefix, ClassB top) {
    System.out.print("Outer"+i+prefix+" :{\n");
    printMembers(top);
    System.out.print("  }\n");
    for(Member m:top.getMs()){
      if(!(m instanceof NestedClass)){continue;}
      NestedClass nc=(NestedClass)m;
      if(nc.getInner() instanceof ClassB){
        printType(i,"::"+nc.getName(),(ClassB)nc.getInner());
      }
    }

  }
  private static void printMembers(ClassB top) {
    for(Member m:top.getMs()){
      if(!(m instanceof MethodWithType)){continue;}
      MethodWithType mwt=(MethodWithType)m;
      mwt=mwt.withDoc(Doc.empty());
      mwt=mwt.withInner(Optional.empty());
      String txt=sugarVisitors.ToFormattedText.of(mwt);
      txt=txt.replace("{","");
      txt=txt.replace("}","");
      txt=txt.replace("\n"," ");
      System.out.print("  "+txt+"\n");
/*      System.out.print("  "+mwt.getMt().getReturnType());
      System.out.print(" "+mwt.getMs().getName());
      System.out.print("(");
      {int i=-1;for(String n:mwt.getMs().getNames()){i+=1;
      System.out.print(mwt.getMt().getTs()
      }}
      System.out.print(")\n");
  */
    }

  }
  private static String whyIsNotExecutable(ClassB cb) {
    /*if(cb.getH() instanceof Ast.TraitHeader){
      return "\n  The requested path is a trait";
    }*/
    for(Member m: cb.getMs()){
      if(!(m instanceof MethodWithType)){continue;}
      MethodWithType mt=(MethodWithType)m;
      /*if (!mt.getInner().isPresent() && !mt.isFieldGenerated()){
        return "\n  The method "+mt.getMs()+" of the requested path is abstract";
      }*/
    }
    for(Member m: cb.getMs()){
      if(!(m instanceof NestedClass)){continue;}
      NestedClass nc=(NestedClass)m;
      if (!(nc.getInner() instanceof ClassB)){
        return "\n  The nested class "+nc.getName()+" of the requested path is not compiled yet";
      }
      String nestedRes=whyIsNotExecutable((ClassB)nc.getInner());
      if(nestedRes!=null){
        return "::"+nc.getName()+nestedRes;
      }
    }
    return null;
  }

  public static String whyIsNotExecutable(Path path, Program p1) {
    ClassB cb=p1.extract(path);
    String whyNot=whyIsNotExecutable(cb);
    if (whyNot!=null){
      return "The requested path is incomplete.\n  "
          +ToFormattedText.of(path)+whyNot;
    }
    return "The requested path is incomplete since it refers to other incomplete classes in the program";
  }
}
