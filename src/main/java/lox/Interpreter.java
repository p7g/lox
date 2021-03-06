package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  public Environment globals = new Environment();
  private final Map<Expr, Integer> locals = new HashMap<>();
  private Environment environment = globals;

  public Interpreter() {
    globals.define("time", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native function>";
      }
    });
    globals.define("print", new LoxCallable() {
      @Override
      public int arity() { return 1; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.print(Interpreter.stringify(arguments.get(0)));
        return null;
      }

      @Override
      public String toString() {
        return "<native function>";
      }
    });
    globals.define("println", new LoxCallable() {
      @Override
      public int arity() { return 1; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.println(Interpreter.stringify(arguments.get(0)));
        return null;
      }

      @Override
      public String toString() {
        return "<native function>";
      }
    });
    globals.define("assert", new LoxCallable() {
      @Override
      public int arity() { return 2; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!isTruthy(arguments.get(0))) {
          throw new AssertionError(arguments.get(1).toString());
        }
        return null;
      }

      @Override
      public String toString() {
        return "<native function>";
      }
    });
  }

  public void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    }
    catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  public void interpret(Expr expression) {
    try {
      System.out.println(evaluate(expression));
    }
    catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  public void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    }
    finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    double levels = 1;
    if (stmt.levels != null) {
      Object result = evaluate(stmt.levels);
      if (result instanceof Double) {
        levels = (double) result;
        if ((int) levels > stmt.maxLevels || levels < 0) {
          throw new RuntimeError(stmt.token, "Invalid number of levels");
        }
      }
      else {
        throw new RuntimeError(stmt.token, "Break expression must be number");
      }
    }
    if (levels > 0) {
      throw new Break((int) levels);
    }
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(
          stmt.superclass.name,
          "Superclass must be a class"
        );
      }
    }

    environment.define(stmt.name.lexeme, null);

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(
        method,
        environment,
        method.name.lexeme.equals("init")
      );
      methods.put(method.name.lexeme, function);
    }

    Map<String, LoxFunction> staticMethods = new HashMap<>();
    for (Stmt.Function method : stmt.staticMethods) {
      LoxFunction function = new LoxFunction(
        method,
        environment,
        false
      );
      staticMethods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(
      stmt.name.lexeme,
      (LoxClass) superclass,
      methods,
      staticMethods
    );

    if (superclass != null) {
      environment = environment.enclosing;
    }

    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    }
    else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitLetStmt(Stmt.Let stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(value);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      }
      catch (Break error) {
        if (error.levels > 1) {
          error.levels -= 1;
          throw error;
        }
        return null;
      }
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    }
    else {
      globals.assign(expr.name, value);
    }

    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case PERCENT:
        checkNumberOperands(expr.operator, left, right);
        return (double) left % (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        if (left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        }
        throw new RuntimeError(
          expr.operator,
          "Operands must be two numbers or a string and something else"
        );
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case BANG_EQUAL:
        return !isEqual(left, right);
      case BACKTICK:
        return right;
      case PIPE:
        checkNumberOperands(expr.operator, left, right);
        return (double) (toInt(left) | toInt(right));
      case CARET:
        checkNumberOperands(expr.operator, left, right);
        return (double) (toInt(left) ^ toInt(right));
      case AMPERSAND:
        checkNumberOperands(expr.operator, left, right);
        return (double) (toInt(left) & toInt(right));
      case LESS_LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) (toInt(left) << toInt(right));
      case GREATER_GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) (toInt(left) >> toInt(right));
      default: break;
    }

    // unreachable
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Invalid callee type");
    }

    LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
        expr.paren,
        "Expected " + function.arity()
        + " arguments but got " + arguments.size()
      );
    }
    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties");
  }

  @Override
  public Object visitLambdaExpr(Expr.Lambda expr) {
    return new LoxFunction(expr, environment, false);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) {
        return left;
      }
    }
    else {
      if (!isTruthy(left)) {
        return left;
      }
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass) environment.getAt(distance, "super");

    // "this" is always one environment nearer than "super"
    LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");

    LoxFunction method = superclass.findMethod(object, expr.method.lexeme);

    if (method == null) {
      throw new RuntimeError(
        expr.method,
        "Undefined property '" + expr.method.lexeme + "'"
      );
    }

    return method;
  }

  @Override
  public Object visitTernaryExpr(Expr.Ternary expr) {
    if (
      expr.leftOperator.type == TokenType.QUESTION_MARK
      && expr.rightOperator.type == TokenType.COLON
    ) {
      Object left = evaluate(expr.left);
      if (isTruthy(left)) {
        return evaluate(expr.middle);
      }
      return evaluate(expr.right);
    }

    // unreachable
    return null;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookupVariable(expr.keyword, expr);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      case BANG:
        return !isTruthy(right);
      default: break;
    }

    // unreachable
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookupVariable(expr.name, expr);
  }

  private Object lookupVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    }
    else {
      return globals.get(name);
    }
  }

  private static void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) {
      return;
    }

    throw new RuntimeError(operator, "Operand must be a number");
  }

  private static void checkNumberOperands(
    Token operator,
    Object left, Object right
  ) {
    if (left instanceof Double && right instanceof Double) {
      return;
    }

    throw new RuntimeError(operator, "Operands must be numbers");
  }

  private static int toInt(Object value) {
    return ((Double) value).intValue();
  }

  private static boolean isTruthy(Object object) {
    if (object == null) {
      return false;
    }
    if (object instanceof Boolean) {
      return (boolean) object;
    }

    return true;
  }

  private static Boolean isEqual(Object a, Object b) {
    // nil is only equal to nil
    if (a == null) {
      if (b == null) {
        return true;
      }
      return false;
    }

    return a.equals(b);
  }

  private static String stringify(Object object) {
    if (object == null) {
      return "nil";
    }

    // hack to remove ".0" from integer-valued doubles
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
