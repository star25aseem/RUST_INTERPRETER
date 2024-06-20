package MYOWN;
// import java.util.HashMap;
import java.util.*;
enum TokenType
{
    //Single Character tokens
    Leftparen,Rightparen,Leftcurl,Rightcurl,Comma,Dot,Minus,Plus,SemiColon,Slash,Multiply,
    // one or two character tokens
    Not,Notequals,Assign,Equals,Greater,GreaterThanEqual,Less,LessThanEqual,
    NewLine,Tab,range,
    // bang means '!'
    
    //Literals
    Identifiers,String,Number,

    //Keywords
    AND,CLASS,ELSE,FALSE,fn,FOR,IF,NIL,OR,PRINT,RETURN,SUPER,THIS,TRUE,let,WHILE,
    EOF,Let,In;
}

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;
    private String sources;
    private int current;
    ArrayList<Token> List=new ArrayList<Token>();
    Token(TokenType type,String lexeme , Object literal,int line)
    {
        this.lexeme=lexeme;
        this.type=type;
        this.line=line;
        this.literal=literal;
    }
    public String toString(){
        return type+" "+lexeme;
    }
    public ArrayList<Token> tokenGenerator(String inputStream,int line)
    {
        sources=inputStream;
        current=0;
       
        while(!isAtEnd())
        {
            char c = inputStream.charAt(current);
            switch (c){
                case '(': addToken(TokenType.Leftparen,"(","(",line); break;
                case ')': addToken(TokenType.Rightparen,")",")",line); break;
                case '"': string(line); break;
                case '+':addToken(TokenType.Plus,"+","+", line);
                break;
                case '-':addToken(TokenType.Minus, "-","-", line);
                break;
                case '*':addToken(TokenType.Multiply, "*","*", line);
                break;
                case ';':addToken(TokenType.SemiColon, ";", ";", line);
                line++;
                break;
                case '{':
                addToken(TokenType.Leftcurl, "{", "{", line);
                break;
                case '}':
                addToken(TokenType.Rightcurl, "}", "}", line);
                break;
                case '/': if(peekNext()=='/'){
                    while(peekNext()!='\n'){
                        current++;
                    }
                }
                else{
                addToken(TokenType.Slash, "/", "/", line);
                }
                break;
                case ',':
                addToken(TokenType.Comma, "comma", ",", line);
                break;
                case '.':
                if(peekNext()=='.')
                {
                    addToken(TokenType.range, "..", "..", line);
                    current++;
                }
                else
                {
                    // Main.error(line, "unexpected '.' did you mean .. ?");
                    // System.exit(1);
                    break;
                }
                break;
                case '!':
                if(peekNext()=='=')
                {
                    addToken( TokenType.Notequals, "!=", "!=", line);
                }
                else
                {
                    addToken( TokenType.Not, "!", "!", line);
                }
                break;
                case '=':
                if(peekNext()=='=')
                {
                    addToken( TokenType.Equals, "==", "==", line);
                    current++;
                }
                else
                {
                    addToken( TokenType.Assign, "=", "=", line);
                }
                break;
                case '<':
                if(peekNext()=='=')
                {
                    addToken( TokenType.LessThanEqual, "<=", "<=", line);
                    current++;
                }
                else
                {
                    addToken( TokenType.Less, "<","<", line);
                }
                break;
                case '>':
                if(peekNext()=='=')
                {
                    addToken( TokenType.GreaterThanEqual, ">=", ">=", line);
                }
                else
                {
                    addToken( TokenType.Greater, ">",">", line);
                }
                break;
                case '\n':
                line++;
                break;
                case '\t':                   
                break;
                case ' ':
                break; //ignoring space   
                default:
                if(c >= '0' && c <= '9')
                {
                    analyseNumbers(new StringBuilder(""), line);
                }
                else if(((c>='A' &&c<='Z') || (c>='a' && c<='z')))
                {
                    analyseMultiCharacters(line);
                }
                else if (isAlpha(c)) {
                    identifier();
                  }
                else
                {
                    // Main.error(line, "UnIdentified Literal !!");
                    // System.exit(1);
                    break;
                }
                break;
            }
            current++;
        }
        addToken( TokenType.EOF, "EOF", "EOF", line);
        return List;
    }
    private void addToken(TokenType type,String lexeme,Object literal,int line)
    {
        List.add(new Token(type, lexeme, literal, line));
    }
    private int string(int line)
    {
        current++;
        int start=current;
        int end=start;
        while(!isAtEnd() && sources.charAt(end)!='"')
        {
            end++;
            current++;
            if(current==sources.length())
            {
                System.out.printf("Unterminated String Exception at line : %d\n",line);
                // System.exit(1);
                return -1;
            }
        }
        if(end<sources.length())
        {
            addToken( TokenType.String,sources.substring(start,end),sources.substring(start, end), line);
        }
        return current;
    }
    private void identifier() {
        int start=current;
        while (isAlphaNumeric(peek()))
        {
            current++;
        }
        
        addToken(TokenType.Identifiers,sources.substring(start,current),"",line);
      }
    private void analyseMultiCharacters(int line)
    {      
        StringBuilder temp=new StringBuilder(String.valueOf(peek()));
        current++;
        while(!isAtEnd() && isAlpha(peek()))
        {
            temp=temp.append(sources.charAt(current++));
            if(!isAtEnd() && !isAlpha(peek()))
            {
                break;
            }
        }
        switch(temp.toString())
        {
            case "let":
            addToken(TokenType.Let,"let" ,temp, line);
            if(!isAtEnd() && peek()=='(')
            {
                // Main.error(line, "Illegal use of parenthesis Dont use parenthese after in!! in line: %d\n");
                // System.exit(1);
                return;
            }
            break;
            case "for":
            addToken(TokenType.FOR,temp.toString() ,temp, line);
            break;
            
            case "if":
            addToken( TokenType.IF, temp.toString(),temp, line);
            break;
            
            case "else":
            addToken( TokenType.ELSE, temp.toString(),temp, line);
            break;
            
            case "while":
            addToken( TokenType.WHILE,temp.toString(),temp, line);
            break;
            case "mut":
            break;
            case "in":
            addToken( TokenType.In, temp.toString(),temp, line);
            if(peek()=='(')
            {
                System.out.printf("Illegal use of parenthesis Dont use parenthese after in!! in line: %d\n",line);
                // System.exit(1);
                return;
            }
            break;
            case "fn":
            addToken(TokenType.fn, "function", "fn", line);
            break;
            case "print":
            if(peek()=='!')
            {
                addToken(TokenType.PRINT, "print!", null, line);
                current++;
            }
            else
            {
                Main.error(null, "expected ! infront of print statement");
            }
            break;
            default:
            addToken( TokenType.Identifiers, temp.toString(),temp, line);
            break;
        }
        current--;
    }
    private void analyseNumbers(StringBuilder temp,int line)
    {//for i in 1..10
        int start=current;
        while (!isAtEnd() && peek() >='0' && peek()<='9')
        {
            current++;
        }
        if(isAtEnd())
        {
            current=sources.length();
        }
        else if(peek()!='.')
        {
            addToken( TokenType.Number,sources.substring(start, current), Integer.valueOf(sources.substring(start, current)), line);
            current--;
            return;
        }
        if(!isAtEnd() && peek()=='.' && peekNext()=='.')
        {
            addToken( TokenType.Number,sources.substring(start, current), Double.parseDouble(sources.substring(start, current)), line);
            current--;
            return;
        }
        if (!isAtEnd() && peek() == '.' && peekNext() >='0' && peekNext()<='9') 
        {
        // Consuming "."
        current++;
        while (!isAtEnd() && peek() >='0' && peek()<='9'){ 
        current++;
        }
        addToken( TokenType.Number,sources.substring(start, current), Double.parseDouble(sources.substring(start, current)), line);
        current--;
        }
        else if(isAtEnd() && isDigit(sources.charAt(current-1)))
        {
            addToken( TokenType.Number,sources.substring(start, current), Double.parseDouble(sources.substring(start, current)), line);
        }
    }
    private char peek()
    {
        return sources.charAt(current);
    }
    private char peekNext()
    {
        if(current+1<sources.length())
        {
            return sources.charAt(current+1);
        }
        return '\0';
    }
    private boolean isAtEnd() {
    return current >= sources.length();
    }
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    private boolean isDigit(char c)
    {
        return (c>='0' && c<='9');
    }
}   

