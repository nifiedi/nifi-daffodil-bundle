# NiFi Daffodil EDI Processor Bundle
A custom Apache NiFi processor bundle that leverages Apache Daffodil to convert Electronic Data Interchange (EDI) formats to and from JSON/XML.

## Overview
This project provides NiFi processors for parsing and unparsing EDI data using Apache Daffodil's Data Format Description Language (DFDL) implementation. It supports both X12 and EDIFACT standards, allowing seamless conversion between EDI formats and structured data formats like JSON and XML.

## Features
- EDI to JSON/XML Conversion : Parse EDI documents (X12, EDIFACT) into JSON or XML format
- JSON/XML to EDI Conversion : Convert JSON or XML data back to EDI format
- Configurable Delimiters : Customize segment terminators, data element separators, and composite element separators
- Schema-based Processing : Use DFDL schemas to define the structure of EDI documents
- Validation Support : Validate EDI documents against schemas
- Character Encoding Support : Configure the character encoding for EDI documents
## Processors
### ParseEdi
Converts EDI documents to JSON or XML format.

Properties:

- EDI_SCHEMA_FILE : Path to the EDI schema file (XSD)
- SEGMENT_TERMINATOR : Character that terminates segments (e.g., ~ )
- DATA_ELEMENT_SEPARATOR : Character that separates data elements (e.g., * )
- COMPOSITE_ELEMENT_SEPARATOR : Character that separates composite elements (e.g., : )
- ESCAPE_CHARACTER : Character used for escaping
- MEDIA_TYPE : Output format (JSON or XML)
- EDI_ENCODING : Character encoding (default: UTF-8)
- VALIDATION_MODE : Validation mode for the parser
### UnparseEdi
Converts JSON or XML data to EDI format.

Properties:

- EDI_SCHEMA_FILE : Path to the EDI schema file (XSD)
- EDI_STANDARD : EDI standard to use (X12 or UNEDIFACT)
- SEGMENT_TERMINATOR : Character that terminates segments (e.g., ~ )
- DATA_ELEMENT_SEPARATOR : Character that separates data elements (e.g., * )
- COMPOSITE_ELEMENT_SEPARATOR : Character that separates composite elements (e.g., : )
- ESCAPE_CHARACTER : Character used for escaping
- EDI_ENCODING : Character encoding (default: UTF-8)
- MEDIA_TYPE : Input format (JSON or XML)
- VALIDATION_MODE : Validation mode for the unparser
## Installation
1. Build the NAR file using Maven:
   
   ```
   mvn clean install
   ```
2. Copy the generated NAR file from nifi-daffodil-nar/target/nifi-daffodil-nar-2.2.0.nar to NiFi's lib directory.
3. Restart NiFi to load the new processors.
## Usage Examples
### Converting X12 850 Purchase Order to JSON
1. Add a ParseEdi processor to your NiFi canvas
2. Configure the processor with the following properties:
   - EDI_SCHEMA_FILE: Path to your X12 850 schema file (e.g., X12_4010_850.xsd )
   - SEGMENT_TERMINATOR: ~
   - DATA_ELEMENT_SEPARATOR: *
   - COMPOSITE_ELEMENT_SEPARATOR: :
   - MEDIA_TYPE: JSON
3. Connect the processor to your EDI data source and destination
### Converting JSON to X12 850 Purchase Order
1. Add an UnparseEdi processor to your NiFi canvas
2. Configure the processor with the following properties:
   - EDI_SCHEMA_FILE: Path to your X12 850 schema file (e.g., X12_4010_850.xsd )
   - EDI_STANDARD: X12
   - SEGMENT_TERMINATOR: ~
   - DATA_ELEMENT_SEPARATOR: *
   - COMPOSITE_ELEMENT_SEPARATOR: :
   - MEDIA_TYPE: JSON
3. Connect the processor to your JSON data source and destination
## Requirements
- Apache NiFi 2.2.0+
- Java 21
- Apache Daffodil 3.10.0
## Development
This project uses Maven for dependency management and building. The main components are:

- nifi-daffodil-processors : Contains the processor implementations
- nifi-daffodil-nar : Packages the processors into a NiFi Archive (NAR)
## License
This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Acknowledgments
- Apache NiFi - A dataflow system
- Apache Daffodil - An implementation of the Data Format Description Language (DFDL)
- X12 - A standard for EDI
- EDIFACT - United Nations rules for Electronic Data Interchange for Administration, Commerce and Transport
