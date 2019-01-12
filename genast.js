const fs = require('fs').promises;
const path = require('path');

function printLines(lines, indent) {
  return lines.join(`\n${'  '.repeat(indent)}`);
}

function fieldAssignments(fields, indent) {
  return printLines(
    fields.map(f => f.split(' ')).map(([, name]) => `this.${name} = ${name};`),
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

const IMPLEMENTS = {
  'Lambda': ['CallableNode'],
  'Function': ['CallableNode'],
};
function implements(className) {
  const implements = IMPLEMENTS[className];
  if (implements && implements.length) {
    return ` implements ${implements.join(', ')}`;
  }
  return '';
}

const callableNodeGetters = `
    public List<Token> getParams() {
      return this.params;
    }

    public List<Stmt> getBody() {
      return this.body;
    }

    public Token getName() {
      return this.name;
    }
`;
const EXTRA = {
  'Lambda': callableNodeGetters,
  'Function': callableNodeGetters,
};
function extra(className) {
  return EXTRA[className] || '';
}

function defineAst(outputDir, baseName, types) {
  const outPath = path.join(outputDir, `${baseName}.java`);

  return fs.writeFile(
    outPath,
    `\
package lox;

import java.util.List;

abstract class ${baseName} {
  interface Visitor<R> {
${Object.entries(types)
    .map(
      ([className]) => `\
    R visit${className + baseName}(${className} ${baseName.toLowerCase()});\
`,
    )
    .join('\n')}
  }

  abstract <R> R accept(Visitor<R> visitor);

${Object.entries(types)
    .map(
      ([className, fields]) => `\
  static class ${className} extends ${baseName}${implements(className)} {
    ${fieldProperties(fields, 2)}

    public ${className}(${fields.join(', ')}) {
      ${fieldAssignments(fields, 3)}
    }
    ${extra(className)}
    <R> R accept(Visitor<R> visitor) {
      return visitor.visit${className + baseName}(this);
    }
  }
`,
    )
    .join('\n')}
}`,
  );
}

(async () => {
  if (process.argv.length !== 3) {
    // eslint-disable-next-line
    console.error('Usage: node genast <output directory>');
    process.exit(1);
  }

  console.log('Generating Stmt class...'); // eslint-disable-line
  await defineAst(process.argv[2], 'Stmt', {
    Block: [
      'List<Stmt> statements',
    ],
    Break: [
      'Token token',
      'Expr levels',
      'int maxLevels',
    ],
    Class: [
      'Token name',
      'Expr.Variable superclass',
      'List<Stmt.Function> methods',
      'List<Stmt.Function> staticMethods',
    ],
    Expression: [
      'Expr expression',
    ],
    Function: [
      'Token name',
      'List<Token> params',
      'List<Stmt> body',
    ],
    If: [
      'Expr condition',
      'Stmt thenBranch',
      'Stmt elseBranch',
    ],
    Let: [
      'Token name',
      'Expr initializer',
    ],
    Return: [
      'Token keyword',
      'Expr value',
    ],
    While: [
      'Expr condition',
      'Stmt body',
    ],
  });

  console.log('Generating Expr class...'); // eslint-disable-line
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
    Get: [
      'Expr object',
      'Token name',
    ],
    Grouping: [
      'Expr expression',
    ],
    Lambda: [
      'Token name',
      'List<Token> params',
      'List<Stmt> body',
    ],
    Literal: [
      'Object value',
    ],
    Logical: [
      'Expr left',
      'Token operator',
      'Expr right',
    ],
    Set: [
      'Expr object',
      'Token name',
      'Expr value',
    ],
    Super: [
      'Token keyword',
      'Token method',
    ],
    Ternary: [
      'Expr left',
      'Token leftOperator',
      'Expr middle',
      'Token rightOperator',
      'Expr right',
    ],
    This: [
      'Token keyword',
    ],
    Unary: [
      'Token operator',
      'Expr right',
    ],
    Variable: [
      'Token name',
    ],
  });
  console.log('Done'); // eslint-disable-line
})().catch(console.error); // eslint-disable-line
