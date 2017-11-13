FROM golang:1.9 as builder

RUN apt-get update && apt-get install -y git

WORKDIR /go/src/cadtra/server/src/
COPY . .

RUN go-wrapper download \
 && CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o api .

WORKDIR /external
RUN git clone https://github.com/vishnubob/wait-for-it.git

# Final Image
FROM alpine:latest

RUN apk --no-cache add bash ca-certificates

WORKDIR /app
COPY --from=builder /go/src/cadtra/server/src/api .
COPY --from=builder /external/wait-for-it/wait-for-it.sh .

ENTRYPOINT /bin/bash wait-for-it.sh $DATABASE_HOST:5432 -t 0 -- ./api