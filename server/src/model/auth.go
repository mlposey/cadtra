package model

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"
)

// AuthLayer handles resource authorization using Google OAuth 2.0 Id Tokens.
type AuthLayer struct {
	clientId string
	endpoint string

	tokenCache map[string]map[string]interface{}
	cacheMutex sync.RWMutex
}

// Creates and returns a new AuthLayer instance.
// AuthLayer requires a Google client id to be in the CLIENT_ID environment
// variable.
func NewAuthLayer() *AuthLayer {
	clientId := os.Getenv("CLIENT_ID")
	if clientId == "" {
		log.Fatal("Environment variable CLIENT_ID should not be empty")
	}

	return &AuthLayer{
		clientId: clientId,
		endpoint: "https://www.googleapis.com/oauth2/v3/tokeninfo",
		// Todo: Verify that this is the appropriate creation syntax.
		tokenCache: make(map[string]map[string]interface{}),
	}
}

// Authenticate ensures that only requests with a valid Google id token in
// the Authorization header can access protected endpoints.
func (auth *AuthLayer) Authenticate(handler http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		idToken := GetBearerToken(r)

		// Build a token verification request to send to Google.
		authReq := auth.buildAuthRequest(w, idToken)
		if authReq == nil {
			return
		}

		// Send the request, verifying the token.
		client := http.Client{Timeout: 10 * time.Second}
		resp, e := client.Do(authReq)
		if e != nil {
			WriteErrorResponse(w, http.StatusInternalServerError, 0,
				"Could not validate token",
				"Administrator should check logs")
			log.Print("Missing ca-certificates required to make HTTPS call to Google")
		}

		if resp.StatusCode != http.StatusOK {
			WriteErrorResponse(w, http.StatusBadRequest, 0,
				"Invalid id token",
				"The integrity of the supplied id token cannot be verified")
			return
		}

		// Extract token claims from the response.
		claims := make(map[string]interface{})
		json.NewDecoder(resp.Body).Decode(&claims)
		if claims["aud"].(string) != auth.clientId {
			WriteErrorResponse(w, http.StatusBadRequest, 0,
				"Invalid id token",
				"The token is not meant for this application")
			return
		}

		auth.addToken(idToken, claims)
		// At this point, we're positive the token is valid and meant for us,
		// so we can let them attempt to access the resource they wanted.
		handler(w, r)
		// Google could decide the token is not valid after this transaction,
		// so we won't reuse it when we're done.
		auth.removeToken(idToken)
	}
}

// buildAuthRequest creates an HTTP request to send to Google's token
// authorization endpoint. If the procedure fails, w will contain all necessary
// error response information, and the returned request will be nil.
func (auth *AuthLayer) buildAuthRequest(w http.ResponseWriter,
	idToken string) *http.Request {
	req, err := http.NewRequest("GET", auth.endpoint, nil)
	if err != nil {
		WriteErrorResponse(w, http.StatusInternalServerError, 0,
			"Failed to verify token id",
			"A connection could not be established with Google authorization servers")
		return nil
	}

	if isParamAdded := addTokenToRequest(w, idToken, req); !isParamAdded {
		return nil
	}
	return req
}

// GetTokenClaim retrieves a claim from a request's bearer token or nil
// if the token/claim was not present.
//
// If you plan on calling this function multiple times, it will be more
// efficient to retrieve the token with GetBearerToken and then pass that
// value to GetClaimWithToken.
func (auth *AuthLayer) GetClaim(r *http.Request, key string) interface{} {
	return auth.getClaim(GetBearerToken(r), key)
}

// GetClaimWithToken retrieves a claim from a request's bearer token or
// nil if the token/claim was not present.
func (auth *AuthLayer) GetClaimWithToken(token, key string) interface{} {
	return auth.getClaim(token, key)
}

// GetEmailClaim attempts to retrieve an 'email' claim from a Google id token.
// Should the process fail, an error response is constructed and sent to w and
// this function wil return an empty string.
func (auth *AuthLayer) GetEmailClaim(w http.ResponseWriter, r *http.Request) string {
	emailClaim := auth.GetClaim(r, "email")
	if emailClaim == nil {
		WriteErrorResponse(w, http.StatusBadRequest, 0,
			"Token lacks email claim",
			"ID tokens must have an 'email' claim in them")
		return ""
	}
	return emailClaim.(string)
}

func (auth *AuthLayer) getClaim(token, key string) interface{} {
	auth.cacheMutex.RLock()
	defer auth.cacheMutex.RUnlock()

	claims, exists := auth.tokenCache[token]
	if !exists {
		return nil
	}
	claim, exists := claims[key]
	if !exists {
		return nil
	}
	return claim
}

// addToken stores the claims for an id token.
func (auth *AuthLayer) addToken(idToken string, claims map[string]interface{}) {
	auth.cacheMutex.Lock()
	auth.tokenCache[idToken] = claims
	auth.cacheMutex.Unlock()
}

// removeToken deletes an id token and its claims.
func (auth *AuthLayer) removeToken(idToken string) {
	auth.cacheMutex.Lock()
	delete(auth.tokenCache, idToken)
	auth.cacheMutex.Unlock()
}

// GetBearerToken retrieves a token from a Bearer Authorization header.
// If the token is missing or the context is not Bearer, an empty string
// is returned.
func GetBearerToken(r *http.Request) string {
	authHeader := r.Header.Get("Authorization")
	parts := strings.Split(authHeader, " ")
	if len(parts) != 2 || parts[0] != "Bearer" {
		return ""
	}
	return parts[1]
}

// addTokenToRequest adds a Google id token to the parameters of the authorization
// request. If the process fails (e.g., idToken is an empty string) w will
// contain the appropriate error response and the method will return false.
func addTokenToRequest(w http.ResponseWriter, idToken string,
	googleReq *http.Request) bool {
	if idToken == "" {
		WriteErrorResponse(w, http.StatusUnauthorized, 0,
			"Missing authorization token",
			"This resource requires a Google IdToken as a Bearer Authorization header")
		return false
	}

	q := googleReq.URL.Query()
	q.Add("id_token", idToken)
	googleReq.URL.RawQuery = q.Encode()
	return true
}
