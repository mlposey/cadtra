package controller

import (
	"cadtra/server/src/model"
	"encoding/json"
	"errors"
	"log"
	"net"
	"net/http"

	"github.com/gorilla/mux"
)

// Service controls communication between outside applications (that send HTTP
// requests) and main application logic such as authentication or database
// manipulation.
type Service struct {
	Router *mux.Router
	// routers specific to API versions
	versionRouters map[string]*mux.Router
	db             model.Database
	port           string
	auth           *model.AuthLayer
}

// NewService creates and returns a Service instance.
//
// database should be initialized and pointed at the application data store.
// port indicates where this service will listen for HTTP requests.
func NewService(database model.Database, port string) (*Service, error) {
	if database == nil {
		return nil, errors.New("Service given nil database")
	}
	if port == "" {
		return nil, errors.New("Service port can't be blank")
	}

	service := &Service{
		Router:         mux.NewRouter(),
		versionRouters: make(map[string]*mux.Router),
		db:             database,
		port:           port,
		auth:           model.NewAuthLayer(),
	}
	service.AddController(&Users{db: database, auth: service.auth})
	service.AddController(&Clubs{db: database, auth: service.auth})

	return service, nil
}

// AddController registers a controller's routes under the /api path.
func (service *Service) AddController(controllers Controller) {
	for _, route := range controllers.Routes() {
		router, exists := service.versionRouters[route.Version]
		if !exists {
			router = service.Router.PathPrefix("/api/" + route.Version).Subrouter()
			service.versionRouters[route.Version] = router
		}

		if route.IsProtected {
			router.HandleFunc(route.Path,
				service.auth.Authenticate(route.Handler)).Methods(route.Method)
		} else {
			router.HandleFunc(route.Path, route.Handler).Methods(route.Method)
		}
	}
}

// Start makes service begin listening for connections on the specified port.
func (service *Service) Start() error {
	log.Println("Starting service...")
	go func() {
		net.Dial("tcp", "localhost:"+service.port)
		log.Println("And we're live.")
	}()
	return http.ListenAndServe(":"+service.port, service.Router)
}

func WriteJSONResponse(w http.ResponseWriter, status int, respObj interface{}) {
	w.WriteHeader(status)
	w.Header().Set("Content-Type", "application/json")
	encoder := json.NewEncoder(w)
	encoder.SetIndent("", "  ")
	encoder.Encode(respObj)
	// TODO: Handle failed encoding with internal server error.
}
