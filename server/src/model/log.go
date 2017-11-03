package model

import (
	"time"
	"github.com/lib/pq"
)

// Log models the details of a specific run event.
type Log struct {
	Id            int       `json:"id"`
	UserId        int       `json:"user-id"`
	StartedAt     time.Time `json:"started-at"`
	EndedAt       time.Time `json:"ended-at"`
	Polyline      string    `json:"polyline"`
	Distance      float64   `json:"distance"`
	SplitInterval float64   `json:"split-interval"`
	Splits        []float64 `json:"splits"`
	Comment       string    `json:"comment"`
}

// AddRunLog creates a new run log for a user.
// log should have values for every field except Id.
func (db *PsqlDB) AddRunLog(log *Log) error {
	_, err := db.Exec(`
		INSERT INTO logs (user_id, started_at, ended_at, polyline, distance,
			split_interval, splits, comment)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
		log.UserId, log.StartedAt, log.EndedAt, log.Polyline, log.Distance,
		log.SplitInterval, pq.Float64Array(log.Splits), log.Comment,
	)
	return err
}

// GetRunLogs retrieves a list of runs completed by the user.
func (db *PsqlDB) GetRunLogs(email string) ([]*Log, error) {
	rows, err := db.Query(`
		SELECT * FROM logs WHERE user_id = (
			SELECT id FROM users WHERE email = $1
		)`, email)
	if err != nil {
		return nil, err
	}

	var logs []*Log
	for rows.Next() {
		var splits pq.Float64Array
		log := &Log{}
		err = rows.Scan(&log.Id, &log.UserId, &log.StartedAt, &log.EndedAt,
			&log.Polyline, &log.Distance, &log.SplitInterval, &splits,
			&log.Comment)

		if err != nil {
			return logs, err
		}
		log.Splits = []float64(splits)
		logs = append(logs, log)
	}
	return logs, nil
}
