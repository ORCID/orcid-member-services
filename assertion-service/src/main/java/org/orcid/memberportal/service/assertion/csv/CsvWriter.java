package org.orcid.memberportal.service.assertion.csv;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvWriter {

    public String writeCsv(String[] headers, List<List<String>> rows) throws IOException {
        StringBuffer buffer = new StringBuffer();
        CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT.withHeader(headers));
        for (List<String> row : rows) {
            csvPrinter.printRecord(row);
            csvPrinter.flush();
        }
        csvPrinter.close();
        return buffer.toString();
    }

}
