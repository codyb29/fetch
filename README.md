# fetch
A simple command line application that fetches HTML files, given a set of endpoints.

Docker run example:
```bash
// build the image
$ docker build -t fetch .
// To observe standard fetching behavior
$ docker run -it --name <container_name> fetch <url1> <url2> ... <url_n>
// To observe the archive caching
$ docker start -ai <container_name>
```
