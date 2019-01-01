package lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitBreakStmt(Break stmt);
    R visitExpressionStmt(Expression stmt);
    R visitIfStmt(If stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
  }

  abstract <R> R accept(Visitor<R> visitor);

  static class Block extends Stmt {
    final List<Stmt> statements;

    Block(List<Stmt> statements) {
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

    Break(Token token, Expr levels, int maxLevels) {
      this.token = token;
      this.levels = levels;
      this.maxLevels = maxLevels;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }
  }

  static class Expression extends Stmt {
    final Expr expression;

    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  static class If extends Stmt {
    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;

    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }
  }

  static class Var extends Stmt {
    final Token name;
    final Expr initializer;

    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }
  }

  static class While extends Stmt {
    final Expr condition;
    final Stmt body;

    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }

}