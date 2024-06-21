package org.dicio.sentences_compiler.compiler;

import org.dicio.sentences_compiler.construct.Section;
import org.dicio.sentences_compiler.util.CompilerError;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class CompilerToCount extends CompilerBase {

    @Override
    public void compile(OutputStreamWriter output, OutputStreamWriter sectionIdsOutput)
            throws IOException, CompilerError {
        super.compile(output, sectionIdsOutput);

        final Map<String, Integer> results = new HashMap<>();
        int totalAlternativeCount = 0;
        for (final Section section : getSections()) {
            int alternativeCount = section.countAlternatives();
            totalAlternativeCount += alternativeCount;
            results.put(section.getSectionId(), alternativeCount);
        }

        results.put("total_alternative_count", totalAlternativeCount);
        System.out.println("The total number of alternatives is " + totalAlternativeCount);
        new JSONObject(results)
                .write(output).close();
    }
}
