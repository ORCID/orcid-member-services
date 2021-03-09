package org.orcid.service.assertions.download;

import java.io.IOException;

public interface CsvWriter {
	
	public String writeCsv() throws IOException;

}
