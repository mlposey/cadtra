package model

import "errors"

// User models a user profile. Tags which can be empty will
// not be displayed when the profile is public.
type User struct {
	entity
	Email string `json:"email,omitempty"`
	// TODO: Find a library for country lists.
	Country     string          `json:"country"`
	Preferences UserPreferences `json:"-"`
}

// AddUser adds a new user to the database.
// user should contain values for Email and Name.
// If insertion succeeds, user will be given values for Country, Id, and Since.
func (db *PsqlDB) AddUser(user *User) error {
	// Todo: Find a good default for this.
	user.Country = "USA"

	return db.QueryRow(`SELECT create_user($1, $2, $3)`,
		user.Email, user.Name, user.Country,
	).Scan(&user.Id, &user.Since, &user.Avatar)
}

// GetUser retrieves a subset of a user's details.
// The returned User will contain values for Name, Avatar, Country, and Since.
func (db *PsqlDB) GetUser(userId int) (*User, error) {
	user := &User{}
	err := db.QueryRow(`
		SELECT name, avatar, country, since FROM users
		WHERE id = $1`,
		userId,
	).Scan(&user.Name, &user.Avatar, &user.Country, &user.Since)
	return user, err
}

// GetUserByEmail uses an email address to retrieve user details.
// The returned user will contain values for Id, Email, Name, Avatar,
// Country, and Since.
func (db *PsqlDB) GetUserByEmail(email string) (*User, error) {
	user := &User{Email: email}
	err := db.QueryRow(`
		SELECT id, name, avatar, country, since FROM users
		WHERE email = $1`,
		email,
	).Scan(&user.Id, &user.Name, &user.Avatar, &user.Country, &user.Since)
	return user, err
}

// UserPreferences defines preferences for a specific user.
type UserPreferences struct {
	UsesMetric bool `json:"uses-metric"`
}

// GetUserPreferences retrieves a map of the user's preferences.
func (db *PsqlDB) GetUserPreferences(email string) (*UserPreferences, error) {
	prefs := &UserPreferences{}
	err := db.QueryRow(`
		SELECT uses_metric FROM preferences WHERE user_id = (
			SELECT id FROM users WHERE email = $1
		)`,
		email,
	).Scan(&prefs.UsesMetric)
	return prefs, err
}

// UpdateUserPreferences sets the user's preferences to each value in pref.
func (db *PsqlDB) UpdateUserPreferences(email string, pref *UserPreferences) error {
	_, err := db.Exec(`
		UPDATE preferences
		SET uses_metric = $1
		WHERE user_id = (
			SELECT id FROM users WHERE email = $2
		)`,
		pref.UsesMetric, email,
	)
	return err
}

// UserRelationContext describes what type of relation is being modeled.
type UserRelationContext string

const (
	FriendOf UserRelationContext = "friend"
	MemberOf UserRelationContext = "club"
	Blocked  UserRelationContext = "blocked"
)

// getPsqlTableName returns the name of the table that stores the type of
// relation or an empty string if the context is invalid.
func (ctx UserRelationContext) getPsqlTableName() string {
	switch ctx {
	case FriendOf, Blocked:
		return "user_relations"
	case MemberOf:
		return "club_relations"
	}
	return ""
}

// UserRelation models a user's relationship with another entity.
type UserRelation struct {
	Id          int                 `json:"id"`
	SenderId    int                 `json:"sender-id"`
	ReceiverId  int                 `json:"receiver-id"`
	Context     UserRelationContext `json:"context"`
	HasAccepted bool                `json:"has-accepted"`
}

// AddUserRelation creates a relation between a sender and receiver. The
// relation object requires defaults for all values except Id.
func (db *PsqlDB) AddUserRelation(relation *UserRelation) error {
	if relation.Context == MemberOf {
		_, err := db.Exec(`
			INSERT INTO $1 (sender_id, receiver_id) VALUES ($2, $3)`,
			relation.Context.getPsqlTableName(), relation.SenderId,
			relation.ReceiverId,
		)
		return err
	} else if relation.Context == FriendOf || relation.Context == Blocked {
		_, err := db.Exec(`
			INSERT INTO $1 (context, sender_id, receiver_id) VALUES ($2, $3, $4)`,
			relation.Context.getPsqlTableName(), relation.Context,
			relation.SenderId, relation.ReceiverId,
		)
		return err
	} else {
		return errors.New("Missing relation context")
	}
}

// UpdateUserRelation accepts a friend relation if accept is true or deletes it
// if accept is false.
func (db *PsqlDB) UpdateUserRelation(ctx UserRelationContext, relationId int,
	accept bool) error {
	if ctx == Blocked {
		// Todo: Unblock users.
		return errors.New("Not implemented")
	}

	if !accept {
		return db.DeleteUserRelation(ctx, relationId)
	}
	_, err := db.Exec(`
		UPDATE $1 SET has_accepted = true WHERE id = $2`,
		ctx.getPsqlTableName(), relationId,
	)
	return err
}

// DeleteUserRelation deletes a relation between a user and another user or club.
func (db *PsqlDB) DeleteUserRelation(ctx UserRelationContext, relationId int) error {
	_, err := db.Exec(`DELETE FROM $1 WHERE id = $2`,
		ctx.getPsqlTableName(), relationId,
	)
	return err
}
