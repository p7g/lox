package lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd()) {
      statements.add(declaration(0));
    }

    return statements;
  }

  private Stmt statement(int loopCount) {
    if (match(FOR)) {
      return forStatement(loopCount);
    }
    if (match(IF)) {
      return ifStatement(loopCount);
    }
    if (match(LEFT_BRACE)) {
      return block(loopCount);
    }
    if (match(WHILE)) {
      return whileStatement(loopCount);
    }
    if (loopCount > 0 && match(BREAK)) {
      return breakStatement(loopCount);
    }

    return expressionStatement();
  }

  private Stmt breakStatement(int loopCount) {
    Token token = previous();

    Expr levels = null;
    if (!check(SEMICOLON)) {
      levels = expression();
    }

    consume(SEMICOLON, "Expected ';' after break statement");

    return new Stmt.Break(token, levels, loopCount);
  }

  private Stmt forStatement(int loopCount) {
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    }
    else if (match(VAR)) {
      initializer = varDeclaration();
    }
    else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expected ';' after for condition");

    Expr increment = null;
    if (!check(LEFT_BRACE)) {
      increment = expression();
    }

    consume(LEFT_BRACE, "Expected '{' after for clauses");
    Stmt body = block(loopCount + 1);

    if (increment != null) {
      body = new Stmt.Block(
        Arrays.asList(body, new Stmt.Expression(increment))
      );
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }
    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt ifStatement(int loopCount) {
    Expr condition = expression();

    consume(LEFT_BRACE, "Expected '{' after if condition");

    Stmt thenBranch = block(loopCount);
    Stmt elseBranch = null;
    if (match(ELSE)) {
      // hack for 'else if'
      if (match(IF)) {
        elseBranch = ifStatement(loopCount);
      }
      else {
        consume(LEFT_BRACE, "Expected '{' after else");
        elseBranch = block(loopCount);
      }
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expected variable name");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expected ';' after variable declaration");
    return new Stmt.Var(name, initializer);
  }

  private Stmt whileStatement(int loopCount) {
    Expr condition = expression();

    consume(LEFT_BRACE, "Expected '{' after while condition");
    Stmt body = block(loopCount + 1);

    return new Stmt.While(condition, body);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected ';' after expression");
    return new Stmt.Expression(expr);
  }

  private Stmt block(int loopCount) {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration(loopCount));
    }

    consume(RIGHT_BRACE, "Expected '}' after block");
    return new Stmt.Block(statements);
  }

  private Stmt declaration(int loopCount) {
    try {
      if (match(VAR)) {
        return varDeclaration();
      }

      return statement(loopCount);
    }
    catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Expr expression() {
    return sequence();
  }

  private Expr sequence() {
    Expr expr = assignment();

    while (match(COMMA)) {
      Token operator = previous();
      Expr right = assignment();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr assignment() {
    Expr expr = conditional();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target");
    }

    return expr;
  }

  private Expr conditional() {
    Expr expr = or();

    if (match(QUESTION_MARK)) {
      Token leftOp = previous();
      Expr middle = conditional();
      consume(COLON, "Expected ':'");
      Token rightOp = previous();
      Expr right = conditional();
      return new Expr.Ternary(expr, leftOp, middle, rightOp, right);
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = addition();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = addition();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr addition() {
    Expr expr = multiplication();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = multiplication();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr multiplication() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      }
      else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();

    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 32) {
          error(peek(), "Cannot have more than 32 arguments");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (match(FALSE)) {
      return new Expr.Literal(false);
    }
    if (match(TRUE)) {
      return new Expr.Literal(true);
    }
    if (match(NIL)) {
      return new Expr.Literal(null);
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expected ')' after expression");
      return new Expr.Grouping(expr);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    throw error(peek(), "Expected expression");
  }

  private boolean match(TokenType ...types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
