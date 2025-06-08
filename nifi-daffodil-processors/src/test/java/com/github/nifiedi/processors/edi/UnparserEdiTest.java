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
 * @date 2025/5/28 21:24
 */


public class UnparserEdiTest {

    private TestRunner runner = TestRunners.newTestRunner(UnparseEdi.class);

    @Test
    public void jsonToEdiTest(){
        runner.setProperty("EDI_SCHEMA_FILE","./src/test/resources/X12_4010_850.xsd");
        runner.setProperty("SEGMENT_TERMINATOR","~");
        runner.setProperty("DATA_ELEMENT_SEPARATOR","*");
        runner.setProperty("COMPOSITE_ELEMENT_SEPARATOR",":");
        runner.setProperty("EDI_STANDARD","X12");
        runner.setProperty("MEDIA_TYPE","JSON");
        runner.enqueue(this.getClass().getClassLoader().getResourceAsStream("X12_4010_850.json"));
        runner.run();
        List<MockFlowFile> ediString = runner.getFlowFilesForRelationship("success");
        MockFlowFile mockFlowFile = ediString.get(0);
        String edi = mockFlowFile.getContent();
        //auto count segment count, json value is 39, must be 41
        assert "41".equals(edi.substring(1205,1207));
        assert mockFlowFile.getAttribute("mime.type").equals("application/json");
    }

    @Test
    public void jsonToDesadv() {
        runner.setProperty("EDI_SCHEMA_FILE", "./src/test/resources/EANCOM_96A_DESADV.xsd");
        runner.setProperty("SEGMENT_TERMINATOR", "'%WSP*; %NL;%WSP*;");
        runner.setProperty("DATA_ELEMENT_SEPARATOR", "+");
        runner.setProperty("COMPOSITE_ELEMENT_SEPARATOR", ":");
        runner.setProperty("ESCAPE_CHARACTER", "?");
        runner.setProperty("EDI_STANDARD","UNEDIFACT");
        runner.setProperty("MEDIA_TYPE", "JSON");
        runner.enqueue(this.getClass().getClassLoader().getResourceAsStream("EANCOM_96A_DESADV.json"));
        runner.run();
        List<MockFlowFile> results = runner.getFlowFilesForRelationship("success");
        Relationship expectedRel = ParseEdi.REL_SUCCESS;
        runner.assertTransferCount(expectedRel, 1);
        MockFlowFile result = results.get(0);
        assert result.getAttribute("mime.type").equals("application/json");
    }
}