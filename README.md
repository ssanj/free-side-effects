# Test-free #

A simple Free Monad example based on the KVS Example by Ken Scambler: https://git.io/vrGIs

Defines a Free Monad that can:
1. Query the existence of a file.
1. Read a file.
1. Write a file.

Has two interpreters:
1. consoleInterpreter - writes what it's doing to the console.
1. testIntepreter - reads and writes state to a map.

## Running ##

```
sbt run
```

and

```
sbt test
```

## Todo ##
1. FilesystemInterpreter - will read/write from the file system.
1. A Logging DSL that we can mix in with ~>.


