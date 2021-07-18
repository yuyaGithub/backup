package kc;

import java.util.ArrayList;

/**
 * 構文解析のクラス
 * @author info
 *
 */
public class Kc {

	private LexicalAnalyzer lexer; //使用する字句解析器

	private Token token; //字句解析器から受け取ったトークン

	private VarTable variableTable; //変数表

	private PseudoIseg iseg; //アセンブラコード表

 	ArrayList<Integer> breakAddrList = new ArrayList<>(); //break文のJUMP命令の番地を記憶する

	boolean inLoop = false;  //ループ内部か?


    /**
     * ソースファイル名を引数とするコンストラクタ.
     */
    Kc (String sourceFileName) {
    	lexer = new LexicalAnalyzer(sourceFileName);
    	iseg = new PseudoIseg();
    	variableTable = new VarTable();
    }

    /**
     * K21言語プログラム部解析
     */
    void parseProgram() {
    	token = lexer.nextToken();
    	if(token.checkSymbol(Symbol.MAIN)) parseMain_function();
    	else syntaxError("0");
    	if(token.checkSymbol(Symbol.EOF)) {
    		iseg.appendCode(Operator.HALT);
    	}else {
    		syntaxError("ファイル末ではありません");
    	}
    }

    //以降、必要なparse...メソッドを追記する。

    /**
     * Main_function部分の解析
     */
    void parseMain_function(){
    	if(token.checkSymbol(Symbol.MAIN)) token = lexer.nextToken();
    	else syntaxError("mainが期待されます");
    	if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    	else syntaxError("(が期待されます");
    	if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    	else syntaxError(")が期待されます");
    	if(token.checkSymbol(Symbol.LBRACE)) parseBlock();
    	else syntaxError("{が期待されます");
    }

    /**
     * Block部分の解析
     */
    void parseBlock() {
    	int tableSize = variableTable.size(); //ブロック開始時点の変数表のサイズ
    	if(token.checkSymbol(Symbol.LBRACE)) token = lexer.nextToken();
    	else syntaxError("{が期待されます");
    	while(firstStatement(token)) parseStatement();
    	if(token.checkSymbol(Symbol.RBRACE)) token = lexer.nextToken();
    	else syntaxError("}が期待されます");
    	variableTable.removeTail(tableSize);
    }

    /**
     * Statement部分の解析
     */
    void parseStatement() {
    	if(token.checkSymbol(Symbol.INT)) {
    		parseVar_decl_statement();
    	}else if(token.checkSymbol(Symbol.IF)){
    		parseIf_statement();
    	}else if(token.checkSymbol(Symbol.WHILE)) {
    		parseWhile_statement();
    	}else if(token.checkSymbol(Symbol.FOR)) {
    		parseFor_statement();
    	}else if(firstExp_statement(token)) {
    		parseExp_statement();
    	}else if(token.checkSymbol(Symbol.OUTPUTCHAR)) {
    		parseOutputchar_statement();
    	}else if(token.checkSymbol(Symbol.OUTPUTINT)) {
    		parseOutputint_statement();
    	}else if(token.checkSymbol(Symbol.BREAK)) {
    		parseBreak_statement();
    	}else if(token.checkSymbol(Symbol.LBRACE)) {
    		parseBlock();
    	}else if(token.checkSymbol(Symbol.SEMICOLON)) {
    		token = lexer.nextToken();
    	}else {
    		syntaxError("statementのFist集合が期待されます");
    	}
    }

    /**
     * Var_decl_statement部分の解析
     */
    void parseVar_decl_statement() {
    	parseVar_decl();
    	if(token.checkSymbol(Symbol.SEMICOLON)) token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    }

    /**
     * Var_decl部分の解析
     */
    void parseVar_decl() {
    	if(token.checkSymbol(Symbol.INT)) token = lexer.nextToken();
    	else syntaxError("intが期待されます");
    	parseName_list();
    }

    /**
     * Name_list部分の解析
     */
    void parseName_list() {
    	if(token.checkSymbol(Symbol.NAME)) parseName();
    	else syntaxError("nameが期待されます");
    	while(token.checkSymbol(Symbol.COMMA)) {
    		token = lexer.nextToken();
    		parseName();
    	}
    }

    /**
     * Name部分の解析
     */
    void parseName() {
    	String name = ""; //宣言された変数
    	int size = 1; //配列のサイズ
    	int value = 0; //各変数の値
    	int address;

    	//どの分岐でもNameが来るため最初にName判定
    	if(token.checkSymbol(Symbol.NAME)) {
    		name = token.getStrValue();
    		token = lexer.nextToken();
    	}else {
    		syntaxError("nameが期待されます");
    	}
    	if(variableTable.exist(name)) {
    		syntaxError("二重登録です");
    	}
    	//variableTable.registerNewVariable(Type.INT, name, 1);
    	// = <Constant>解析部分
    	if(token.checkSymbol(Symbol.ASSIGN)) {
    		token = lexer.nextToken();
    		value = parseConstant();
    		variableTable.registerNewVariable(Type.INT, name, 1);
    		address = variableTable.getAddress(name);
    		iseg.appendCode(Operator.PUSHI,value);
    		iseg.appendCode(Operator.POP,address);
    	// [ INT ] 解析部分
    	}else if(token.checkSymbol(Symbol.LBRACKET)) {
    		token = lexer.nextToken();
    		if(token.checkSymbol(Symbol.INTEGER)){
    			size = token.getIntValue();
    			value = parseConstant();
    			variableTable.registerNewVariable(Type.ARRAYOFINT, name, size);
    			if(token.checkSymbol(Symbol.RBRACKET)) token = lexer.nextToken();
    			else syntaxError("]が期待されます");
    		}else if(token.checkSymbol(Symbol.RBRACKET)) {
    			token = lexer.nextToken();
    			if(token.checkSymbol(Symbol.ASSIGN)) {
    				token = lexer.nextToken();
    			}
    			else syntaxError("=が期待されます");
    			if(token.checkSymbol(Symbol.LBRACE)) token = lexer.nextToken();
    			else syntaxError("{が期待されます");
    			//value = parseConstant_list();
    		    value = parseConstant();
    			ArrayList<Integer> valueList = new ArrayList<>();
    			valueList.add(value);
    			while(token.checkSymbol(Symbol.COMMA)) {
    				token = lexer.nextToken();
    				if(firstConstantOrConstantlist(token)) value = parseConstant();
    				else syntaxError("ll");
    				valueList.add(value);
    			}
    			if(token.checkSymbol(Symbol.RBRACE)) {
    				size = valueList.size();
        		    //System.out.println(size);
        			variableTable.registerNewVariable(Type.ARRAYOFINT, name, size);
        			//System.out.println(name);
        		    address = variableTable.getAddress(name);
        		    //System.out.println(address);
        		    for(int i = 0; i < size; ++i) {
        		    	//System.out.println(valueList.get(i));
        		    	iseg.appendCode(Operator.PUSHI,valueList.get(i));
        		    	//System.out.println(variableTable.getAddress(name));

        		    	iseg.appendCode(Operator.POP,address + i);
        		    }
    				token = lexer.nextToken();
    			}else {
    				syntaxError("}が期待されます");
    			}
    			//System.out.println(valueList.size());

    		//最初がName以外はエラーとなる
    		}else {
    			syntaxError("期待されるコードがえれませんでした");
    		}
    	}
    	variableTable.registerNewVariable(Type.INT, name, 1);
    }

    /**
     * Constant_list部分の解析
     */
    int parseConstant_list() {
    	int num = 0;
    	if(firstConstantOrConstantlist(token)) {
    		num = parseConstant();
    	}else {
    		syntaxError("ConstantのFirst集合のどれかが期待されます");
    	}
    	while(token.checkSymbol(Symbol.COMMA)) {
    		token = lexer.nextToken();
    		if(firstConstantOrConstantlist(token)) num = parseConstant();
    		else syntaxError("ConstantのFirst集合のどれかが期待されます");
    	}
    	return num;
    }

    /**
     * Constant部分の解析
     */
    int parseConstant() {
    	int value = 0;
    	int sign = 1;
    	if (this.token.checkSymbol(Symbol.CHARACTER)) {
    		value = token.getIntValue();
    		this.token = lexer.nextToken();
    	} else {
    		if (token.checkSymbol(Symbol.SUB)) {
    			sign = -1;
    			token = lexer.nextToken();
    		}
    		if (this.token.checkSymbol(Symbol.INTEGER)) {
    			value = sign * token.getIntValue();
    			token = lexer.nextToken();
          } else {
    	        syntaxError("整数または文字が期待されます");
          }
       }
    	return value;
    }

    /**
     * If_statement部分の解析
     */
    void parseIf_statement() {
    	if(token.checkSymbol(Symbol.IF)) token = lexer.nextToken();
    	else syntaxError("ifが期待されます");
    	if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    	else syntaxError("(が期待されます");
    	if(firstExpression(token)) parseExpression();
    	else syntaxError("ExpressionのFirst集合が期待されます");
    	int beqAddr = iseg.appendCode(Operator.BEQ);
    	if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    	else syntaxError(")が期待されます");

    	if(firstStatement(token)) parseStatement();
    	else syntaxError("statementのFirst集合が期待されます");
    	int stLastAddr = iseg.getLastCodeAddress();

    	iseg.replaceCode(beqAddr, stLastAddr + 1); //最後の番地の次にとぶ
    }

    /**
     * While_statement部分の解析
     */
    void parseWhile_statement() {
    	if(token.checkSymbol(Symbol.WHILE)) token = lexer.nextToken();
    	else syntaxError("whileが期待されます");
    	if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    	else syntaxError("(が期待されます");
    	int lastAddr = iseg.getLastCodeAddress(); //条件式直前の番地を記憶
    	if(firstExpression(token)) parseExpression();
    	else syntaxError("式が期待されます");
    	if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    	else syntaxError(")が期待されます");
    	int beqAddr = iseg.appendCode(Operator.BEQ);
    	boolean outerLoop = inLoop;
    	ArrayList<Integer> outerList = breakAddrList;
    	inLoop = true;
    	breakAddrList = new ArrayList<>();
    	if(firstStatement(token)) parseStatement();
    	else syntaxError("式が期待されます");
    	int jumpAddr = iseg.appendCode(Operator.JUMP,lastAddr + 1); //条件式に飛ぶようにする
    	for(int i = 0; i < breakAddrList.size(); ++i) {
    		int breakAddr = breakAddrList.get(i);
    		iseg.replaceCode(breakAddr, jumpAddr + 1);
    	}
    	inLoop = outerLoop;
    	breakAddrList = outerList;
    	iseg.replaceCode(beqAddr, jumpAddr + 1);
    	//System.out.println(iseg.getLastCodeAddress());
    }

    /**
     * For_statement部分の解析
     */
    void parseFor_statement() {
    	if(token.checkSymbol(Symbol.FOR)) token = lexer.nextToken();
    	else syntaxError("forが期待されます");
    	int tablesize = variableTable.size(); //for分前の変数表のサイズを保存
    	int removeAddr;
    	//
    	if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    	else syntaxError("(が期待されます");
    	//Expression,Var_declの場合分け
    	if(firstExpression(token)) {
    		parseExpression();
    		removeAddr = iseg.appendCode(Operator.REMOVE);
    	}else if(token.checkSymbol(Symbol.INT)) {
    		parseVar_decl();
    	}else {
    		syntaxError("式が期待されます");
    	}
    	if(token.checkSymbol(Symbol.SEMICOLON)) token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    	removeAddr = iseg.getLastCodeAddress();
    	//L1以降　
    	if(firstExpression(token)) parseExpression();
    	else syntaxError("ExpressionのFirst集合が期待されます");
    	if(token.checkSymbol(Symbol.SEMICOLON))token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    	int beqAddr = iseg.appendCode(Operator.BEQ,-1);
    	int jumpAddr = iseg.appendCode(Operator.JUMP,-1);
    	//L2以降
    	if(firstExpression(token)) {
    		parseExpression();
    		iseg.appendCode(Operator.REMOVE);
    		iseg.appendCode(Operator.JUMP,removeAddr + 1);
    	}else {
    		syntaxError("ExpressionのFisrt集合が期待されます");
    	}
    	int j = iseg.getLastCodeAddress() + 1;
    	if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    	else syntaxError(")が期待されます");

    	//L3以降　
    	if(firstStatement(token)) {
    		parseStatement();
    		iseg.appendCode(Operator.JUMP,jumpAddr + 1);
    	}
    	else {
    		syntaxError("statementのFirst集合が期待されます");
    	}
    	iseg.replaceCode(jumpAddr, j);
    	iseg.replaceCode(beqAddr, iseg.getLastCodeAddress() + 1);
    	variableTable.removeTail(tablesize);
    }

    /**
     * Exp_statement部分の解析
     */
    void parseExp_statement() {
    	if(firstExpression(token)) parseExpression();
    	else syntaxError("式が期待されます");
    	if(token.checkSymbol(Symbol.SEMICOLON)) {
    		token = lexer.nextToken();
    		iseg.appendCode(Operator.REMOVE);
    	}else {
    		syntaxError(";が期待されます");
    	}
    }

    /**
     * Outputchar_statement部分の解析
     */
    void parseOutputchar_statement() {
    	if(token.checkSymbol(Symbol.OUTPUTCHAR))token = lexer.nextToken();
    	else syntaxError("outputcharが期待されます");
    	if(token.checkSymbol(Symbol.LPAREN))token = lexer.nextToken();
    	else syntaxError("(が期待されます");
       	if(firstExpression(token)) parseExpression();
    	else syntaxError("式が期待されます");
       	iseg.appendCode(Operator.OUTPUTC);
       	iseg.appendCode(Operator.OUTPUTLN);
       	if(token.checkSymbol(Symbol.RPAREN))token = lexer.nextToken();
    	else syntaxError(")が期待されます");
       	if(token.checkSymbol(Symbol.SEMICOLON))token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    }

    /**
     * Outputint_statement部分の解析
     */
    void parseOutputint_statement() {
    	if(token.checkSymbol(Symbol.OUTPUTINT)) {
    		token = lexer.nextToken();
    	}
    	else syntaxError("outputintが期待されます");
    	if(token.checkSymbol(Symbol.LPAREN)) token = lexer.nextToken();
    	else syntaxError("(が期待されます");
       	if(firstExpression(token)) parseExpression();
    	else syntaxError("式が期待されます");
       	iseg.appendCode(Operator.OUTPUT);
       	iseg.appendCode(Operator.OUTPUTLN);
       	if(token.checkSymbol(Symbol.RPAREN))token = lexer.nextToken();
    	else syntaxError(")が期待されます");
       	if(token.checkSymbol(Symbol.SEMICOLON))token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    }

    /**
     * Break_statement部分の解析
     */
    void parseBreak_statement() {
      	if(token.checkSymbol(Symbol.BREAK))token = lexer.nextToken();
    	else syntaxError("breakが期待されます");
      	if(!inLoop) {
      		syntaxError("ループ内ではありません");
      	}
      	int addr = iseg.appendCode(Operator.JUMP,-1);
      	breakAddrList.add(addr);
      	if(token.checkSymbol(Symbol.SEMICOLON))token = lexer.nextToken();
    	else syntaxError(";が期待されます");
    }

    /**
     * Expression部分の解析
     */
    void parseExpression() {
    	boolean hasLeftValue = true;
      	if(firstExpression(token)) hasLeftValue = parseExp();
    	else syntaxError("式が期待されます");
      	if(token.checkSymbol(Symbol.ASSIGN) || token.checkSymbol(Symbol.ASSIGNADD) || token.checkSymbol(Symbol.ASSIGNSUB) ||
      			token.checkSymbol(Symbol.ASSIGNMUL) || token.checkSymbol(Symbol.ASSIGNDIV)) {
      		if(!hasLeftValue) {
      			syntaxError("左辺値がありません");
      		}
      		if(token.checkSymbol(Symbol.ASSIGN)) {
      			token = lexer.nextToken();
      			parseExpression();
      			iseg.appendCode(Operator.ASSGN);
      		}else if(token.checkSymbol(Symbol.ASSIGNADD)) {
      			iseg.appendCode(Operator.COPY);
      			iseg.appendCode(Operator.LOAD);
      			token = lexer.nextToken();
      			parseExpression();
      			iseg.appendCode(Operator.ADD);
      			iseg.appendCode(Operator.ASSGN);
      		}else if(token.checkSymbol(Symbol.ASSIGNSUB)) {
      			iseg.appendCode(Operator.COPY);
      			iseg.appendCode(Operator.LOAD);
      			token = lexer.nextToken();
      			parseExpression();
      			iseg.appendCode(Operator.SUB);
      			iseg.appendCode(Operator.ASSGN);
      		}else if(token.checkSymbol(Symbol.ASSIGNMUL)) {
      			iseg.appendCode(Operator.COPY);
      			iseg.appendCode(Operator.LOAD);
      			token = lexer.nextToken();
      			parseExpression();
      			iseg.appendCode(Operator.MUL);
      			iseg.appendCode(Operator.ASSGN);
      		}else if(token.checkSymbol(Symbol.ASSIGNDIV)) {
      			iseg.appendCode(Operator.COPY);
      			iseg.appendCode(Operator.LOAD);
      			token = lexer.nextToken();
      			parseExpression();
      			iseg.appendCode(Operator.DIV);
      			iseg.appendCode(Operator.ASSGN);
      		}
      	}
    }

    /**
     * Exp部分の解析
     */
    boolean parseExp() {
    	int i = 0; //||の個数
    	boolean hasLeftValue = false;
    	if(firstExpression(token)) hasLeftValue = parseLogical_term();
    	else syntaxError("式が期待されます");
    	while(token.checkSymbol(Symbol.OR)) {
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseLogical_term();
    		else syntaxError("式が期待されます");
    		i++;
    		hasLeftValue = false;
    	}
    	for(int j = 0;j < i;j++) {
    		iseg.appendCode(Operator.OR);
    	}
    	return hasLeftValue;
    }

    /**
     * Logical_term部分の解析
     */
    boolean parseLogical_term() {
    	int i = 0; //&&演算子の個数
    	boolean hasLeftValue = false;
     	if(firstExpression(token)) hasLeftValue = parseLogical_factor();
    	else syntaxError("式が期待されます");
    	while(token.checkSymbol(Symbol.AND)) {
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseLogical_factor();
    		else syntaxError("式が期待されます");
    		i++;
    		hasLeftValue = false;
    	}
    	for(int j = 0;j < i;j++) {
    		iseg.appendCode(Operator.AND);
    	}
    	return hasLeftValue;
    }

    /**
     * Logical_factor部分の解析
     */
    boolean parseLogical_factor() {
    	boolean hasLeftValue = false;
    	if(firstExpression(token)) hasLeftValue = parseArithmetic_expression();
    	else syntaxError("式が期待されます");
    	if(token.checkSymbol(Symbol.EQUAL) || token.checkSymbol(Symbol.NOTEQ) || token.checkSymbol(Symbol.LESS) ||
    			token.checkSymbol(Symbol.GREAT)) {
    		hasLeftValue = false;
    		Symbol op = token.getSymbol();
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseArithmetic_expression();
    		else syntaxError("式が期待されます");
    		int compAddr = iseg.appendCode(Operator.COMP);
    		if(op == Symbol.EQUAL) {
    			iseg.appendCode(Operator.BEQ,compAddr + 4);
    		}else if(op == Symbol.LESS) {
    			iseg.appendCode(Operator.BLT,compAddr + 4);
    		}else if(op == Symbol.LESSEQ) {
    			iseg.appendCode(Operator.BLE,compAddr + 4);
    		}else if(op == Symbol.NOTEQ) {
    			iseg.appendCode(Operator.BNE,compAddr + 4);
    		}else if(op == Symbol.GREAT) {
    			iseg.appendCode(Operator.BGT,compAddr + 4);
    		}else if(op == Symbol.GREATEQ) {
    			iseg.appendCode(Operator.BGE,compAddr + 4);
    		}
    		iseg.appendCode(Operator.PUSHI,0);
    		iseg.appendCode(Operator.JUMP,compAddr + 5);
    		iseg.appendCode(Operator.PUSHI,1);
    	}
    	return hasLeftValue;
    }


    /**
     * Arithmetic_expression部分の解析
     */
    boolean parseArithmetic_expression() {
    	boolean hasLeftValue = false;
    	if(firstExpression(token)) hasLeftValue = parseArithmetic_term();
    	else syntaxError("式が期待されます");
    	while(token.checkSymbol(Symbol.ADD) || token.checkSymbol(Symbol.SUB)) {
    		if(token.checkSymbol(Symbol.ADD)) {
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseArithmetic_term();
        		else syntaxError("式が期待されます");
    			iseg.appendCode(Operator.ADD);
    		}else {
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseArithmetic_term();
        		else syntaxError("式が期待されます");
    			iseg.appendCode(Operator.SUB);
    		}
    		hasLeftValue = false;
    	}
    	return hasLeftValue;
    }

    /**
     * Arithmetic_term部分の解析
     */
    boolean parseArithmetic_term() {
    	boolean hasLeftValue = false;
    	if(firstExpression(token)) hasLeftValue = parseArithmetic_factor();
    	else syntaxError("66");
    	while(token.checkSymbol(Symbol.MUL) || token.checkSymbol(Symbol.DIV) || token.checkSymbol(Symbol.MOD)) {
    		if(token.checkSymbol(Symbol.MUL)) {
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseArithmetic_factor();
        		else syntaxError("式が期待されます");
    			iseg.appendCode(Operator.MUL);
    		}else if(token.checkSymbol(Symbol.DIV)){
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseArithmetic_factor();
        		else syntaxError("式が期待されます");
    			iseg.appendCode(Operator.DIV);
    		}else {
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseArithmetic_factor();
        		else syntaxError("式が期待されます");
    			iseg.appendCode(Operator.MOD);
    		}
    		hasLeftValue = false;
    	}
    	return hasLeftValue;
    }

    /**
     * Arithmetic_factor部分の解析
     */
    boolean parseArithmetic_factor() {
    	if(firstUnsinged(token)) {
    		boolean hasLeftValue = parseUnsigned_factor();
    		return hasLeftValue;
    	}else if(token.checkSymbol(Symbol.SUB)) {
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseArithmetic_factor();
    		else syntaxError("式が期待されます");
    		iseg.appendCode(Operator.CSIGN);
    		return false;
    	}else if(token.checkSymbol(Symbol.NOT)) {
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseArithmetic_factor();
    		else syntaxError("式が期待されます");
    		iseg.appendCode(Operator.NOT);
    		return false;
    	}else {
    		syntaxError("");
    		return false;
    	}
    }

    /**
     * Unsigned部分の解析
     */
    boolean parseUnsigned_factor() {
    	int value = 0; //宣言された整数の値
    	int charCode = 0; //宣言された文字のコード
    	String name = ""; //宣言された変数の変数名　
    	int address = 0; //宣言された変数の番地　
    	boolean isLeftValue = false; //左辺値の判定，falseなら左辺値ではない
    	if(token.checkSymbol(Symbol.NAME)) {
    		//isLeftValue = true;
    		name = token.getStrValue();
    		if(!variableTable.exist(name)) {
    			syntaxError("未登録の変数です");
    		}
    		address = variableTable.getAddress(name);
    		token = lexer.nextToken();
    		if(token.checkSymbol(Symbol.INC)) {
    			iseg.appendCode(Operator.PUSH, address);
        		iseg.appendCode(Operator.PUSHI, address);
        		iseg.appendCode(Operator.PUSH, address);
    			iseg.appendCode(Operator.INC);
    			iseg.appendCode(Operator.ASSGN);
    			iseg.appendCode(Operator.REMOVE);
    			token = lexer.nextToken();
    		}else if(token.checkSymbol(Symbol.DEC)) {
    			iseg.appendCode(Operator.PUSH, address);
        		iseg.appendCode(Operator.PUSHI, address);
        		iseg.appendCode(Operator.PUSH, address);
    			iseg.appendCode(Operator.DEC);
    			iseg.appendCode(Operator.ASSGN);
    			iseg.appendCode(Operator.REMOVE);
    			token = lexer.nextToken();
    		}else if(token.checkSymbol(Symbol.LBRACKET)) {
    			if(!variableTable.checkType(name, Type.ARRAYOFINT)) {
    				syntaxError(name + " の宣言と型が一致しません");
    			}
    			iseg.appendCode(Operator.PUSHI, address);
    			token = lexer.nextToken();
    			if(firstExpression(token)) parseExpression();
    			else syntaxError("9999");
    			if(token.checkSymbol(Symbol.RBRACKET)) token = lexer.nextToken();
    			else syntaxError("]が期待されます");
    			iseg.appendCode(Operator.ADD);
    			   if (token.checkSymbol(Symbol.ASSIGN) ||
    					token.checkSymbol(Symbol.ASSIGNADD) ||
    					token.checkSymbol(Symbol.ASSIGNSUB) ||
    					token.checkSymbol(Symbol.ASSIGNMUL) ||
    					token.checkSymbol(Symbol.ASSIGNDIV) ||
    					token.checkSymbol(Symbol.ASSIGNMOD)) {
    				   isLeftValue = true;
    			   } else {
    				   this.iseg.appendCode(Operator.LOAD);
    				   isLeftValue = false;
    			   }
    		}else {
    			if(token.checkSymbol(Symbol.ASSIGN)) {
    				isLeftValue = true;
        			iseg.appendCode(Operator.PUSHI,address);
        		}else if(token.checkSymbol(Symbol.ASSIGNADD)){
        			isLeftValue = true;
        			iseg.appendCode(Operator.PUSHI,address);
        		}else if(token.checkSymbol(Symbol.ASSIGNSUB)){
        			isLeftValue = true;
        			iseg.appendCode(Operator.PUSHI,address);
        		}else if(token.checkSymbol(Symbol.ASSIGNMUL)){
        			isLeftValue = true;
        			iseg.appendCode(Operator.PUSHI,address);
        		}else if(token.checkSymbol(Symbol.ASSIGNDIV)){
        			isLeftValue = true;
        			iseg.appendCode(Operator.PUSHI,address);
        		}else {
        			iseg.appendCode(Operator.PUSH,address);
        		}
    		}
    	}else if(token.checkSymbol(Symbol.INC) || token.checkSymbol(Symbol.DEC)) {

    		Symbol op = token.getSymbol(); //INC,DECの保存用変数　
    		token = lexer.nextToken();
    		if(token.checkSymbol(Symbol.NAME)) {
    			name = token.getStrValue();
    			if(!variableTable.exist(name)) {
        			syntaxError("未登録の変数です");
        		}
    			token = lexer.nextToken();
    			address = variableTable.getAddress(name);
    			iseg.appendCode(Operator.PUSHI,address);
    			if(token.checkSymbol(Symbol.LBRACKET)) {
        			token = lexer.nextToken();
        			if(firstExpression(token)) parseExpression();
        			else syntaxError("式が期待されます");
        			if(token.checkSymbol(Symbol.RBRACKET)) {
        				iseg.appendCode(Operator.ADD);
        				token = lexer.nextToken();
        			}
        			else syntaxError("]が期待されます");
        			iseg.appendCode(Operator.COPY);
        			iseg.appendCode(Operator.LOAD);
        			if(op == Symbol.INC) {
        				iseg.appendCode(Operator.INC);
        			}else {
        				iseg.appendCode(Operator.DEC);
        			}
        			iseg.appendCode(Operator.ASSGN);
        			//iseg.appendCode(Operator.REMOVE);
        		}else {
        			iseg.appendCode(Operator.PUSH, address);
        			if(op == Symbol.INC) {
        				iseg.appendCode(Operator.INC);
        			}else {
        				iseg.appendCode(Operator.DEC);
        			}
        			iseg.appendCode(Operator.ASSGN);
        		}
    		}
    		else syntaxError("nameが期待されます");
    	}else if(token.checkSymbol(Symbol.INTEGER)) {
    		value = token.getIntValue();
    		token = lexer.nextToken();
    		iseg.appendCode(Operator.PUSHI, value);
    	}else if(token.checkSymbol(Symbol.CHARACTER)) {
    		charCode = token.getIntValue();
    		token = lexer.nextToken();
    		iseg.appendCode(Operator.PUSHI, charCode);
    	}else if(token.checkSymbol(Symbol.LPAREN)) {
    		token = lexer.nextToken();
    		if(firstExpression(token)) parseExpression();
    		else syntaxError("式がが期待されます");
    		if(token.checkSymbol(Symbol.RPAREN)) token = lexer.nextToken();
    		else syntaxError(")が期待されます");
    	}else if(token.checkSymbol(Symbol.INPUTCHAR)) {
    		token = lexer.nextToken();
    		iseg.appendCode(Operator.INPUTC);
    	}else if(token.checkSymbol(Symbol.INPUTINT)) {
    		token = lexer.nextToken();
    		iseg.appendCode(Operator.INPUT);
    	}else {
    		syntaxError("10006");
    	}
    	return isLeftValue;
    }

    //以降Firstメソッドが多い物に関しての判定メソッドを記述する

    /**
     * statementのFirst集合
     * @param t
     * @return
     */
    boolean firstStatement(Token t) {
    	if(t.checkSymbol(Symbol.INT) ||t.checkSymbol(Symbol.IF) ||t.checkSymbol(Symbol.WHILE)  ||t.checkSymbol(Symbol.FOR)
    			||t.checkSymbol(Symbol.SUB) ||t.checkSymbol(Symbol.NOT) ||t.checkSymbol(Symbol.NAME) ||t.checkSymbol(Symbol.INC)
    			||t.checkSymbol(Symbol.DEC) ||t.checkSymbol(Symbol.INTEGER) ||t.checkSymbol(Symbol.CHARACTER) ||t.checkSymbol(Symbol.LPAREN)
    			||t.checkSymbol(Symbol.INPUTCHAR) ||t.checkSymbol(Symbol.INPUTINT) ||t.checkSymbol(Symbol.OUTPUTCHAR) ||t.checkSymbol(Symbol.OUTPUTINT)
    			||t.checkSymbol(Symbol.BREAK) ||t.checkSymbol(Symbol.LBRACE) ||t.checkSymbol(Symbol.SEMICOLON)) {
    		return true;
    	}
    	return false;
    }

    /**
     * ExpstatementのFirst集合
     * @param t
     * @return
     */
    boolean firstExp_statement(Token t) {
    	return (t.checkSymbol(Symbol.SUB) || t.checkSymbol(Symbol.NOT) || t.checkSymbol(Symbol.NAME)
    			|| t.checkSymbol(Symbol.INC) || t.checkSymbol(Symbol.DEC) || t.checkSymbol(Symbol.INTEGER) || t.checkSymbol(Symbol.CHARACTER)
    			|| t.checkSymbol(Symbol.LPAREN) || t.checkSymbol(Symbol.INPUTCHAR) || t.checkSymbol(Symbol.INPUTINT));
    }

    /**
     * ConstantのFirst集合
     * @param t
     * @return
     */
    boolean firstConstantOrConstantlist(Token t) {
    	return (t.checkSymbol(Symbol.SUB )|| t.checkSymbol(Symbol.INTEGER) || t.checkSymbol(Symbol.CHARACTER));
    }

    /**
     * Expression,Exp,Logical,ArithmeticのFirst集合
     * @param t
     * @return
     */
    boolean firstExpression(Token t) {
    	return (t.checkSymbol(Symbol.SUB) || t.checkSymbol(Symbol.NOT) || t.checkSymbol(Symbol.NAME)
    			|| t.checkSymbol(Symbol.INC) || t.checkSymbol(Symbol.DEC) || t.checkSymbol(Symbol.INTEGER)
    			|| t.checkSymbol(Symbol.CHARACTER)|| t.checkSymbol(Symbol.LPAREN)
    			|| t.checkSymbol(Symbol.INPUTCHAR) || t.checkSymbol(Symbol.INPUTINT));
    }

    /**
     * Unsingedのfirst集合
     * @param t
     * @return
     */
    boolean firstUnsinged(Token t) {
    	return (t.checkSymbol(Symbol.NAME) || t.checkSymbol(Symbol.INC) || t.checkSymbol(Symbol.DEC) || t.checkSymbol(Symbol.INTEGER)
    			|| t.checkSymbol(Symbol.CHARACTER) || t.checkSymbol(Symbol.LPAREN) || t.checkSymbol(Symbol.INPUTCHAR) ||
    			t.checkSymbol(Symbol.INPUTINT));
    }



    /**
     * 現在読んでいるファイルを閉じる (lexerのcloseFile()に委譲)
     */
    void closeFile() {
    	lexer.closeFile();
    }

    /**
     * アセンブラコードをファイルに出力する (isegのdump2file()に委譲)
     *
     */
    void dump2file() {
    	iseg.dump2file();
    }

    /**
     * アセンブラコードをファイルに出力する (isegのdump2file()に委譲)
     *
     */
    void dump2file (String fileName) {
    	iseg.dump2file(fileName);
    }

    /**
     * エラーメッセージを出力しプログラムを終了する
     * @param message 出力エラーメッセージ
     */
    private void syntaxError (String message) {
        System.out.print (lexer.analyzeAt());
        //下記の文言は自動採点で使用するので変更しないでください。
        System.out.println ("で構文解析プログラムが構文エラーを検出");
        System.out.println (message);
        closeFile();
        System.exit(0);
    }

    /**
     * 引数で指定したK21言語ファイルを解析する
     * 読み込んだファイルが文法上正しければアセンブラコードを出力するs
     */
    public static void main (String[] args) {
        Kc parser;

        if (args.length == 0) {
            System.out.println ("Usage: java kc.Kc21 file [objectfile]");
            System.exit (0);
        }

        parser = new Kc (args[0]);

        parser.parseProgram();
        parser.closeFile();


        if (args.length == 1) {
            parser.dump2file();
        }else {

            parser.dump2file (args[1]);
        }
    }
}
