package ruke.vrj.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import ruke.vrj.antlr.vrjLexer;
import ruke.vrj.antlr.vrjParser;
import ruke.vrj.exception.CompileException;
import ruke.vrj.lib.DefinitionPhaseResult;
import ruke.vrj.lib.TokenSymbolMap;
import ruke.vrj.phase.*;
import ruke.vrj.symbol.Symbol;

import java.util.ArrayList;

/**
 * Created by Ruke on 23/09/2016.
 */
public class Compiler {
    
    private ArrayList<CompileException> definitionErrors = new ArrayList<>();
    private ArrayList<CompileException> referenceErrors = new ArrayList<>();
    
    public ArrayList<CompileException> getAllErrors() {
        ArrayList<CompileException> errors = new ArrayList<>();
        
        errors.addAll(getDefinitionErrors());
        errors.addAll(getReferenceErrors());
        
        return errors;
    }
    
    public ArrayList<CompileException> getDefinitionErrors() {
        return definitionErrors;
    }
    
    public ArrayList<CompileException> getReferenceErrors() {
        return referenceErrors;
    }
    
    public DefinitionPhaseResult runDefinitionPhase(ANTLRInputStream input) {
        Lexer lexer = new vrjLexer(input);
        TokenStream token = new CommonTokenStream(lexer);
        vrjParser parser = new vrjParser(token);
    
        Symbol scope = new Symbol("vrj");
        TokenSymbolMap symbols = new TokenSymbolMap();
        
        DefinitionPhase definitionPhase = new DefinitionPhase(scope);
        TypePhase typePhase = new TypePhase(scope);
        
        definitionPhase.setTokenSymbolMap(symbols);
        typePhase.setTokenSymbolMap(symbols);
        
        definitionPhase.visit(parser.init());
        parser.reset();
    
        typePhase.visit(parser.init());
        parser.reset();
        
        return new DefinitionPhaseResult(parser, scope, symbols, definitionPhase.getErrors());
    }
    
    public ArrayList<CompileException> runReferencePhase(DefinitionPhaseResult data, int line, int range) {
        ReferencePhase referencePhase = new ReferencePhase(data.getScope(), line, range);
        referencePhase.setTokenSymbolMap(data.getSymbols());
    
        referencePhase.visit(data.getParser().init());
        data.getParser().reset();
        
        return new ArrayList<>(referencePhase.getErrors());
    }
    
    public ArrayList<Symbol> runSuggestPhase(DefinitionPhaseResult data, int line, int _char) {
        SuggestionPhase suggestPhase = new SuggestionPhase(data.getScope(), line, _char);
        suggestPhase.setTokenSymbolMap(data.getSymbols());
    
        suggestPhase.visit(data.getParser().init());
        data.getParser().reset();
        
        return suggestPhase.getSuggestions();
    }
    
    public String runTranslatePhase(DefinitionPhaseResult data) {
        TranslatorPhase translatePhase = new TranslatorPhase(data.getSymbols());
        String result = translatePhase.visit(data.getParser().init()).translate();
        
        data.getParser().reset();
        
        return result.trim();
    }
    
    public String compile(ANTLRInputStream input) {
        DefinitionPhaseResult data = runDefinitionPhase(input);
        
        definitionErrors = data.getErrors();
        referenceErrors = runReferencePhase(data, -1, -1);
        
        if (definitionErrors.isEmpty() && referenceErrors.isEmpty()) {
            return runTranslatePhase(data);
        }
        
        return null;
    }
    
}
