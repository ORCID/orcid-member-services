package org.orcid.service.assertions.download;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

public abstract class CsvWriter {

    public abstract String writeCsv() throws IOException;

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
