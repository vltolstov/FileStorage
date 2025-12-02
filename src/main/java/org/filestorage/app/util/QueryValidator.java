package org.filestorage.app.util;

import org.filestorage.app.exception.QueryNotValidException;

public class QueryValidator {

    public static void validate(String query) {
        if(query == null || query.isEmpty()) {
            throw new QueryNotValidException("Query should not be null or empty");
        }

        if (!query.matches("^[a-zA-Z0-9._\\-/]+$")) {
            throw new QueryNotValidException("Query not valid");
        }
    }

}
