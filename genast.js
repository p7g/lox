const fs = require('fs').promises;
const path = require('path');

(async () => {
  if (process.argv.length !== 3) {
    console.error('Usage: node genast <output directory>');
    process.exit(1);
  }

  console.log('Generating Stmt class...');
  await defineAst(process.argv[2], 'Stmt', {
    Block: [
      'List<Stmt> statements',
    ],
    Break: [
      'Token token',
      'Expr levels',
      'int maxLevels',
    ],
    Expression: [
      'Expr expression',
    ],
    If: [
      'Expr condition',
      'Stmt thenBranch',
      'Stmt elseBranch',
    ],
    Var: [
      'Token name',
      'Expr initializer',
    ],
    While: [
      'Expr condition',
      'Stmt body',
    ],
  });

  console.log('Generating Expr class...');
  await defineAst(process.argv[2], 'Expr', {
    Assign: [
      'Token name',
      'Expr value',
    ],
    Call: [
      'Expr callee',
      'Token paren',
      'List<Expr> arguments',
    ],
    Binary: [
      'Expr left',
      'Token operator',
      'Expr right',
    ],
    Ternary: [
      'Expr left',
      'Token leftOperator',
      'Expr middle',
      'Token rightOperator',
      'Expr right',
    ],
    Grouping: [
      'Expr expression',
    ],
    Literal: [
      'Object value',
    ],
    Logical: [
      'Expr left',
      'Token operator',
      'Expr right',
    ],
    Unary: [
      'Token operator',
      'Expr right',
    ],
    Variable: [
      'Token name',
    ],
  });
  console.log('Done');
})().catch(console.error);

function defineAst(outputDir, baseName, types) {
  const outPath = path.join(outputDir, `${baseName}.java`);

  return fs.writeFile(outPath, `\
package lox;

import java.util.List;

abstract class ${baseName} {
  interface Visitor<R> {
${Object.entries(types).map(([className, fields]) => `\
    R visit${className + baseName}(${className} ${baseName.toLowerCase()});\
`).join('\n')}
  }

  abstract <R> R accept(Visitor<R> visitor);

${Object.entries(types).map(([className, fields]) => `\
  static class ${className} extends ${baseName} {
    ${fieldProperties(fields, 2)}

    ${className}(${fields.join(', ')}) {
      ${fieldAssignments(fields, 3)}
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visit${className + baseName}(this);
    }
  }
`).join('\n')}
}`);
}

function fieldAssignments(fields, indent) {
  return printLines(
    fields
      .map(f => f.split(' '))
      .map(([, name]) => `this.${name} = ${name};`),
    indent,
  );
}

function fieldProperties(fields, indent) {
  return printLines(
    fields
      .map(f => f.split(' '))
      .map(([type, name]) => `final ${type} ${name};`),
    indent,
  );
}

function printLines(lines, indent) {
  return lines.join(`\n${'  '.repeat(indent)}`);
}
