  //Spotify API testing
  //importing modules
  const express = require("express");
  const request= require("request");
  var user_id = "andrew";
  const token = "Bearer";
  var playlist_url="https://api.spotify.com/v1/users/" + {user_id} + "/playlists"; //this is the endpoint for playlists
  
  //my server requesting data form Spotify server with url and headers parameters
  //first request gets a list of playlists
  request({url:playlist_url, headers:{"Authorization":token}}, function(err,res){
    //parsing the result of the request
        if (res){
            var playlists = JSON.parse(res.body);//calling the output playlist
            var playlist_url = playlist.items[0].href; //navigating the JSON to the first playlist, which is a JSON object
            
            //Now requesting tracks(aka songs) from first playlist
            
            request({url:playlist_url, headers:{"Authorization":token}}, function(err,res){
                if(res){
                    var playlist = JSON.parse(res.body);
                    console.log("Playlist:" + playlist.name);
                    playlist.tracks.forEach(function(track){
                            console.log(track.track.name);
                    });
                }

            })
        }
    })


  // Create an an express server
  const app = express();
  app.get("/", function(req,res){
        res.send("Display this text");
    })
 app.listen(3000, function(){
        console.log("server is up");
})
