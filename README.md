# chain-hash

Simulates a data stream with checksums that apply to each item in the stream.

If you would like to know more, check out the [problem statement](doc/chain_hash.md).


## Run

* Install Java (Tested in Java 10)
* Install [leiningen](https://leiningen.org/)

### Encode

```bash
lein run encode -f <input-file> -o <encoded-file>
```
This creates a file filled with checksums at the location specified by `--output`

The hash of the first block will be printed on stdout.

### Decode
```bash
lein run decode -f <encoded-file> -o <decoded-file> -c <checksum>
```
Takes a checksum "encoded" file and cleans it piece by piece.  If anything has
been modified, the chunk that contains the beginning of the modification will
be marked invalid and the program will throw an error denoting the offending
piece.

## Alternate vision
This is the original work I did before I realized I massively over-thought the assignment.

I was distracted by the "streaming" words in the problem description, and so I
made an approximation of a client/server model. In this model, there's no need
to mix-in the binary data and create a duplicate large file. The hashes are
saved off to the side in a separate file, and the server just concats the data
together when it is called upon to send the next chunk.

This enables a bit of rewind, as the client can ask for an arbitrary piece as
long as it has saved the hashes it saw along the way.

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
