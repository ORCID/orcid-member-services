package org.orcid.memberportal.service.assertion.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvWriterTest {
    
    private CsvWriter csvWriter;
    
    @BeforeEach
    public void setUp() {
        csvWriter = new CsvWriter();
    }

    @Test
    void testWriteCsv() throws IOException {
        String csv = csvWriter.writeCsv(getHeaders(), getRows());
        BufferedReader reader = new BufferedReader(new StringReader(csv));
        String line = reader.readLine();
        assertThat(line).isEqualTo("column1,column2,column3"); // headers
        
        line = reader.readLine();
        assertThat(line).isEqualTo("row0-column1,row0-column2,row0-column3"); // line 1
        
        line = reader.readLine();
        assertThat(line).isEqualTo("row1-column1,row1-column2,row1-column3"); // line 2
        
        line = reader.readLine();
        assertThat(line).isEqualTo("row2-column1,row2-column2,row2-column3"); // line 3
        
        line = reader.readLine();
        assertThat(line).isNull();
    }
    
    private String[] getHeaders() {
        return new String[] { "column1", "column2", "column3" };
    }
    
    private List<List<String>> getRows() {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<String> row = new ArrayList<>();
            row.add("row" + i + "-column1");
            row.add("row" + i + "-column2");
            row.add("row" + i + "-column3");
            rows.add(row);
        }
        return rows;
    }

}
