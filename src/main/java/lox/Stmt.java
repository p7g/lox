package lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitBreakStmt(Break stmt);
    R visitClassStmt(Class stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitLetStmt(Let stmt);
    R visitReturnStmt(Return stmt);
    R visitWhileStmt(While stmt);
  }

  abstract <R> R accept(Visitor<R> visitor);

  static class Block extends Stmt {
    final List<Stmt> statements;

    public Block(List<Stmt> statements) {
      this.statements = statements;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }

  static class Break extends Stmt {
    final Token token;
    final Expr levels;
    final int maxLevels;

    public Break(Token token, Expr levels, int maxLevels) {
      this.token = token;
      this.levels = levels;
      this.maxLevels = maxLevels;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }
  }

  static class Class extends Stmt {
    final Token name;
    final Expr.Variable superclass;
    final List<Stmt.Function> methods;
    final List<Stmt.Function> staticMethods;

    public Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods, List<Stmt.Function> staticMethods) {
      this.name = name;
      this.superclass = superclass;
      this.methods = methods;
      this.staticMethods = staticMethods;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }
  }

  static class Expression extends Stmt {
    final Expr expression;

    public Expression(Expr expression) {
      this.expression = expression;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  static class Function extends Stmt implements CallableNode {
    final Token name;
    final List<Token> params;
    final List<Stmt> body;

    public Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }
    
    public List<Token> getParams() {
      return this.params;
    }

    public List<Stmt> getBody() {
      return this.body;
    }

    public Token getName() {
      return this.name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }
  }

  static class If extends Stmt {
    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;

    public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }
  }

  static class Let extends Stmt {
    final Token name;
    final Expr initializer;

    public Let(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLetStmt(this);
    }
  }

  static class Return extends Stmt {
    final Token keyword;
    final Expr value;

    public Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }
  }

  static class While extends Stmt {
    final Expr condition;
    final Stmt body;

    public While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }

}