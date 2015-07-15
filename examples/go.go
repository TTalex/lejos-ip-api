package main

import(
	"net/http"
	"io/ioutil"
	"fmt"
	"strings"
)

/* 
   Sends an HTTP GET request to the given url
   Returns the HTTP response
*/
func get(url string) string{
	resp, err := http.Get(url)
	if err != nil {
		fmt.Println(err)
		return ""
	}
	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	return string(body)
}

/* 
   Sends an HTTP POST request to the given url, with the given payload
   Returns the HTTP response
*/
func post(url string, payload string) string{
	resp, err := http.Post(url, "text/plain", strings.NewReader(payload))
	if err != nil {
		fmt.Println("error:")
		fmt.Println(err)
		return ""
	}
	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	return string(body)
}

func main(){
	// Connect to the NXT Robot
	post("http://localhost:8082/connect", "00:16:53:0B:2D:DC")
	// Send the "Hello World" string to the bot
	post("http://localhost:8082/utf", "Hello World")
	// Retrieve the response from the bot
	fmt.Println(get("http://localhost:8082/utf"))
}
