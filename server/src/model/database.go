package model

import (
	"database/sql"
	"errors"
	"fmt"
	_ "github.com/lib/pq"
)

// A Database facilitates interaction with models.
// TODO: The large number of functions here is a bit suspect...
//       An ORM would make this a lot cleaner and may be the only clean way
//       to avoid a monolith database interface like this.
type Database interface {
	AddUser(*User) error
	GetUser(userId int) (*User, error)
	GetUserByEmail(email string) (*User, error)

	GetUserPreferences(email string) (*UserPreferences, error)
	UpdateUserPreferences(email string, pref *UserPreferences) error

	AddUserRelation(*UserRelation) error
	UpdateUserRelation(ctx UserRelationContext, relationId int, accept bool) error
	DeleteUserRelation(ctx UserRelationContext, relationId int) error

	AddRunLog(*Log) error
	GetRunLogs(email string) ([]*Log, error)

	AddClub(*Club) error
	GetClub(clubId int) (*Club, error)
	GetClubs() ([]*Club, error)
	DeleteClub(clubId int) error

	AddClubMember(clubId, userId int, role string) error
	GetClubMembers(clubId int) ([]*ClubMember, error)
	DeleteClubMember(clubId, userId int) error
}

// PsqlDB implements the Database interface for PostgreSQL.
type PsqlDB struct {
	*sql.DB
}

// NewDB creates and returns a PsqlDB instance.
// A non-nil error is returned if there was a problem connecting to the
// database.
func NewPsqlDB(host, name, user, password string) (*PsqlDB, error) {
	if host == "" {
		host = "5432"
	}
	makeE := func(v string) error { return errors.New("Missing environment variable " + v) }
	if name == "" {
		return nil, makeE("DATABASE_NAME")
	}
	if user == "" {
		return nil, makeE("DATABASE_USER")
	}
	if password == "" {
		return nil, makeE("DATABASE_PASSWORD")
	}

	db, err := sql.Open("postgres", fmt.Sprintf(
		"dbname=%s user=%s password=%s host=%s sslmode=disable",
		name, user, password, host,
	))

	if err != nil {
		return nil, err
	}
	return &PsqlDB{db}, db.Ping()
}
