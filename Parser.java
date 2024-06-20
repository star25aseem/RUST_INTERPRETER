package MYOWN;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import MYOWN.Expr;
// import static MYOWN.Token.TokenType;
import static MYOWN.Token.*;


class Parser {
    private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  private Expr expression() {
    return assignment();
  }
  
  private Stmt declaration() {
    try {
      if (match(TokenType.fn)) return function("function");
      if (match(TokenType.Let)) return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }
  private Stmt statement() {
    if (match(TokenType.PRINT)) return printStatement();
    if (match(TokenType.WHILE)) return whileStatement();
    if (match(TokenType.FOR)) return forStatement();
    if (match(TokenType.IF)) return ifStatement();
    if (match(TokenType.Leftcurl)) return new Stmt.Block(block());
    return expressionStatement();
  }
  private Stmt forStatement() {
    consume(TokenType.Leftparen, "Expect '(' after 'for'.");

/* Control Flow for-statement < Control Flow for-initializer
    // More here...
*/
//> for-initializer
    Stmt initializer;
    if (match(TokenType.SemiColon)) {
      initializer = null;
    } else if (check(TokenType.Identifiers)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }
//< for-initializer
//> for-condition

    Expr condition = null;
    if (!check(TokenType.SemiColon)) {
      condition = expression();
    }
    consume(TokenType.SemiColon, "Expect ';' after loop condition.");
//< for-condition
//> for-increment

    Expr increment = null;
    if (!check(TokenType.Rightparen)) {
      increment = expression();
    }
    consume(TokenType.Rightparen, "Expect ')' after for clauses.");
//< for-increment
//> for-body
    Stmt body = statement();

//> for-desugar-increment
    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(
              body,
              new Stmt.Expression(increment)));
    }

//< for-desugar-increment
//> for-desugar-condition
    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

//< for-desugar-condition
//> for-desugar-initializer
    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

//< for-desugar-initializer
    return body;
//< for-body
  }
  
  private Stmt ifStatement() {
    Expr condition = expression();
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(TokenType.ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
  private Stmt printStatement() {
    consume(TokenType.Leftparen,  "expected `(` after print!");
    Expr value = expression();
    consume(TokenType.Rightparen, "Expect ')' after print! condition."); 
    consume(TokenType.SemiColon, "Expect ';' after statement");
    return new Stmt.Print(value);
  }
  private Stmt varDeclaration() {
    Token name = consume(TokenType.Identifiers, "Expect variable name.");

    Expr initializer = null;
    if (match(TokenType.Assign)) {
      initializer = expression();
    }

    consume(TokenType.SemiColon, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
  private Stmt whileStatement() {
    Expr condition = expression();
    //consume(TokenType.Leftcurl, "x");
    Stmt body = statement();
    //consume(TokenType.Rightcurl, "Expect '}' after block.");
    return new Stmt.While(condition, body);
  }


  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SemiColon, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }


  private Stmt.Function function(String kind) {
    Token name = consume(TokenType.Identifiers, "Expect " + kind + " name.");
    consume(TokenType.Leftparen, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(TokenType.Rightparen)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(consume(TokenType.Identifiers, "Expect parameter name."));
      } while (match(TokenType.Comma));
    }
    consume(TokenType.Rightparen, "Expect ')' after parameters.");
    consume(TokenType.Leftcurl, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }


  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(TokenType.Rightcurl) && !isAtEnd()) {
      statements.add(declaration());
    }

   consume(TokenType.Rightcurl, "Expect '}' after block.");
    return statements;
  }
  private Expr or() {
    Expr expr = and();

    while (match(TokenType.OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
  private Expr and() {
    Expr expr = equality();

    while (match(TokenType.AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
    List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements; // [parse-error-handling]
  }
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }
  //equality       → comparison ( ( "!=" | "==" ) comparison )* ;
  private Expr equality() {
    Expr expr = comparison();

    while (match(TokenType.Notequals, TokenType.Equals)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
   private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    /*what the above for loop does, is it checks if the current token matches with any of the Tokens provided to
    match function...if not matching it returns false and leaves the current token alone.If it matches then 
    it consumes the current token(ie skipps it) and returns true */
    
    return false;
  }
  private Expr assignment() {
    /* Statements and State parse-assignment < Control Flow or-in-assignment
        Expr expr = equality();
    */
    //> Control Flow or-in-assignment
        Expr expr = or();
    //< Control Flow or-in-assignment
    
        if (match(TokenType.Assign)) {
          Token equals = previous();
          Expr value = assignment();
    
          if (expr instanceof Expr.Variable) {
            Token name = ((Expr.Variable)expr).name;
            return new Expr.Assign(name, value);
    //> Classes assign-set
          } else if (expr instanceof Expr.Get) {
            Expr.Get get = (Expr.Get)expr;
            return new Expr.Set(get.object, get.name, value);
    //< Classes assign-set
          }
    
          error(equals, "Invalid assignment target."); // [no-throw]
        }
    
        return expr;
      }
  private Expr comparison() {
    Expr expr = term();

    while (match(TokenType.Greater,TokenType.GreaterThanEqual, TokenType.Less, TokenType.LessThanEqual)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
 
  private Expr term() {
    Expr expr = factor();

    while (match(TokenType.Minus, TokenType.Plus)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
  
  private Expr factor() {
    Expr expr = unary();

    while (match(TokenType.Slash, TokenType.Multiply)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
  private Expr unary() {
    if (match(TokenType.Not, TokenType.Minus)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(TokenType.Rightparen)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(TokenType.Comma));
    }

    Token paren = consume(TokenType.Rightparen,"Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }
  private Expr call() {
    Expr expr = primary();

    while (true) { 
      if (match(TokenType.Leftparen)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }
  private Expr primary() {
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.Number, TokenType.String)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(TokenType.Identifiers)) {
      return new Expr.Variable(previous());
    }
    if (match(TokenType.Leftparen)) {
      Expr expr = expression();
      consume(TokenType.Rightparen, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    return null;
    // throw error(peek(), "Expect expression.");
  }
  
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
   // return new Token(TokenType.TRUE, "Error", "Hu Ha", 1);
     throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Main.error(token, message);
    return new ParseError();
  }
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SemiColon) return;

      switch (peek().type) {
        case fn:
        case let:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}