package lox;

import java.util.List;

interface CallableNode {
  public List<Token> getParams();
  public List<Stmt> getBody();
  public Token getName();
}