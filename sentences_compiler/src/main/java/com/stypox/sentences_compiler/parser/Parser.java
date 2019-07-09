package com.stypox.sentences_compiler.parser;

import com.stypox.sentences_compiler.lexer.Token;
import com.stypox.sentences_compiler.parser.construct.BaseSentenceConstruct;
import com.stypox.sentences_compiler.parser.construct.ConstructOptional;
import com.stypox.sentences_compiler.parser.construct.OrList;
import com.stypox.sentences_compiler.parser.construct.Sentence;
import com.stypox.sentences_compiler.parser.construct.SentenceConstructList;
import com.stypox.sentences_compiler.parser.construct.Word;
import com.stypox.sentences_compiler.util.CompilerError;
import com.stypox.sentences_compiler.lexer.TokenStream;
import com.stypox.sentences_compiler.lexer.Tokenizer;
import com.stypox.sentences_compiler.parser.construct.Section;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Parser {
    private TokenStream ts;

    public Parser(InputStream inputStream) throws IOException, CompilerError {
        Tokenizer tokenizer = new Tokenizer(inputStream);
        this.ts = tokenizer.tokenize();
    }

    public ArrayList<Section> parse() throws CompilerError {
        ArrayList<Section> sections = new ArrayList<>();
        while (true) {
            Section section = readSection();
            if (section == null) {
                if (!ts.isEmpty()) {
                    throw new CompilerError(CompilerError.Type.expectedSectionOrEndOfFile, ts.get(0), "");
                }
                break;
            }
            
            sections.add(section);
        }
        return sections;
    }
    
    private Section readSection() throws CompilerError {
        Section section = new Section();

        String sectionId = readSectionId();
        if (sectionId == null) {
            return null;
        }
        section.setSectionId(sectionId);

        boolean foundSentences = false;
        while (true) {
            Sentence sentence = readSentence();
            if (sentence == null) {
                break;
            }

            foundSentences = true;
            section.addSentence(sentence);
        }

        if (!foundSentences) {
            throw new CompilerError(CompilerError.Type.expectedSentence, ts.get(0), "");
        }
        return section;
    }

    private String readSectionId() throws CompilerError {
        if (ts.get(0).isType(Token.Type.lettersPlusOther)) {
            if (ts.get(1).equals(Token.Type.grammar, ":")) {
                String sectionId = ts.get(0).getValue();
                ts.movePositionForwardBy(2);
                return sectionId;
            } else {
                throw new CompilerError(CompilerError.Type.invalidToken, ts.get(1), "Expected \":\" after section id");
            }
        } else {
            return null;
        }
    }

    private Sentence readSentence() throws CompilerError {
        Sentence sentence = new Sentence();

        String sentenceId = readSentenceId();
        boolean foundId = (sentenceId != null);
        sentence.setSentenceId(foundId ? sentenceId : "");

        SentenceConstructList sentenceContent = readSentenceContent();
        if (sentenceContent == null) {
            if (foundId) {
                throw new CompilerError(CompilerError.Type.expectedSentenceContent, ts.get(0), "");
            } else {
                return null;
            }
        }

        if (ts.get(0).equals(Token.Type.grammar, ";")) {
            sentence.setSentenceConstructs(sentenceContent);
            ts.movePositionForwardBy(1);
            return sentence;
        } else {
            throw new CompilerError(CompilerError.Type.invalidToken, ts.get(0), "Expected \";\" at the end of sentence");
        }
    }

    private String readSentenceId() throws CompilerError {
        if (ts.get(0).equals(Token.Type.grammar, "[")) {
            if (ts.get(1).isType(Token.Type.lettersPlusOther)) {
                if (ts.get(2).equals(Token.Type.grammar, "]")) {
                    String sentenceId = ts.get(1).getValue();
                    ts.movePositionForwardBy(3);
                    return sentenceId;
                } else {
                    throw new CompilerError(CompilerError.Type.invalidToken, ts.get(2), "Expected \"]\" after sentence id");
                }
            } else {
                throw new CompilerError(CompilerError.Type.invalidToken, ts.get(1), "Expected sentence id after token \"[\"");
            }
        } else {
            return null;
        }
    }

    private SentenceConstructList readSentenceContent() throws CompilerError {
        if (ts.get(0).isType(Token.Type.lettersPlusOther) && ts.get(1).equals(Token.Type.grammar, ":")) {
            // found section id, skip to next section
            return null;
        }

        return readSentenceConstructList();
    }

    private SentenceConstructList readSentenceConstructList() throws CompilerError {
        SentenceConstructList sentenceConstructList = new SentenceConstructList();

        boolean foundSentenceConstruct = false;
        while (true) {
            OrList orList = readOrList();
            if (orList == null) {
                break;
            }

            foundSentenceConstruct = true;
            sentenceConstructList.addConstruct(orList.shrink());
        }

        if (foundSentenceConstruct) {
            return sentenceConstructList;
        } else {
            return null;
        }
    }

    private OrList readOrList() throws CompilerError {
        OrList orList = new OrList();

        boolean foundSentenceConstruct = false;
        while (true) {
            BaseSentenceConstruct sentenceConstruct;
            sentenceConstruct = readWord();
            if (sentenceConstruct == null) {
                sentenceConstruct = readSentenceConstructListInsideParenthesis();
            }

            if (sentenceConstruct == null) {
                if (foundSentenceConstruct) {
                    // found "|" alone at the end of the OrList
                    orList.addConstruct(new ConstructOptional());
                    break;
                } else {
                    return null;
                }
            } else {
                foundSentenceConstruct = true;
                orList.addConstruct(sentenceConstruct);
            }

            if (ts.get(0).equals(Token.Type.grammar, "|")) {
                ts.movePositionForwardBy(1);
            } else {
                break;
            }
        }

        return orList;
    }

    private Word readWord() {
        if (ts.get(0).isType(Token.Type.letters)) {
            Word word = new Word(ts.get(0).getValue());
            ts.movePositionForwardBy(1);
            return word;
        } else {
            return null;
        }
    }

    private SentenceConstructList readSentenceConstructListInsideParenthesis() throws CompilerError {
        if (ts.get(0).equals(Token.Type.grammar, "(")) {
            ts.movePositionForwardBy(1);

            SentenceConstructList sentenceConstructList = readSentenceConstructList();
            if (sentenceConstructList == null) {
                throw new CompilerError(CompilerError.Type.expectedSentenceConstructList, ts.get(0), "");
            } else {
                if (ts.get(0).equals(Token.Type.grammar, ")")) {
                    ts.movePositionForwardBy(1);
                    return sentenceConstructList;
                } else {
                    throw new CompilerError(CompilerError.Type.invalidToken, ts.get(0), "Expected \")\" after list of sentence constructs");
                }
            }
        } else {
            return null;
        }
    }
}
