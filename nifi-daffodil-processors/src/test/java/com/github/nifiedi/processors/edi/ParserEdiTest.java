package com.github.nifiedi.processors.edi;

import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Chaojun.Xu
 * @date 2025/5/28 21:16
 */


public class ParserEdiTest {

    private TestRunner runner = TestRunners.newTestRunner(ParseEdi.class);

    @Test
    public void edi850ToJson() {
        runner.setProperty("EDI_SCHEMA_FILE", "./src/test/resources/X12_4010_850.xsd");
        runner.setProperty("SEGMENT_TERMINATOR", "~");
        runner.setProperty("DATA_ELEMENT_SEPARATOR", "*");
        runner.setProperty("COMPOSITE_ELEMENT_SEPARATOR", "'");
        runner.setProperty("MEDIA_TYPE", "JSON");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("filename", "850.edi");
        runner.enqueue(this.getClass().getClassLoader().getResourceAsStream("X12_4010_850.edi"), attributes);
        runner.run();
        List<MockFlowFile> results = runner.getFlowFilesForRelationship("success");
        Relationship expectedRel = ParseEdi.REL_SUCCESS;
        runner.assertTransferCount(expectedRel, 1);
        MockFlowFile result = results.get(0);
        assert result.getAttribute("mime.type").equals("application/json");
    }

    @Test
    public void edi850ToJsonError() {
        runner.setProperty("EDI_SCHEMA_FILE", "./src/test/resources/X12_4010_850.xsd");
        runner.setProperty("SEGMENT_TERMINATOR", "~");
        runner.setProperty("DATA_ELEMENT_SEPARATOR", "*");
        runner.setProperty("COMPOSITE_ELEMENT_SEPARATOR", "'");
        runner.setProperty("MEDIA_TYPE", "JSON");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("filename", "850.edi");
        runner.enqueue(this.getClass().getClassLoader().getResourceAsStream("X12_4010_850_WrongTranstCount.edi"), attributes);
        runner.run();
        List<MockFlowFile> results = runner.getFlowFilesForRelationship("success");
        Relationship expectedRel = ParseEdi.REL_FAILURE;
        runner.assertTransferCount(expectedRel, 1);
    }

}