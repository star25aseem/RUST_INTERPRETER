package MYOWN;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    public static void main(String[] args) throws IOException {
     // System.out.println(args.length);
      if(args.length > 1)
      {
       System.out.println("Usage : Rust [script]");
       System.exit(64);
      } 
      else if (args.length == 1) 
      {
        runFile(args[0]);
      }
     // else 
      //{
        //runPrompt();
      //}
        byte[] bytes = Files.readAllBytes(Paths.get("C:\\Users\\veera\\Desktop\\Interpreter\\VSwork\\MYOWN\\text.txt"));
        run(new String(bytes,Charset.defaultCharset()));
        if(hadError) System.exit(64);
        if (hadRuntimeError) System.exit(70);
    }
    private static void runFile(String path) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get("C:\\Users\\veera\\Desktop\\Interpreter\\VSwork\\MYOWN\\text.txt"));
        run(new String(bytes,Charset.defaultCharset()));
        if(hadError) System.exit(64);
        if (hadRuntimeError) System.exit(70);
     }
     private static void runPrompt() throws IOException{
      InputStreamReader input = new InputStreamReader(System.in);
      BufferedReader reader = new BufferedReader(input);
      for(;;){
       System.out.println("RustConsole> ");
       String line = reader.readLine();
       if(line == null) break;// to end reading wh en there is no more lines
       run(line);  
       hadError = false;
      }
    }
    private static void run(String source){
      Token t=new Token(TokenType.fn, source, source, 0);
      ArrayList<Token> tokens = new ArrayList<Token>(t.tokenGenerator(source,0));
      Parser parser = new Parser(tokens);
      List<Stmt> statements = parser.parse();
      // Stop if there was a syntax error.
      if (hadError) return;
      Resolver resolver = new Resolver(interpreter);
      resolver.resolve(statements);
      if (hadError) return;
      interpreter.interpret(statements);
      //System.out.println(new AstPrinter().print(expression));
     // for (Token token : tokens) {
       // System.out.println(token);
      //}
    }
    static void error(int line, String message){
        report(line,"",message);
    }
    private static void report(int line, String where, String message){
        System.err.println("[line "+line+"] Error"+where+": "+message);
        hadError = true;
    }
    static void error(Token token, String message) {
     if (token.type == TokenType.EOF) {
        report(token.line, " at end", message);
      } 
      else {
        report(token.line, " at '" + token.lexeme + "'", message);
      }
    }
    static void runtimeError(RuntimeError error) {
      System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
      hadRuntimeError = true;
    }
  }


