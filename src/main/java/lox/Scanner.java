package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

class Scanner {
    private static final Map<Character, Character> escapes;
    static {
        escapes = new HashMap<>();
        escapes.put('b', '\b');
        escapes.put('f', '\f');
        escapes.put('r', '\r');
        escapes.put('n', '\n');
        escapes.put('t', '\t');
        escapes.put('\\', '\\');
        escapes.put('\'', '\'');
        escapes.put('"', '"');
    }

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("break", BREAK);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 0;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ':': addToken(COLON); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case '?': addToken(QUESTION_MARK); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if (match('/')) {
                    // a comment goes on until the end of the line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                }
                else if (match('*')) {
                    while (peek() != '*' && peekNext() != '/') {
                        if (peek() == '\n') {
                            line++;
                            column = 0;
                        }
                        advance();
                    }
                    advance();
                    advance();
                }
                else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\t':
            case '\r':
                // ignore whitespace
                break;
            case '\n':
                line++;
                column = 0;
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                }
                else if (isAlpha(c)) {
                    identifier();
                }
                else {
                    Lox.error(line, column, "Unexpected character");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        // see if the identifier is a reserved word
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);
        if (type == null) {
            type = IDENTIFIER;
        }

        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // decimal stuff
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the '.'
            advance();
        }

        while (isDigit(peek())) {
            advance();
        }

        addToken(
            NUMBER,
            Double.parseDouble(source.substring(start, current))
        );
    }

    private void string() {
        String value = "";
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 0;
            }
            if (match('\\')) {
                char sequence = advance();
                if (sequence == 'u') {
                    // consume 4 characters and try to make a char from that
                    String hex = "";
                    for (int i = 0; i < 4; i++) {
                        hex += advance();
                    }
                    try {
                        char character = (char) Integer.parseInt(hex, 16);
                        value += character;
                    }
                    catch (NumberFormatException error) {
                        Lox.error(line, column, "Invalid hex character escape");
                        return;
                    }
                }
                else if (escapes.containsKey(sequence)) {
                    char replace = escapes.get(sequence);
                    value += replace;
                }
                else {
                    Lox.error(line, column, "Unrecognized escape sequence");
                    return;
                }
            }
            else {
                value += advance();
            }
        }

        if (isAtEnd()) {
            Lox.error(line, column, "Unterminated string literal");
            return;
        }

        advance(); // closing '"'

        addToken(STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            ||  c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        column++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, column));
    }
}
