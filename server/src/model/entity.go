package model

import "time"

// entity embeds shared values into users and clubs.
type entity struct {
	Id     int       `json:"id,omitempty"`
	Name   string    `json:"name,omitempty"`
	Avatar string    `json:"avatar,omitempty"`
	Since  time.Time `json:"since,omitempty"`
}
