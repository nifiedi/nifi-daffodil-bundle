package com.github.nifiedi.processors.edi;

import com.github.nifiedi.processors.edi.dfdl.DfdlDataProcessor;
import com.github.nifiedi.processors.edi.dfdl.MediaType;
import com.github.nifiedi.processors.edi.utils.EdiStandard;
import org.apache.daffodil.japi.ValidationMode;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Chaojun.Xu
 * @date 2025/05/28 20:36
 */


@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"EDI", "XML","JSON","X12","EDIFact", "DFDL"})
@CapabilityDescription("Read xml or json and transform to edi.")
@WritesAttribute(attribute = "mime.type", description = "Set mime.type to application/edi-x12 or application/edifact")
@SeeAlso({ParseEdi.class})
public class UnparseEdi extends AbstractProcessor {
    private DfdlDataProcessor unParser;
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private String segmentTerminator;
    private String dataElementSep;
    private String compElementSep;
    private String escapeCharacter;
    private EdiStandard ediStandard;
    private  String mediaType;



    public static final PropertyDescriptor EDI_STANDARD = new PropertyDescriptor
            .Builder().name("EDI_STANDARD")
            .displayName("Edi Standard")
            .description("EDI statnds, X12 or UNEDIFACT")
            .required(false)
            .allowableValues("X12", "UNEDIFACT")
            .defaultValue("X12")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor EDI_SCHEMA_FILE = new PropertyDescriptor
            .Builder().name("EDI_SCHEMA_FILE")
            .displayName("Edi Schema File")
            .description("edi schema file location")
            .required(true)
            .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
            .build();

    public static final PropertyDescriptor SEGMENT_TERMINATOR = new PropertyDescriptor
            .Builder().name("SEGMENT_TERMINATOR")
            .displayName("Segment Terminator")
            .description("edi segment terminator")
            .required(true)
            .defaultValue("'")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor DATA_ELEMENT_SEPARATOR = new PropertyDescriptor
            .Builder().name("DATA_ELEMENT_SEPARATOR")
            .displayName("Data Element Separator")
            .description("edi data element separator")
            .defaultValue("+")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor COMPOSITE_ELEMENT_SEPARATOR = new PropertyDescriptor
            .Builder().name("COMPOSITE_ELEMENT_SEPARATOR")
            .displayName("Composite Element Separator")
            .description("edi composite element separator")
            .required(true)
            .defaultValue(":")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor ESCAPE_CHARACTER = new PropertyDescriptor
            .Builder().name("ESCAPE_CHARACTER")
            .displayName("Escape Character")
            .description("edi escape character")
            .dependsOn(EDI_STANDARD,"UNEDIFACT")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor EDI_ENCODING = new PropertyDescriptor
            .Builder().name("EDI_ENCODING")
            .displayName("EDI Encoding")
            .description("edi encoding")
            .required(false)
            .build();

    public static final PropertyDescriptor MEDIA_TYPE = new PropertyDescriptor
            .Builder().name("MEDIA_TYPE")
            .displayName("Input Media Type")
            .description("Input media type")
            .allowableValues(MediaType.XML.name(), MediaType.JSON.name())
            .defaultValue(MediaType.JSON.name())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor VALIDATION_MODE = new PropertyDescriptor
            .Builder().name("VALIDATION_MODE")
            .displayName("Validation Mode")
            .description("validation mode. Off,Limited,Full")
            .required(true)
            .allowableValues(ValidationMode.Off.name(),ValidationMode.Limited.name(), ValidationMode.Full.name())
            .defaultValue(ValidationMode.Off.name())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("success relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failure relationship")
            .build();

    @Override
    protected void init(final ProcessorInitializationContext context) {

        descriptors = new ArrayList<>();
        descriptors.add(EDI_SCHEMA_FILE);
        descriptors.add(EDI_STANDARD);
        descriptors.add(SEGMENT_TERMINATOR);
        descriptors.add(DATA_ELEMENT_SEPARATOR);
        descriptors.add(COMPOSITE_ELEMENT_SEPARATOR);
        descriptors.add(ESCAPE_CHARACTER);
        descriptors.add(EDI_ENCODING);
        descriptors.add(MEDIA_TYPE);
        descriptors.add(VALIDATION_MODE);

        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws Throwable {
        String ediSchemaFilePath = context.getProperty(EDI_SCHEMA_FILE).getValue();
        segmentTerminator = context.getProperty(SEGMENT_TERMINATOR).getValue();
        dataElementSep = context.getProperty(DATA_ELEMENT_SEPARATOR).getValue();
        compElementSep = context.getProperty(COMPOSITE_ELEMENT_SEPARATOR).getValue();
        escapeCharacter = context.getProperty(ESCAPE_CHARACTER).getValue();
        ediStandard = EdiStandard.valueOf(context.getProperty(EDI_STANDARD).getValue());
        mediaType = context.getProperty(MEDIA_TYPE).getValue();
        URI schemaUri = new File(ediSchemaFilePath).toURI();
        String encoding =  context.getProperty(EDI_ENCODING).getValue();
        if(null == encoding || encoding.isEmpty()){
            encoding = "UTF-8";
        }
        String validationMode = context.getProperty(VALIDATION_MODE).getValue();
        String finalEncoding = encoding;
        HashMap<String, String> variables =new HashMap<String, String>() {{
            this.put("dfdl:encoding", finalEncoding);
            this.put("{http://www.ibm.com/dfdl/EDI/Format}SegmentTerm", segmentTerminator);
            this.put("{http://www.ibm.com/dfdl/EDI/Format}FieldSep", dataElementSep);
            this.put("{http://www.ibm.com/dfdl/EDI/Format}CompositeSep", compElementSep);
        }};
        if(ediStandard.equals(EdiStandard.UNEDIFACT)){
            variables.put("{http://www.ibm.com/dfdl/EDI/Format}EscapeChar", escapeCharacter);
        }
        unParser = new DfdlDataProcessor(schemaUri, variables, ValidationMode.valueOf(validationMode),true);
    }


    @Override
    public void onTrigger(ProcessContext processContext, ProcessSession session) {
        final FlowFile original = session.get();
        if (original == null) {
            return;
        }
        final StopWatch stopWatch = new StopWatch(true);

        try {
            final FlowFile transformed = session.write(original, (inputStream, outputStream) -> {
                try (final InputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
                    unParser.unParse(bufferedInputStream, MediaType.valueOf(mediaType), outputStream);
                } catch (final Throwable e) {
                    getLogger().error("Transformation Failed", original, e);
                    throw new ProcessException(e);
                }
            });
            session.putAttribute(transformed,"mime.type", mediaType.equals(MediaType.JSON.name())?"application/json":"application/xml");
            session.transfer(transformed, REL_SUCCESS);
            session.getProvenanceReporter().modifyContent(transformed, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            getLogger().info("Transformation Completed {}", original);
        } catch (final Exception e) {
            getLogger().error(original + " Transformation Failed", e);
            session.transfer(original, REL_FAILURE);
        }


    }
}