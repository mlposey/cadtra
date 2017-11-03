package main

import (
	"cadtra-server/src/controller"
	"cadtra-server/src/model"
	"log"
	"os"
)

func main() {
	database, e := model.NewPsqlDB(
		os.Getenv("DATABASE_HOST"),
		os.Getenv("DATABASE_NAME"),
		os.Getenv("DATABASE_USER"),
		os.Getenv("DATABASE_PASSWORD"))
	if e != nil {
		log.Fatal("Could not access database; ", e)
	}

	service, err := controller.NewService(database, "8080")
	if err != nil {
		log.Fatal("Could not start service; ", err)
	}

	err = service.Start()
	if err != nil {
		log.Fatal("Service stopped; ", err)
	}
}
