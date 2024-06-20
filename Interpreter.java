package MYOWN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import MYOWN.Stmt.Return;

public class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  Interpreter() {
    globals.define("clock", new Callable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter,
      List<Object> arguments) {
      return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }
    @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }
  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }
  void executeBlock(List<Stmt> statements,
    Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    Function function = new Function(stmt);
    environment.define(stmt.name.lexeme, function);
    return null;
  }
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      
      execute(stmt.body);
    }
    return null;
  }
  @Override
  public Void visitForStmt(Stmt.For stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    throw new RuntimeError(expr.name,
        "Only instances have properties.");
  }
  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);
    Object value = evaluate(expr.value);
    return value;
  }
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case Not:
        return !isTruthy(right);
      case Minus:
        return -(double)right;
    }
    // Unreachable.
    return null;
  }
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }
  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
      Object right = evaluate(expr.right);
      Object left = evaluate(expr.left);
      checkNumberOperands(expr.operator, left, right);
      if(right instanceof Integer){
        right=Double.parseDouble(String.valueOf(right));
      }
      if(left instanceof Integer){
        left=Double.parseDouble(String.valueOf(left));
      }
      switch (expr.operator.type){
          case Minus : return (double) left - (double) right;
          case Plus: return (double) left + (double) right;
          case Multiply: return (double)left * (double) right;
          case Slash: return (double)left / (double) right;
          case Notequals:
              return !isEqual(left, right);
          case Equals:
              return isEqual(left, right);
          case Greater:
              checkNumberOperands(expr.operator, left, right);
              return (double)left > (double)right;
          case GreaterThanEqual:
              checkNumberOperands(expr.operator, left, right);
              return (double)left >= (double)right;
          case Less:
              checkNumberOperands(expr.operator, left, right);
              return (double)left < (double)right;
          case LessThanEqual:
              checkNumberOperands(expr.operator, left, right);
              return (double)left <= (double)right;
      }
      return null;
  }
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) { 
      arguments.add(evaluate(argument));
    }
    if (!(callee instanceof Callable)) {
      throw new RuntimeError(expr.paren,
      "Can only call functions and classes.");
    }
    Callable function = (Callable)callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }
    return function.call(this, arguments);
  }
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if((left instanceof Double || left instanceof Integer) && (right instanceof Double || right instanceof Integer)) return;
    throw new RuntimeError(operator,"Both Operands must be numbers");
}
private boolean isTruthy(Object object) {
  if (object == null) return false;
  if (object instanceof Boolean) return (boolean)object;
  return true;
}
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Main.runtimeError(error);
    }
  }
  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double || object instanceof Integer) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
