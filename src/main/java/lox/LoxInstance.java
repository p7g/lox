package lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
  private final Map<String, Object> fields = new HashMap<>();

  public LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  public LoxInstance() {}

  public Object get(Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    LoxFunction method = klass.findMethod(this, name.lexeme);
    if (method != null) {
      return method;
    }

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'");
  }

  public void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  public String toString() {
    return klass.name + " instance";
  }
}