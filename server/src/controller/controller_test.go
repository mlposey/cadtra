package controller_test

import (
	"cadtra/server/src/controller"
	"cadtra/server/src/model"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
)

var gService *controller.Service
var gDB *sql.DB

// Initialize the gService and gDB variables to be used by
// tests in the controller_test package.
func init() {
	host := os.Getenv("DATABASE_HOST")
	name := os.Getenv("DATABASE_NAME")
	user := os.Getenv("DATABASE_USER")
	pwd := os.Getenv("DATABASE_PASSWORD")

	var err error
	gDB, err = sql.Open("postgres", fmt.Sprintf(
		"dbname=%s user=%s password=%s host=%s sslmode=disable",
		name, user, pwd, host,
	))

	// This won't directly be used by tests.
	database, err := model.NewPsqlDB(host, name, user, pwd)
	if err != nil {
		log.Fatal("Could not access database; ", err)
	}

	gService, err = controller.NewService(database, "8080")
	if err != nil {
		log.Fatal("Could not start service; ", err)
	}
}

func clearDatabase(t *testing.T) {
	// TODO: Clear relevant tables.
}

// sendRequest submits and then records the result of an HTTP request.
func sendRequest(request *http.Request) *httptest.ResponseRecorder {
	recorder := httptest.NewRecorder()
	gService.Router.ServeHTTP(recorder, request)
	return recorder
}

// checkError emits an error on t if e is not nil.
func checkError(t *testing.T, e error) {
	if e != nil {
		t.Error(e)
	}
}

// checkCode emits an error on t if expected does not equal actual.
func checkCode(t *testing.T, expected, actual int) {
	t.Helper()
	if expected != actual {
		t.Error("Expected response code ", expected, ", got ", actual)
	}
}
