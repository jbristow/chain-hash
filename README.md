# chain-hash

Simulates a data stream with checksums that apply to each item in the stream.


## Run

* Install Java (Tested in Java 10)
* Install [leiningen](https://leiningen.org/)

### Generate hash list
```bash
lein run gen-hashes -f <filename>
```
Generates a file `<filename>.shashes` that contains the text hashes of each block of data.

### "Stream" a hashed file
```bash
lein run fetch -f <filename> -c <first-hash>
```
Runs a simulation of streaming the file. It loads each chunk and checks the
current hash against what that chunk hashes to.  Then the program throws away
all the data that is not the next hash and moves on to the next piece.

If a chunk does not pass validation, execution stops and an error is thrown
containing the offending hash and information on which chunk failed validation.

```bash
lein run fetch -f <filename> -c <first-hash> -o <output-filename>
```
Same as above, but instead of throwing away the data, it appends the data to
the given output file.

> WARNING: This process doesn't check for file existence. Unless Jon removes
> this message, assume that using an existing file as the output target will
> ADD all of the data to the end of that file.

### Fetch a specific piece.

```bash
lein run fetch-piece -f <filename> -p <number> -c <hash>
```

Fetches piece `n` from the named file. The provided checksum should be the
checksum provided by piece `n-1` (or the provided initial hash if n == 1)

### Return the number of pieces

```bash
lein run count-pieces -f <filename>
```

Returns the number of pieces in the given file.

## Test

```bash
lein test
```

## Notes

* Startup time is very slow, thanks to a combination of the JVM and clojure. 
* Future enhancements would add better error checking and filepath
  obfuscation/checking to avoid arbitrary file loading.
* This would probably be more handy if there were an actual web service sending these files.
