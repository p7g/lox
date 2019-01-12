package lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
  public final String name;
  public final LoxClass superclass;
  private final Map<String, LoxFunction> methods;

  public LoxClass(
    String name,
    LoxClass superclass,
    Map<String, LoxFunction> methods,
    Map<String, LoxFunction> staticMethods
  ) {
    super(new LoxClass(name, staticMethods));
    this.superclass = superclass;
    this.name = name;
    this.methods = methods;
  }

  public LoxClass(String name, Map<String, LoxFunction> methods) {
    this.superclass = null;
    this.name = name;
    this.methods = methods;
  }

  public LoxFunction findMethod(LoxInstance instance, String name) {
    if (methods.containsKey(name)) {
      return methods.get(name).bind(instance);
    }

    if (superclass != null) {
      return superclass.findMethod(instance, name);
    }

    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);

    LoxFunction initializer = methods.get("init");
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }

    return instance;
  }

  @Override
  public int arity() {
    LoxFunction initializer = methods.get("init");

    if (initializer != null) {
      return initializer.arity();
    }

    return 0;
  }
}