package controller

import (
	"cadtra/server/src/model"
	"encoding/json"
	logger "log"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
)

// Users is a Controller implementation that handles routes for the users resource.
type Users struct {
	db   model.Database
	auth *model.AuthLayer
}

func (u *Users) Routes() []Route {
	return []Route{
		{ // POST /v1/users
			Version:     "v1",
			Path:        "/users",
			Method:      "POST",
			Handler:     u.postUsers,
			IsProtected: true,
		}, { // GET /v1/users/{id:[0-9]+}
			Version:     "v1",
			Path:        "/users/{id:[0-9]+}",
			Method:      "GET",
			Handler:     u.getUser,
			IsProtected: false,
		}, { // GET /v1/users/me
			Version:     "v1",
			Path:        "/users/me",
			Method:      "GET",
			Handler:     u.getSelf,
			IsProtected: true,
		}, { // GET /v1/users/me/preferences
			Version:     "v1",
			Path:        "/users/me/preferences",
			Method:      "GET",
			Handler:     u.getPreferences,
			IsProtected: true,
		}, { // PUT /v1/users/me/preferences
			Version:     "v1",
			Path:        "/users/me/preferences",
			Method:      "PUT",
			Handler:     u.putPreferences,
			IsProtected: true,
		}, { // POST /v1/users/me/relations
			Version:     "v1",
			Path:        "/users/me/relations",
			Method:      "POST",
			Handler:     u.postRelations,
			IsProtected: true,
		}, { // GET /v1/users/me/relations
			Version:     "v1",
			Path:        "/users/me/relations",
			Method:      "GET",
			Handler:     u.getRelations,
			IsProtected: true,
		}, { // PUT /v1/users/me/relations/{id:[0-9]+}
			Version:     "v1",
			Path:        "/users/me/relations/{id:[0-9]+}",
			Method:      "PUT",
			Handler:     u.putRelation,
			IsProtected: true,
		}, { // DELETE /v1/users/me/relations/{id:[0-9]+}
			Version:     "v1",
			Path:        "/users/me/relations/{id:[0-9]+}",
			Method:      "DELETE",
			Handler:     u.deleteRelation,
			IsProtected: true,
		}, { // POST /v1/users/me/logs
			Version:     "v1",
			Path:        "/users/me/logs",
			Method:      "POST",
			Handler:     u.postLogs,
			IsProtected: true,
		}, { // GET /v1/users/me/logs
			Version:     "v1",
			Path:        "/users/me/logs",
			Method:      "GET",
			Handler:     u.getLogs,
			IsProtected: true,
		},
	}
}

// POST /v1/users
func (u *Users) postUsers(w http.ResponseWriter, r *http.Request) {
	idToken := model.GetBearerToken(r)
	user := &model.User{
		// todo: handle missing claims.
		Email: u.auth.GetClaimWithToken(idToken, "email").(string),
	}
	user.Name = u.auth.GetClaimWithToken(idToken, "name").(string)

	err := u.db.AddUser(user)
	if err != nil {
		// todo: do something witty and return
	}

	WriteJSONResponse(w, http.StatusCreated, user)
}

// GET /v1/users/{id:[0-9]+}
func (u *Users) getUser(w http.ResponseWriter, r *http.Request) {
	userId, _ := strconv.Atoi(mux.Vars(r)["id"])
	user, err := u.db.GetUser(userId)
	if err != nil {
		model.WriteErrorResponse(w, http.StatusNotFound, 0,
			"User not found",
			"No user with the id "+mux.Vars(r)["id"]+" exists")
		return
	}

	WriteJSONResponse(w, http.StatusOK, user)
}

// GET /v1/users/me
func (u *Users) getSelf(w http.ResponseWriter, r *http.Request) {
	email := u.auth.GetEmailClaim(w, r)
	if email == "" {
		return
	}

	user, err := u.db.GetUserByEmail(email)
	if err != nil {
		model.WriteUnregisteredAccountError(w)
		return
	}

	WriteJSONResponse(w, http.StatusOK, user)
}

// GET /v1/users/me/preferences
func (u *Users) getPreferences(w http.ResponseWriter, r *http.Request) {
	email := u.auth.GetEmailClaim(w, r)
	if email == "" {
		return
	}

	prefs, err := u.db.GetUserPreferences(email)
	if err != nil {
		model.WriteUnregisteredAccountError(w)
		return
	}

	WriteJSONResponse(w, http.StatusOK, prefs)
}

// PUT /v1/users/me/preferences
func (u *Users) putPreferences(w http.ResponseWriter, r *http.Request) {
	email := u.auth.GetEmailClaim(w, r)
	if email == "" {
		return
	}

	prefs := &model.UserPreferences{}
	json.NewDecoder(r.Body).Decode(prefs)

	err := u.db.UpdateUserPreferences(email, prefs)
	if err != nil {
		model.WriteUnregisteredAccountError(w)
		return
	}
	w.WriteHeader(http.StatusOK)
}

// POST /v1/users/me/relations
func (u *Users) postRelations(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// GET /v1/users/me/relations
func (u *Users) getRelations(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// PUT /v1/users/me/relations/{id:[0-9]+}
func (u *Users) putRelation(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// DELETE /v1/users/me/relations/{id:[0-9]+}
func (u *Users) deleteRelation(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// POST /v1/users/me/logs
func (u *Users) postLogs(w http.ResponseWriter, r *http.Request) {
	email := u.auth.GetEmailClaim(w, r)
	if email == "" {
		return
	}
	user, err := u.db.GetUserByEmail(email)
	if err != nil {
		model.WriteUnregisteredAccountError(w)
		return
	}

	log := &model.Log{}
	json.NewDecoder(r.Body).Decode(log)
	log.UserId = user.Id

	err = u.db.AddRunLog(log)
	if err != nil {
		// The user is registered, the token is valid, and the email claim
		// exists; yet here we are. This is quite a pickle.
		model.WriteErrorResponse(w, http.StatusInternalServerError, 0,
			"Could not add run log",
			"Contact an administrator")
		logger.Println(err)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

// GET /v1/users/me/logs
func (u *Users) getLogs(w http.ResponseWriter, r *http.Request) {
	email := u.auth.GetEmailClaim(w, r)
	if email == "" {
		return
	}

	logs, err := u.db.GetRunLogs(email)
	if err != nil {
		logger.Println(err)
		model.WriteUnregisteredAccountError(w)
		return
	}

	WriteJSONResponse(w, http.StatusOK, logs)
}
