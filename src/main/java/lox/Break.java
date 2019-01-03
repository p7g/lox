package lox;

class Break extends RuntimeException {
  public int levels;

  public Break(int levels) {
    super(null, null, false, false);
    this.levels = levels;
  }
}
