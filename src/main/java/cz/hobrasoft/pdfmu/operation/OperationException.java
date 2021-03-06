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
package cz.hobrasoft.pdfmu.operation;

import cz.hobrasoft.pdfmu.PdfmuUtils;
import cz.hobrasoft.pdfmu.WritingMapper;
import cz.hobrasoft.pdfmu.error.ErrorType;
import cz.hobrasoft.pdfmu.jackson.RpcError;
import cz.hobrasoft.pdfmu.jackson.RpcError.Data;
import cz.hobrasoft.pdfmu.jackson.RpcResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * This exception is thrown by {@link Operation#execute(Namespace)} to notify
 * {@link cz.hobrasoft.pdfmu.Main#main(String[])} that the operation has
 * encountered a condition it cannot recover from.
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public class OperationException extends Exception {

    private static final Logger logger = Logger.getLogger(OperationException.class.getName());

    // Configuration
    private static final int defaultErrorCode = -1;

    private ErrorType errorType = null;
    private Map<String, Object> messageArguments = null;

    public OperationException(ErrorType errorType) {
        super(errorType.toString());
        init(errorType, null);
    }

    public OperationException(ErrorType errorType, Map.Entry<String, Object>... entries) {
        super(errorType.toString());
        init(errorType, PdfmuUtils.sortedMap(entries));
    }

    public OperationException(ErrorType errorType, Map<String, Object> messageArguments) {
        super(errorType.toString());
        init(errorType, messageArguments);
    }

    public OperationException(ErrorType errorType, Throwable cause) {
        super(errorType.toString(), cause);
        init(errorType, null);
    }

    public OperationException(ErrorType errorType, Throwable cause, Map.Entry<String, Object>... entries) {
        super(errorType.toString(), cause);
        init(errorType, PdfmuUtils.sortedMap(entries));
    }

    /**
     * Creates a chained operation exception with error identifier and message
     * arguments.
     *
     * @param errorType the error identifier.
     * @param cause the original cause.
     * @param messageArguments the arguments of the message.
     */
    public OperationException(ErrorType errorType, Throwable cause, Map<String, Object> messageArguments) {
        super(errorType.toString(), cause);
        init(errorType, messageArguments);
    }

    private void init(ErrorType errorType, Map<String, Object> messageArguments) {
        assert errorType != null;
        this.errorType = errorType;
        this.messageArguments = messageArguments;
    }

    /**
     * Returns the error code associated with this exception
     *
     * <p>
     * The code should uniquely identify the error that caused the exception.
     *
     * @return the error code associated with this exception
     */
    public int getCode() {
        if (errorType != null) {
            return errorType.getCode();
        } else {
            return defaultErrorCode;
        }
    }

    @Override
    public String getLocalizedMessage() {
        if (errorType != null) {
            String pattern = errorType.getMessagePattern();
            if (pattern != null) {
                StrSubstitutor sub = new StrSubstitutor(messageArguments);
                return sub.replace(pattern);
            } else {
                return super.getLocalizedMessage();
            }
        } else {
            return super.getLocalizedMessage();
        }
    }

    private RpcError getRpcError() {
        RpcError re = new RpcError(getCode(), getLocalizedMessage());
        Throwable cause = getCause();
        if (cause != null || messageArguments != null) {
            re.data = new Data();
            if (cause != null) {
                re.data.causeClass = cause.getClass();
                re.data.causeMessage = cause.getLocalizedMessage();
            }
            re.data.arguments = messageArguments;
        }
        return re;
    }

    public void writeInWritingMapper(WritingMapper wm) {
        RpcResponse response = new RpcResponse(getRpcError());

        try {
            wm.writeValue(response);
        } catch (IOException ex) {
            logger.severe("Could not write JSON output.");
        }
    }
}
