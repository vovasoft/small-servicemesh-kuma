package main

import (
	"fmt"
	"log"
	"net/http"
)

func helloHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Hello from Golang Service!")
}

func main() {
	http.HandleFunc("/hello", helloHandler)

	log.Println("Starting Golang service on port 9091...")
	err := http.ListenAndServe(":9091", nil)
	if err != nil {
		log.Fatalf("Failed to start Golang service: %v", err)
	}
}
