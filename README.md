snake_backend
=============

work in progress, backend for a browser Snake game, my playground to learn Scala/Akka/Play

### Messages
#### Client --> Server
```json
{
	"cmd": "enter"
	"data":
		{
			"name":"Ahmed"
			"email":"ahmed@gmail.com"
		}
}

{
	"cmd": "update"
	"data":
		{
			"score": 15
		}
}
```


#### Server --> Client
```json
{
	"cmd": "error"
	"data":
		{
			"message": "Not entered"
		}
}


}

{
	"cmd": "update"
	"data":
		{
			"name": "Ahmed",
			"event": "quit"
		}
}

{
	"cmd": "update"
	"data":
		{
			"name": "Ahmed",
			"event": "set",
			"value": 15
		}
}
```