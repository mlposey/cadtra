package model

import (
	"encoding/json"
	"net/http"
)

// ErrorCode uniquely identifies one type of error.
type ErrorCode int

// ErrorResponse models the JSON body of HTTP failure responses.
type ErrorResponse struct {
	Code    ErrorCode `json:"code"`
	Message string    `json:"message"`
	Details string    `json:"details"`
}

// WriteErrorResponse constructs an ErrorResponse and writes it to w.
func WriteErrorResponse(w http.ResponseWriter, respCode int, errCode ErrorCode,
	message, details string) {
	w.WriteHeader(respCode)
	w.Header().Set("Content-Type", "application/json")

	err := ErrorResponse{Code: errCode, Message: message, Details: details}
	respBody, _ := json.MarshalIndent(err, "", "  ")
	w.Write(respBody)
}

// WriteUnregisteredAccountError constructs an ErrorResponse appropriate for
// requests made by unregistered users.
func WriteUnregisteredAccountError(w http.ResponseWriter) {
	WriteErrorResponse(w, http.StatusNotFound, 0,
		"Account not registered",
		"The valid token does not belong to any user account")
}
