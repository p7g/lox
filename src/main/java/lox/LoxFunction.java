package lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final CallableNode declaration;
  private final Environment closure;
  private boolean isInitializer;

  public LoxFunction(
    CallableNode declaration,
    Environment closure,
    boolean isInitializer
  ) {
    this.closure = closure;
    this.declaration = declaration;
    this.isInitializer = isInitializer;
  }

  public LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer);
  }

  @Override
  public int arity() {
    return declaration.getParams().size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.getParams().size(); i++) {
      environment.define(
        declaration.getParams().get(i).lexeme,
        arguments.get(i)
      );
    }

    try {
      interpreter.executeBlock(declaration.getBody(), environment);
    }
    catch (Return returnValue) {
      if (isInitializer) {
        return closure.getAt(0, "this");
      }

      return returnValue.value;
    }

    if (isInitializer) { // blegh
      return closure.getAt(0, "this");
    }

    return null;
  }

  @Override
  public String toString() {
    return "<fun " + declaration.getName().lexeme + ">";
  }
}
