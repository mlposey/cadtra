# Cadtra
Cadtra is a (poorly named) running companion for Android devices. It is written in Java (client) and Go (server).

The API is documented [here](http://srv.marcusposey.com:8080/). Requests can also be sent (e.g., with curl) directly
to srv.marcusposey.com:8000/api/v1

# Feature Roadmap
The table below contains notable items that have been designed and are either complete or in progress. Refer to documentation in `server/api-docs` for expected behavior of pending
features.

|          Feature        |        Server        |         Client       |
| ----------------------- | -------------------- | -------------------- |
| User Accounts           |  :heavy_check_mark:  |  :heavy_check_mark:  |
| Google Sign-In          |  :heavy_check_mark:  |  :heavy_check_mark:  |
| Run Metrics<sup>1</sup> |  :heavy_check_mark:  |  :heavy_check_mark:  |
| Route Tracking          |  :heavy_check_mark:  |  :heavy_check_mark:  |
| Session Storage         |  :heavy_check_mark:  |  limited<sup>2</sup> |
| Friends                 |                      |                      |
| Clubs                   |                      |                      |

1. Distance traveled, total duration, average pace
2. The app's session retrieval is limited to the distance, time, and pace. E.g., the route is stored and uploaded, but it cannot be visually retrieved later on.