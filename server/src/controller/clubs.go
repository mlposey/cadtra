package controller

import (
	"cadtra-server/src/model"
	"net/http"
)

type Clubs struct {
	db   model.Database
	auth *model.AuthLayer
}

func (c *Clubs) Routes() []Route {
	return []Route{
		{ // POST /v1/clubs
			Version:     "v1",
			Path:        "/clubs",
			Method:      "POST",
			Handler:     c.postClubs,
			IsProtected: true,
		}, { // GET /v1/clubs
			Version:     "v1",
			Path:        "/clubs",
			Method:      "GET",
			Handler:     c.getClubs,
			IsProtected: false,
		}, { // GET /v1/clubs/{id:[0-9]+}
			Version:     "v1",
			Path:        "/clubs/{id:[0-9]+}",
			Method:      "GET",
			Handler:     c.getClub,
			IsProtected: false,
		}, { // DELETE /v1/clubs/{id:[0-9]+}
			Version:     "v1",
			Path:        "/clubs/{id:[0-9]+}",
			Method:      "DELETE",
			Handler:     c.deleteClub,
			IsProtected: true,
		}, { // POST /v1/clubs/{id:[0-9]+}/members
			Version:     "v1",
			Path:        "/clubs/{id:[0-9]+}/members",
			Method:      "POST",
			Handler:     c.postClubMember,
			IsProtected: true,
		}, { // GET /v1/clubs/{id:[0-9]+}/members
			Version:     "v1",
			Path:        "/clubs/{id:[0-9]+}/members",
			Method:      "GET",
			Handler:     c.getClubMembers,
			IsProtected: true,
		}, { // DELETE /v1/clubs/{id:[0-9]+}/members/{member-id:[0-9]+}
			Version:     "v1",
			Path:        "/clubs/{id:[0-9]+}/members/{member-id:[0-9]+}",
			Method:      "DELETE",
			Handler:     c.deleteClubMember,
			IsProtected: true,
		},
	}
}

// POST /v1/clubs
func (c *Clubs) postClubs(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// GET /v1/clubs
func (c *Clubs) getClubs(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// GET /v1/clubs/{id:[0-9]+}
func (c *Clubs) getClub(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// DELETE /v1/clubs/{id:[0-9]+}
func (c *Clubs) deleteClub(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// POST /v1/clubs/{id:[0-9]+}/members
func (c *Clubs) postClubMember(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// GET /v1/clubs/{id:[0-9]+}/members
func (c *Clubs) getClubMembers(w http.ResponseWriter, r *http.Request) {
	// Todo
}

// DELETE /v1/clubs/{id:[0-9]+}/members/{member-id:[0-9]+}
func (c *Clubs) deleteClubMember(w http.ResponseWriter, r *http.Request) {
	// Todo
}
