/* 
 * Copyright (C) 2016 Hobrasoft s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.hobrasoft.pdfmu;

import cz.hobrasoft.pdfmu.error.ErrorType;
import cz.hobrasoft.pdfmu.operation.OperationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public class ExceptionMessagePattern {

    private final ErrorType errorType;
    private final String regex;
    private final List<String> groupNames;

    public ExceptionMessagePattern(ErrorType errorType, String regex, List<String> groupNames) {
        this.errorType = errorType;
        this.regex = regex;
        this.groupNames = groupNames;
    }

    /**
     * Tries to convert an exception to an {@link OperationException}.
     *
     * @param e the exception to parse
     * @return an instance of {@link OperationException} if the pattern matches
     * the message of e, or null otherwise.
     */
    public OperationException getOperationException(Exception e) {
        String message = e.getMessage();
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(message);
        if (m.matches()) {
            Map<String, String> arguments = PdfmuUtils.getMatcherGroups(m, groupNames);
            Map<String, Object> argumentsObjects = new LinkedHashMap<>();
            argumentsObjects.putAll(arguments);
            return new OperationException(errorType, e, argumentsObjects);
        }
        return null;
    }
}
