package ruke.vrj.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by Ruke on 23/09/2016.
 */
public class CompilerTest {
    
    private static Compiler compile(String code) {
        Compiler compiler = new Compiler();
        compiler.compile(new ANTLRInputStream(code + "\n"));
        return compiler;
    }
    
    private static String translate(String code) {
        return new Compiler().compile(new ANTLRInputStream(code + "\n"));
    }
    
    @Test
    public void mustCheckVisibilityInStructs() {
        String code = String.join("\n",
            "struct foo",
                "private integer a",
                "integer b",
                "private method privateDoSomething takes nothing returns nothing",
                "endmethod",
                "method doSomething takes nothing returns nothing",
                "endmethod",
            "endstruct",
            "function bar takes nothing returns nothing",
                "local foo f",
                "set f.a = 42",
                "set f.b = 24",
                "call f.privateDoSomething()",
                "call f.doSomething()",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            "11:4 - f.a is not a variable",
            compiler.getAllErrors().get(0).getMessage()
        );
    
        Assert.assertEquals(
            "13:5 - f.privateDoSomething is not a function",
            compiler.getAllErrors().get(1).getMessage()
        );
    }
    
    @Test
    public void mustCheckVisibilityOnLibraries() {
        String code = String.join("\n",
            "library A",
                "function myPublic takes nothing returns nothing",
                "endfunction",
                "private function myPrivate takes nothing returns nothing",
                "endfunction",
            "endlibrary",
            "library B",
                "function init takes nothing returns nothing",
                    "call A.myPublic()",
                    "call A.myPrivate()",
                "endfunction",
            "endlibrary"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            "10:5 - A.myPrivate is not a function",
            compiler.getAllErrors().get(0).getMessage()
        );
    }
    
    @Test
    public void mustRequireValidLibrary() {
        String code = String.join("\n",
            "globals",
                "integer B",
            "endglobals",
            "library A requires B, C, D",
            "endlibrary",
            "library C",
            "endlibrary"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            "4:25 - D is not a library",
            compiler.getAllErrors().get(0).getMessage()
        );
    
        Assert.assertEquals(
            "4:19 - B is not a library",
            compiler.getAllErrors().get(1).getMessage()
        );
    }
    
    @Test
    public void mustDeclareValidLibraryInitializer() {
        String code = String.join("\n",
            "library A initializer InitA",
                "function InitA takes integer i returns nothing",
                "endfunction",
            "endlibrary",
            "library B initializer InitB",
                "function InitB takes nothing returns nothing",
                "endfunction",
            "endlibrary"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            "1:22 - Initializers must not take any parameters",
            compiler.getAllErrors().get(0).getMessage()
        );
    }
    
    @Test
    @Ignore
    public void mustTranslate() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "local integer i = 42",
                "if i then",
                    "local real r = 44",
                    "call foo()",
                "endif",
                "return i",
            "endfunction",
            "globals",
                "real pii = 3.14",
            "endglobals"
        );
        
        Assert.assertEquals(
            String.join("\n",
                "globals",
                    "real pii = 3.14",
                "endglobals",
                "function foo takes nothing returns nothing",
                    "local integer i",
                    "local real r",
                    "set i = 42",
                    "if i then",
                        "set r = 44",
                        "call foo()",
                    "endif",
                    "return i",
                "endfunction"
            ),
            translate(code));
    }
    
    @Test
    public void mustDetectAlreadyDefined() {
        String code = String.join("\n",
            "globals",
                "real pi = 3.14",
                "real pi = 14.3",
            "endglobals"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "3:0 - Variable pi is already defined"
        );
    }
    
    @Test
    public void mustDetectUndefined() {
        String code = String.join("\n",
            "globals",
                "real pi = pi2",
            "endglobals"
        );
        
        Compiler compiler = compile(code);
        
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "2:10 - Incompatible type. Expected real but nothing given"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(1).getMessage(),
            "2:10 - pi2 is not a variable"
        );
    }
    
    @Test
    public void mustDetectTypeCompatibility() {
        String code = String.join("\n",
            "globals",
                "real pi = true",
            "endglobals"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "2:10 - Incompatible type. Expected real but boolean given"
        );
    }
    
    @Test
    public void mustDetectNonFunctions() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "call real()",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "2:5 - real is not a function"
        );
    }
    
    @Test
    public void mustDetectValidType() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "local foo bar",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "2:6 - foo is not a valid type"
        );
    }
    
    @Test
    public void mustCheckForNumericExpression() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "local integer i",
                "set i = true - 5",
                "set i = i + 5",
                "set i = 1 % null",
                "set i = false / foo",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "5:12 - null is not a numeric expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(1).getMessage(),
            "6:8 - false is not a numeric expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(2).getMessage(),
            "3:8 - true is not a numeric expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(3).getMessage(),
            "6:16 - foo is not a variable"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(4).getMessage(),
            "6:16 - foo is not a numeric expression"
        );
    }
    
    @Test
    public void mustCheckBooleanExpression() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "local boolean bar",
                "set bar = 1 or 2 and null",
                "set bar = false or true",
                "set bar = null and false",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "3:21 - null is not a boolean expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(1).getMessage(),
            "3:15 - 2 is not a boolean expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(2).getMessage(),
            "5:10 - null is not a boolean expression"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(3).getMessage(),
            "3:10 - 1 is not a boolean expression"
        );
    }
    
    @Test
    public void mustCheckConditionals() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
                "if 1 then",
                "elseif true then",
                "elseif null then",
                "else",
                "endif",
                "loop",
                    "exitwhen 1",
                    "exitwhen true",
                "endloop",
            "endfunction"
        );
        
        Compiler compiler = compile(code);
        
        Assert.assertEquals(
            "2:3 - 1 is not a boolean expression",
            compiler.getAllErrors().get(0).getMessage()
        );
    
        Assert.assertEquals(
            "8:9 - 1 is not a boolean expression",
            compiler.getAllErrors().get(1).getMessage()
        );
    
        Assert.assertEquals(
            "4:7 - null is not a boolean expression",
            compiler.getAllErrors().get(2).getMessage()
        );
    }
    
    @Test
    public void mustCheckParams() {
        String code = String.join("\n",
            "function foo takes nothing returns nothing",
            "endfunction",
            "function bar takes integer a, boolean b returns nothing",
            "endfunction",
            "function baz takes nothing returns nothing",
                "call foo()",
                "call foo(1, 2)",
                "call bar()",
                "call bar(true, 1)",
                "call bar(1, false)",
            "endfunction"
        );
    
        Compiler compiler = compile(code);
    
        Assert.assertEquals(
            compiler.getAllErrors().get(0).getMessage(),
            "8:5 - Incorrect argument count. Expected 2 arguments"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(1).getMessage(),
            "9:9 - Incompatible type. Expected integer but boolean given"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(2).getMessage(),
            "9:15 - Incompatible type. Expected boolean but integer given"
        );
    
        Assert.assertEquals(
            compiler.getAllErrors().get(3).getMessage(),
            "7:5 - Incorrect argument count. Expected 0 arguments"
        );
    }
    
}