# chain-hash

Simulates a data stream with checksums that apply to each item in the stream.

If you would like to know more, check out the [problem statement](docs/chain_hash.md).


## Run

* Install Java (Tested in Java 10)
* Install [leiningen](https://leiningen.org/)

### Server operations
#### Generate hash list
```bash
lein run gen-hashes -f <filename>
```
Generates a file `<filename>.shashes` that contains the text hashes of each block of data.

#### Return the number of pieces

```bash
lein run count-pieces -f <filename>
```

Returns the number of pieces in the given file.

#### Return piece `n`

Implemented as a call from `chain_hash.client` to `chain_hash.server/fetch-piece` The intention here is to show the seam where a call to an intermediary api or web service would go

### Client operations
#### Stream a hashed file (Simulation)
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

#### Fetch a specific piece.

```bash
lein run fetch-piece -f <filename> -p <number> -c <hash>
```

Fetches piece `n` from the named file. The provided checksum should be the
checksum provided by piece `n-1` (or the provided initial hash if n == 1)

## Test

```bash
lein test
```

## Notes

* Startup time can be very slow, thanks to a combination of the JVM and leiningen spin-up. 
    * Most of this can be gotten-rid-of using `java -jar target/uberjar/<project>-<version>-STANDALONE.jar` after a `lein uberjar`
    * GrallVM is also promising, but their static linker doesn't work on DARWIN yet. :(
* Future enhancements would add better error checking and filepath
  obfuscation/checking to avoid arbitrary file loading.
* This would probably be more handy if there were an actual web service sending these files.
