package com.stypox.sentences_compiler.parser.construct;

import java.util.ArrayList;

public class CapturingGroup implements BaseSentenceConstruct {
    @Override
    public ArrayList<ArrayList<String>> unfold() {
        return new ArrayList<ArrayList<String>>() {{ add(new ArrayList<String>() {{ add("."); }}); }};
    }
}