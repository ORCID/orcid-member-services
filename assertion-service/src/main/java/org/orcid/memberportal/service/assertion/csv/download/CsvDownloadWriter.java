package org.orcid.memberportal.service.assertion.csv.download;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.orcid.memberportal.service.assertion.csv.CsvWriter;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class CsvDownloadWriter extends CsvWriter {
    
    @Autowired
    protected AssertionRepository assertionsRepository;

    public abstract String writeCsv(String salesforceId) throws IOException;
    
    protected String getDateString(String year, String month, String day) {
        if (!StringUtils.isBlank(year)) {
            String endDate = year;
            if (!StringUtils.isBlank(month)) {
                endDate += '-' + month;
                if (!StringUtils.isBlank(day)) {
                    endDate += '-' + day;
                }
            }
            return endDate;
        } else {
            return StringUtils.EMPTY;
        }
    }

}
