package lox;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitCallExpr(Call expr);
    R visitBinaryExpr(Binary expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitLambdaExpr(Lambda expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitSetExpr(Set expr);
    R visitSuperExpr(Super expr);
    R visitTernaryExpr(Ternary expr);
    R visitThisExpr(This expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
  }

  abstract <R> R accept(Visitor<R> visitor);

  static class Assign extends Expr {
    final Token name;
    final Expr value;

    public Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }
  }

  static class Call extends Expr {
    final Expr callee;
    final Token paren;
    final List<Expr> arguments;

    public Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }
  }

  static class Binary extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    public Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }
  }

  static class Get extends Expr {
    final Expr object;
    final Token name;

    public Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }
  }

  static class Grouping extends Expr {
    final Expr expression;

    public Grouping(Expr expression) {
      this.expression = expression;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }
  }

  static class Lambda extends Expr implements CallableNode {
    final Token name;
    final List<Token> params;
    final List<Stmt> body;

    public Lambda(Token name, List<Token> params, List<Stmt> body) {
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
      return visitor.visitLambdaExpr(this);
    }
  }

  static class Literal extends Expr {
    final Object value;

    public Literal(Object value) {
      this.value = value;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }
  }

  static class Logical extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    public Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }
  }

  static class Set extends Expr {
    final Expr object;
    final Token name;
    final Expr value;

    public Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }
  }

  static class Super extends Expr {
    final Token keyword;
    final Token method;

    public Super(Token keyword, Token method) {
      this.keyword = keyword;
      this.method = method;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSuperExpr(this);
    }
  }

  static class Ternary extends Expr {
    final Expr left;
    final Token leftOperator;
    final Expr middle;
    final Token rightOperator;
    final Expr right;

    public Ternary(Expr left, Token leftOperator, Expr middle, Token rightOperator, Expr right) {
      this.left = left;
      this.leftOperator = leftOperator;
      this.middle = middle;
      this.rightOperator = rightOperator;
      this.right = right;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTernaryExpr(this);
    }
  }

  static class This extends Expr {
    final Token keyword;

    public This(Token keyword) {
      this.keyword = keyword;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }
  }

  static class Unary extends Expr {
    final Token operator;
    final Expr right;

    public Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }
  }

  static class Variable extends Expr {
    final Token name;

    public Variable(Token name) {
      this.name = name;
    }
    
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }
  }

}