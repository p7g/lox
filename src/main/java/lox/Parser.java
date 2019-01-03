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

  public Expr parseExpression() throws Exception {
    try {
      return expression();
    }
    catch (ParseError error) {
      throw new Exception();
    }
  }

  private Stmt statement(int loopCount) {
    if (match(FOR)) {
      return forStatement(loopCount);
    }
    if (match(IF)) {
      return ifStatement(loopCount);
    }
    if (match(DO)) {
      return block(loopCount);
    }
    if (match(RETURN)) {
      return returnStatement();
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
    else if (match(LET)) {
      initializer = letDeclaration();
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
    if (!check(DO)) {
      increment = expression();
    }

    consume(DO, "Expected 'do' after for clauses");
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

    consume(DO, "Expected 'do' after if condition");

    List<Stmt> body = new ArrayList<>();
    while (!check(END) && !check(ELSE) && !isAtEnd()) {
      body.add(declaration(loopCount));
    }
    Stmt thenBranch = new Stmt.Block(body);

    Stmt elseBranch = null;
    if (match(ELSE)) {
      // hack for 'else if'
      if (match(IF)) {
        elseBranch = ifStatement(loopCount);
      }
      else {
        consume(DO, "Expected 'do' after else");
        List<Stmt> elseBody = new ArrayList<>();
        while (!check(END) && !isAtEnd()) {
          elseBody.add(declaration(loopCount));
        }
        elseBranch = new Stmt.Block(elseBody);
      }
    }
    else {
      consume(END, "Expected 'end' after if statement");
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt letDeclaration() {
    Token name = consume(IDENTIFIER, "Expected variable name");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expected ';' after variable declaration");
    return new Stmt.Let(name, initializer);
  }

  private Stmt returnStatement() {
    Token keyword = previous();

    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expected ';' after return value");
    return new Stmt.Return(keyword, value);
  }

  private Stmt whileStatement(int loopCount) {
    Expr condition = expression();

    consume(DO, "Expected 'do' after while condition");
    Stmt body = block(loopCount + 1);

    return new Stmt.While(condition, body);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    if (!check(END)) {
      consume(SEMICOLON, "Expected ';' after expression");
    }
    return new Stmt.Expression(expr);
  }

  private Stmt block(int loopCount) {
    List<Stmt> statements = new ArrayList<>();

    while (!check(END) && !isAtEnd()) {
      statements.add(declaration(loopCount));
    }

    consume(END, "Expected 'end' after block");
    return new Stmt.Block(statements);
  }

  private Stmt declaration(int loopCount) {
    try {
      if (match(LET)) {
        return letDeclaration();
      }
      if (match(FUN)) {
        return function(loopCount, "function");
      }

      return statement(loopCount);
    }
    catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Function function(int loopCount, String kind) {
    Token name = consume(IDENTIFIER, "Expected " + kind + " name");

    List<Token> parameters = new ArrayList<>();

    if (match(LEFT_PAREN)) {
      if (!check(RIGHT_PAREN)) {
        do {
          if (parameters.size() >= 32) {
            error(peek(), "Expected no more than 32 parameters");
          }

          parameters.add(consume(IDENTIFIER, "Expected identifier"));
        } while (match(COMMA));
      }
      consume(RIGHT_PAREN, "Expected ')' after parameter list");
    }

    List<Stmt> body = new ArrayList<>();
    while (!check(END) && !isAtEnd()) {
      body.add(declaration(0));
    }
    consume(END, "Expected 'end' after " + kind + " body");

    return new Stmt.Function(name, parameters, body);
  }

  private Expr expression() {
    return lambda();
  }

  private Expr lambda() {
    if (!match(BACKSLASH)) {
      return sequence();
    }

    List<Token> params = new ArrayList<>();
    if (!check(MINUS_GREATER) && !check(DO)) {
      do {
        params.add(consume(IDENTIFIER, "Expected identifier"));
      } while (match(COMMA));
    }

    List<Stmt> body = new ArrayList<>();
    if (match(DO)) {
      while (!check(END) && !isAtEnd()) {
        body.add(declaration(0));
      }
      consume(END, "Expected 'end' after block");
    }
    else {
      Token arrow = consume(
        MINUS_GREATER,
        "Expected '->' or block after parameter list"
      );
      Expr expr = expression();
      body.add(new Stmt.Return(arrow, expr));
    }

    return new Expr.Lambda(params, body);
  }

  private Expr sequence() {
    Expr expr = assignment();

    while (match(BACKTICK)) {
      Token operator = previous();
      Expr right = assignment();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr assignment() {
    Expr expr = conditional();

    if (match(
      EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL,
      PERCENT_EQUAL, LESS_LESS_EQUAL, GREATER_GREATER_EQUAL,
      AMPERSAND_EQUAL, CARET_EQUAL, PIPE_EQUAL
    )) {
      Token equals = previous();
      Expr value = assignment();

      if (equals.type != EQUAL) {
        TokenType operatorType = null;
        switch (equals.type) {
          case PLUS_EQUAL:
            operatorType = PLUS;
            break;
          case MINUS_EQUAL:
            operatorType = MINUS;
            break;
          case STAR_EQUAL:
            operatorType = STAR;
            break;
          case SLASH_EQUAL:
            operatorType = SLASH;
            break;
          case PERCENT_EQUAL:
            operatorType = PERCENT;
            break;
          case LESS_LESS_EQUAL:
            operatorType = LESS_LESS;
            break;
          case GREATER_GREATER_EQUAL:
            operatorType = GREATER_GREATER;
            break;
          case AMPERSAND_EQUAL:
            operatorType = AMPERSAND;
            break;
          case CARET_EQUAL:
            operatorType = CARET;
            break;
          case PIPE_EQUAL:
            operatorType = PIPE;
            break;
          default: break;
        }
        equals = new Token(
          EQUAL,
          equals.lexeme,
          equals.literal,
          equals.line,
          equals.column
        );
        value = new Expr.Binary(
          expr,
          new Token(
            operatorType,
            equals.lexeme,
            equals.literal,
            equals.line,
            equals.column
          ),
          value
        );
      }

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
    Expr expr = bitwiseOr();

    while (match(AND)) {
      Token operator = previous();
      Expr right = bitwiseOr();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr bitwiseOr() {
    Expr expr = bitwiseXor();

    while (match(PIPE)) {
      Token operator = previous();
      Expr right = bitwiseXor();
      expr = new Expr.Bitwise(expr, operator, right);
    }

    return expr;
  }

  private Expr bitwiseXor() {
    Expr expr = bitwiseAnd();

    while (match(CARET)) {
      Token operator = previous();
      Expr right = bitwiseAnd();
      expr = new Expr.Bitwise(expr, operator, right);
    }

    return expr;
  }

  private Expr bitwiseAnd() {
    Expr expr = equality();

    while (match(AMPERSAND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Bitwise(expr, operator, right);
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
    Expr expr = shift();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = shift();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr shift() {
    Expr expr = addition();

    while (match(LESS_LESS, GREATER_GREATER)) {
      Token operator = previous();
      Expr right = addition();
      expr = new Expr.Shift(expr, operator, right);
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

    while (match(SLASH, STAR, PERCENT)) {
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
      if (previous().type == SEMICOLON || previous().type == END) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case LET:
        case FOR:
        case IF:
        case WHILE:
        case RETURN:
          return;
        default: break;
      }

      advance();
    }
  }
}
