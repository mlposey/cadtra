package model

import (
	"errors"
	"time"
)

// Clubs model groups of users that share information or participate
// in activities.
type Club struct {
	entity
	Owner       int `json:"owner"`
	MemberCount int `json:"member-count"`
}

// ClubMember models a user as members of a club would see them.
type ClubMember struct {
	Id int `json:"id"`
	// Profile should be a public profile, i.e., with sensitive fields
	// left blank so as to not be marshaled.
	Profile User      `json:"profile"`
	Role    string    `json:"role"`
	Since   time.Time `json:"since"`
}

func (db *PsqlDB) AddClub(*Club) error {
	return errors.New("Not implemented")
}
func (db *PsqlDB) GetClub(clubId int) (*Club, error) {
	return nil, errors.New("Not implemented")
}
func (db *PsqlDB) GetClubs() ([]*Club, error) {
	return nil, errors.New("Not implemented")
}
func (db *PsqlDB) DeleteClub(clubId int) error {
	return errors.New("Not implemented")
}

func (db *PsqlDB) AddClubMember(clubId, userId int, role string) error {
	return errors.New("Not implemented")
}
func (db *PsqlDB) GetClubMembers(clubId int) ([]*ClubMember, error) {
	return nil, errors.New("Not implemented")
}
func (db *PsqlDB) DeleteClubMember(clubId, userId int) error {
	return errors.New("Not implemented")
}
