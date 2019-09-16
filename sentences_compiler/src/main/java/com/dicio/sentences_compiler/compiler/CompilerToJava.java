package com.dicio.sentences_compiler.compiler;

import com.dicio.sentences_compiler.construct.Section;
import com.dicio.sentences_compiler.lexer.Tokenizer;
import com.dicio.sentences_compiler.parser.Parser;
import com.dicio.sentences_compiler.util.CompilerError;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

public class CompilerToJava implements CompilerBase {
    private Tokenizer tokenizer;
    private String variablePrefix;
    private String packageName;
    private String className;

    public CompilerToJava(String variablePrefix, String packageName, String className) {
        this.tokenizer = new Tokenizer();
        this.variablePrefix = variablePrefix;
        this.packageName = packageName;
        this.className = className;
    }

    @Override
    public void addInputStream(InputStreamReader input, String inputStreamName) throws IOException, CompilerError {
        tokenizer.tokenize(input, inputStreamName);
    }

    private List<Section> getSections() throws CompilerError {
        Parser parser = new Parser(tokenizer.getTokenStream());
        return parser.parse();
    }

    public void compileToVariables(OutputStreamWriter output) throws IOException, CompilerError {
        List<Section> sections = getSections();
        for (Section section : sections) {
            section.compileToJava(output, variablePrefix + section.getSectionId());
        }
        output.flush();
    }

    @Override
    public void compile(OutputStreamWriter output) throws IOException, CompilerError {
        output.write("package ");
        output.write(packageName);
        output.write(";\n" +
                "import com.dicio.component.input.standard.Sentence;\n" +
                "import com.dicio.component.input.standard.StandardRecognizer;\n" +
                "import com.dicio.component.input.InputRecognizer;\n" +
                "public class ");
        output.write(className);

        output.write(" {\n");
        compileToVariables(output);
        output.write("}\n");
        output.flush();
    }
}
